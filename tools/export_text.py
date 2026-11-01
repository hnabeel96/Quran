"""Builds app/src/main/assets/quran.json from the Tanzil Uthmani text + Saheeh International.

Source DB: a SQLite build of Tanzil (CC BY 3.0, verbatim text) and Saheeh International
(via QuranEnc). Run: python3 tools/export_text.py path/to/quran.db
Output shape:
  {"surahs": [{"n":1,"ar":"الفاتحة","tr":"Al-Faatiha","en":"The Opening","t":"meccan",
               "a":[["<arabic>","<english>"], ...]}, ...]}
Ayah 1 of every surah except 1 and 9 carries the basmala in Tanzil; it is split off
(shown as a header in the app) so ayah 1 holds only its own words.
"""
import json, re, sqlite3, sys, os

db = sqlite3.connect(sys.argv[1])
BASMALA = db.execute("select arabic from ayahs where surah=1 and ayah=1").fetchone()[0]
out = {"basmala": BASMALA, "surahs": []}
for n, ar, tr, en, typ, total in db.execute(
        "select id,name_ar,name_translit,name_en,type,total_verses from surahs order by id"):
    ayahs = []
    for a, text, eng in db.execute(
            "select ayah,arabic,saheeh from ayahs where surah=? order by ayah", (n,)):
        if a == 1 and n not in (1, 9):
            words = text.split(" ")
            # two surahs (95, 97) carry a shadda on the first ba in Tanzil; compare without it
            assert " ".join(words[:4]).replace("\u0651", "") == BASMALA.replace("\u0651", ""), n
            text = " ".join(words[4:])
        eng = re.sub(r"\[\d+\]", "", eng).strip()   # drop footnote reference numbers
        ayahs.append([text, eng])
    assert len(ayahs) == total
    out["surahs"].append({"n": n, "ar": ar, "tr": tr, "en": en, "t": typ, "a": ayahs})

dest = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "quran.json")
os.makedirs(os.path.dirname(dest), exist_ok=True)
with open(dest, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
print("wrote", dest, sum(len(s["a"]) for s in out["surahs"]), "ayahs")
