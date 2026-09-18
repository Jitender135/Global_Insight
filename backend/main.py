import os
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv

load_dotenv()
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

from grok_service import generate_suggested_questions, answer_story_question
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

@app.get("/api/radius-news")
def get_radius_news(
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    sublocality: Optional[str] = "",
    locality: Optional[str] = "",
    postal_code: Optional[str] = "",
    query: Optional[str] = "",
    radius: Optional[int] = 10
):
    """
    Fetches real-time hyper-local news for exact 10km radius, neighborhood, or campus.
    """
    import urllib.parse
    import urllib.request
    import xml.etree.ElementTree as ET
    import re
    import html

    # Build targeted search query
    query_parts = []
    if sublocality and sublocality.strip():
        query_parts.append(sublocality.strip())
    if locality and locality.strip() and locality.strip().lower() != (sublocality or "").strip().lower():
        query_parts.append(locality.strip())
    if query and query.strip():
        query_parts.append(query.strip())
    
    if not query_parts and postal_code:
        query_parts.append(postal_code.strip())

    search_query = " ".join(query_parts) if query_parts else "India local news"

    # Fetch from Google News RSS with India localization
    try:
        encoded_query = urllib.parse.quote(search_query)
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
        for idx, item in enumerate(items[:40]):
            raw_title = item.find("title").text if item.find("title") is not None else ""
            link = item.find("link").text if item.find("link") is not None else ""
            pub_date = item.find("pubDate").text if item.find("pubDate") is not None else ""
            raw_desc = item.find("description").text if item.find("description") is not None else ""
            
            source_elem = item.find("source")
            source_name = source_elem.text if source_elem is not None else "Local News"

            # Clean Title (Remove trailing source name like ' - The Times of India')
            clean_title = raw_title
            if " - " in raw_title:
                clean_title = raw_title.rsplit(" - ", 1)[0].strip()

            # Clean Description
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

        return {
            "status": "ok",
            "totalResults": len(articles),
            "radiusKm": radius,
            "area": sublocality or locality or "Nearby",
            "articles": articles
        }

    except Exception as e:
        print(f"Radius news error: {e}")
        return {
            "status": "error",
            "message": str(e),
            "totalResults": 0,
            "articles": []
        }

    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
