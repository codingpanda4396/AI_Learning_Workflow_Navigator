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
  CapabilityProfile,
  DiagnosisGenerateResponse,
  DiagnosisMetadata,
  DiagnosisNextAction,
  DiagnosisQuestion,
  DiagnosisSubmitResponse,
} from '@/types/diagnosis';
import type {
  LearningPlanPreview,
  LearningPlanRequest,
  LearningPlanPersonalization,
  PathDifficulty,
  PathMasteryStatus,
  PlanAdjustments,
  PlanAlternative,
  PlanBenefit,
  PreviewNextAction,
  PlanStrategyComparison,
  PlanStrategyOption,
  PlanStageStatus,
  PlanTaskPreview,
  PlanUnlock,
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

function normalizeDiagnosisFallback(value: unknown) {
  const row = value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
  const source = row.content_source ?? row.contentSource;
  return {
    applied: Boolean(row.applied),
    reasons: readArray(row.reasons),
    contentSource: source ? readCodeLabel(source) : undefined,
  };
}

function normalizeDiagnosisMetadata(value: unknown): DiagnosisMetadata | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const row = value as Record<string, unknown>;
  return {
    questionCount: toNumber(row.question_count ?? row.questionCount),
    answerCount: toNumber(row.answer_count ?? row.answerCount),
    profileVersion: toNumber(row.profile_version ?? row.profileVersion),
  };
}

function normalizeDiagnosisTarget(value: unknown): DiagnosisNextAction['target'] | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const row = value as Record<string, unknown>;
  const params = row.params && typeof row.params === 'object' ? (row.params as Record<string, unknown>) : undefined;
  return {
    route: String(row.route ?? ''),
    params: params
      ? Object.fromEntries(Object.entries(params).map(([key, item]) => [key, typeof item === 'number' ? item : String(item ?? '')]))
      : undefined,
  };
}

function normalizeDiagnosisNextAction(value: unknown): DiagnosisNextAction | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const row = value as Record<string, unknown>;
  return {
    code: readCode(row.code),
    label: readLabel(row.label ?? row.code),
    target: normalizeDiagnosisTarget(row.target),
  };
}

function normalizeDiagnosisQuestion(value: unknown): DiagnosisQuestion {
  const row = value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
  const rawOptions = Array.isArray(row.options) ? row.options : [];
  return {
    questionId: String(row.question_id ?? row.questionId ?? ''),
    dimension: String(row.dimension ?? ''),
    type: readCode(row.type) as DiagnosisQuestion['type'],
    required: Boolean(row.required),
    options: rawOptions.map((item, index) => {
      const option = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
      return {
        code: String(option.code ?? option.value ?? `option-${index + 1}`),
        label: String(option.label ?? option.code ?? option.value ?? ''),
        order: toNumber(option.order),
      };
    }),
    title: String(row.title ?? ''),
    description: String(row.description ?? ''),
    placeholder: String(row.placeholder ?? ''),
    submitHint: String(row.submit_hint ?? row.submitHint ?? ''),
    sectionLabel: String(row.section_label ?? row.sectionLabel ?? ''),
  };
}

function normalizeCapabilityProfile(value: unknown): CapabilityProfile {
  const row = value && typeof value === 'object' ? (value as Record<string, unknown>) : {};
  return {
    currentLevel: readCodeLabel(row.current_level ?? row.currentLevel),
    strengths: readArray(row.strengths),
    weaknesses: readArray(row.weaknesses),
    learningPreference: row.learning_preference || row.learningPreference ? readCodeLabel(row.learning_preference ?? row.learningPreference) : undefined,
    timeBudget: row.time_budget || row.timeBudget ? readCodeLabel(row.time_budget ?? row.timeBudget) : undefined,
    goalOrientation: row.goal_orientation || row.goalOrientation ? readCodeLabel(row.goal_orientation ?? row.goalOrientation) : undefined,
  };
}

export function normalizeDiagnosisGenerateResponse(data: Record<string, unknown>): DiagnosisGenerateResponse {
  const questions = (Array.isArray(data.questions) ? data.questions : []).map((item) => normalizeDiagnosisQuestion(item));
  return {
    diagnosisId: String(data.diagnosis_id ?? data.diagnosisId ?? ''),
    sessionId: String(data.session_id ?? data.sessionId ?? ''),
    status: String(data.status ?? 'GENERATED') as DiagnosisGenerateResponse['status'],
    questions,
    nextAction: normalizeDiagnosisNextAction(data.next_action ?? data.nextAction),
    fallback: normalizeDiagnosisFallback(data.fallback),
    metadata: normalizeDiagnosisMetadata(data.metadata),
  };
}

