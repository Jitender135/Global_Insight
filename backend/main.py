import os
import re
import json
import time
import uuid
import base64
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from dotenv import load_dotenv

load_dotenv()
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

from grok_service import generate_suggested_questions, answer_story_question
from ai_service import translate_vernacular_articles, moderate_community_notice
from cache_manager import cache_manager

app = FastAPI(
    title="Global Insight Story Intelligence API",
    description="FastAPI Backend powered by Grok AI for story Q&A, caching, and grounded intelligence.",
    version="1.0.0"
)

# Static files for user-captured community spotlight live photos
UPLOADS_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOADS_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOADS_DIR), name="uploads")

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

class CommunityPostRequest(BaseModel):
    title: str
    content: str
    author_name: Optional[str] = "Local Resident"
    author_role: Optional[str] = "Verified Resident"
    village: Optional[str] = ""
    tehsil: Optional[str] = ""
    district: Optional[str] = ""
    state: Optional[str] = ""
    lat: Optional[float] = 0.0
    lon: Optional[float] = 0.0
    image_base64: Optional[str] = None

class CommunityUpvoteRequest(BaseModel):
    spotlight_id: str

SPOTLIGHT_FILE = os.path.join(os.path.dirname(__file__), "data", "community_spotlights.json")

# High-resolution, context-specific category images (both plain and emoji keys)
CATEGORY_IMAGES = {
    "Health & Blood Camp": "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800&auto=format&fit=crop&q=80",
    "Health & Medical Camp": "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800&auto=format&fit=crop&q=80",
    "Traffic & Road Repair": "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800&auto=format&fit=crop&q=80",
    "Road Repair & Diversion": "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800&auto=format&fit=crop&q=80",
    "Panchayat & Civic Notice": "https://images.unsplash.com/photo-1577495508048-b635879837f1?w=800&auto=format&fit=crop&q=80",
    "Power & Water Schedule": "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?w=800&auto=format&fit=crop&q=80",
    "Agriculture & Mandi": "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?w=800&auto=format&fit=crop&q=80",
    "School & Student Notice": "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800&auto=format&fit=crop&q=80",
    "Emergency Alert": "https://images.unsplash.com/photo-1582139329536-e7284fece509?w=800&auto=format&fit=crop&q=80",
    "Local Culture & Events": "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80",
    # Emoji-prefixed aliases
    "🏥 Health & Blood Camp": "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800&auto=format&fit=crop&q=80",
    "🚧 Traffic & Road Repair": "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800&auto=format&fit=crop&q=80",
    "📢 Panchayat & Civic Notice": "https://images.unsplash.com/photo-1577495508048-b635879837f1?w=800&auto=format&fit=crop&q=80",
    "⚡ Power & Water Schedule": "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?w=800&auto=format&fit=crop&q=80",
    "🌾 Agriculture & Mandi": "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?w=800&auto=format&fit=crop&q=80",
    "🎓 School & Student Notice": "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800&auto=format&fit=crop&q=80",
    "🚨 Emergency Alert": "https://images.unsplash.com/photo-1582139329536-e7284fece509?w=800&auto=format&fit=crop&q=80",
    "🎉 Local Culture & Events": "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80"
}
DEFAULT_SPOTLIGHT_IMAGE = "https://images.unsplash.com/photo-1577495508048-b635879837f1?w=800&auto=format&fit=crop&q=80"

def _load_spotlights() -> List[Dict[str, Any]]:
    if not os.path.exists(SPOTLIGHT_FILE):
        return []
    try:
        with open(SPOTLIGHT_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"Error loading spotlights: {e}")
        return []

