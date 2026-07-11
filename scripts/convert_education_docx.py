# -*- coding: utf-8 -*-
"""
Convert İslam Bilgileri.docx → education JSON (UTF-8).

Hierarchy (Word styles):
  H1 → category (ana kırılım)
  H2 → module (alt kırılım)
  H3 → lesson (tıklanınca içerik açılır)
  H4+ / body / tables → lesson content

If a category has H3 without H2 (Siyer, Hadis…), lessons go under one
synthetic module named after the category; the app lists those lessons
directly on the category screen.
"""

from __future__ import annotations

import json
import re
import shutil
import unicodedata
from pathlib import Path

from docx import Document
from docx.oxml.ns import qn
from docx.table import Table
from docx.text.paragraph import Paragraph

DOCX_PATH = Path(r"c:\Users\dgknb\Desktop\İslam Bilgileri.docx")
OUT_DIR = Path(r"c:\Users\dgknb\Desktop\Proje\Mobil\rag-api-server\src\main\resources\education")
CATALOG_VERSION = 5

CATEGORY_ICONS = {
    "temel-egitimler": "school-outline",
    "kuran-egitimleri": "book-outline",
    "siyer": "walk-outline",
    "hadis": "chatbubbles-outline",
    "fikih": "scale-outline",
    "akaid": "shield-checkmark-outline",
    "islam-tarihi": "time-outline",
    "islam-ahlaki": "heart-outline",
}

CATEGORY_SUBTITLES = {
    "temel-egitimler": "İslam, iman ve temel ibadet bilgileri",
    "kuran-egitimleri": "Kur'an okuma, tecvid ve ezber",
    "siyer": "Peygamber Efendimizin hayatı",
    "hadis": "Hadis ve sünnet bilgileri",
    "fikih": "İbadet ve günlük hayat hükümleri",
    "akaid": "İnanç esasları",
    "islam-tarihi": "İslam medeniyeti ve tarih",
    "islam-ahlaki": "Güzel ahlak ve erdemler",
}

SKIP_LESSON_TITLES = {
    "ders özeti",
    "genel özet",
}

QUIZ_TITLE_RE = re.compile(
    r"(mini\s*quiz|genel\s*quiz|genel\s*değerlendirme|15\s*soru)",
    re.IGNORECASE,
)


def strip_section_number(text: str) -> str:
    """Strip 1. / 1.2 / 1.Temel — keep '40 Hadis'."""
    text = re.sub(r"^\d+\.\d+(?:\.\d+)*\.?\s*", "", text)
    text = re.sub(r"^\d+\.\s+", "", text)
    text = re.sub(r"^\d+\.(?=[^\d\s])", "", text)
    return text


def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKC", text or "").strip().lower()
    tr = str.maketrans(
        {"ç": "c", "ğ": "g", "ı": "i", "ö": "o", "ş": "s", "ü": "u", "â": "a", "î": "i", "û": "u"}
    )
    text = text.translate(tr)
    text = strip_section_number(text)
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    text = re.sub(r"[\s_]+", "-", text).strip("-")
    return re.sub(r"-+", "-", text) or "konu"


def clean_title(text: str) -> str:
    text = unicodedata.normalize("NFKC", (text or "").strip())
    text = strip_section_number(text)
    text = re.sub(r"^📝\s*", "", text)
    return text.strip()


def heading_level(style_name: str) -> int | None:
    if not style_name:
        return None
    m = re.match(r"Heading\s+(\d+)", style_name, re.I)
    return int(m.group(1)) if m else None


def is_list_style(style_name: str) -> bool:
    return bool(style_name) and "list" in style_name.lower()


def estimate_reading_minutes(blocks: list[dict]) -> int:
    chars = 0
    for b in blocks:
        if b.get("text"):
            chars += len(b["text"])
        for item in b.get("items") or []:
            chars += len(item)
    return max(2, min(45, round(chars / 900)))


