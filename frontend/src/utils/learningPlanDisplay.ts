const CONCEPT_TITLE_MAP: Record<string, string> = {
  'foundation of 链表': '理解链表的基本结构',
  'foundation of 图': '理解图的基本结构',
  'foundation of tree': '理解树的基本结构',
  'foundation of stack': '理解栈的基本结构',
  'foundation of queue': '理解队列的基本结构',
  'foundation of hash table': '理解哈希表的基本结构',
};

const STRATEGY_CODE_LABELS: Record<string, string> = {
  PRACTICE_FIRST: '先练再纠偏',
  CONCEPT_FIRST: '先理解概念再练习',
  EXAMPLE_FIRST: '先看例子再总结',
  PROJECT_DRIVEN: '边做边学',
};

const RHYTHM_CODE_LABELS: Record<string, string> = {
  STANDARD: '每周 4-6 小时',
  EXAM: '准备考试或测验',
  LIGHT: '每周 2-3 小时',
  INTENSIVE: '每周 8 小时以上',
};

export function cleanText(value: unknown, fallback = ''): string {
  const text = String(value ?? '').replace(/\s+/g, ' ').trim();
  return text || fallback;
}

/** 产品文案短句：过长截断，避免策略说明整段上屏 */
export function productCopy(value: unknown, fallback = '', maxLen = 100): string {
  const s = cleanText(value, fallback);
  if (s === fallback || s.length <= maxLen) return s;
  const truncated = s.slice(0, maxLen).replace(/,|;|，|；\s*[^,;，；]*$/, '').trim();
  return truncated || fallback;
}

export function normalizeConceptTitle(value: unknown, fallback = '当前关键知识点'): string {
  const source = cleanText(value);
  if (!source) {
    return fallback;
  }
  const normalizedKey = source.toLowerCase();
  if (CONCEPT_TITLE_MAP[normalizedKey]) {
    return CONCEPT_TITLE_MAP[normalizedKey];
  }
  if (/^foundation of\s+/i.test(source)) {
    const target = source.replace(/^foundation of\s+/i, '').trim();
    if (target) {
      return /[\u4e00-\u9fa5]/.test(target) ? `理解${target}的基本结构` : `理解${target}基础`;
    }
  }
  return source;
}

export function formatPreviewDisplayTitle(value: unknown, fallback = '当前关键知识点'): string {
  const title = normalizeConceptTitle(value, fallback);
  return cleanText(title, fallback);
}

export function mapStrategyCodeToLabel(code: unknown, fallback = '稳步推进'): string {
  const normalizedCode = cleanText(code).toUpperCase();
  return STRATEGY_CODE_LABELS[normalizedCode] || fallback;
}

export function mapRhythmCodeToLabel(code: unknown, fallback = '按当前节奏推进'): string {
  const normalizedCode = cleanText(code).toUpperCase();
  return RHYTHM_CODE_LABELS[normalizedCode] || fallback;
}

export function pickCompactList(values: Array<unknown>, fallback: string[], max = 3): string[] {
  const normalized = values.map((item) => cleanText(item)).filter(Boolean);
  if (normalized.length) {
    return normalized.slice(0, max);
  }
  return fallback.slice(0, max);
}
