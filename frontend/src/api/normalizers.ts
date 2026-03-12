import type { LoginResponse } from '@/types/auth';
import type {
  GrowthDashboard,
  LearningReport,
  NextStepRecommendation,
  ReportQuestionResult,
  WeakPointItem,
} from '@/types/feedback';
import type { QuizQuestion, QuizSnapshot } from '@/types/quiz';
import type { LearningPlanPreview, LearningPlanRequest, PlanAdjustments } from '@/types/learningPlan';
import type { CurrentSessionInfo, SessionOverview } from '@/types/session';
import type { TaskDetail, TaskRunResult } from '@/types/task';
import { toNumber } from '@/utils/format';

function readArray(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).filter(Boolean);
  }
  return [];
}

function normalizePlanAdjustments(value: unknown, fallback: PlanAdjustments): PlanAdjustments {
  if (!value || typeof value !== 'object') {
    return fallback;
  }
  const row = value as Record<string, unknown>;
  return {
    intensity: String(row.intensity ?? fallback.intensity) as PlanAdjustments['intensity'],
    learningMode: String(row.learning_mode ?? row.learningMode ?? fallback.learningMode) as PlanAdjustments['learningMode'],
    prioritizeFoundation: Boolean(row.prioritize_foundation ?? row.prioritizeFoundation ?? fallback.prioritizeFoundation),
  };
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
  };
}

export function normalizeOverview(data: Record<string, unknown>): SessionOverview {
  const progress = (data.progress as Record<string, unknown> | undefined) ?? {};
  const nextTask = (data.next_task as Record<string, unknown> | undefined) ?? data.nextTask;
  const timeline = Array.isArray(data.timeline) ? data.timeline : [];
  const masterySummary = (Array.isArray(data.mastery_summary ?? data.masterySummary)
    ? (data.mastery_summary ?? data.masterySummary)
    : []) as Record<string, unknown>[];

  return {
    sessionId: toNumber(data.session_id ?? data.sessionId) ?? 0,
    courseId: String(data.course_id ?? data.courseId ?? ''),
    chapterId: String(data.chapter_id ?? data.chapterId ?? ''),
    goalText: String(data.goal_text ?? data.goalText ?? ''),
    currentNodeId: toNumber(data.current_node_id ?? data.currentNodeId),
    currentStage: String(data.current_stage ?? data.currentStage ?? ''),
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
    status: String(item.status ?? ''),
  };
}

