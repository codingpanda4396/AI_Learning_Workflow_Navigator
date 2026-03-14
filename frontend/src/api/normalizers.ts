import type { LoginResponse } from '@/types/auth';
import type { CodeLabel } from '@/types/common';
import type {
  GrowthDashboard,
  LearningReport,
  NextStepRecommendation,
  ReportQuestionResult,
  WeakPointItem,
} from '@/types/feedback';
import type { QuizQuestion, QuizSnapshot } from '@/types/quiz';
import type {
  LearningPlanPreview,
  LearningPlanRequest,
  PathDifficulty,
  PathMasteryStatus,
  PlanAdjustments,
} from '@/types/learningPlan';
import type { CurrentSessionInfo, SessionOverview } from '@/types/session';
import type { TaskDetail, TaskRunResult } from '@/types/task';
import { toNumber } from '@/utils/format';

function readArray(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).filter(Boolean);
  }
  return [];
}

function readCode(value: unknown): string {
  if (value && typeof value === 'object') {
    return String((value as Record<string, unknown>).code ?? '');
  }
  return String(value ?? '');
}

function readLabel(value: unknown, fallback = ''): string {
  if (value && typeof value === 'object') {
    return String((value as Record<string, unknown>).label ?? (value as Record<string, unknown>).code ?? fallback);
  }
  return String(value ?? fallback);
}

function readCodeLabel(value: unknown, fallback = ''): CodeLabel {
  return {
    code: readCode(value) || fallback,
    label: readLabel(value, fallback),
  };
}

function normalizePlanAdjustments(value: unknown, defaultAdjustments: PlanAdjustments): PlanAdjustments {
  if (!value) {
    return defaultAdjustments;
  }
  if (typeof value === 'string') {
    return {
      ...defaultAdjustments,
      intensity: value as PlanAdjustments['intensity'],
    };
  }
  if (typeof value !== 'object') {
    return defaultAdjustments;
  }
  const row = value as Record<string, unknown>;
  const rawLearningMode = readCode(row.learning_mode ?? row.learningMode) || defaultAdjustments.learningMode;
  const rawIntensity = readCode(row.intensity) || defaultAdjustments.intensity;
  return {
    intensity: rawIntensity as PlanAdjustments['intensity'],
    learningMode: normalizeLearningMode(rawLearningMode),
    prioritizeFoundation: Boolean(
      row.prioritize_foundation
        ?? row.prioritizeFoundation
        ?? row.prefer_prerequisite
        ?? row.preferPrerequisite
        ?? defaultAdjustments.prioritizeFoundation
    ),
  };
}

function normalizeLearningMode(value: string): PlanAdjustments['learningMode'] {
  switch (value) {
    case 'PRACTICE_DRIVEN':
    case 'LEARN_BY_DOING':
      return 'LEARN_BY_DOING';
    case 'LEARN_THEN_PRACTICE':
    case 'MIXED':
    case 'EXPLAIN_THEN_PRACTICE':
    default:
      return 'EXPLAIN_THEN_PRACTICE';
  }
}

function normalizePathDifficulty(value: unknown): PathDifficulty {
  if (typeof value === 'string') {
    if (value === 'FOUNDATION' || value === 'CORE' || value === 'CHALLENGE') {
      return value;
    }
    const numeric = Number(value);
    if (!Number.isNaN(numeric)) {
      return normalizePathDifficulty(numeric);
    }
  }
  if (typeof value === 'number') {
    if (value <= 1) {
      return 'FOUNDATION';
    }
    if (value >= 3) {
      return 'CHALLENGE';
    }
  }
  return 'CORE';
}

function normalizePathMasteryStatus(status: unknown, mastery: unknown): PathMasteryStatus {
  if (typeof status === 'string' && (status === 'WEAK' || status === 'PARTIAL' || status === 'STABLE' || status === 'NEW')) {
    return status;
  }
  const masteryScore = toNumber(mastery);
  if (masteryScore !== undefined) {
    if (masteryScore < 50) {
      return 'WEAK';
    }
    if (masteryScore < 80) {
      return 'PARTIAL';
    }
    return 'STABLE';
  }
  return status === 'READY' ? 'STABLE' : 'PARTIAL';
}

