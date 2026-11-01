# Muraja'ah — Qur'an revision app

A personal Android app for revising memorized Qur'an, with meaning.

**Modes**
- **Ayah** — recite from memory, tap to reveal the whole ayah and its translation.
- **Word** — reveal one word per tap, each with its English meaning underneath.
- **Letter** — reveal one letter (with its harakat) per tap, to catch fine details like وَ vs فَ.

Tap the reading area for the next step, hold to peek at the whole ayah, *Back* to undo.
Tap the surah title to jump to any surah and ayah. The app remembers where you stopped.

## Data
- Arabic: Tanzil Uthmani text (tanzil.net, CC BY 3.0, unmodified). The basmala of each surah is shown as a header.
- Translation: Saheeh International (via QuranEnc.com).
- Word-by-word meanings: Quran.com API (fetched by `tools/fetch_wbw.py` in the *Fetch data* workflow).
- Font: Amiri Quran (SIL OFL).

## Build
GitHub Actions builds a signed APK on every push to `main` and publishes it as a Release
(`build-N`). Download the latest APK from the Releases page and install it on the phone.