export function normalizeQuizSnapshot(data: Record<string, unknown>): QuizSnapshot {
  const questions = Array.isArray(data.questions) ? data.questions : [];
  return {
    sessionId: toNumber(data.session_id ?? data.sessionId) ?? 0,
    taskId: toNumber(data.task_id ?? data.taskId),
    quizId: toNumber(data.quiz_id ?? data.quizId),
    generationStatus: String(data.generation_status ?? data.generationStatus ?? ''),
    quizStatus: String(data.quiz_status ?? data.quizStatus ?? data.status ?? ''),
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

export function mergeReportPayloads(payloads: {
  feedback?: Record<string, unknown> | null;
  report?: Record<string, unknown> | null;
  weakPoints?: Record<string, unknown> | null;
}): LearningReport {
  const feedback = payloads.feedback ?? {};
  const report = payloads.report ?? {};
  const weakPayload = payloads.weakPoints ?? {};
  const weakPoints = (Array.isArray(report.weak_points ?? report.weakPoints)
    ? (report.weak_points ?? report.weakPoints)
    : Array.isArray(weakPayload.weak_nodes ?? weakPayload.weakNodes)
      ? (weakPayload.weak_nodes ?? weakPayload.weakNodes)
      : []) as Record<string, unknown>[];
  const questionResultsRaw = (Array.isArray(report.question_results ?? report.questionResults)
    ? (report.question_results ?? report.questionResults)
    : Array.isArray(feedback.question_results ?? feedback.questionResults)
      ? (feedback.question_results ?? feedback.questionResults)
      : []) as Record<string, unknown>[];

  return {
    sessionId: toNumber(report.session_id ?? feedback.session_id ?? report.sessionId ?? feedback.sessionId) ?? 0,
    taskId: toNumber(report.task_id ?? feedback.task_id ?? report.taskId ?? feedback.taskId),
    nodeId: toNumber(report.node_id ?? report.nodeId),
    nodeName: String(report.node_name ?? report.nodeName ?? ''),
    stageCode: String(report.stage_code ?? report.stageCode ?? ''),
    stageLabel: String(report.stage_label ?? report.stageLabel ?? ''),
    overallScore: toNumber(report.overall_score ?? report.overallScore),
    overallAccuracy: toNumber(report.overall_accuracy ?? report.overallAccuracy),
    correctCount: toNumber(report.correct_count ?? report.correctCount),
    questionCount: toNumber(report.question_count ?? report.questionCount ?? feedback.question_count),
    diagnosisSummary: String(report.diagnosis_summary ?? report.diagnosisSummary ?? ''),
    overallSummary: String(feedback.overall_summary ?? feedback.overallSummary ?? ''),
    strengths: readArray(report.strengths ?? feedback.strengths),
    weaknesses: readArray(report.weaknesses ?? feedback.weaknesses),
    reviewFocus: readArray(report.review_focus ?? report.reviewFocus ?? feedback.review_focus ?? feedback.reviewFocus),
    weakPoints: weakPoints.map((item) => normalizeWeakPoint(item)),
    questionResults: questionResultsRaw.map((item) => normalizeQuestionResult(item)),
    recommendedAction: String(feedback.recommended_action ?? feedback.recommendedAction ?? ''),
    suggestedNextAction: String(feedback.suggested_next_action ?? feedback.suggestedNextAction ?? ''),
    selectedAction: String(feedback.selected_action ?? feedback.selectedAction ?? ''),
    nextRoundAdvice: String(feedback.next_round_advice ?? feedback.nextRoundAdvice ?? ''),
    nextStep: normalizeNextStep(report.next_step ?? report.nextStep),
    growthRecorded: Boolean(report.growth_recorded ?? report.growthRecorded),
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
  const adjustments = normalizePlanAdjustments(data.adjustments, request.adjustments);
  const reasons = (Array.isArray(data.reasons) ? data.reasons : []) as Record<string, unknown>[];
  const pathNodes = (Array.isArray(data.path_nodes ?? data.pathNodes) ? (data.path_nodes ?? data.pathNodes) : []) as Record<string, unknown>[];
  const taskPreviews = (Array.isArray(data.task_previews ?? data.taskPreviews)
    ? (data.task_previews ?? data.taskPreviews)
    : []) as Record<string, unknown>[];

  return {
    summary: {
      recommendedStart: String(summary.recommended_start ?? summary.recommendedStart ?? ''),
      recommendedRhythm: String(summary.recommended_rhythm ?? summary.recommendedRhythm ?? request.adjustments.intensity) as LearningPlanPreview['summary']['recommendedRhythm'],
      estimatedMinutes: toNumber(summary.estimated_minutes ?? summary.estimatedMinutes) ?? 0,
      estimatedKnowledgeCount: toNumber(summary.estimated_knowledge_count ?? summary.estimatedKnowledgeCount) ?? 0,
      stageCount: toNumber(summary.stage_count ?? summary.stageCount) ?? 4,
      personalizedHeadline: String(summary.personalized_headline ?? summary.personalizedHeadline ?? ''),
      personalizedSummary: String(summary.personalized_summary ?? summary.personalizedSummary ?? ''),
    },
    reasons: reasons.map((item, index) => ({
      key: String(item.key ?? `reason-${index + 1}`),
      title: String(item.title ?? ''),
      label: String(item.label ?? ''),
      description: String(item.description ?? ''),
    })),
    pathNodes: pathNodes.map((item, index) => ({
      id: String(item.id ?? `path-${index + 1}`),
      name: String(item.name ?? ''),
      masteryStatus: String(item.mastery_status ?? item.masteryStatus ?? 'PARTIAL') as LearningPlanPreview['pathNodes'][number]['masteryStatus'],
      difficulty: String(item.difficulty ?? 'CORE') as LearningPlanPreview['pathNodes'][number]['difficulty'],
      reasonTags: readArray(item.reason_tags ?? item.reasonTags),
      estimatedMinutes: toNumber(item.estimated_minutes ?? item.estimatedMinutes) ?? 0,
      isStartingPoint: Boolean(item.is_starting_point ?? item.isStartingPoint),
      isPrerequisite: Boolean(item.is_prerequisite ?? item.isPrerequisite),
      isFocus: Boolean(item.is_focus ?? item.isFocus),
    })),
    taskPreviews: taskPreviews.map((item) => ({
      stage: String(item.stage ?? '') as LearningPlanPreview['taskPreviews'][number]['stage'],
      stageGoal: String(item.stage_goal ?? item.stageGoal ?? ''),
      learnerAction: String(item.learner_action ?? item.learnerAction ?? ''),
      aiSupport: String(item.ai_support ?? item.aiSupport ?? ''),
      estimatedMinutes: toNumber(item.estimated_minutes ?? item.estimatedMinutes) ?? 0,
    })),
    adjustments,
    goalText: String(data.goal_text ?? data.goalText ?? request.goalText),
    courseId: String(data.course_id ?? data.courseId ?? request.courseId),
    chapterId: String(data.chapter_id ?? data.chapterId ?? request.chapterId),
    diagnosisSummary: String(data.diagnosis_summary ?? data.diagnosisSummary ?? ''),
    nextStepNote: String(data.next_step_note ?? data.nextStepNote ?? ''),
  };
}
