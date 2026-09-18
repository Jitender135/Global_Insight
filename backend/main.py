import os
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8080, reload=True)