def first_paragraph_summary(blocks: list[dict], fallback: str) -> str:
    for b in blocks:
        if b.get("type") == "paragraph" and b.get("text"):
            t = b["text"].strip()
            if len(t) > 20:
                return (t[:140] + "…") if len(t) > 140 else t
    return fallback


def flush_bullets(buf: list[str], blocks: list[dict]) -> None:
    if buf:
        blocks.append({"type": "bullet", "text": None, "title": None, "url": None, "items": list(buf)})
        buf.clear()


def parse_quiz(paras: list[str], quiz_id: str, title: str, module_id: str) -> dict | None:
    questions: list[dict] = []
    current_q: str | None = None
    options: list[dict] = []
    q_re = re.compile(r"^(\d+)[\.\)]\s*(.+)$")
    opt_re = re.compile(r"^([A-Da-d])[\.\)]\s*(.+)$")

    def commit():
        nonlocal current_q, options
        if current_q and options:
            questions.append({"question": current_q, "options": options})
        current_q = None
        options = []

    for raw in paras:
        line = raw.strip()
        if not line:
            continue
        qm = q_re.match(line)
        if qm:
            commit()
            current_q = qm.group(2).strip()
            options = []
            continue
        om = opt_re.match(line)
        if om and current_q is not None:
            label = om.group(1).upper()
            text = om.group(2).strip()
            correct = "✅" in text or "✓" in text
            text = text.replace("✅", "").replace("✓", "").strip()
            options.append({"label": label, "text": text, "correct": correct})
    commit()
    if not questions:
        return None
    return {
        "id": quiz_id,
        "title": title,
        "moduleId": module_id,
        "passPercent": 70,
        "questions": questions,
    }


def iter_body(doc: Document):
    for child in doc.element.body.iterchildren():
        if child.tag == qn("w:p"):
            yield ("p", Paragraph(child, doc))
        elif child.tag == qn("w:tbl"):
            yield ("t", Table(child, doc))


def table_to_block(table: Table) -> dict:
    rows: list[str] = []
    for row in table.rows:
        cells: list[str] = []
        for cell in row.cells:
            text = unicodedata.normalize("NFKC", (cell.text or "").replace("\n", " ").strip())
            if not cells or cells[-1] != text:
                cells.append(text)
        rows.append("\t".join(cells))
    return {"type": "table", "text": None, "title": None, "url": None, "items": rows}


def unique_id(base: str, used: set[str]) -> str:
    candidate = base
    n = 2
    while candidate in used:
        candidate = f"{base}-{n}"
        n += 1
    used.add(candidate)
    return candidate


