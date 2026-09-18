import hashlib
import time
from typing import Optional, Dict, Any

class StoryCacheManager:
    def __init__(self, ttl_seconds: int = 86400):
        # Cache storage: key -> { "data": dict, "expires_at": timestamp }
        self._cache: Dict[str, Dict[str, Any]] = {}
        self.ttl = ttl_seconds

    def _generate_key(self, story_id: str, query: str) -> str:
        raw = f"{story_id}:{query.strip().lower()}"
        return hashlib.sha256(raw.encode("utf-8")).hexdigest()

    def get(self, story_id: str, query: str) -> Optional[Dict[str, Any]]:
        key = self._generate_key(story_id, query)
        entry = self._cache.get(key)
        if entry:
            if time.time() < entry["expires_at"]:
                return entry["data"]
            else:
                del self._cache[key]
        return None

    def set(self, story_id: str, query: str, data: Dict[str, Any]):
        key = self._generate_key(story_id, query)
        self._cache[key] = {
            "data": data,
            "expires_at": time.time() + self.ttl
        }

# Global singleton instance
cache_manager = StoryCacheManager()