function normalizeWeakPoint(item: Record<string, unknown>): WeakPointItem {
  return {
    nodeId: toNumber(item.node_id ?? item.nodeId) ?? 0,
    nodeName: String(item.node_name ?? item.nodeName ?? '未命名知识点'),
    masteryScore: toNumber(item.mastery_score ?? item.masteryScore),
    trainingAccuracy: toNumber(item.training_accuracy ?? item.trainingAccuracy),
    latestEvaluationScore: toNumber(item.latest_evaluation_score ?? item.latestEvaluationScore),
    attemptCount: toNumber(item.attempt_count ?? item.attemptCount),
    recentErrorTags: readArray(item.recent_error_tags ?? item.recentErrorTags),
    reasons: readArray(item.reasons),
  };
}

export function normalizeLoginResponse(data: Record<string, unknown>): LoginResponse {
  const user = (data.user as Record<string, unknown> | undefined) ?? {};
  return {
    token: String(data.token ?? ''),
    user: {
      id: toNumber(user.id),
      username: String(user.username ?? ''),
    },
  };
}

export function normalizeCurrentSession(data: Record<string, unknown>): CurrentSessionInfo | null {
  const hasActiveSession = Boolean(data.has_active_session ?? data.hasActiveSession);
  const session = (data.session as Record<string, unknown> | undefined) ?? {};
  if (!hasActiveSession || !session.session_id) {
    return null;
  }
  return {
    sessionId: toNumber(session.session_id ?? session.sessionId) ?? 0,
    courseId: String(session.course_id ?? session.courseId ?? ''),
    chapterId: String(session.chapter_id ?? session.chapterId ?? ''),
    goalText: String(session.goal_text ?? session.goalText ?? ''),
    currentNodeId: toNumber(session.current_node_id ?? session.currentNodeId),
    currentStage: String(session.current_stage ?? session.currentStage ?? ''),
    sessionStatus: String(session.session_status ?? session.sessionStatus ?? '') as CurrentSessionInfo['sessionStatus'],
  };
}

export function normalizeOverview(data: Record<string, unknown>): SessionOverview {
  const progress = (data.progress as Record<string, unknown> | undefined) ?? {};
  const nextTask = (data.next_task as Record<string, unknown> | undefined) ?? data.nextTask;
  const timeline = Array.isArray(data.timeline) ? data.timeline : [];
  const masterySummary = (Array.isArray(data.mastery_summary ?? data.masterySummary)
    ? (data.mastery_summary ?? data.masterySummary)
    : []) as Record<string, unknown>[];
  const summary = data.summary && typeof data.summary === 'object' ? (data.summary as Record<string, unknown>) : null;

  return {
    sessionId: toNumber(data.session_id ?? data.sessionId) ?? 0,
    courseId: String(data.course_id ?? data.courseId ?? ''),
    chapterId: String(data.chapter_id ?? data.chapterId ?? ''),
    goalText: String(data.goal_text ?? data.goalText ?? ''),
    currentNodeId: toNumber(data.current_node_id ?? data.currentNodeId),
    currentStage: String(data.current_stage ?? data.currentStage ?? ''),
    sessionStatus: String(data.session_status ?? data.sessionStatus ?? '') as SessionOverview['sessionStatus'],
    timeline: timeline.map((item) => {
      const row = item as Record<string, unknown>;
      return {
        taskId: toNumber(row.task_id ?? row.taskId) ?? 0,
        stage: String(row.stage ?? ''),
        nodeId: toNumber(row.node_id ?? row.nodeId),
        status: String(row.status ?? ''),
      };
    }),
    nextTask: nextTask
      ? {
          taskId: toNumber((nextTask as Record<string, unknown>).task_id ?? (nextTask as Record<string, unknown>).taskId) ?? 0,
          stage: String((nextTask as Record<string, unknown>).stage ?? ''),
          nodeId: toNumber((nextTask as Record<string, unknown>).node_id ?? (nextTask as Record<string, unknown>).nodeId),
        }
      : null,
    masterySummary: masterySummary.map((row) => {
      return {
        nodeId: toNumber(row.node_id ?? row.nodeId) ?? 0,
        nodeName: String(row.node_name ?? row.nodeName ?? ''),
        masteryValue: toNumber(row.mastery_value ?? row.masteryValue),
      };
    }),
    progress: {
      completedTaskCount: toNumber(progress.completed_task_count ?? progress.completedTaskCount) ?? 0,
      totalTaskCount: toNumber(progress.total_task_count ?? progress.totalTaskCount) ?? 0,
      completionRate: toNumber(progress.completion_rate ?? progress.completionRate) ?? 0,
    },
    summary: summary
      ? {
          currentTaskTitle: String(summary.current_task_title ?? summary.currentTaskTitle ?? ''),
          currentTaskDescription: String(summary.current_task_description ?? summary.currentTaskDescription ?? ''),
          nextStepHint: String(summary.next_step_hint ?? summary.nextStepHint ?? ''),
          primaryActionLabel: String(summary.primary_action_label ?? summary.primaryActionLabel ?? ''),
          primaryActionPath: String(summary.primary_action_path ?? summary.primaryActionPath ?? ''),
          recentReportSummary: String(summary.recent_report_summary ?? summary.recentReportSummary ?? ''),
        }
      : undefined,
  };
}