export function normalizeDiagnosisSubmitResponse(data: Record<string, unknown>): DiagnosisSubmitResponse {
  const insights = data.insights && typeof data.insights === 'object' ? (data.insights as Record<string, unknown>) : undefined;
  const reasoningSteps = (Array.isArray(data.reasoning_steps ?? data.reasoningSteps)
    ? (data.reasoning_steps ?? data.reasoningSteps)
    : []) as Record<string, unknown>[];
  const strengthSources = (Array.isArray(data.strength_sources ?? data.strengthSources)
    ? (data.strength_sources ?? data.strengthSources)
    : []) as Record<string, unknown>[];
  const weaknessSources = (Array.isArray(data.weakness_sources ?? data.weaknessSources)
    ? (data.weakness_sources ?? data.weaknessSources)
    : []) as Record<string, unknown>[];
  return {
    diagnosisId: String(data.diagnosis_id ?? data.diagnosisId ?? ''),
    sessionId: String(data.session_id ?? data.sessionId ?? ''),
    status: String(data.status ?? 'SUBMITTED') as DiagnosisSubmitResponse['status'],
    capabilityProfile: normalizeCapabilityProfile(data.capability_profile ?? data.capabilityProfile),
    insights: insights
      ? {
          summary: String(insights.summary ?? ''),
          planExplanation: String(insights.plan_explanation ?? insights.planExplanation ?? ''),
        }
      : undefined,
    nextAction: normalizeDiagnosisNextAction(data.next_action ?? data.nextAction),
    fallback: normalizeDiagnosisFallback(data.fallback),
    metadata: normalizeDiagnosisMetadata(data.metadata),
    reasoningSteps: reasoningSteps.map((item) => ({
      dimension: String(item.dimension ?? 'FOUNDATION'),
      questionId: String(item.question_id ?? item.questionId ?? 'unknown-question'),
      questionTitle: String(item.question_title ?? item.questionTitle ?? '诊断问题'),
      selectedAnswerLabel: String(item.selected_answer_label ?? item.selectedAnswerLabel ?? '基于你的作答信息'),
      inferredConclusion: String(item.inferred_conclusion ?? item.inferredConclusion ?? '系统基于已收集的回答，形成当前能力判断。'),
    })),
    strengthSources: strengthSources.map((item) => ({
      label: String(item.label ?? '当前表现具备可持续推进条件'),
      dimension: String(item.dimension ?? 'FOUNDATION'),
      sourceQuestionId: String(item.source_question_id ?? item.sourceQuestionId ?? 'unknown-question'),
    })),
    weaknessSources: weaknessSources.map((item) => ({
      label: String(item.label ?? '当前仍存在需要补强的环节'),
      dimension: String(item.dimension ?? 'FOUNDATION'),
      sourceQuestionId: String(item.source_question_id ?? item.sourceQuestionId ?? 'unknown-question'),
    })),
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

function normalizePlanAlternatives(value: unknown): PlanAlternative[] {
  const items = Array.isArray(value) ? value : [];
  return items.map((item, index) => {
    const row = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
    return {
      key: String(row.key ?? row.id ?? `alternative-${index + 1}`),
      title: String(row.title ?? row.name ?? row.direction ?? '备选方向'),
      description: String(row.description ?? row.trade_off ?? row.tradeOff ?? row.reason ?? '当前阶段优先级略低，先放在下一跳。'),
    };
  }).filter((item) => item.title || item.description);
}

function normalizePlanBenefits(value: unknown): PlanBenefit[] {
  const items = Array.isArray(value) ? value : [];
  return items.map((item, index) => {
    const row = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
    return {
      key: String(row.key ?? row.id ?? `benefit-${index + 1}`),
      title: String(row.title ?? row.headline ?? row.name ?? '完成后的直接收益'),
      description: String(row.description ?? row.detail ?? row.value ?? '先把关键一步打通，后续学习会更顺。'),
    };
  }).filter((item) => item.title || item.description);
}

function normalizePlanUnlocks(value: unknown): PlanUnlock[] {
  const items = Array.isArray(value) ? value : [];
  return items.map((item, index) => {
    const row = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
    return {
      key: String(row.key ?? row.id ?? `unlock-${index + 1}`),
      title: String(row.title ?? row.name ?? row.stage ?? '后续阶段'),
      description: String(row.description ?? row.detail ?? row.reason ?? '完成这一小步后即可继续推进。'),
    };
  }).filter((item) => item.title || item.description);
}

function normalizePlanStageStatuses(value: unknown): PlanStageStatus[] {
  const items = Array.isArray(value) ? value : [];
  return items.map((item) => {
    const row = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
    const stage = readCode(row.stage) as PlanStageStatus['stage'];
    const status = readCode(row.status).toUpperCase();
    const normalizedStatus: PlanStageStatus['status'] =
      status === 'CURRENT' || status === 'LOCKED' || status === 'PENDING' || status === 'COMPLETED' || status === 'OPTIONAL' || status === 'REVIEW'
        ? status
        : 'PENDING';
    return {
      stage,
      label: String(row.label ?? ''),
      status: normalizedStatus,
      description: String(row.description ?? row.reason ?? ''),
    };
  }).filter((item) => item.stage);
}

function normalizePlanStrategyOptions(value: unknown): PlanStrategyOption[] {
  const items = Array.isArray(value) ? value : [];
  return items
    .map((item, index) => {
      const row = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
      return {
        key: String(row.key ?? row.id ?? row.code ?? `strategy-${index + 1}`),
        title: String(row.title ?? row.name ?? row.strategy ?? '策略方案'),
        fitFor: String(row.fit_for ?? row.fitFor ?? row.suitable_for ?? row.suitableFor ?? row.when_to_use ?? row.whenToUse ?? ''),
        tradeoff: String(row.trade_off ?? row.tradeOff ?? row.cost ?? row.cons ?? row.reason ?? ''),
        timeShortPlan: String(row.time_short_plan ?? row.timeShortPlan ?? row.if_no_time ?? row.ifNoTime ?? '').trim() || undefined,
      };
    })
    .filter((item) => item.title || item.fitFor || item.tradeoff);
}

function normalizePlanStrategyComparison(value: unknown): PlanStrategyComparison | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }
  const row = value as Record<string, unknown>;
  const options = normalizePlanStrategyOptions(row.options ?? row.items ?? row.strategies ?? row.alternatives);
  if (!options.length && !row.recommended_reason && !row.recommendedReason && !row.recommendation) {
    return undefined;
  }
  return {
    recommendedKey: String(row.recommended_key ?? row.recommendedKey ?? row.selected_key ?? row.selectedKey ?? '').trim() || undefined,
    recommendedReason: String(row.recommended_reason ?? row.recommendedReason ?? row.recommendation ?? '').trim() || undefined,
    options,
  };
}

