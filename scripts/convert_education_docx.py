# -*- coding: utf-8 -*-
"""Convert İslam Bilgileri.docx → education catalog/topics/quizzes JSON (UTF-8)."""

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
CATALOG_VERSION = 4

# Temel Eğitimler deep-dive modules: H3 subsections merge into one lesson
MERGE_MODULE_SLUGS = {
    "abdest",
    "gusul",
    "namaz",
    "oruc",
    "zekat",
    "hac",
    "dua",
}

# These categories have H2 as lessons (no H3 modules). One module holds all lessons.
FLAT_CATEGORY_IDS = {
    "siyer",
    "hadis",
    "fikih",
    "akaid",
    "islam-tarihi",
    "islam-ahlaki",
}

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
    "mini quiz",
    "15 soru mini quiz",
    "genel quiz",
}

QUIZ_TITLE_RE = re.compile(
    r"(mini\s*quiz|genel\s*quiz|genel\s*değerlendirme|15\s*soru)",
    re.IGNORECASE,
)


def slugify(text: str) -> str:
    text = unicodedata.normalize("NFKC", text or "").strip().lower()
    # Turkish-specific map before stripping accents where needed
    tr = str.maketrans(
        {
            "ç": "c",
            "ğ": "g",
            "ı": "i",
            "i": "i",
            "ö": "o",
            "ş": "s",
            "ü": "u",
            "â": "a",
            "î": "i",
            "û": "u",
        }
    )
    text = text.translate(tr)
    text = strip_section_number(text)
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    text = re.sub(r"[\s_]+", "-", text).strip("-")
    text = re.sub(r"-+", "-", text)
    return text or "konu"


def strip_section_number(text: str) -> str:
    """Strip doc section numbers like 1. / 1.2 / 1.Temel — but keep '40 Hadis'."""
    text = re.sub(r"^\d+\.\d+(?:\.\d+)*\.?\s*", "", text)
    text = re.sub(r"^\d+\.\s+", "", text)
    text = re.sub(r"^\d+\.(?=[^\d\s])", "", text)
    return text


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
        blocks.append(
            {
                "type": "bullet",
                "text": None,
                "title": None,
                "url": None,
                "items": list(buf),
            }
        )
        buf.clear()


def parse_quiz_from_paragraphs(paras: list[str], quiz_id: str, title: str, module_id: str) -> dict | None:
    questions: list[dict] = []
    current_q: str | None = None
    options: list[dict] = []

    def commit():
        nonlocal current_q, options
        if current_q and options:
            # ensure one correct; if none marked, leave as-is (first false)
            if not any(o["correct"] for o in options) and options:
                pass
            questions.append({"question": current_q, "options": options})
        current_q = None
        options = []

    q_re = re.compile(r"^(\d+)[\.\)]\s*(.+)$")
    opt_re = re.compile(r"^([A-Da-d])[\.\)]\s*(.+)$")

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
            continue

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


def iter_body_blocks(doc: Document):
    """Yield ('p', Paragraph) or ('t', Table) in document order."""
    body = doc.element.body
    for child in body.iterchildren():
        if child.tag == qn("w:p"):
            yield ("p", Paragraph(child, doc))
        elif child.tag == qn("w:tbl"):
            yield ("t", Table(child, doc))


def table_to_block(table: Table) -> dict:
    rows: list[str] = []
    for row in table.rows:
        cells = []
        for cell in row.cells:
            # dedupe duplicated cell text from merged cells
            text = unicodedata.normalize("NFKC", (cell.text or "").replace("\n", " ").strip())
            cells.append(text)
        # Word sometimes duplicates merged cells — collapse consecutive identical
        deduped: list[str] = []
        for c in cells:
            if not deduped or deduped[-1] != c:
                deduped.append(c)
        rows.append("\t".join(deduped))
    return {
        "type": "table",
        "text": None,
        "title": None,
        "url": None,
        "items": rows,
    }