export function normalizeTaskDetail(data: Record<string, unknown>): TaskDetail {
  return {
    taskId: toNumber(data.task_id ?? data.taskId) ?? 0,
    sessionId: toNumber(data.session_id ?? data.sessionId),
    nodeId: toNumber(data.node_id ?? data.nodeId),
    nodeName: String(data.node_name ?? data.nodeName ?? ''),
    stage: String(data.stage ?? ''),
    objective: String(data.objective ?? ''),
    status: String(data.status ?? ''),
    hasOutput: Boolean(data.has_output ?? data.hasOutput),
    output: data.output,
  };
}

export function normalizeTaskRun(data: Record<string, unknown>): TaskRunResult {
  return {
    taskId: toNumber(data.task_id ?? data.taskId) ?? 0,
    stage: String(data.stage ?? ''),
    nodeId: toNumber(data.node_id ?? data.nodeId),
    status: String(data.status ?? ''),
    generationMode: String(data.generation_mode ?? data.generationMode ?? ''),
    generationReason: String(data.generation_reason ?? data.generationReason ?? ''),
    output: data.output,
  };
}

function normalizeQuestion(item: Record<string, unknown>): QuizQuestion {
  const rawOptions = item.options;
  let options: string[] = [];
  if (Array.isArray(rawOptions)) {
    options = rawOptions.map((entry) => {
      if (typeof entry === 'string') {
        return entry;
      }
      if (entry && typeof entry === 'object') {
        const obj = entry as Record<string, unknown>;
        return String(obj.label ?? obj.text ?? obj.value ?? JSON.stringify(obj));
      }
      return String(entry);
    });
  }

  return {
    questionId: toNumber(item.question_id ?? item.questionId) ?? 0,
    type: String(item.type ?? item.question_type ?? item.questionType ?? ''),
    stem: String(item.stem ?? ''),
    options,
    evaluationFocus: String(item.evaluation_focus ?? item.evaluationFocus ?? item.explanation ?? ''),
    difficulty: String(item.difficulty ?? ''),
    status: String(item.status ?? '') as QuizQuestion['status'],
  };
}