function normalizeKickoffSteps(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (typeof item === 'string') {
        return item.trim();
      }
      if (item && typeof item === 'object') {
        const row = item as Record<string, unknown>;
        return String(row.step ?? row.title ?? row.description ?? row.action ?? '').trim();
      }
      return String(item ?? '').trim();
    })
    .filter(Boolean)
    .slice(0, 4);
}

function normalizeTaskPreview(value: unknown): PlanTaskPreview[] {
  const items = Array.isArray(value) ? value : [];
  return items
    .map((item, index) => {
      const row = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
      const stageCode = readCode(row.stage).toUpperCase();
      const stage = (stageCode || String(row.stage_code ?? row.stageCode ?? '')).toUpperCase();
      const normalizedStage: PlanTaskPreview['stage'] =
        stage === 'STRUCTURE' || stage === 'UNDERSTANDING' || stage === 'TRAINING' || stage === 'REFLECTION'
          ? stage
          : 'STRUCTURE';
      return {
        stage: normalizedStage,
        title: String(row.title ?? row.task_title ?? row.taskTitle ?? row.goal ?? `学习任务 ${index + 1}`),
        learningGoal: String(row.learning_goal ?? row.learningGoal ?? row.goal ?? '').trim(),
        learnerAction: String(row.learner_action ?? row.learnerAction ?? row.action ?? '').trim(),
        aiSupport: String(row.ai_support ?? row.aiSupport ?? '').trim(),
        estimatedTaskMinutes: toNumber(row.estimated_task_minutes ?? row.estimatedTaskMinutes ?? row.estimated_minutes ?? row.estimatedMinutes) ?? 15,
        expectedArtifact: String(row.expected_artifact ?? row.expectedArtifact ?? '').trim() || undefined,
        completionCriteria: String(row.completion_criteria ?? row.completionCriteria ?? '').trim() || undefined,
        checkMethod: String(row.check_method ?? row.checkMethod ?? '').trim() || undefined,
      };
    })
    .filter((item) => item.title);
}