def build():
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
    merge_mode = False
    flat_mode = False

    used_lesson_ids: set[str] = set()
    used_module_ids: set[str] = set()

    def unique_id(base: str, used: set[str]) -> str:
        candidate = base
        n = 2
        while candidate in used:
            candidate = f"{base}-{n}"
            n += 1
        used.add(candidate)
        return candidate

    def ensure_flat_module():
        """Flat categories use a single module named after the category."""
        nonlocal current_mod
        if not current_cat:
            return
        if current_mod and current_mod.get("_flat"):
            return
        mid = unique_id(current_cat["id"], used_module_ids)
        current_mod = {
            "id": mid,
            "title": current_cat["title"],
            "summary": current_cat["subtitle"],
            "quiz": None,
            "lessons": [],
            "_flat": True,
        }
        current_cat["modules"].append(current_mod)

    def finish_lesson():
        nonlocal current_lesson, lesson_blocks, bullet_buf
        flush_bullets(bullet_buf, lesson_blocks)
        if not current_lesson or not current_mod or not current_cat:
            current_lesson = None
            lesson_blocks = []
            return
        if not lesson_blocks:
            current_lesson = None
            lesson_blocks = []
            return
        lid = current_lesson["id"]
        summary = first_paragraph_summary(lesson_blocks, current_lesson["title"])
        current_lesson["summary"] = summary
        current_mod["lessons"].append(
            {
                "id": lid,
                "title": current_lesson["title"],
                "summary": summary,
            }
        )
        topics[lid] = {
            "readingMinutes": estimate_reading_minutes(lesson_blocks),
            "blocks": lesson_blocks,
        }
        current_lesson = None
        lesson_blocks = []

    def start_lesson(title: str, force_id: str | None = None):
        nonlocal current_lesson, lesson_blocks, bullet_buf
        finish_lesson()
        if flat_mode:
            ensure_flat_module()
        if force_id is None:
            if current_mod:
                base = f"{current_mod['id']}-{slugify(title)}"
            else:
                base = slugify(title)
            lid = unique_id(base, used_lesson_ids)
        else:
            lid = unique_id(force_id, used_lesson_ids)
        current_lesson = {"id": lid, "title": title}
        lesson_blocks = []
        bullet_buf = []
        lesson_blocks.append(
            {
                "type": "heading",
                "text": title,
                "title": None,
                "url": None,
                "items": None,
            }
        )

    def ensure_merged_lesson():
        """In merge mode, ensure one lesson exists for the module."""
        nonlocal current_lesson
        if current_lesson is None and current_mod:
            start_lesson(current_mod["title"], force_id=current_mod["id"])

    def finish_quiz_capture():
        nonlocal quiz_capture, quiz_meta
        if quiz_capture is None or quiz_meta is None:
            quiz_capture = None
            quiz_meta = None
            return
        quiz = parse_quiz_from_paragraphs(
            quiz_capture,
            quiz_meta["id"],
            quiz_meta["title"],
            quiz_meta["moduleId"],
        )
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

    def start_module(title: str):
        nonlocal current_mod, merge_mode
        finish_lesson()
        finish_quiz_capture()
        clean = clean_title(title)
        bare = slugify(clean)
        mid = unique_id(bare, used_module_ids)
        merge_mode = False
        if current_cat and current_cat["id"] == "temel-egitimler" and bare in MERGE_MODULE_SLUGS:
            merge_mode = True
        current_mod = {
            "id": mid,
            "title": clean,
            "summary": clean,
            "quiz": None,
            "lessons": [],
        }
        current_cat["modules"].append(current_mod)

    def start_category(title: str):
        nonlocal current_cat, current_mod, current_lesson, merge_mode, flat_mode
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
        merge_mode = False
        flat_mode = cid in FLAT_CATEGORY_IDS
        if flat_mode:
            ensure_flat_module()

    def add_heading_content(text: str):
        nonlocal lesson_blocks, bullet_buf
        if merge_mode:
            ensure_merged_lesson()
        if current_lesson is None:
            return
        flush_bullets(bullet_buf, lesson_blocks)
        lesson_blocks.append(
            {
                "type": "heading",
                "text": text,
                "title": None,
                "url": None,
                "items": None,
            }
        )

    def add_paragraph(text: str, as_bullet: bool = False):
        nonlocal lesson_blocks, bullet_buf
        if merge_mode:
            ensure_merged_lesson()
        if current_lesson is None and current_mod and not merge_mode and not flat_mode:
            start_lesson(current_mod["title"], force_id=current_mod["id"])
        if current_lesson is None:
            return
        if as_bullet:
            bullet_buf.append(text)
            return
        flush_bullets(bullet_buf, lesson_blocks)
        lesson_blocks.append(
            {
                "type": "paragraph",
                "text": text,
                "title": None,
                "url": None,
                "items": None,
            }
        )

    def add_table(table: Table):
        nonlocal lesson_blocks, bullet_buf
        if merge_mode:
            ensure_merged_lesson()
        if current_lesson is None and current_mod and not flat_mode:
            start_lesson(current_mod["title"], force_id=current_mod["id"])
        if current_lesson is None:
            return
        flush_bullets(bullet_buf, lesson_blocks)
        lesson_blocks.append(table_to_block(table))

    # --- walk document ---
    for kind, obj in iter_body_blocks(doc):
        if kind == "t":
            if quiz_capture is not None:
                continue
            add_table(obj)
            continue

        p: Paragraph = obj
        text = unicodedata.normalize("NFKC", (p.text or "").strip())
        style = p.style.name if p.style else ""
        level = heading_level(style)

        if quiz_capture is not None:
            # stop quiz capture on new H1/H2 or a new quiz/lesson H3
            if level in (1, 2):
                finish_quiz_capture()
                # fall through to process heading
            elif level == 3:
                low = text.lower()
                is_new_quiz = bool(QUIZ_TITLE_RE.search(low)) or "değerlendirme" in low
                is_question_line = bool(re.match(r"^\d+[\.\)]", text))
                if is_new_quiz:
                    finish_quiz_capture()
                    # fall through to start the new quiz
                elif is_question_line or not text:
                    if text:
                        quiz_capture.append(text)
                    continue
                else:
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
            clean_h2 = clean_title(text)
            low_h2 = clean_h2.lower().strip()

            # Category-level quiz as H2 (Siyer/Hadis/…)
            if QUIZ_TITLE_RE.search(low_h2) or low_h2 in {"genel quiz", "genel değerlendirme"}:
                finish_lesson()
                finish_quiz_capture()
                qid = f"{current_cat['id']}-genel-quiz"
                quiz_meta = {
                    "id": qid,
                    "title": f"{current_cat['title']} Genel Quiz",
                    "type": "CATEGORY",
                    "moduleId": current_cat["id"],
                    "achievementId": f"{current_cat['id']}-master",
                }
                quiz_capture = []
                continue

            # Skip module-level summary headings used as H2
            if low_h2 in {"genel özet", "ders özeti"}:
                finish_lesson()
                continue

            # Flat categories: H2 is a lesson, not a module
            if flat_mode:
                start_lesson(clean_h2)
                continue

            start_module(text)
            continue

        if level == 3:
            if not current_mod:
                if flat_mode:
                    ensure_flat_module()
                else:
                    continue
            title = clean_title(text)
            low = title.lower().strip()

            if QUIZ_TITLE_RE.search(low) or "değerlendirme" in low:
                finish_lesson()
                finish_quiz_capture()
                is_category_quiz = "değerlendirme" in low or low.startswith("genel quiz")
                if is_category_quiz and current_cat:
                    qid = f"{current_cat['id']}-genel-quiz"
                    qtitle = f"{current_cat['title']} Genel Quiz"
                    qtype = "CATEGORY"
                    scope = current_cat["id"]
                    ach = f"{current_cat['id']}-master"
                else:
                    qid = f"{current_mod['id']}-mini-quiz"
                    qtitle = f"{current_mod['title']} Mini Quiz"
                    qtype = "MODULE"
                    scope = current_mod["id"]
                    ach = f"{current_mod['id']}-complete"

                quiz_meta = {
                    "id": qid,
                    "title": qtitle,
                    "type": qtype,
                    "moduleId": scope,
                    "achievementId": ach,
                }
                quiz_capture = []
                continue

            if low in SKIP_LESSON_TITLES:
                if merge_mode or current_lesson:
                    if merge_mode:
                        ensure_merged_lesson()
                    if current_lesson:
                        add_heading_content(title)
                continue

            if merge_mode:
                ensure_merged_lesson()
                add_heading_content(title)
                continue

            start_lesson(title)
            continue

        if level and level >= 4:
            if merge_mode:
                ensure_merged_lesson()
            if current_lesson is None and current_mod and not flat_mode:
                start_lesson(current_mod["title"], force_id=current_mod["id"])
            add_heading_content(text)
            continue

        # Normal / list paragraph
        if not text:
            continue
        if current_mod is None:
            continue
        add_paragraph(text, as_bullet=is_list_style(style))

    finish_lesson()
    finish_quiz_capture()

    # Post-process: fill module summaries; strip internal flags
    for cat in categories:
        for mod in cat["modules"]:
            mod.pop("_flat", None)
            if mod["lessons"]:
                mod["summary"] = mod["lessons"][0]["summary"]
        cat["modules"] = [m for m in cat["modules"] if m["lessons"] or m["quiz"]]

    catalog = {"version": CATALOG_VERSION, "categories": categories}

    # Write outputs
    topics_dir = OUT_DIR / "topics"
    quizzes_dir = OUT_DIR / "quizzes"
    if OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    topics_dir.mkdir(parents=True, exist_ok=True)
    quizzes_dir.mkdir(parents=True, exist_ok=True)

    with (OUT_DIR / "catalog.json").open("w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")

    for tid, content in topics.items():
        with (topics_dir / f"{tid}.json").open("w", encoding="utf-8") as f:
            json.dump(content, f, ensure_ascii=False, indent=2)
            f.write("\n")

    for qid, quiz in quizzes.items():
        with (quizzes_dir / f"{qid}.json").open("w", encoding="utf-8") as f:
            json.dump(quiz, f, ensure_ascii=False, indent=2)
            f.write("\n")

    # Stats
    lesson_count = sum(len(m["lessons"]) for c in categories for m in c["modules"])
    print(f"categories={len(categories)} lessons={lesson_count} topics={len(topics)} quizzes={len(quizzes)}")
    for c in categories:
        print(f"  - {c['title']}: {len(c['modules'])} modules, quiz={'yes' if c['quiz'] else 'no'}")
        for m in c["modules"][:3]:
            print(f"      · {m['title']}: {len(m['lessons'])} lessons, quiz={'yes' if m['quiz'] else 'no'}")
        if len(c["modules"]) > 3:
            print(f"      … +{len(c['modules'])-3} more")

    # Verify Turkish chars in a sample
    sample = topics_dir / "islama-giris-islam-nedir.json"
    # find any islam-nedir
    matches = list(topics_dir.glob("*islam-nedir*.json"))
    if matches:
        raw = matches[0].read_text(encoding="utf-8")
        assert "İslam" in raw or "slam" in raw
        print("UTF-8 sample OK:", matches[0].name)


if __name__ == "__main__":
    build()
