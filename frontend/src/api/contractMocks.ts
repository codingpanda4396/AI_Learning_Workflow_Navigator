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
      sectionLabel: 'Foundation',
      title: 'How solid is your current foundation?',
      description: 'Used to place the starting node.',
      options: [
        { code: 'BEGINNER', label: 'Beginner', order: 1 },
        { code: 'BASIC', label: 'Basic', order: 2 },
      ],
    },
  ],
  nextAction: {
    code: 'PATH_PLAN',
    label: 'Open plan preview',
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
    currentLevel: { code: 'BEGINNER', label: 'Beginner' },
    strengths: ['Keeps a stable learning rhythm'],
    weaknesses: ['Needs concept framing before practice'],
    learningPreference: { code: 'CONCEPT_FIRST', label: 'Concept first' },
    timeBudget: { code: 'STANDARD', label: '4-6 hours / week' },
    goalOrientation: { code: 'PROJECT', label: 'Project delivery' },
  },
  insights: {
    summary: 'You have enough commitment but still need concept scaffolding.',
    planExplanation: 'The plan should start from core prerequisite concepts.',
  },
  nextAction: diagnosisGenerateMock.nextAction,
  fallback: {
    applied: true,
    reasons: ['PROFILE_SUMMARY_TIMEOUT'],
    contentSource: { code: 'RULE_TEMPLATE', label: 'Rule template' },
  },
  metadata: {
    answerCount: 1,
    profileVersion: 2,
  },
} as const;

export const planPreviewRequestMock: LearningPlanRequest = {
  sessionId: 88,
  diagnosisId: '101',
  goalText: 'Build a working chapter-level study plan',
  courseName: 'Learning Architecture',
  chapterName: 'Unified Diagnosis Flow',
  adjustments: {
    intensity: 'STANDARD',
    learningMode: 'EXPLAIN_THEN_PRACTICE',
    prioritizeFoundation: true,
  },
};

export const planPreviewMock = {
  id: 'preview-501',
  status: { code: 'PREVIEW_READY', label: 'Preview ready' },
  previewOnly: true,
  committed: false,
  planSource: { code: 'RULE_ENGINE', label: 'Rule engine' },
  contentSource: { code: 'LLM', label: 'LLM' },
  fallbackApplied: false,
  fallbackReasons: [],
  focuses: ['Stabilize prerequisite concepts', 'Keep stage order explicit'],
  summary: {
    headline: 'Start from the prerequisite concept cluster first',
    recommendedStartNode: {
      id: 'node-1',
      nodeKey: 'node-1',
      displayName: 'Prerequisite cluster',
      nodeName: 'Prerequisite cluster',
    },
    recommendedPace: { code: 'STANDARD', label: 'Standard' },
    estimatedTotalMinutes: 180,
    estimatedNodeCount: 3,
    estimatedStageCount: 4,
  },
  reasons: [
    {
      key: 'foundation',
      label: 'Foundation',
      title: 'Fill the missing prerequisite first',
      description: 'The diagnosis shows the user is blocked by missing conceptual framing.',
    },
  ],
  pathPreview: [
    {
      node: {
        id: 'node-1',
        nodeKey: 'node-1',
        displayName: 'Prerequisite cluster',
        nodeName: 'Prerequisite cluster',
      },
      difficulty: { code: 'FOUNDATION', label: 'Foundation' },
      mastery: 32,
      status: { code: 'WEAK', label: 'Weak' },
      isRecommendedStart: true,
      estimatedNodeMinutes: 45,
      reasonTag: 'Prerequisite gap',
    },
  ],
  taskPreview: [
    {
      stage: { code: 'STRUCTURE', label: 'Structure' },
      title: 'Frame the chapter',
      learningGoal: 'Understand the map before drilling details',
      learnerAction: 'Review the guided concept map',
      aiSupport: 'Generate a concise scaffold and checkpoints',
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
  nextStepNote: 'Confirm to create the formal learning session.',
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