export function normalizeQuizSnapshot(data: Record<string, unknown>): QuizSnapshot {
  const questions = Array.isArray(data.questions) ? data.questions : [];
  return {
    sessionId: toNumber(data.session_id ?? data.sessionId) ?? 0,
    taskId: toNumber(data.task_id ?? data.taskId),
    quizId: toNumber(data.quiz_id ?? data.quizId),
    generationStatus: String(data.generation_status ?? data.generationStatus ?? '') as QuizSnapshot['generationStatus'],
    quizStatus: String(data.quiz_status ?? data.quizStatus ?? data.status ?? '') as QuizSnapshot['quizStatus'],
    questionCount: toNumber(data.question_count ?? data.questionCount),
    answeredCount: toNumber(data.answered_count ?? data.answeredCount),
    failureReason: String(data.failure_reason ?? data.failureReason ?? ''),
    retryable: Boolean(data.retryable),
    questions: questions.map((item) => normalizeQuestion(item as Record<string, unknown>)),
  };
}

function normalizeQuestionResult(item: Record<string, unknown>): ReportQuestionResult {
  return {
    questionId: toNumber(item.question_id ?? item.questionId ?? item.practice_item_id ?? item.practiceItemId) ?? 0,
    type: String(item.type ?? item.question_type ?? item.questionType ?? ''),
    stem: String(item.stem ?? ''),
    userAnswer: String(item.user_answer ?? item.userAnswer ?? ''),
    score: toNumber(item.score),
    correct: item.correct === undefined ? undefined : Boolean(item.correct),
    feedback: String(item.feedback ?? ''),
    errorTags: readArray(item.error_tags ?? item.errorTags),
  };
}

function normalizeNextStep(raw: unknown): NextStepRecommendation | null {
  if (!raw || typeof raw !== 'object') {
    return null;
  }
  const item = raw as Record<string, unknown>;
  return {
    recommendedAction: String(item.recommended_action ?? item.recommendedAction ?? ''),
    reason: String(item.reason ?? ''),
    targetNodeId: toNumber(item.target_node_id ?? item.targetNodeId),
    targetNodeName: String(item.target_node_name ?? item.targetNodeName ?? ''),
    targetTaskType: String(item.target_task_type ?? item.targetTaskType ?? ''),
    confidence: toNumber(item.confidence),
  };
}

export function normalizeSessionReport(data: Record<string, unknown>): LearningReport {
  const weakPoints = (Array.isArray(data.weak_points ?? data.weakPoints)
    ? (data.weak_points ?? data.weakPoints)
    : []) as Record<string, unknown>[];
  const questionResultsRaw = (Array.isArray(data.question_results ?? data.questionResults)
    ? (data.question_results ?? data.questionResults)
    : []) as Record<string, unknown>[];
  return {
    sessionId: toNumber(data.session_id ?? data.sessionId) ?? 0,
    taskId: toNumber(data.task_id ?? data.taskId),
    nodeId: toNumber(data.node_id ?? data.nodeId),
    nodeName: String(data.node_name ?? data.nodeName ?? ''),
    stageCode: String(data.stage_code ?? data.stageCode ?? ''),
    stageLabel: String(data.stage_label ?? data.stageLabel ?? ''),
    overallScore: toNumber(data.overall_score ?? data.overallScore),
    overallAccuracy: toNumber(data.overall_accuracy ?? data.overallAccuracy),
    correctCount: toNumber(data.correct_count ?? data.correctCount),
    questionCount: toNumber(data.question_count ?? data.questionCount),
    diagnosisSummary: String(data.diagnosis_summary ?? data.diagnosisSummary ?? ''),
    overallSummary: String(data.overall_summary ?? data.overallSummary ?? ''),
    strengths: readArray(data.strengths),
    weaknesses: readArray(data.weaknesses),
    reviewFocus: readArray(data.review_focus ?? data.reviewFocus),
    weakPoints: weakPoints.map((item) => normalizeWeakPoint(item)),
    questionResults: questionResultsRaw.map((item) => normalizeQuestionResult(item)),
    recommendedAction: String(data.recommended_action ?? data.recommendedAction ?? ''),
    suggestedNextAction: String(data.suggested_next_action ?? data.suggestedNextAction ?? ''),
    selectedAction: String(data.selected_action ?? data.selectedAction ?? ''),
    nextRoundAdvice: String(data.next_round_advice ?? data.nextRoundAdvice ?? ''),
    nextStep: normalizeNextStep(data.next_step ?? data.nextStep),
    growthRecorded: Boolean(data.growth_recorded ?? data.growthRecorded),
    source: String(data.source ?? ''),
  };
}

