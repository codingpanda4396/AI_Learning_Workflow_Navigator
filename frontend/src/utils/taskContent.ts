export interface LearningContentViewModel {
  title: string;
  summary: string;
  keyPoints: string[];
  misconceptions: string[];
  suggestedSequence: string[];
  supplementaryNotes: string[];
}

type JsonRecord = Record<string, unknown>;

function isRecord(value: unknown): value is JsonRecord {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function toText(value: unknown): string {
  if (typeof value === 'string') {
    return value.trim();
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return '';
}

function toStringList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value
      .flatMap((item) => {
        if (typeof item === 'string' || typeof item === 'number') {
          return [String(item).trim()];
        }
        if (isRecord(item)) {
          return [
            toText(item.title) ||
              toText(item.name) ||
              toText(item.label) ||
              toText(item.content) ||
              toText(item.text) ||
              JSON.stringify(item),
          ];
        }
        return [];
      })
      .filter(Boolean);
  }

  const text = toText(value);
  if (!text) {
    return [];
  }

  return text
    .split(/\r?\n|[;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseOutput(output: unknown): unknown {
  if (typeof output !== 'string') {
    return output;
  }

  const trimmed = output.trim();
  if (!trimmed) {
    return '';
  }

  try {
    return JSON.parse(trimmed);
  } catch {
    return trimmed;
  }
}

function pickList(source: JsonRecord, keys: string[]): string[] {
  for (const key of keys) {
    const items = toStringList(source[key]);
    if (items.length) {
      return items;
    }
  }
  return [];
}

function pickText(source: JsonRecord, keys: string[]): string {
  for (const key of keys) {
    const text = toText(source[key]);
    if (text) {
      return text;
    }
  }
  return '';
}

export function normalizeLearningContent(output: unknown, preferredTitle?: string): LearningContentViewModel {
  const parsed = parseOutput(output);

  if (isRecord(parsed)) {
    const title =
      pickText(parsed, ['title', 'taskTitle', 'topic', 'objective']) ||
      preferredTitle ||
      '本次学习内容';
    const summary =
      pickText(parsed, ['summary', 'description', 'taskGoal', 'goal', 'objective', 'content']) ||
      '先理解这一步的关键概念，再按推荐顺序完成学习。';
    const keyPoints = pickList(parsed, ['keyPoints', 'corePoints', 'highlights', 'knowledgePoints', 'points']);
    const misconceptions = pickList(parsed, ['misconceptions', 'commonMistakes', 'pitfalls', 'warnings']);
    const suggestedSequence = pickList(parsed, ['suggestedSequence', 'recommendedOrder', 'steps', 'sequence']);
    const supplementaryNotes = pickList(parsed, ['supplementaryNotes', 'notes', 'tips', 'extra', 'references']);

    return {
      title,
      summary,
      keyPoints: keyPoints.length ? keyPoints : [summary],
      misconceptions,
      suggestedSequence: suggestedSequence.length ? suggestedSequence : ['先通读任务目标', '再梳理核心知识点', '最后用自己的话复述一遍'],
      supplementaryNotes,
    };
  }

  const text = toText(parsed);
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  return {
    title: preferredTitle || '本次学习内容',
    summary: lines[0] || '先完成这一轮学习，再回到总览查看下一步。',
    keyPoints: lines.slice(0, 4).length ? lines.slice(0, 4) : ['围绕当前任务目标完成学习'],
    misconceptions: [],
    suggestedSequence: ['先理解任务目标', '再梳理重点', '确认自己已经能解释关键概念'],
    supplementaryNotes: lines.slice(4),
  };
}
