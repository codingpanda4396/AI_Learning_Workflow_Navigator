import apiClient from '@/api/client';
import { normalizeLearningPlanPreview } from '@/api/normalizers';
import type { ApiEnvelope } from '@/types/common';
import type { LearningPlanPreview, LearningPlanRequest, PlanAdjustments, PlanConfirmResult } from '@/types/learningPlan';

function toBackendLearningMode(mode: PlanAdjustments['learningMode']): string {
  switch (mode) {
    case 'LEARN_BY_DOING':
      return 'PRACTICE_DRIVEN';
    case 'EXPLAIN_THEN_PRACTICE':
    default:
      return 'LEARN_THEN_PRACTICE';
  }
}

function toApiPayload(payload: {
  goalId: string;
  diagnosisId: string;
  goalText: string;
  courseName: string;
  chapterName: string;
  adjustments: PlanAdjustments;
}) {
  return {
    goalId: payload.goalId,
    diagnosisId: payload.diagnosisId,
    goalText: payload.goalText,
    courseName: payload.courseName,
    chapterName: payload.chapterName,
    adjustments: {
      intensity: {
        code: payload.adjustments.intensity,
        label: payload.adjustments.intensity,
      },
      learningMode: {
        code: toBackendLearningMode(payload.adjustments.learningMode),
        label: payload.adjustments.learningMode,
      },
      prioritizeFoundation: payload.adjustments.prioritizeFoundation,
    },
  };
}

function unwrapEnvelope<T>(payload: unknown): T {
  if (payload && typeof payload === 'object' && 'data' in (payload as Record<string, unknown>)) {
    return ((payload as Record<string, unknown>).data ?? {}) as T;
  }
  return (payload ?? {}) as T;
}

export async function fetchLearningPlanPreviewApi(payload: LearningPlanRequest): Promise<LearningPlanPreview> {
  const response = await apiClient.post<ApiEnvelope<Record<string, unknown>>>('/api/learning-plans/preview', toApiPayload(payload));
  return normalizeLearningPlanPreview(unwrapEnvelope<Record<string, unknown>>(response.data), payload);
}

export async function regenerateLearningPlanApi(payload: LearningPlanRequest): Promise<LearningPlanPreview> {
  return fetchLearningPlanPreviewApi(payload);
}

export async function confirmLearningPlanApi(planId: number): Promise<PlanConfirmResult> {
  const response = await apiClient.post<ApiEnvelope<Record<string, unknown>>>(`/api/learning-plans/${planId}/confirm`);
  const data = unwrapEnvelope<Record<string, unknown>>(response.data);
  return {
    planId: Number(data.plan_id ?? data.planId ?? 0) || undefined,
    sessionId: Number(data.session_id ?? data.sessionId ?? 0),
    currentNodeId: Number(data.current_node_id ?? data.currentNodeId ?? 0) || undefined,
    firstTaskId: Number(data.first_task_id ?? data.firstTaskId ?? 0) || undefined,
    nextPage: typeof data.next_page === 'string' ? String(data.next_page) : typeof data.nextPage === 'string' ? String(data.nextPage) : undefined,
  };
}