def build() -> None:
    doc = Document(str(DOCX_PATH))
    categories: list[dict] = []
    topics: dict[str, dict] = {}
    quizzes: dict[str, dict] = {}

    current_cat: dict | None = None
    current_mod: dict | None = None
    current_lesson: dict | None = None
    lesson_blocks: list[dict] = []
    bullet_buf: list[str] = []
    quiz_capture: list[str] | None = None
    quiz_meta: dict | None = None
    used_lesson_ids: set[str] = set()
    used_module_ids: set[str] = set()

    def finish_lesson() -> None:
        nonlocal current_lesson, lesson_blocks, bullet_buf
        flush_bullets(bullet_buf, lesson_blocks)
        if not current_lesson or not current_mod:
            current_lesson = None
            lesson_blocks = []
            return
        if not lesson_blocks:
            current_lesson = None
            lesson_blocks = []
            return
        lid = current_lesson["id"]
        summary = first_paragraph_summary(lesson_blocks, current_lesson["title"])
        current_mod["lessons"].append({"id": lid, "title": current_lesson["title"], "summary": summary})
        topics[lid] = {"readingMinutes": estimate_reading_minutes(lesson_blocks), "blocks": lesson_blocks}
        current_lesson = None
        lesson_blocks = []

    def ensure_default_module() -> None:
        """When H3 appears under H1 with no H2, create one module for the category."""
        nonlocal current_mod
        if not current_cat:
            return
        if current_mod:
            return
        mid = unique_id(current_cat["id"], used_module_ids)
        current_mod = {
            "id": mid,
            "title": current_cat["title"],
            "summary": current_cat["subtitle"],
            "quiz": None,
            "lessons": [],
            "_synthetic": True,
        }
        current_cat["modules"].append(current_mod)

    def start_lesson(title: str) -> None:
        nonlocal current_lesson, lesson_blocks, bullet_buf
        finish_lesson()
        ensure_default_module()
        assert current_mod is not None
        lid = unique_id(f"{current_mod['id']}-{slugify(title)}", used_lesson_ids)
        current_lesson = {"id": lid, "title": title}
        lesson_blocks = [{"type": "heading", "text": title, "title": None, "url": None, "items": None}]
        bullet_buf = []

    def finish_quiz_capture() -> None:
        nonlocal quiz_capture, quiz_meta
        if quiz_capture is None or quiz_meta is None:
            quiz_capture = None
            quiz_meta = None
            return
        quiz = parse_quiz(quiz_capture, quiz_meta["id"], quiz_meta["title"], quiz_meta["moduleId"])
        if quiz:
            quizzes[quiz["id"]] = quiz
            qmeta = {
                "id": quiz["id"],
                "title": quiz["title"],
                "type": quiz_meta["type"],
                "questionCount": len(quiz["questions"]),
                "passPercent": 70,
                "achievementId": quiz_meta["achievementId"],
            }
            if quiz_meta["type"] == "CATEGORY" and current_cat:
                current_cat["quiz"] = qmeta
            elif quiz_meta["type"] == "MODULE" and current_mod:
                current_mod["quiz"] = qmeta
        quiz_capture = None
        quiz_meta = None

    def start_quiz(title: str, as_category: bool) -> None:
        nonlocal quiz_capture, quiz_meta
        finish_lesson()
        finish_quiz_capture()
        assert current_cat is not None
        if as_category:
            qid = f"{current_cat['id']}-genel-quiz"
            quiz_meta = {
                "id": qid,
                "title": f"{current_cat['title']} Genel Quiz",
                "type": "CATEGORY",
                "moduleId": current_cat["id"],
                "achievementId": f"{current_cat['id']}-master",
            }
        else:
            ensure_default_module()
            assert current_mod is not None
            qid = f"{current_mod['id']}-mini-quiz"
            quiz_meta = {
                "id": qid,
                "title": f"{current_mod['title']} Mini Quiz",
                "type": "MODULE",
                "moduleId": current_mod["id"],
                "achievementId": f"{current_mod['id']}-complete",
            }
        quiz_capture = []

    def start_module(title: str) -> None:
        nonlocal current_mod
        finish_lesson()
        finish_quiz_capture()
        assert current_cat is not None
        clean = clean_title(title)
        mid = unique_id(slugify(clean), used_module_ids)
        current_mod = {"id": mid, "title": clean, "summary": clean, "quiz": None, "lessons": []}
        current_cat["modules"].append(current_mod)

    def start_category(title: str) -> None:
        nonlocal current_cat, current_mod, current_lesson
        finish_lesson()
        finish_quiz_capture()
        clean = clean_title(title)
        cid = slugify(clean)
        current_cat = {
            "id": cid,
            "title": clean,
            "subtitle": CATEGORY_SUBTITLES.get(cid, clean),
            "icon": CATEGORY_ICONS.get(cid, "library-outline"),
            "quiz": None,
            "modules": [],
        }
        categories.append(current_cat)
        current_mod = None
        current_lesson = None

    def add_heading(text: str) -> None:
        nonlocal lesson_blocks, bullet_buf
        if current_lesson is None:
            return
        flush_bullets(bullet_buf, lesson_blocks)
        lesson_blocks.append({"type": "heading", "text": text, "title": None, "url": None, "items": None})

    def add_paragraph(text: str, as_bullet: bool = False) -> None:
        nonlocal lesson_blocks, bullet_buf
        if current_lesson is None:
            return
        if as_bullet:
            bullet_buf.append(text)
            return
        flush_bullets(bullet_buf, lesson_blocks)
        lesson_blocks.append({"type": "paragraph", "text": text, "title": None, "url": None, "items": None})

    def add_table(table: Table) -> None:
        nonlocal lesson_blocks, bullet_buf
        if current_lesson is None:
            return
        flush_bullets(bullet_buf, lesson_blocks)
        lesson_blocks.append(table_to_block(table))

    for kind, obj in iter_body(doc):
        if kind == "t":
            if quiz_capture is None:
                add_table(obj)
            continue

        p: Paragraph = obj
        text = unicodedata.normalize("NFKC", (p.text or "").strip())
        style = p.style.name if p.style else ""
        level = heading_level(style)

        # --- quiz capture mode ---
        if quiz_capture is not None:
            if level in (1, 2):
                finish_quiz_capture()
            elif level == 3:
                low = text.lower()
                if QUIZ_TITLE_RE.search(low) or "değerlendirme" in low:
                    finish_quiz_capture()
                else:
                    if text:
                        quiz_capture.append(text)
                    continue
            else:
                if text:
                    quiz_capture.append(text)
                continue

        if not text and level is None:
            continue

        if level == 1:
            start_category(text)
            continue

        if level == 2:
            if not current_cat:
                start_category("Genel")
            clean = clean_title(text)
            low = clean.lower()
            if QUIZ_TITLE_RE.search(low) or "değerlendirme" in low:
                start_quiz(clean, as_category=True)
                continue
            if low in SKIP_LESSON_TITLES:
                continue
            start_module(clean)
            continue

        if level == 3:
            if not current_cat:
                continue
            title = clean_title(text)
            low = title.lower().strip()

            if QUIZ_TITLE_RE.search(low) or "değerlendirme" in low:
                as_category = (
                    "değerlendirme" in low
                    or low.startswith("genel quiz")
                    or current_mod is None
                    or bool(current_mod.get("_synthetic"))
                )
                start_quiz(title, as_category=as_category)
                continue

            if low in SKIP_LESSON_TITLES:
                # Keep module-level summary text inside last lesson if any
                if current_lesson:
                    add_heading(title)
                continue

            start_lesson(title)
            continue

        if level and level >= 4:
            add_heading(text)
            continue

        if not text or current_lesson is None:
            continue
        add_paragraph(text, as_bullet=is_list_style(style))

    finish_lesson()
    finish_quiz_capture()

    for cat in categories:
        for mod in cat["modules"]:
            mod.pop("_synthetic", None)
            if mod["lessons"]:
                mod["summary"] = mod["lessons"][0]["summary"]
        cat["modules"] = [m for m in cat["modules"] if m["lessons"] or m["quiz"]]

    catalog = {"version": CATALOG_VERSION, "categories": categories}

    if OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    topics_dir = OUT_DIR / "topics"
    quizzes_dir = OUT_DIR / "quizzes"
    topics_dir.mkdir(parents=True)
    quizzes_dir.mkdir(parents=True)

    (OUT_DIR / "catalog.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    for tid, content in topics.items():
        (topics_dir / f"{tid}.json").write_text(
            json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
    for qid, quiz in quizzes.items():
        (quizzes_dir / f"{qid}.json").write_text(
            json.dumps(quiz, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )

    lesson_count = sum(len(m["lessons"]) for c in categories for m in c["modules"])
    print(f"version={CATALOG_VERSION} categories={len(categories)} lessons={lesson_count} topics={len(topics)} quizzes={len(quizzes)}")
    for c in categories:
        mods = c["modules"]
        print(f"  - {c['title']}: {len(mods)} modül, quiz={'evet' if c['quiz'] else 'hayır'}")
        for m in mods[:4]:
            print(f"      · {m['title']}: {len(m['lessons'])} ders")
        if len(mods) > 4:
            print(f"      … +{len(mods) - 4} modül daha")

    sample = next(topics_dir.glob("*islam-nedir*.json"), None)
    if sample:
        raw = sample.read_text(encoding="utf-8")
        assert "İslam" in raw
        print("UTF-8 OK:", sample.name)


if __name__ == "__main__":
    build()
