"""Fetches word-by-word English meanings from the Quran.com API (run in GitHub Actions,
which has internet access) and aligns them with the Tanzil words in assets/quran.json.

Output: app/src/main/assets/wbw.json  ->  {"1:1": ["In (the) name", "(of) Allah", ...], ...}
One gloss per Tanzil word (split on spaces). Where Quran.com splits an ayah into a different
number of words, glosses are aligned by matching the bare letters of each word.
"""
import json, os, re, sys, time, urllib.request

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
API = "https://api.quran.com/api/v4/verses/by_chapter/{c}?language=en&words=true" \
      "&word_fields=text_uthmani,text_imlaei_simple&per_page=50&page={p}"

def get(url):
    for attempt in range(5):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "quran-revision-app/1.0"})
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.load(r)
        except Exception as e:
            print("retry", attempt, url, e, file=sys.stderr)
            time.sleep(2 + attempt * 3)
    raise SystemExit("failed: " + url)

MARKS = re.compile(r"[ً-ٰٟۖ-ۭـ]")
def bare(w):
    w = MARKS.sub("", w)
    for a, b in (("ٱ", "ا"), ("أ", "ا"), ("إ", "ا"), ("آ", "ا"), ("ى", "ي"), ("ة", "ه")):
        w = w.replace(a, b)
    return w

def align(tanzil, qc):
    """tanzil: list of word strings; qc: list of (text, gloss). Returns gloss per tanzil word."""
    if len(tanzil) == len(qc):
        return [g for _, g in qc]
    out, j = [], 0
    for w in tanzil:
        target, parts, acc = bare(w), [], ""
        while j < len(qc) and len(acc) < len(target):
            acc += bare(qc[j][0]); parts.append(qc[j][1]); j += 1
        out.append(" ".join(parts))
    if j < len(qc) and out:                      # leftovers go on the last word
        out[-1] += " " + " ".join(g for _, g in qc[j:])
    return out

def main():
    quran = json.load(open(os.path.join(ASSETS, "quran.json"), encoding="utf-8"))
    result, mismatched = {}, []
    for s in quran["surahs"]:
        c, page, verses = s["n"], 1, []
        while page:
            d = get(API.format(c=c, p=page))
            verses += d["verses"]
            page = d["pagination"].get("next_page")
        assert len(verses) == len(s["a"]), (c, len(verses))
        for v, (arabic, _) in zip(verses, s["a"]):
            qc = [(w.get("text_uthmani") or w.get("text_imlaei_simple") or "",
                   (w.get("translation") or {}).get("text") or "")
                  for w in v["words"] if w.get("char_type_name") == "word"]
            tz = arabic.split(" ")
            if len(tz) != len(qc):
                mismatched.append(v["verse_key"])
            result[v["verse_key"]] = align(tz, qc)
        print("surah", c, "ok", flush=True)
    with open(os.path.join(ASSETS, "wbw.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, separators=(",", ":"))
    print("ayahs:", len(result), "count mismatches:", len(mismatched), mismatched[:40])

if __name__ == "__main__":
    main()
