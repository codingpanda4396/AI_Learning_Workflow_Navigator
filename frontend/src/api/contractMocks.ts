import { normalizeDiagnosisGenerateResponse, normalizeDiagnosisSubmitResponse, normalizeLearningPlanPreview } from '@/api/normalizers';
import type { LearningPlanRequest } from '@/types/learningPlan';

export const diagnosisGenerateMock = {
  diagnosisId: '101',
  sessionId: '88',
  status: 'GENERATED',
  questions: [
    {
      questionId: 'foundation',
      dimension: 'FOUNDATION',
      type: 'SINGLE_CHOICE',
      required: true,
      sectionLabel: '基础',
      title: '你在这个主题上的基础如何？',
      description: '用于确定起始节点。',
      options: [
        { code: 'BEGINNER', label: '入门', order: 1 },
        { code: 'BASIC', label: '基础', order: 2 },
      ],
    },
  ],
  nextAction: {
    code: 'PATH_PLAN',
    label: '打开学习计划预览',
    target: {
      route: '/plan',
      params: {
        sessionId: 88,
        diagnosisId: '101',
      },
    },
  },
  fallback: {
    applied: false,
    reasons: [],
    contentSource: {
      code: 'LLM',
      label: 'LLM',
    },
  },
  metadata: {
    questionCount: 1,
  },
} as const;

export const diagnosisSubmitMock = {
  diagnosisId: '101',
  sessionId: '88',
  status: 'SUBMITTED',
  capabilityProfile: {
    currentLevel: { code: 'BEGINNER', label: '入门' },
    strengths: ['能保持稳定的学习节奏'],
    weaknesses: ['练习前需要概念框架支撑'],
    learningPreference: { code: 'CONCEPT_FIRST', label: '概念优先' },
    timeBudget: { code: 'STANDARD', label: '每周 4-6 小时' },
    goalOrientation: { code: 'PROJECT', label: '项目交付' },
  },
  insights: {
    summary: '你有足够投入度，但仍需要概念脚手架支撑。',
    planExplanation: '计划应从核心前置概念开始。',
  },
  nextAction: diagnosisGenerateMock.nextAction,
  fallback: {
    applied: true,
    reasons: ['PROFILE_SUMMARY_TIMEOUT'],
    contentSource: { code: 'RULE_TEMPLATE', label: '规则模板' },
  },
  metadata: {
    answerCount: 1,
    profileVersion: 2,
  },
  reasoningSteps: [
    {
      dimension: 'FOUNDATION',
      questionId: 'foundation',
      questionTitle: '你在这个主题上的基础如何？',
      selectedAnswerLabel: '基础',
      inferredConclusion: '你已有一定基础，但仍需通过阶段训练巩固稳定性。',
    },
  ],
  strengthSources: [
    {
      label: '能保持稳定的学习节奏',
      dimension: 'TIME_BUDGET',
      sourceQuestionId: 'time_budget',
    },
  ],
  weaknessSources: [
    {
      label: '练习前需要概念框架支撑',
      dimension: 'FOUNDATION',
      sourceQuestionId: 'foundation',
    },
  ],
} as const;

export const planPreviewRequestMock: LearningPlanRequest = {
  sessionId: 88,
  diagnosisId: '101',
  goalText: '建立可用的章节级学习计划',
  courseName: '学习架构',
  chapterName: '统一诊断流程',
  adjustments: {
    intensity: 'STANDARD',
    learningMode: 'EXPLAIN_THEN_PRACTICE',
    prioritizeFoundation: true,
  },
};

export const planPreviewMock = {
  id: 'preview-501',
  status: { code: 'PREVIEW_READY', label: '预览已就绪' },
  previewOnly: true,
  committed: false,
  planSource: { code: 'RULE_ENGINE', label: '规则引擎' },
  contentSource: { code: 'LLM', label: 'LLM' },
  fallbackApplied: false,
  fallbackReasons: [],
  focuses: ['巩固前置概念', '保持阶段顺序清晰'],
  summary: {
    headline: '先从前置概念簇开始',
    recommendedStartNode: {
      id: 'node-1',
      nodeKey: 'node-1',
      displayName: '前置概念簇',
      nodeName: '前置概念簇',
    },
    recommendedPace: { code: 'STANDARD', label: '标准' },
    estimatedTotalMinutes: 180,
    estimatedNodeCount: 3,
    estimatedStageCount: 4,
  },
  reasons: [
    {
      key: 'foundation',
      label: '基础',
      title: '先补齐缺失的前置知识',
      description: '诊断显示用户因缺少概念框架而受阻。',
    },
  ],
  whyStartHere: '系统建议从前置概念簇开始，因为当前薄弱点会影响后续推进。',
  keyWeaknesses: ['前置概念薄弱', '边界条件处理'],
  priorityNodes: [
    {
      nodeId: 'node-1',
      title: '前置概念簇',
      reason: '这是当前最影响后续学习推进的起点。',
    },
  ],
  pathPreview: [
    {
      node: {
        id: 'node-1',
        nodeKey: 'node-1',
        displayName: '前置概念簇',
        nodeName: '前置概念簇',
      },
      difficulty: { code: 'FOUNDATION', label: '基础' },
      mastery: 32,
      status: { code: 'WEAK', label: '薄弱' },
      isRecommendedStart: true,
      estimatedNodeMinutes: 45,
      reasonTag: '前置知识缺口',
    },
  ],
  taskPreview: [
    {
      stage: { code: 'STRUCTURE', label: '搭建结构' },
      title: '搭建章节框架',
      learningGoal: '在深入细节前先理解整体脉络',
      learnerAction: '复习引导式概念图',
      aiSupport: '生成简明脚手架与检查点',
      estimatedTaskMinutes: 25,
    },
  ],
  adjustments: {
    intensity: 'STANDARD',
    learningMode: 'LEARN_THEN_PRACTICE',
    prioritizeFoundation: true,
  },
  context: {
    sessionId: 88,
    diagnosisId: '101',
    goalText: planPreviewRequestMock.goalText,
    courseName: planPreviewRequestMock.courseName,
    chapterName: planPreviewRequestMock.chapterName,
    diagnosisSummary: diagnosisSubmitMock.insights.summary,
  },
  nextStepNote: '确认后将创建正式学习会话。',
  metadata: {
    schemaVersion: '2026-03-14',
    persistedPreview: true,
    estimatedTotalMinutesScope: 'path_preview_total',
    estimatedNodeMinutesScope: 'per_path_node',
    estimatedTaskMinutesScope: 'per_stage_task',
  },
} as const;

export const normalizedDiagnosisGenerateMock = normalizeDiagnosisGenerateResponse(diagnosisGenerateMock as unknown as Record<string, unknown>);
export const normalizedDiagnosisSubmitMock = normalizeDiagnosisSubmitResponse(diagnosisSubmitMock as unknown as Record<string, unknown>);
export const normalizedPlanPreviewMock = normalizeLearningPlanPreview(planPreviewMock as unknown as Record<string, unknown>, planPreviewRequestMock);
