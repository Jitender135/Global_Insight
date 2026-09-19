import os
import json
import httpx
import asyncio
import re
from typing import Dict, Any, List
from dotenv import load_dotenv

load_dotenv()
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

async def call_free_ai_api(prompt: str, system_prompt: str) -> str:
    """
    Executes AI prompt against 100% FREE AI models on Groq / Gemini.
    """
    groq_key = os.getenv("GROQ_API_KEY", "") or GROQ_API_KEY
    if groq_key:
        url = "https://api.groq.com/openai/v1/chat/completions"
        headers = {
            "Authorization": f"Bearer {groq_key}",
            "Content-Type": "application/json"
        }

        # Active Groq model IDs verified on API (prioritizing fast direct output models)
        models_to_try = [
            "qwen/qwen3.8-27b",
            "groq/compound-mini",
            "groq/compound",
            "openai/gpt-oss-20b",
            "openai/gpt-oss-120b"
        ]

        for model in models_to_try:
            try:
                payload = {
                    "model": model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": prompt}
                    ],
                    "temperature": 0.2,
                    "max_tokens": 1024
                }
                async with httpx.AsyncClient(timeout=25.0) as client:
                    res = await client.post(url, headers=headers, json=payload)
                    if res.status_code == 200:
                        data = res.json()
                        msg = data["choices"][0]["message"]
                        res_text = (msg.get("content") or "").strip()
                        if res_text:
                            return res_text
            except Exception as e:
                print(f"Groq model {model} error: {e}")
                continue

    gemini_key = os.getenv("GEMINI_API_KEY", "") or GEMINI_API_KEY
    if gemini_key:
        try:
            async with httpx.AsyncClient(timeout=25.0) as client:
                url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_key}"
                payload = {
                    "contents": [{"parts": [{"text": f"{system_prompt}\n\n{prompt}"}]}]
                }
                res = await client.post(url, json=payload)
                if res.status_code == 200:
                    data = res.json()
                    return data["candidates"][0]["content"]["parts"][0]["text"].strip()
        except Exception as e:
            print(f"Gemini call error: {e}")

    return ""


async def generate_suggested_questions(title: str, summary: str, source: str) -> List[str]:
    system_prompt = (
        "You are Global Insight AI. Analyze the news story and return EXACTLY 4 short, engaging questions "
        "that readers would tap to get instant quick facts and context.\n"
        "EXAMPLES: 'What happened?', 'Why did this happen?', 'Who is affected?', 'What changed?'\n"
        "Output ONLY a raw JSON array of 4 string questions, e.g. "
        '["What actually happened?", "Why did this happen?", "Who is affected?", "What happens next?"]'
    )
    prompt = f"Story Title: {title}\nStory Source: {source}\nStory Content: {summary}"

    try:
        raw_res = await call_free_ai_api(prompt, system_prompt)
        if "[" in raw_res and "]" in raw_res:
            start = raw_res.find("[")
            end = raw_res.rfind("]") + 1
            return json.loads(raw_res[start:end])
    except Exception as e:
        print(f"Suggested questions error: {e}")

    return [
        "What actually happened?",
        "Why did this happen?",
        "Who is affected?",
        "What happens next?"
    ]

async def answer_story_question(
    story_title: str,
    story_summary: str,
    source_name: str,
    source_url: str,
    user_query: str,
    chat_history: List[Dict[str, str]]
) -> Dict[str, Any]:

    system_prompt = (
        "You are Global Insight Story Intelligence AI assistant.\n\n"
        "CRITICAL STORY SCOPING & TOPIC RELEVANCE RULES:\n"
        "1. TOPIC RELEVANCE CHECK:\n"
        "   - Check if the user's question is relevant to the news story, its topic, background, entities, impact, rules, or domain.\n"
        "   - IF THE QUESTION IS COMPLETELY UNRELATED to news or this story (e.g. cooking recipes like 'how to cook Maggi', general coding, unrelated personal homework, random trivia):\n"
        "     DO NOT answer the unrelated recipe or trivia!\n"
        "     INSTEAD reply concisely: '<b>Outside Story Scope:</b><br/>• That is outside the scope of this news story. I am your Global Insight Assistant scoped to help you explore and understand details about this news event.<br/>• Try asking: <i>What happened?</i>, <i>Why does this matter?</i>, or <i>Who is affected?</i>'\n\n"
        "2. RELEVANT STORY & DOMAIN QUERIES:\n"
        "   - If the query is related to the story or its domain (background concepts, official portals, documents, scores, policies, facts):\n"
        "     Provide crisp, direct, verified answers (40 to 70 words max).\n"
        "3. FORMATTING:\n"
        "   - Output clean HTML styling (e.g. <b>bold</b>, • bullet points). Do NOT output raw Markdown headers."
    )

    history_text = "\n".join([f"{msg.get('role', 'User')}: {msg.get('text', '')}" for msg in chat_history])

    prompt = (
        f"STORY CONTEXT:\n"
        f"Title: {story_title}\n"
        f"Source: {source_name}\n"
        f"Summary: {story_summary}\n\n"
        f"CONVERSATION HISTORY:\n{history_text}\n\n"
        f"USER QUESTION: {user_query}"
    )

    try:
        answer_text = await call_free_ai_api(prompt, system_prompt)
        if not answer_text or len(answer_text.strip()) == 0:
            summary_snippet = story_summary.strip() if story_summary and len(story_summary.strip()) > 0 else story_title
            answer_text = (
                f"<b>Key Takeaways from {source_name}:</b><br/>"
                f"• {summary_snippet}<br/>"
                f"• Refer to official primary releases from {source_name} for additional updates."
            )
    except Exception as e:
        print(f"AI API error: {e}")
        summary_snippet = story_summary.strip() if story_summary and len(story_summary.strip()) > 0 else story_title
        answer_text = (
            f"<b>Story Fact Sheet ({source_name}):</b><br/>"
            f"• {summary_snippet}"
        )

    sources = []
    if source_name and source_name.strip():
        sources.append({
            "sourceName": source_name,
            "url": source_url or "",
            "tier": "Primary Source"
        })

    follow_ups = [
        "What happens next?",
        "Why does this matter?",
        "Explain simply"
    ]

    return {
        "answer_text": answer_text,
        "sources": sources,
        "follow_ups": follow_ups
    }


