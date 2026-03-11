import apiClient from '@/api/client';
import { normalizeTaskDetail, normalizeTaskRun } from '@/api/normalizers';
import type { TaskDetail, TaskRunResult } from '@/types/task';

export async function fetchTaskDetailApi(taskId: number): Promise<TaskDetail> {
  const { data } = await apiClient.get(`/api/task/${taskId}`);
  return normalizeTaskDetail(data);
}

export async function runTaskApi(taskId: number): Promise<TaskRunResult> {
  const { data } = await apiClient.post(`/api/task/${taskId}/run`);
  return normalizeTaskRun(data);
}
