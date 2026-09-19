import pytest
from fastapi.testclient import TestClient
from backend.main import app

client = TestClient(app)

def test_health_check():
    response = client.get("/")
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["status"] == "online"
    assert "version" in json_data
    assert "service" in json_data

def test_radius_news_default():
    response = client.get("/api/radius-news")
    assert response.status_code == 200
    json_data = response.json()
    assert "status" in json_data
    assert json_data["status"] == "ok"
    assert "articles" in json_data
    assert isinstance(json_data["articles"], list)
    assert len(json_data["articles"]) > 0

def test_radius_news_with_coordinates():
    response = client.get("/api/radius-news?lat=28.4595&lon=77.0266&radius=10")
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["status"] == "ok"
    assert "articles" in json_data
    # Verify article schema
    first_article = json_data["articles"][0]
    assert "title" in first_article
    assert "url" in first_article
    assert "publishedAt" in first_article

def test_suggest_questions_endpoint():
    payload = {
        "story_id": "test_story_001",
        "title": "Major AI Advancement Announced",
        "summary": "Researchers have released a breakthrough model achieving high efficiency.",
        "source": "Tech Insights"
    }
    response = client.post("/api/suggest-questions", json=payload)
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["story_id"] == "test_story_001"
    assert "questions" in json_data
    assert isinstance(json_data["questions"], list)
    assert len(json_data["questions"]) > 0

def test_ask_story_endpoint_and_cache():
    payload = {
        "story_id": "test_story_002",
        "title": "Campus Tech Event 2026",
        "summary": "Annual tech fest hosted at BML Munjal University.",
        "source_name": "University Times",
        "source_url": "https://example.com/news",
        "user_query": "When is the event scheduled?",
        "chat_history": []
    }

    # First call - generated (or mocked)
    response_1 = client.post("/api/ask-story", json=payload)
    assert response_1.status_code == 200
    json_1 = response_1.json()
    assert "answer_text" in json_1

    # Second identical call - should return cached=True
    response_2 = client.post("/api/ask-story", json=payload)
    assert response_2.status_code == 200
    json_2 = response_2.json()
    assert json_2["cached"] is True
    assert json_2["answer_text"] == json_1["answer_text"]
