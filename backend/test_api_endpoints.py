import urllib.request
import json
import time

def test_endpoints():
    time.sleep(2)
    # 1. Test GET /
    req = urllib.request.Request("http://127.0.0.1:8085/")
    with urllib.request.urlopen(req, timeout=5) as r:
        print("Root health:", r.read().decode("utf-8"))

    # 2. Test GET /api/community/spotlights
    req = urllib.request.Request("http://127.0.0.1:8085/api/community/spotlights?village=Kharkhari&tehsil=Farrukhnagar")
    with urllib.request.urlopen(req, timeout=5) as r:
        data = json.loads(r.read().decode("utf-8"))
        print(f"Community Spotlights Count: {data.get('totalResults')}")
        for s in data.get("spotlights", []):
            print(f"  • [{s.get('category')}] {s.get('title')} ({s.get('author_role')}) - Upvotes: {s.get('upvotes')}")

    # 3. Test POST /api/community/post with valid notice
    post_payload = json.dumps({
        "title": "NH-48 Flyover Crack Inspection",
        "content": "PWD engineers inspecting flyover near Hero Honda Chowk today. Moderate traffic delays expected until 4 PM. Please use alternate routes.",
        "author_name": "Sanjay Rao",
        "author_role": "Verified Resident",
        "village": "Gurugram",
        "tehsil": "Gurugram",
        "district": "Gurugram",
        "state": "Haryana"
    }).encode("utf-8")
    req = urllib.request.Request("http://127.0.0.1:8085/api/community/post", data=post_payload, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=15) as r:
        res = json.loads(r.read().decode("utf-8"))
        print("\nPost Community Notice Result:", res.get("status"), res.get("message"))
        spot_id = res.get("spotlight", {}).get("id")

    # 4. Test Upvote
    if spot_id:
        upvote_payload = json.dumps({"spotlight_id": spot_id}).encode("utf-8")
        req = urllib.request.Request("http://127.0.0.1:8085/api/community/upvote", data=upvote_payload, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=5) as r:
            res_up = json.loads(r.read().decode("utf-8"))
            print("Upvote Result:", res_up)

    # 5. Test Radius News integration
    req = urllib.request.Request("http://127.0.0.1:8085/api/radius-news?village=Kharkhari&tehsil=Farrukhnagar&district=Gurugram&radius=10")
    with urllib.request.urlopen(req, timeout=20) as r:
        news_data = json.loads(r.read().decode("utf-8"))
        spotlights_in_feed = [a for a in news_data.get("articles", []) if a.get("isCommunitySpotlight")]
        print(f"\nRadius News Total: {len(news_data.get('articles', []))}, Spotlights in feed: {len(spotlights_in_feed)}")
        for sp in spotlights_in_feed:
            print(f"  • Feed Spotlight: {sp.get('title')} ({sp.get('spotlightCategory')})")

if __name__ == "__main__":
    test_endpoints()