export function normalizeGrowthDashboard(data: Record<string, unknown>): GrowthDashboard {
  const recent = (data.recent_performance ?? data.recentPerformance ?? {}) as Record<string, unknown>;
  const masteryNodes = (Array.isArray(data.mastery_nodes ?? data.masteryNodes)
    ? (data.mastery_nodes ?? data.masteryNodes)
    : []) as Record<string, unknown>[];

  return {
    sessionId: toNumber(data.session_id ?? data.sessionId) ?? 0,
    courseId: String(data.course_id ?? data.courseId ?? ''),
    chapterId: String(data.chapter_id ?? data.chapterId ?? ''),
    learnedNodeCount: toNumber(data.learned_node_count ?? data.learnedNodeCount),
    masteredNodeCount: toNumber(data.mastered_node_count ?? data.masteredNodeCount),
    averageMasteryScore: toNumber(data.average_mastery_score ?? data.averageMasteryScore),
    currentNodeId: toNumber(data.current_node_id ?? data.currentNodeId),
    currentNodeName: String(data.current_node_name ?? data.currentNodeName ?? ''),
    currentStageCode: String(data.current_stage_code ?? data.currentStageCode ?? ''),
    currentStageLabel: String(data.current_stage_label ?? data.currentStageLabel ?? ''),
    topWeakPoints: readArray(data.top_weak_points ?? data.topWeakPoints),
    recentPerformance: {
      attemptCount: toNumber(recent.attempt_count ?? recent.attemptCount),
      averageScore: toNumber(recent.average_score ?? recent.averageScore),
      latestScore: toNumber(recent.latest_score ?? recent.latestScore),
      topErrorTags: readArray(recent.top_error_tags ?? recent.topErrorTags),
    },
    recommendedNextStep: normalizeNextStep(data.recommended_next_step ?? data.recommendedNextStep),
    masteryNodes: masteryNodes.map((row) => {
      return {
        nodeId: toNumber(row.node_id ?? row.nodeId) ?? 0,
        nodeName: String(row.node_name ?? row.nodeName ?? ''),
        masteryScore: toNumber(row.mastery_score ?? row.masteryScore),
        masteryStatus: String(row.mastery_status ?? row.masteryStatus ?? ''),
        current: Boolean(row.is_current ?? row.current),
        recommended: Boolean(row.is_recommended ?? row.recommended),
      };
    }),
  };
}