function normalizeNextActionDetails(value: unknown): PreviewNextAction[] {
  const items = Array.isArray(value) ? value : [];
  const normalized: Array<PreviewNextAction | null> = items.map((item) => {
      if (typeof item === 'string') {
        const title = item.trim();
        return title ? { title } : null;
      }
      if (!item || typeof item !== 'object') {
        return null;
      }
      const row = item as Record<string, unknown>;
      const title = String(row.title ?? row.step ?? row.action ?? row.learner_action ?? row.learnerAction ?? '').trim();
      if (!title) {
        return null;
      }
      return {
        title,
        learnerAction: String(row.learner_action ?? row.learnerAction ?? '').trim() || undefined,
        expectedArtifact: String(row.expected_artifact ?? row.expectedArtifact ?? '').trim() || undefined,
        completionCriteria: String(row.completion_criteria ?? row.completionCriteria ?? '').trim() || undefined,
        estimatedMinutes: toNumber(row.estimated_minutes ?? row.estimatedMinutes),
        aiSupport: String(row.ai_support ?? row.aiSupport ?? '').trim() || undefined,
        checkMethod: String(row.check_method ?? row.checkMethod ?? '').trim() || undefined,
      };
    })
    ;
  return normalized.filter((item): item is PreviewNextAction => item !== null).slice(0, 4);
}