def _save_spotlights(data: List[Dict[str, Any]]):
    os.makedirs(os.path.dirname(SPOTLIGHT_FILE), exist_ok=True)
    with open(SPOTLIGHT_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

def _find_matching_spotlights(village: str = "", tehsil: str = "", district: str = "", state: str = "") -> List[Dict[str, Any]]:
    all_spots = _load_spotlights()
    matches = []
    v_lower = (village or "").strip().lower()
    t_lower = (tehsil or "").strip().lower()
    d_lower = (district or "").strip().lower()

    for s in all_spots:
        if s.get("status") != "active":
            continue
        sv = (s.get("village") or "").lower()
        st = (s.get("tehsil") or "").lower()
        sd = (s.get("district") or "").lower()

        is_match = False
        # Direct village match
        if v_lower and (v_lower in sv or sv in v_lower):
            is_match = True
        # Tehsil match
        elif t_lower and (t_lower in st or st in t_lower):
            is_match = True
        # District match
        elif d_lower and (d_lower in sd or sd in d_lower):
            is_match = True
        elif not v_lower and not t_lower and not d_lower:
            is_match = True

        if is_match:
            matches.append(s)

    # Sort: High urgency first, then newest timestamp, then upvotes
    matches.sort(
        key=lambda x: (
            1 if x.get("urgency") == "High" else 0,
            x.get("timestamp", 0),
            x.get("upvotes", 0)
        ),
        reverse=True
    )
    return matches

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


@app.post("/api/community/post")
async def post_community_notice(req: CommunityPostRequest):
    if not req.title or not req.title.strip():
        raise HTTPException(status_code=400, detail="Notice title is required.")
    if not req.content or not req.content.strip():
        raise HTTPException(status_code=400, detail="Notice description is required.")

    loc_str = ", ".join(filter(None, [req.village, req.tehsil, req.district, req.state])) or "Local Area"

    # AI Moderation using Groq AI Content Guard
    mod_result = await moderate_community_notice(
        title=req.title.strip(),
        content=req.content.strip(),
        author_role=req.author_role or "Verified Resident",
        location=loc_str
    )

    if not mod_result.get("approved", False):
        return {
            "status": "rejected",
            "approved": False,
            "reason": mod_result.get("rejection_reason") or "Notice does not meet community safety guidelines."
        }

    now_ts = time.time()
    created_str = "Just now"

    # Decode and save mandatory live camera reference photo if provided
    image_url = None
    if req.image_base64 and req.image_base64.strip():
        try:
            raw_b64 = req.image_base64.strip()
            if "," in raw_b64:
                raw_b64 = raw_b64.split(",", 1)[1]
            img_bytes = base64.b64decode(raw_b64)
            img_filename = f"spotlight_{int(now_ts)}_{uuid.uuid4().hex[:6]}.jpg"
            img_path = os.path.join(UPLOADS_DIR, img_filename)
            with open(img_path, "wb") as f:
                f.write(img_bytes)
            image_url = f"http://10.0.2.2:8085/uploads/{img_filename}"
        except Exception as e:
            print(f"Error saving user camera photo: {e}")

    new_spotlight = {
        "id": f"spotlight_{int(now_ts)}_{uuid.uuid4().hex[:6]}",
        "title": req.title.strip(),
        "content": req.content.strip(),
        "polished_title": mod_result.get("polished_title") or req.title.strip(),
        "polished_content": mod_result.get("polished_content") or req.content.strip(),
        "author_name": req.author_name.strip() if req.author_name else "Local Resident",
        "author_role": req.author_role.strip() if req.author_role else "Verified Resident",
        "category": mod_result.get("category") or "Panchayat & Civic Notice",
        "village": req.village.strip() if req.village else "",
        "tehsil": req.tehsil.strip() if req.tehsil else "",
        "district": req.district.strip() if req.district else "",
        "state": req.state.strip() if req.state else "",
        "lat": req.lat or 0.0,
        "lon": req.lon or 0.0,
        "image_url": image_url,
        "created_at": created_str,
        "timestamp": now_ts,
        "urgency": mod_result.get("urgency") or "Normal",
        "upvotes": 1,
        "verified_by_ai": True,
        "ai_model": "AI Content Verification",
        "status": "active"
    }

    spots = _load_spotlights()
    spots.insert(0, new_spotlight)
    _save_spotlights(spots)

    return {
        "status": "approved",
        "approved": True,
        "spotlight": new_spotlight,
        "message": f"Notice verified as {new_spotlight['category']} and broadcasted live."
    }


@app.get("/api/community/spotlights")
async def get_community_spotlights(village: str = "", tehsil: str = "", district: str = "", state: str = ""):
    matches = _find_matching_spotlights(village, tehsil, district, state)
    return {
        "status": "ok",
        "totalResults": len(matches),
        "spotlights": matches
    }


@app.post("/api/community/upvote")
async def upvote_community_spotlight(req: CommunityUpvoteRequest):
    spots = _load_spotlights()
    for s in spots:
        if s.get("id") == req.spotlight_id:
            s["upvotes"] = s.get("upvotes", 0) + 1
            _save_spotlights(spots)
            return {
                "status": "ok",
                "spotlight_id": req.spotlight_id,
                "upvotes": s["upvotes"]
            }
    raise HTTPException(status_code=404, detail="Spotlight notice not found")


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

        # Prepend active Community / User-Reported Spotlights
        matching_spots = _find_matching_spotlights(village_name, tehsil_name, district_name, state_name)
        spotlight_articles = []
        for s in matching_spots:
            cat = s.get("category", "Panchayat & Civic Notice")
            # 1. Use user's live captured camera photo if available
            img = s.get("image_url")
            # 2. Fall back to distinct category image
            if not img:
                clean_cat = re.sub(r'[^\w\s&]', '', cat).strip()
                img = CATEGORY_IMAGES.get(clean_cat)
                if not img:
                    for k, v in CATEGORY_IMAGES.items():
                        if clean_cat.lower() in k.lower() or k.lower() in clean_cat.lower():
                            img = v
                            break
            if not img:
                img = DEFAULT_SPOTLIGHT_IMAGE

            loc_tag = s.get("village") or s.get("tehsil") or s.get("district") or "Local Area"
            spotlight_articles.append({
                "id": s.get("id"),
                "title": s.get("polished_title") or s.get("title"),
                "description": s.get("polished_content") or s.get("content"),
                "author": f"{s.get('author_name', 'Local')} ({s.get('author_role', 'Resident')})",
                "source": {
                    "id": "community-spotlight",
                    "name": f"Community Spotlight • {cat}"
                },
                "url": "",
                "urlToImage": img,
                "publishedAt": s.get("created_at", "Just now"),
                "isVernacular": False,
                "isCommunitySpotlight": True,
                "spotlightCategory": cat,
                "authorRole": s.get("author_role", "Verified Resident"),
                "urgency": s.get("urgency", "Normal"),
                "upvotes": s.get("upvotes", 1),
                "locationTag": loc_tag
            })

        articles = spotlight_articles + articles
        for idx, item in enumerate(articles):
            if not item.get("id"):
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
    uvicorn.run(app, host="127.0.0.1", port=8085)
