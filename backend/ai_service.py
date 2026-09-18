import os
import json
import httpx
from typing import Dict, Any, List

GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

async def call_free_ai_api(prompt: str, system_prompt: str) -> str:
    """
    Executes AI prompt against 100% FREE AI models on Groq / Gemini.
    """
    if GROQ_API_KEY:
        url = "https://api.groq.com/openai/v1/chat/completions"
        headers = {
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type": "application/json"
        }

        # Active Groq model IDs verified on API
        models_to_try = [
            "openai/gpt-oss-20b",
            "openai/gpt-oss-120b",
            "qwen/qwen3.8-27b",
            "groq/compound",
            "groq/compound-mini"
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
                    "max_tokens": 500
                }
                async with httpx.AsyncClient(timeout=25.0) as client:
                    res = await client.post(url, headers=headers, json=payload)
                    if res.status_code == 200:
                        data = res.json()
                        res_text = data["choices"][0]["message"]["content"].strip()
                        if res_text:
                            return res_text
            except Exception as e:
                print(f"Groq model {model} error: {e}")
                continue

    if GEMINI_API_KEY:
        try:
            async with httpx.AsyncClient(timeout=25.0) as client:
                url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}"
                payload = {
                    "contents": [{"parts": [{"text": f"{system_prompt}\n\n{prompt}"}]}]
                }
                res = await client.post(url, json=payload)
                if res.status_code == 200:
                    data = res.json()
                    return data["candidates"][0]["content"]["parts"][0]["text"].strip()
        except Exception as e:
            print(f"Gemini call error: {e}")

    return "Unable to process story question right now. Please try again."

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
            answer_text = f"<b>Key Information:</b><br/>• For official details regarding this topic, refer to verified primary releases from {source_name}."
    except Exception as e:
        print(f"AI API error: {e}")
        answer_text = (
            "Unable to generate response right now. Please check internet connection."
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
