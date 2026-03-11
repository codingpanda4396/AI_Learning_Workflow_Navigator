import { defineStore } from 'pinia';
import { fetchGrowthDashboardApi, fetchReportApi, submitNextActionApi } from '@/api/modules/feedback';
import type { GrowthDashboard, LearningReport } from '@/types/feedback';

export const useFeedbackStore = defineStore('feedback', {
  state: () => ({
    report: null as LearningReport | null,
    growthDashboard: null as GrowthDashboard | null,
    loading: false,
    error: '',
  }),
  actions: {
    async fetchReport(sessionId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.report = await fetchReportApi(sessionId);
        return this.report;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取反馈报告失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async submitNextAction(sessionId: number, action: string) {
      this.loading = true;
      this.error = '';
      try {
        this.report = await submitNextActionApi(sessionId, action);
        return this.report;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '提交下一步动作失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchGrowthDashboard(sessionId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.growthDashboard = await fetchGrowthDashboardApi(sessionId);
        return this.growthDashboard;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取成长看板失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
  },
});
