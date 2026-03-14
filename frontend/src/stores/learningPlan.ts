import { defineStore } from 'pinia';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { confirmLearningPlanApi, fetchLearningPlanPreviewApi, regenerateLearningPlanApi, submitLearningPlanStrategyApi } from '@/api/modules/learningPlan';
import type { LearningPlanPreview, LearningPlanRequest, PlanAdjustments, PlanConfirmResult } from '@/types/learningPlan';

export const useLearningPlanStore = defineStore('learningPlan', {
  state: () => ({
    preview: null as LearningPlanPreview | null,
    request: null as LearningPlanRequest | null,
    adjustments: { ...DEFAULT_PLAN_ADJUSTMENTS } as PlanAdjustments,
    loading: false,
    regenerating: false,
    confirming: false,
    strategySubmitting: false,
    strategyNote: '',
    error: '',
  }),
  actions: {
    async generatePreview(payload: Omit<LearningPlanRequest, 'adjustments'> & { adjustments?: Partial<PlanAdjustments> }) {
      this.loading = true;
      this.error = '';
      this.strategyNote = '';
      this.adjustments = {
        ...DEFAULT_PLAN_ADJUSTMENTS,
        ...(payload.adjustments ?? {}),
      };
      this.request = {
        sessionId: payload.sessionId,
        diagnosisId: payload.diagnosisId,
        goalText: payload.goalText,
        courseName: payload.courseName,
        chapterName: payload.chapterName,
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
        this.error = error instanceof Error ? error.message : '生成学习计划预览失败。';
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
      this.strategyNote = '';
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
        this.error = error instanceof Error ? error.message : '重新生成学习计划预览失败。';
        throw error;
      } finally {
        this.regenerating = false;
      }
    },
    async confirmPlan(): Promise<PlanConfirmResult> {
      if (!this.preview?.id) {
        throw new Error('缺少预览 ID。');
      }
      this.confirming = true;
      this.error = '';
      try {
        return await confirmLearningPlanApi(this.preview.id);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '确认学习计划失败。';
        throw error;
      } finally {
        this.confirming = false;
      }
    },
    async submitStrategyFeedback(payload: { action: string; intent: 'strategy' | 'disagree' | 'ai-explain'; prompt?: string }) {
      if (!this.preview?.id) {
        this.strategyNote = '当前规划还没准备好，暂时无法调整策略。';
        return { accepted: false, pending: true };
      }

      this.strategySubmitting = true;
      this.strategyNote = '';
      try {
        await submitLearningPlanStrategyApi(this.preview.id, payload);
        this.strategyNote = payload.intent === 'ai-explain' ? '已把你的问题交给 AI 解释入口，后续可直接对接会话能力。' : '已收到你的调整意图，系统会按新的策略继续生成。';
        return { accepted: true, pending: false };
      } catch {
        this.strategyNote = '已保留这次操作入口；如果后端还未开放，会先按前端策略重算当前规划。';
        return { accepted: false, pending: true };
      } finally {
        this.strategySubmitting = false;
      }
    },
    reset() {
      this.preview = null;
      this.request = null;
      this.adjustments = { ...DEFAULT_PLAN_ADJUSTMENTS };
      this.loading = false;
      this.regenerating = false;
      this.confirming = false;
      this.strategySubmitting = false;
      this.strategyNote = '';
      this.error = '';
    },
  },
});
