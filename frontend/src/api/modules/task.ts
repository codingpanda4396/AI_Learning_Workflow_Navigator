import apiClient from '@/api/client';
import { getTaskDetailMock, getTaskRunResultMock } from '@/api/learnMocks';
import { normalizeTaskDetail, normalizeTaskRun } from '@/api/normalizers';
import type { TaskDetail, TaskRunResult } from '@/types/task';

const useLearnMock = import.meta.env.VITE_USE_LEARN_MOCK === 'true';

export async function fetchTaskDetailApi(taskId: number): Promise<TaskDetail> {
  if (useLearnMock) return Promise.resolve(getTaskDetailMock(taskId));
  const { data } = await apiClient.get(`/api/tasks/${taskId}`);
  return normalizeTaskDetail(data);
}

export async function runTaskApi(taskId: number): Promise<TaskRunResult> {
  if (useLearnMock) return Promise.resolve(getTaskRunResultMock(taskId));
  const { data } = await apiClient.post(`/api/tasks/${taskId}/run`);
  return normalizeTaskRun(data);
}
