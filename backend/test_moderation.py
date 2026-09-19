import asyncio
from ai_service import moderate_community_notice

async def test():
    res = await moderate_community_notice(
        title="NH-48 Flyover Repair Diversion",
        content="NH-48 flyover repair work starts tonight 11 PM. Heavy traffic diverted through service lane near Rajiv Chowk for next 48 hours. Commuters advised to take alternate routes.",
        author_role="Verified Resident",
        location="Gurugram"
    )
    print("Road Diversion Result:", res)

if __name__ == "__main__":
    asyncio.run(test())