function normalizePersonalization(value: unknown): LearningPlanPersonalization | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }

  const row = value as Record<string, unknown>;
  const rawWhatISaw = row.what_i_saw ?? row.whatISaw;
  const whatISaw = (Array.isArray(rawWhatISaw) ? rawWhatISaw : [])
    .map((item: unknown) => {
      if (typeof item === 'string') {
        return item.trim();
      }
      if (item && typeof item === 'object') {
        const text = (item as Record<string, unknown>).text ?? (item as Record<string, unknown>).label ?? (item as Record<string, unknown>).description;
        return String(text ?? '').trim();
      }
      return String(item ?? '').trim();
    })
    .filter(Boolean);

  return {
    learnerState: String(row.learner_state ?? row.learnerState ?? '').trim() || undefined,
    whatISaw,
    whyThisPlanFitsYou: String(row.why_this_plan_fits_you ?? row.whyThisPlanFitsYou ?? '').trim() || undefined,
    mainRiskIfSkip: String(row.main_risk_if_skip ?? row.mainRiskIfSkip ?? '').trim() || undefined,
    thisRoundBoundary: String(row.this_round_boundary ?? row.thisRoundBoundary ?? '').trim() || undefined,
    adaptationHint: String(row.adaptation_hint ?? row.adaptationHint ?? '').trim() || undefined,
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
  const adjustments = normalizePlanAdjustments(data.adjustments, request.adjustments);
  const recommendedEntryRaw = (data.recommended_entry ?? data.recommendedEntry) as Record<string, unknown> | undefined;
  const learnerSnapshotRaw = (data.learner_snapshot ?? data.learnerSnapshot) as Record<string, unknown> | undefined;
  const recommendedStrategyRaw = (data.recommended_strategy ?? data.recommendedStrategy) as Record<string, unknown> | undefined;
  const alternativesRaw = (Array.isArray(data.alternatives_v2) ? data.alternatives_v2 : (Array.isArray(data.alternatives) ? data.alternatives : [])) as Record<string, unknown>[];
  const nextActionsSource = data.next_actions_v2 ?? data.nextActionsV2 ?? data.next_actions ?? data.nextActions;
  const nextActionDetails = normalizeNextActionDetails(nextActionsSource);
  const nextActionsRaw = nextActionDetails.length
    ? nextActionDetails.map((item) => item.title)
    : readArray(nextActionsSource);
  const taskPreviewRaw = data.task_previews ?? data.taskPreviews ?? data.task_preview ?? data.taskPreview;
  const taskPreviews = normalizeTaskPreview(taskPreviewRaw);
  const recommendedTitle = String(recommendedEntryRaw?.title ?? '');
  const estimatedMinutes = toNumber(recommendedEntryRaw?.estimated_minutes ?? recommendedEntryRaw?.estimatedMinutes) ?? 15;

  return {
    id: String(data.plan_id ?? data.planId ?? data.preview_id ?? data.previewId ?? data.id ?? ''),
    status: readCodeLabel(data.status ?? 'PREVIEW_READY', 'PREVIEW_READY'),
    previewOnly: Boolean(data.preview_only ?? data.previewOnly ?? true),
    committed: Boolean(data.committed ?? false),
    goal: String(data.goal ?? request.goalText),
    recommendedEntry: {
      conceptId: String(recommendedEntryRaw?.concept_id ?? recommendedEntryRaw?.conceptId ?? ''),
      title: recommendedTitle || '当前关键知识点',
      estimatedMinutes,
      reason: String(recommendedEntryRaw?.reason ?? '先补这一步，后续更容易推进。'),
    },
    learnerSnapshotV2: {
      currentState: String(learnerSnapshotRaw?.current_state ?? learnerSnapshotRaw?.currentState ?? '你当前需要先补稳关键基础。'),
      evidence: readArray(learnerSnapshotRaw?.evidence).slice(0, 3),
    },
    recommendedStrategy: {
      code: String(recommendedStrategyRaw?.code ?? ''),
      label: String(recommendedStrategyRaw?.label ?? '稳步推进'),
      explanation: String(recommendedStrategyRaw?.explanation ?? '当前先补关键薄弱点更稳妥。'),
    },
    alternativesV2: alternativesRaw.map((item) => ({
      code: String(item.code ?? ''),
      label: String(item.label ?? '备选方案'),
      notRecommendedReason: String(item.not_recommended_reason ?? item.notRecommendedReason ?? '这次优先级略低。'),
    })),
    nextActionsV2: nextActionsRaw.slice(0, 3),
    nextActionsDetail: nextActionDetails.length ? nextActionDetails : undefined,
    whyThisStep: String(data.why_this_step ?? data.whyThisStep ?? recommendedEntryRaw?.reason ?? '').trim() || undefined,
    keyEvidence: readArray(data.key_evidence ?? data.keyEvidence).slice(0, 3),
    skipRisk: String(data.skip_risk ?? data.skipRisk ?? '').trim() || undefined,
    expectedGain: String(data.expected_gain ?? data.expectedGain ?? '').trim() || undefined,
    confidenceHint: String(data.confidence_hint ?? data.confidenceHint ?? '').trim() || undefined,
    startGuide: String(data.start_guide ?? data.startGuide ?? '确认后系统会直接带你进入第一步任务。'),
    explanationGenerated: Boolean(data.explanation_generated ?? data.explanationGenerated),
    focuses: [],
    summary: {
      recommendedStartNode: {
        id: String(recommendedEntryRaw?.concept_id ?? recommendedEntryRaw?.conceptId ?? ''),
        nodeKey: String(recommendedEntryRaw?.concept_id ?? recommendedEntryRaw?.conceptId ?? ''),
        nodeName: recommendedTitle || '当前关键知识点',
        displayName: recommendedTitle || undefined,
      },
      recommendedRhythm: request.adjustments.intensity,
      recommendedRhythmLabel: request.adjustments.intensity,
      estimatedTotalMinutes: estimatedMinutes,
      estimatedKnowledgeCount: 1,
      stageCount: 4,
      personalizedHeadline: '',
      personalizedSummary: '',
    },
    reasons: [],
    whyStartHere: String(recommendedEntryRaw?.reason ?? ''),
    keyWeaknesses: readArray(learnerSnapshotRaw?.evidence),
    priorityNodes: [{
      nodeId: String(recommendedEntryRaw?.concept_id ?? recommendedEntryRaw?.conceptId ?? ''),
      title: recommendedTitle || '当前关键知识点',
      reason: String(recommendedEntryRaw?.reason ?? ''),
    }],
    pathNodes: [],
    taskPreviews,
    adjustments,
    context: {
      sessionId: request.sessionId,
      diagnosisId: request.diagnosisId,
      goalText: request.goalText,
      courseName: request.courseName,
      chapterName: request.chapterName,
      diagnosisSummary: '',
    },
    nextStepNote: String(data.start_guide ?? data.startGuide ?? ''),
    alternatives: [],
    benefits: [],
    nextUnlocks: [],
    stageStatuses: [],
  };
}
