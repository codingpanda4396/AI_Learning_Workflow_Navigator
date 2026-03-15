/**
 * 诊断结果展示：仅输出用户可读文案，不暴露原始 code。
 */

function norm(code: unknown): string {
  return String(code ?? '').trim().toUpperCase();
}

/** 当前水平 / 阶段类 code → 用户看到的结论 */
const LEVEL_LABELS: Record<string, string> = {
  FOUNDATION_FIRST: '先打牢基础再深入',
  FOUNDATION: '从基础开始更稳',
  DEEP: '已有较好基础，可以往深处走',
  MEDIUM: '处于中间阶段，按节奏推进即可',
  BEGINNER: '刚起步，系统会从最稳的步骤带你',
  INTERMEDIATE: '已有一定积累，下一步会帮你巩固再进阶',
  ADVANCED: '基础较扎实，后续会侧重拔高与综合',
};

/** 学习偏好类 code → 用户文案 */
const PREFERENCE_LABELS: Record<string, string> = {
  TEXT_FIRST: '先看讲解再动手',
  TEXT_WITH_MINI_EXAMPLE: '先看简要讲解和示例再练',
  EXAMPLE_FIRST: '先看例子再总结',
  PRACTICE_FIRST: '先练再纠偏',
  CONCEPT_FIRST: '先理解概念再练习',
  DEEP: '喜欢先吃透再练',
};

/** 时间/目标等 code → 用户文案（仅作兜底，优先用后端 label） */
const TIME_BUDGET_LABELS: Record<string, string> = {
  LIGHT: '时间有限，节奏会偏稳',
  STANDARD: '时间适中，按常规节奏推进',
  INTENSIVE: '时间较充足，可以安排更密集',
};

const GOAL_ORIENTATION_LABELS: Record<string, string> = {
  EXAM: '以应试/考核为主',
  PROJECT: '以能做项目为主',
  CONCEPT: '以理解概念为主',
  BALANCED: '理解与练习并重',
};

export function mapDiagnosisLevelToLabel(code: unknown, backendLabel?: string): string {
  const s = (backendLabel && backendLabel.trim()) || LEVEL_LABELS[norm(code)] || '系统已根据你的回答判断当前起点，后续路径会据此安排。';
  return s;
}

export function mapDiagnosisPreferenceToLabel(code: unknown, backendLabel?: string): string {
  return (backendLabel && backendLabel.trim()) || PREFERENCE_LABELS[norm(code)] || '系统会按通用节奏安排，并在学习中持续微调。';
}

export function mapDiagnosisTimeBudgetToLabel(code: unknown, backendLabel?: string): string {
  return (backendLabel && backendLabel.trim()) || TIME_BUDGET_LABELS[norm(code)] || '系统会采用相对稳妥的节奏。';
}

export function mapDiagnosisGoalToLabel(code: unknown, backendLabel?: string): string {
  return (backendLabel && backendLabel.trim()) || GOAL_ORIENTATION_LABELS[norm(code)] || '会优先从与你目标最相关的步骤开始。';
}

/** 维度 code → 简短中文（用于「依据：你在xxx中的回答」） */
const DIMENSION_LABELS: Record<string, string> = {
  FOUNDATION: '基础',
  EXPERIENCE: '经验',
  GOAL_STYLE: '目标',
  TIME_BUDGET: '时间',
  LEARNING_PREFERENCE: '学习方式',
  TOPIC_CORE: '主题掌握',
  TOPIC_OPERATION: '主题运用',
  TOPIC_CONCEPT: '概念理解',
};

export function mapDimensionToLabel(dimension: unknown): string {
  return DIMENSION_LABELS[norm(dimension)] || '你的回答';
}
