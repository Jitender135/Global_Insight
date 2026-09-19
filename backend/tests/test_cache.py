import time
import pytest
from backend.cache_manager import StoryCacheManager

def test_cache_set_and_get():
    cache = StoryCacheManager(ttl_seconds=10)
    story_id = "story_123"
    query = "What caused the traffic event?"
    data = {"answer": "Road construction", "citations": []}

    cache.set(story_id, query, data)
    retrieved = cache.get(story_id, query)

    assert retrieved is not None
    assert retrieved["answer"] == "Road construction"

def test_cache_case_and_whitespace_insensitivity():
    cache = StoryCacheManager(ttl_seconds=10)
    story_id = "story_456"
    data = {"answer": "High efficiency"}

    cache.set(story_id, "  Global Insight News  ", data)

    # Retrieval with different casing and extra spaces should hit cache
    retrieved = cache.get(story_id, "global insight news")
    assert retrieved is not None
    assert retrieved["answer"] == "High efficiency"

def test_cache_miss():
    cache = StoryCacheManager(ttl_seconds=10)
    retrieved = cache.get("non_existent_story", "some query")
    assert retrieved is None

def test_cache_ttl_expiration():
    # Set cache with 1 second TTL
    cache = StoryCacheManager(ttl_seconds=1)
    story_id = "story_789"
    query = "Will it expire?"
    data = {"status": "valid"}

    cache.set(story_id, query, data)
    assert cache.get(story_id, query) is not None

    # Wait for TTL to expire
    time.sleep(1.1)
    assert cache.get(story_id, query) is None

def test_cache_overwrite():
    cache = StoryCacheManager(ttl_seconds=10)
    story_id = "story_101"
    query = "Update test"

    cache.set(story_id, query, {"version": 1})
    assert cache.get(story_id, query)["version"] == 1

    cache.set(story_id, query, {"version": 2})
    assert cache.get(story_id, query)["version"] == 2
