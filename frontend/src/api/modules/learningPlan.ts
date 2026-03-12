import apiClient from '@/api/client';
import { normalizeLearningPlanPreview } from '@/api/normalizers';
import { createSessionApi, planSessionApi } from '@/api/modules/session';
import { createMockLearningPlanPreview } from '@/mocks/learningPlan';
import type { LearningPlanPreview, LearningPlanRequest, PlanAdjustments, PlanConfirmResult } from '@/types/learningPlan';

function toApiPayload(payload: { sessionId?: number; goalText: string; courseId: string; chapterId: string; adjustments: PlanAdjustments }) {
  return {
    session_id: payload.sessionId,
    goal_text: payload.goalText,
    course_id: payload.courseId,
    chapter_id: payload.chapterId,
    adjustments: {
      intensity: payload.adjustments.intensity,
      learning_mode: payload.adjustments.learningMode,
      prioritize_foundation: payload.adjustments.prioritizeFoundation,
    },
  };
}

export async function fetchLearningPlanPreviewApi(payload: LearningPlanRequest): Promise<LearningPlanPreview> {
  try {
    const response = await apiClient.post('/api/learning-plan/preview', toApiPayload(payload), {
      validateStatus: () => true,
    });
    if (response.status >= 200 && response.status < 300 && response.data) {
      return normalizeLearningPlanPreview(response.data as Record<string, unknown>, payload);
    }
  } catch {
    // Fallback keeps the page runnable before backend endpoint is ready.
  }
  return createMockLearningPlanPreview(payload);
}

export async function regenerateLearningPlanApi(payload: LearningPlanRequest): Promise<LearningPlanPreview> {
  try {
    const response = await apiClient.post('/api/learning-plan/regenerate', toApiPayload(payload), {
      validateStatus: () => true,
    });
    if (response.status >= 200 && response.status < 300 && response.data) {
      return normalizeLearningPlanPreview(response.data as Record<string, unknown>, payload);
    }
  } catch {
    // Fallback keeps the page runnable before backend endpoint is ready.
  }
  return createMockLearningPlanPreview(payload);
}

export async function confirmLearningPlanApi(payload: LearningPlanRequest): Promise<PlanConfirmResult> {
  try {
    const response = await apiClient.post('/api/learning-plan/confirm', toApiPayload(payload), {
      validateStatus: () => true,
    });
    if (response.status >= 200 && response.status < 300) {
      const sessionId = Number(
        (response.data as Record<string, unknown>)?.session_id ??
          (response.data as Record<string, unknown>)?.sessionId ??
          0,
      );
      if (sessionId > 0) {
        return { sessionId };
      }
    }
  } catch {
    // Fallback below keeps the flow available.
  }

  const sessionId = payload.sessionId ?? 0;
  if (sessionId > 0) {
    await planSessionApi(sessionId);
    return { sessionId };
  }

  const createResponse = await createSessionApi({
    goalText: payload.goalText,
    courseId: payload.courseId,
    chapterId: payload.chapterId,
  });
  const createdSessionId = Number(createResponse.session_id ?? 0);
  if (createdSessionId > 0) {
    await planSessionApi(createdSessionId);
  }
  return { sessionId: createdSessionId };
}
