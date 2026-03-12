import apiClient from '@/api/client';
import { normalizeLearningPlanPreview } from '@/api/normalizers';
import type { LearningPlanPreview, LearningPlanRequest, PlanAdjustments, PlanConfirmResult } from '@/types/learningPlan';

function toApiPayload(payload: {
  goalId: string;
  diagnosisId: string;
  goalText: string;
  courseId: string;
  chapterId: string;
  adjustments: PlanAdjustments;
}) {
  return {
    goalId: payload.goalId,
    diagnosisId: payload.diagnosisId,
    goalText: payload.goalText,
    courseId: payload.courseId,
    chapterId: payload.chapterId,
    adjustments: {
      intensity: payload.adjustments.intensity,
      learningMode: payload.adjustments.learningMode,
      preferPrerequisite: payload.adjustments.prioritizeFoundation,
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
  const response = await apiClient.post('/api/learning-plans/preview', toApiPayload(payload));
  return normalizeLearningPlanPreview(unwrapEnvelope<Record<string, unknown>>(response.data), payload);
}

export async function regenerateLearningPlanApi(payload: LearningPlanRequest): Promise<LearningPlanPreview> {
  return fetchLearningPlanPreviewApi(payload);
}

export async function confirmLearningPlanApi(planId: number): Promise<PlanConfirmResult> {
  const response = await apiClient.post(`/api/learning-plans/${planId}/confirm`);
  const data = unwrapEnvelope<Record<string, unknown>>(response.data);
  return {
    planId: Number(data.plan_id ?? data.planId ?? 0) || undefined,
    sessionId: Number(data.session_id ?? data.sessionId ?? 0),
    currentNodeId: Number(data.current_node_id ?? data.currentNodeId ?? 0) || undefined,
    firstTaskId: Number(data.first_task_id ?? data.firstTaskId ?? 0) || undefined,
    nextPage: typeof data.next_page === 'string' ? String(data.next_page) : typeof data.nextPage === 'string' ? String(data.nextPage) : undefined,
  };
}