export function normalizeLearningPlanPreview(data: Record<string, unknown>, request: LearningPlanRequest): LearningPlanPreview {
  const summary = (data.summary as Record<string, unknown> | undefined) ?? {};
  const context = (data.context as Record<string, unknown> | undefined) ?? {};
  const metadata = (data.metadata as Record<string, unknown> | undefined) ?? {};
  const adjustments = normalizePlanAdjustments(data.adjustments, request.adjustments);
  const reasons = (Array.isArray(data.reasons) ? data.reasons : []) as Record<string, unknown>[];
  const pathNodes = (Array.isArray(data.path_preview ?? data.pathPreview ?? data.path_nodes ?? data.pathNodes)
    ? (data.path_preview ?? data.pathPreview ?? data.path_nodes ?? data.pathNodes)
    : []) as Record<string, unknown>[];
  const taskPreviews = (Array.isArray(data.task_preview ?? data.taskPreview ?? data.task_previews ?? data.taskPreviews)
    ? (data.task_preview ?? data.taskPreview ?? data.task_previews ?? data.taskPreviews)
    : []) as Record<string, unknown>[];

  return {
    previewId: toNumber(data.preview_id ?? data.previewId ?? data.plan_id ?? data.planId) ?? 0,
    status: String(data.status ?? 'PREVIEW_READY') as LearningPlanPreview['status'],
    previewOnly: Boolean(data.preview_only ?? data.previewOnly ?? true),
    committed: Boolean(data.committed ?? false),
    summary: {
      recommendedStartNode: {
        id: String(
          (summary.recommended_start_node as Record<string, unknown> | undefined)?.id
            ?? (summary.recommendedStartNode as Record<string, unknown> | undefined)?.id
            ?? summary.recommended_start_node_id
            ?? summary.recommendedStartNodeId
            ?? ''
        ),
        nodeKey: String(
          (summary.recommended_start_node as Record<string, unknown> | undefined)?.node_key
            ?? (summary.recommended_start_node as Record<string, unknown> | undefined)?.nodeKey
            ?? (summary.recommendedStartNode as Record<string, unknown> | undefined)?.nodeKey
            ?? summary.recommended_start_node_id
            ?? summary.recommendedStartNodeId
            ?? ''
        ),
        nodeName: String(
          (summary.recommended_start_node as Record<string, unknown> | undefined)?.node_name
            ?? (summary.recommended_start_node as Record<string, unknown> | undefined)?.nodeName
            ?? (summary.recommendedStartNode as Record<string, unknown> | undefined)?.nodeName
            ?? summary.recommended_start_node_name
            ?? summary.recommendedStartNodeName
            ?? summary.recommended_start
            ?? summary.recommendedStart
            ?? ''
        ),
      },
      recommendedRhythm: (readCode(summary.recommended_pace ?? summary.recommendedPace ?? summary.recommended_rhythm ?? summary.recommendedRhythm) || request.adjustments.intensity) as LearningPlanPreview['summary']['recommendedRhythm'],
      recommendedRhythmLabel: readLabel(summary.recommended_pace ?? summary.recommendedPace ?? summary.recommended_rhythm ?? summary.recommendedRhythm),
      estimatedTotalMinutes: toNumber(summary.estimated_total_minutes ?? summary.estimatedTotalMinutes ?? summary.estimated_minutes ?? summary.estimatedMinutes) ?? 0,
      estimatedKnowledgeCount: toNumber(summary.estimated_node_count ?? summary.estimatedNodeCount ?? summary.estimated_knowledge_count ?? summary.estimatedKnowledgeCount) ?? 0,
      stageCount: toNumber(summary.estimated_stage_count ?? summary.estimatedStageCount ?? summary.stage_count ?? summary.stageCount) ?? 4,
      personalizedHeadline: String(summary.headline ?? summary.personalized_headline ?? summary.personalizedHeadline ?? ''),
      personalizedSummary: String(context.diagnosis_summary ?? context.diagnosisSummary ?? data.learner_profile_summary ?? data.learnerProfileSummary ?? data.diagnosis_summary ?? data.diagnosisSummary ?? ''),
    },
    reasons: reasons.map((item, index) => ({
      key: String(item.key ?? item.type ?? `reason-${index + 1}`),
      title: String(item.title ?? ''),
      label: String(item.label ?? item.type ?? ''),
      description: String(item.description ?? ''),
    })),
    pathNodes: pathNodes.map((item, index) => {
      const reasonTags = readArray(item.reason_tags ?? item.reasonTags).length
        ? readArray(item.reason_tags ?? item.reasonTags)
        : item.reasonTag
          ? [String(item.reasonTag)]
          : [];
      const isStartingPoint = Boolean(item.is_recommended_start ?? item.isRecommendedStart ?? item.is_starting_point ?? item.isStartingPoint);
      const explicitPrerequisite = item.is_prerequisite ?? item.isPrerequisite;
      const explicitFocus = item.is_focus ?? item.isFocus;
      const inferredPrerequisite = reasonTags.some((tag) => /prerequisite|前置|基础/i.test(tag));

      return {
        node: {
          id: String(
            (item.node as Record<string, unknown> | undefined)?.id
              ?? item.node_id
              ?? item.nodeId
              ?? item.id
              ?? `path-${index + 1}`
          ),
          nodeKey: String(
            (item.node as Record<string, unknown> | undefined)?.node_key
              ?? (item.node as Record<string, unknown> | undefined)?.nodeKey
              ?? item.node_id
              ?? item.nodeId
              ?? item.id
              ?? `path-${index + 1}`
          ),
          nodeName: String(
            (item.node as Record<string, unknown> | undefined)?.node_name
              ?? (item.node as Record<string, unknown> | undefined)?.nodeName
              ?? item.node_name
              ?? item.nodeName
              ?? item.name
              ?? ''
          ),
        },
        masteryStatus: normalizePathMasteryStatus(readCode(item.status ?? item.mastery_status ?? item.masteryStatus), item.mastery),
        difficulty: normalizePathDifficulty(readCode(item.difficulty) || item.difficulty),
        reasonTags,
        estimatedNodeMinutes: toNumber(item.estimated_node_minutes ?? item.estimatedNodeMinutes ?? item.estimated_minutes ?? item.estimatedMinutes) ?? 0,
        isStartingPoint,
        isPrerequisite: explicitPrerequisite === undefined ? inferredPrerequisite : Boolean(explicitPrerequisite),
        isFocus: explicitFocus === undefined ? !isStartingPoint && !inferredPrerequisite : Boolean(explicitFocus),
      };
    }),
    taskPreviews: taskPreviews.map((item) => ({
      stage: readCode(item.stage) as LearningPlanPreview['taskPreviews'][number]['stage'],
      title: String(item.title ?? ''),
      learningGoal: String(item.learning_goal ?? item.learningGoal ?? item.goal ?? item.stage_goal ?? item.stageGoal ?? ''),
      learnerAction: String(item.learner_action ?? item.learnerAction ?? ''),
      aiSupport: String(item.ai_support ?? item.aiSupport ?? ''),
      estimatedTaskMinutes: toNumber(item.estimated_task_minutes ?? item.estimatedTaskMinutes ?? item.estimated_minutes ?? item.estimatedMinutes) ?? 0,
    })),
    adjustments,
    context: {
      sessionId: toNumber(context.session_id ?? context.sessionId ?? request.sessionId),
      diagnosisId: String(context.diagnosis_id ?? context.diagnosisId ?? data.diagnosis_id ?? data.diagnosisId ?? request.diagnosisId),
      goalText: String(context.goal_text ?? context.goalText ?? data.goal_text ?? data.goalText ?? request.goalText),
      courseName: String(context.course_name ?? context.courseName ?? data.course_name ?? data.courseName ?? data.course_id ?? data.courseId ?? request.courseName),
      chapterName: String(context.chapter_name ?? context.chapterName ?? data.chapter_name ?? data.chapterName ?? data.chapter_id ?? data.chapterId ?? request.chapterName),
      diagnosisSummary: String(context.diagnosis_summary ?? context.diagnosisSummary ?? data.diagnosis_summary ?? data.diagnosisSummary ?? ''),
    },
    nextStepNote: String(data.next_step_note ?? data.nextStepNote ?? ''),
    planSource: data.plan_source || data.planSource ? readCodeLabel(data.plan_source ?? data.planSource) : undefined,
    contentSource: data.content_source || data.contentSource ? readCodeLabel(data.content_source ?? data.contentSource) : undefined,
    fallbackApplied: Boolean(data.fallback_applied ?? data.fallbackApplied),
    fallbackReasons: readArray(data.fallback_reasons ?? data.fallbackReasons),
    metadata: {
      schemaVersion: String(metadata.schema_version ?? metadata.schemaVersion ?? ''),
      persistedPreview: Boolean(metadata.persisted_preview ?? metadata.persistedPreview),
      estimatedTotalMinutesScope: String(metadata.estimated_total_minutes_scope ?? metadata.estimatedTotalMinutesScope ?? ''),
      estimatedNodeMinutesScope: String(metadata.estimated_node_minutes_scope ?? metadata.estimatedNodeMinutesScope ?? ''),
      estimatedTaskMinutesScope: String(metadata.estimated_task_minutes_scope ?? metadata.estimatedTaskMinutesScope ?? ''),
    },
  };
}
