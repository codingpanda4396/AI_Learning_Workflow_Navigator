import { defineStore } from 'pinia';
import { createSessionApi, fetchCurrentSessionApi, fetchOverviewApi, planSessionApi } from '@/api/modules/session';
import type { CurrentSessionInfo, SessionCreatePayload, SessionOverview } from '@/types/session';

export const useSessionStore = defineStore('session', {
  state: () => ({
    overview: null as SessionOverview | null,
    currentSession: null as CurrentSessionInfo | null,
    loading: false,
    error: '',
  }),
  actions: {
    async createSession(payload: SessionCreatePayload) {
      this.loading = true;
      this.error = '';
      try {
        const response = await createSessionApi(payload);
        return Number(response.session_id ?? 0);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '创建学习会话失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async planSession(sessionId: number) {
      this.loading = true;
      this.error = '';
      try {
        await planSessionApi(sessionId);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '规划任务失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchOverview(sessionId: number) {
      this.loading = true;
      this.error = '';
      try {
        const overview = await fetchOverviewApi(sessionId);
        this.overview = overview;
        return overview;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取总览失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchCurrentSession() {
      this.loading = true;
      this.error = '';
      try {
        this.currentSession = await fetchCurrentSessionApi();
        return this.currentSession;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取当前学习会话失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
  },
});
