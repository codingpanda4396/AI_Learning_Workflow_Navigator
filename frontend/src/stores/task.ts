import { defineStore } from 'pinia';
import { fetchTaskDetailApi, runTaskApi } from '@/api/modules/task';
import type { TaskDetail, TaskRunResult } from '@/types/task';

export const useTaskStore = defineStore('task', {
  state: () => ({
    currentTaskDetail: null as TaskDetail | null,
    currentTaskResult: null as TaskRunResult | null,
    loading: false,
    error: '',
  }),
  actions: {
    async fetchTaskDetail(taskId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.currentTaskDetail = await fetchTaskDetailApi(taskId);
        return this.currentTaskDetail;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取任务详情失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async runTask(taskId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.currentTaskResult = await runTaskApi(taskId);
        return this.currentTaskResult;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '执行任务失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
  },
});