def is_vernacular_text(text: str) -> bool:
    """Checks if text contains Devanagari or other Indic script characters."""
    if not text:
        return False
    # Devanagari Unicode range: \u0900 to \u097F
    return bool(re.search(r"[\u0900-\u097F]", text))


async def translate_vernacular_story(
    title: str,
    description: str,
    source_name: str,
    target_lang: str = "en"
) -> Dict[str, str]:
    """
    Translates a vernacular news story into crisp English using Groq free AI models.
    Preserves exact village/town, person, and organizational names.
    """
    system_prompt = (
        "You are an expert bilingual news editor for Global Insight.\n"
        "Translate the following vernacular/Hindi hyper-local news story into crisp, fluent, professional English.\n"
        "Maintain journalistic accuracy, retaining exact village, person, and organization names.\n"
        "Respond in strict JSON format with exactly two keys: 'translated_title' and 'translated_description' (2-3 crisp sentences).\n"
        'Example output: {"translated_title": "...", "translated_description": "..."}'
    )
    prompt = f"Source: {source_name}\nVernacular Title: {title}\nVernacular Description: {description}"

    try:
        raw_res = await call_free_ai_api(prompt, system_prompt)
        if "{" in raw_res and "}" in raw_res:
            start = raw_res.find("{")
            end = raw_res.rfind("}") + 1
            data = json.loads(raw_res[start:end])
            t_title = (data.get("translated_title") or "").strip()
            t_desc = (data.get("translated_description") or "").strip()
            if t_title:
                return {
                    "translated_title": t_title,
                    "translated_description": t_desc or description
                }
    except Exception as e:
        print(f"Vernacular translation error: {e}")

    return {
        "translated_title": title,
        "translated_description": description
    }


async def translate_vernacular_articles(
    articles: List[Dict[str, Any]],
    target_lang: str = "en",
    max_to_translate: int = 12
) -> List[Dict[str, Any]]:
    """
    Processes a list of articles, detecting vernacular (Hindi) stories and
    translating them into English (or preserving native Hindi if target_lang=='hi').
    Attaches vernacular badges and original text.
    """
    vernacular_tasks = []
    task_indices = []

    for idx, article in enumerate(articles):
        title = article.get("title", "")
        desc = article.get("description", "")
        src_obj = article.get("source") or {}
        src_name = src_obj.get("name") if isinstance(src_obj, dict) else str(src_obj)

        is_hindi = is_vernacular_text(title) or is_vernacular_text(desc)
        if is_hindi:
            article["isVernacular"] = True
            article["originalTitle"] = title
            article["originalDescription"] = desc
            article["vernacularSource"] = src_name or "Hindi Press"

            if target_lang != "hi" and len(vernacular_tasks) < max_to_translate:
                vernacular_tasks.append(
                    translate_vernacular_story(title, desc, src_name or "Hindi Press", target_lang)
                )
                task_indices.append(idx)
            else:
                article["vernacularBadge"] = f"Hindi Press • {src_name}"
        else:
            article["isVernacular"] = False
            article["vernacularBadge"] = None
            article["originalTitle"] = None
            article["originalDescription"] = None

    if vernacular_tasks:
        try:
            # Run parallel translations with a 15-second total timeout
            results = await asyncio.wait_for(
                asyncio.gather(*vernacular_tasks, return_exceptions=True),
                timeout=15.0
            )
            for task_idx, result in zip(task_indices, results):
                if isinstance(result, dict) and result.get("translated_title"):
                    art = articles[task_idx]
                    trans_title = result["translated_title"]
                    trans_desc = result.get("translated_description", art.get("description", ""))
                    src_name = art.get("vernacularSource", "Hindi Press")

                    art["title"] = trans_title
                    art["description"] = trans_desc
                    art["vernacularBadge"] = f"Translated from Hindi • {src_name}"
                elif isinstance(result, dict):
                    art = articles[task_idx]
                    src_name = art.get("vernacularSource", "Hindi Press")
                    art["vernacularBadge"] = f"Hindi Press • {src_name}"
        except Exception as e:
            print(f"Batch vernacular translation timed out or failed: {e}")
            for task_idx in task_indices:
                art = articles[task_idx]
                src_name = art.get("vernacularSource", "Hindi Press")
                art["vernacularBadge"] = f"Hindi Press • {src_name}"

    return articles

