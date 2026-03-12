import { defineStore } from 'pinia';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { confirmLearningPlanApi, fetchLearningPlanPreviewApi, regenerateLearningPlanApi } from '@/api/modules/learningPlan';
import type { LearningPlanPreview, LearningPlanRequest, PlanAdjustments } from '@/types/learningPlan';

export const useLearningPlanStore = defineStore('learningPlan', {
  state: () => ({
    preview: null as LearningPlanPreview | null,
    request: null as LearningPlanRequest | null,
    adjustments: { ...DEFAULT_PLAN_ADJUSTMENTS } as PlanAdjustments,
    loading: false,
    regenerating: false,
    confirming: false,
    error: '',
  }),
  actions: {
    async generatePreview(payload: Omit<LearningPlanRequest, 'adjustments'> & { adjustments?: Partial<PlanAdjustments> }) {
      this.loading = true;
      this.error = '';
      this.adjustments = {
        ...DEFAULT_PLAN_ADJUSTMENTS,
        ...(payload.adjustments ?? {}),
      };
      this.request = {
        sessionId: payload.sessionId,
        goalId: payload.goalId,
        diagnosisId: payload.diagnosisId,
        goalText: payload.goalText,
        courseId: payload.courseId,
        chapterId: payload.chapterId,
        adjustments: this.adjustments,
      };
      try {
        const preview = await fetchLearningPlanPreviewApi(this.request);
        this.preview = preview;
        this.adjustments = preview.adjustments;
        this.request = {
          ...this.request,
          adjustments: preview.adjustments,
        };
        return preview;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '瀛︿範瑙勫垝鐢熸垚澶辫触';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    setAdjustments(adjustments: PlanAdjustments) {
      this.adjustments = { ...adjustments };
      if (this.preview) {
        this.preview = {
          ...this.preview,
          adjustments: { ...adjustments },
        };
      }
      if (this.request) {
        this.request = {
          ...this.request,
          adjustments: { ...adjustments },
        };
      }
    },
    async regeneratePreview() {
      if (!this.request) {
        return null;
      }
      this.regenerating = true;
      this.error = '';
      const payload: LearningPlanRequest = {
        ...this.request,
        adjustments: { ...this.adjustments },
      };
      try {
        const preview = await regenerateLearningPlanApi(payload);
        this.preview = preview;
        this.adjustments = preview.adjustments;
        this.request = {
          ...payload,
          adjustments: preview.adjustments,
        };
        return preview;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '瀛︿範瑙勫垝閲嶆柊鐢熸垚澶辫触';
        throw error;
      } finally {
        this.regenerating = false;
      }
    },
    async confirmPlan() {
      if (!this.preview?.planId) {
        throw new Error('Missing learning plan id.');
      }
      this.confirming = true;
      this.error = '';
      try {
        const result = await confirmLearningPlanApi(this.preview.planId);
        return result.sessionId;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '纭瀛︿範瑙勫垝澶辫触';
        throw error;
      } finally {
        this.confirming = false;
      }
    },
    reset() {
      this.preview = null;
      this.request = null;
      this.adjustments = { ...DEFAULT_PLAN_ADJUSTMENTS };
      this.loading = false;
      this.regenerating = false;
      this.confirming = false;
      this.error = '';
    },
  },
});
