import os
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv

load_dotenv()
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

from grok_service import generate_suggested_questions, answer_story_question
from ai_service import translate_vernacular_articles
from cache_manager import cache_manager

app = FastAPI(
    title="Global Insight Story Intelligence API",
    description="FastAPI Backend powered by Grok AI for story Q&A, caching, and grounded intelligence.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class SuggestQuestionsRequest(BaseModel):
    story_id: str
    title: str
    summary: Optional[str] = ""
    source: Optional[str] = "Global News"

class AskStoryRequest(BaseModel):
    story_id: str
    title: str
    summary: Optional[str] = ""
    source_name: Optional[str] = "Global News"
    source_url: Optional[str] = ""
    user_query: str
    chat_history: Optional[List[Dict[str, str]]] = []

@app.get("/")
def health_check():
    return {
        "status": "online",
        "service": "Global Insight Intelligence API (Grok)",
        "version": "1.0.0"
    }

@app.post("/api/suggest-questions")
async def suggest_questions(req: SuggestQuestionsRequest):
    questions = await generate_suggested_questions(req.title, req.summary, req.source)
    return {"story_id": req.story_id, "questions": questions}

@app.post("/api/ask-story")
async def ask_story(req: AskStoryRequest):
    # Check Cache First (story_id + user_query)
    cached_response = cache_manager.get(req.story_id, req.user_query)
    if cached_response:
        cached_response["cached"] = True
        return cached_response

    # Generate response via Grok API
    res = await answer_story_question(
        story_title=req.title,
        story_summary=req.summary,
        source_name=req.source_name,
        source_url=req.source_url,
        user_query=req.user_query,
        chat_history=req.chat_history
    )

    res["cached"] = False
    # Store in Cache
    cache_manager.set(req.story_id, req.user_query, res)
    return res


DEFAULT_LOCAL_IMAGES = [
    "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1495020689067-958852a7765e?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1476242906366-d8eb64c2f661?w=800&auto=format&fit=crop&q=80"
]

def _fetch_google_news_rss(search_query: str, sublocality: str = "", locality: str = "", max_items: int = 40, lang: str = "en"):
    import urllib.parse
    import urllib.request
    import xml.etree.ElementTree as ET
    import re
    import html

    try:
        encoded_query = urllib.parse.quote(search_query)
        if lang == "hi":
            rss_url = f"https://news.google.com/rss/search?q={encoded_query}&hl=hi&gl=IN&ceid=IN:hi"
        else:
            rss_url = f"https://news.google.com/rss/search?q={encoded_query}&hl=en-IN&gl=IN&ceid=IN:en"

        req = urllib.request.Request(
            rss_url,
            headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
        )
        with urllib.request.urlopen(req, timeout=12) as response:
            xml_data = response.read()

        root = ET.fromstring(xml_data)
        items = root.findall("./channel/item")

        articles = []
        for idx, item in enumerate(items[:max_items]):
            raw_title = item.find("title").text if item.find("title") is not None else ""
            link = item.find("link").text if item.find("link") is not None else ""
            pub_date = item.find("pubDate").text if item.find("pubDate") is not None else ""
            raw_desc = item.find("description").text if item.find("description") is not None else ""
            
            source_elem = item.find("source")
            source_name = source_elem.text if source_elem is not None else ("Hindi Press" if lang == "hi" else "Local News")

            clean_title = raw_title
            if " - " in raw_title:
                clean_title = raw_title.rsplit(" - ", 1)[0].strip()

            clean_desc = re.sub(r"<[^<]+?>", "", html.unescape(raw_desc)).strip()
            if not clean_desc or clean_desc.lower() == clean_title.lower():
                clean_desc = f"Latest local report from {sublocality or locality or 'your area'}: {clean_title}"

            img_url = DEFAULT_LOCAL_IMAGES[idx % len(DEFAULT_LOCAL_IMAGES)]

            articles.append({
                "id": str(idx + 1),
                "source": {
                    "id": None,
                    "name": source_name
                },
                "author": source_name,
                "title": clean_title,
                "description": clean_desc,
                "url": link,
                "urlToImage": img_url,
                "publishedAt": pub_date
            })
        return articles
    except Exception as e:
        print(f"Error fetching Google News RSS for '{search_query}' (lang={lang}): {e}")
        return []


@app.get("/api/radius-news")
async def get_radius_news(
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    village: Optional[str] = "",
    tehsil: Optional[str] = "",
    district: Optional[str] = "",
    state: Optional[str] = "",
    sublocality: Optional[str] = "",
    locality: Optional[str] = "",
    postal_code: Optional[str] = "",
    query: Optional[str] = "",
    radius: Optional[int] = 10,
    target_lang: Optional[str] = "en"
):
    """
    Fetches real-time hyper-local news with:
    1. Hierarchical Administrative Geo-Stepping (Village -> Tehsil -> District -> State)
    2. Vernacular Press Aggregation (Dainik Jagran, Amar Ujala, Dainik Bhaskar)
    3. Groq AI High-Speed Translation into crisp English briefings
    """
    try:
        village_name = (village or sublocality or "").strip()
        tehsil_name = (tehsil or "").strip()
        district_name = (district or locality or "").strip()
        state_name = (state or "").strip()

        articles = []
        existing_urls = set()
        existing_titles = set()

        def _merge_articles(new_items):
            for item in new_items:
                url = item.get("url")
                title = item.get("title")
                if url not in existing_urls and title not in existing_titles:
                    item["id"] = str(len(articles) + 1)
                    articles.append(item)
                    existing_urls.add(url)
                    existing_titles.add(title)

        # -------------------------------------------------------------
        # Level 1: Village / Colony / Sublocality (Immediate 10 km)
        # -------------------------------------------------------------
        geo_level = "village"
        geo_label = village_name or district_name or "Local Area"
        effective_radius = radius or 10
        is_expanded = False
        expansion_reason = None

        query_parts = []
        if village_name:
            query_parts.append(village_name)
        if district_name and district_name.lower() != village_name.lower():
            query_parts.append(district_name)
        if query and query.strip():
            query_parts.append(query.strip())
        if not query_parts and postal_code and postal_code.strip():
            query_parts.append(postal_code.strip())

        query_level1 = " ".join(query_parts) if query_parts else (district_name or "India") + " local news"
        # Fetch both English and Vernacular (Hindi) local feeds
        _merge_articles(_fetch_google_news_rss(query_level1, village_name, district_name, lang="en"))
        _merge_articles(_fetch_google_news_rss(query_level1, village_name, district_name, lang="hi"))

        # -------------------------------------------------------------
        # Level 2: Tehsil / Block / Sub-District Auto-Stepping (< 5 stories)
        # -------------------------------------------------------------
        if len(articles) < 5 and tehsil_name and tehsil_name.lower() != village_name.lower():
            geo_level = "tehsil"
            geo_label = tehsil_name
            is_expanded = True
            expansion_reason = f"Aggregated from your governing Tehsil/Block ({tehsil_name})"

            tehsil_parts = [tehsil_name]
            if district_name and district_name.lower() != tehsil_name.lower():
                tehsil_parts.append(district_name)
            query_level2 = " ".join(tehsil_parts) + " news"

            _merge_articles(_fetch_google_news_rss(query_level2, tehsil_name, district_name, lang="en"))
            _merge_articles(_fetch_google_news_rss(query_level2, tehsil_name, district_name, lang="hi"))

        # -------------------------------------------------------------
        # Level 3: District Rural & Urban Belt Auto-Stepping (< 5 stories)
        # -------------------------------------------------------------
        if len(articles) < 5 and district_name:
            geo_level = "district"
            geo_label = district_name
            is_expanded = True
            effective_radius = 20
            expansion_reason = f"Expanded to District {district_name} (20 km radius) to bring you surrounding local updates"

            query_level3 = f"{district_name} news"
            _merge_articles(_fetch_google_news_rss(query_level3, village_name or tehsil_name, district_name, lang="en"))
            _merge_articles(_fetch_google_news_rss(query_level3, village_name or tehsil_name, district_name, lang="hi"))

        # -------------------------------------------------------------
        # Level 4: State Regional Safety Net (< 5 stories)
        # -------------------------------------------------------------
        if len(articles) < 5 and state_name:
            geo_level = "state"
            geo_label = state_name
            is_expanded = True
            effective_radius = 25
            expansion_reason = f"Expanded to {state_name} regional news"

            query_level4 = f"{state_name} local news"
            _merge_articles(_fetch_google_news_rss(query_level4, "", state_name, lang="en"))
            _merge_articles(_fetch_google_news_rss(query_level4, "", state_name, lang="hi"))

        # Fallback if still 0
        if not articles:
            _merge_articles(_fetch_google_news_rss("India local news", "", "", lang="en"))
            _merge_articles(_fetch_google_news_rss("India local news", "", "", lang="hi"))

        # Translate vernacular stories via Groq AI
        if articles:
            articles = await translate_vernacular_articles(articles, target_lang=target_lang or "en")
            for idx, item in enumerate(articles):
                item["id"] = str(idx + 1)

        display_area = village_name
        if geo_level == "tehsil":
            display_area = f"{village_name} (Tehsil {tehsil_name})" if village_name else f"Tehsil {tehsil_name}"
        elif geo_level == "district":
            display_area = f"{district_name} District"
        elif geo_level == "state":
            display_area = f"{state_name} Region"

        return {
            "status": "ok",
            "totalResults": len(articles),
            "radiusKm": effective_radius,
            "isExpanded": is_expanded,
            "geoLevel": geo_level,
            "geoLabel": geo_label,
            "expansionReason": expansion_reason,
            "area": display_area or "Nearby",
            "articles": articles
        }

    except Exception as e:
        print(f"Radius news error: {e}")
        return {
            "status": "error",
            "message": str(e),
            "totalResults": 0,
            "radiusKm": radius or 10,
            "isExpanded": False,
            "geoLevel": "unknown",
            "geoLabel": "",
            "expansionReason": None,
            "articles": []
        }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
