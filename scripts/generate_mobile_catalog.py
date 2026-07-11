# -*- coding: utf-8 -*-
import json
from pathlib import Path

catalog_path = Path(
    r"c:\Users\dgknb\Desktop\Proje\Mobil\rag-api-server\src\main\resources\education\catalog.json"
)
out = Path(
    r"c:\Users\dgknb\Desktop\Proje\Mobil\aislam-mobile\src\constants\educationCatalog.ts"
)

catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
categories_json = json.dumps(catalog["categories"], ensure_ascii=False, indent=2)

content = f"""import type {{ EducationQuizMeta }} from '../types/education';

export type EducationLessonSeed = {{
  id: string;
  title: string;
  summary: string;
}};

export type EducationModuleSeed = {{
  id: string;
  title: string;
  summary: string;
  quiz: EducationQuizMeta | null;
  lessons: EducationLessonSeed[];
}};

export type EducationCategorySeed = {{
  id: string;
  title: string;
  subtitle: string;
  icon: string;
  quiz: EducationQuizMeta | null;
  modules: EducationModuleSeed[];
}};

export const EDUCATION_CATALOG_VERSION = {catalog["version"]};

export const EDUCATION_CATEGORIES: EducationCategorySeed[] = {categories_json};

export function getEducationLesson(lessonId: string) {{
  for (const category of EDUCATION_CATEGORIES) {{
    for (const module of category.modules) {{
      const lesson = module.lessons.find((item) => item.id === lessonId);
      if (lesson) {{
        return {{ category, module, lesson }};
      }}
    }}
  }}
  return null;
}}
"""

out.write_text(content, encoding="utf-8")
print(f"wrote {out} ({out.stat().st_size} bytes)")
