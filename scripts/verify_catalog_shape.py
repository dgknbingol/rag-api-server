# -*- coding: utf-8 -*-
import json
from pathlib import Path

c = json.loads(
    Path(
        r"c:\Users\dgknb\Desktop\Proje\Mobil\rag-api-server\src\main\resources\education\catalog.json"
    ).read_text(encoding="utf-8")
)
print("version", c["version"])
for cat in c["categories"]:
    mods = cat["modules"]
    counts = [len(m["lessons"]) for m in mods]
    print(f"{cat['id']}: {len(mods)} module(s), lesson_counts={counts}")
    if len(mods) == 1:
        titles = [l["title"] for l in mods[0]["lessons"][:6]]
        print("  ->", titles)
