/**
 * 学习执行阶段唯一状态中心：聚合 session/task/quiz/feedback，统一阶段跳转。
 * 主流程页面均依赖本 store，禁止页面自行 router.push 主流程。
 */

import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import router from '@/router';
import { toLearningFlowSnapshot } from '@/domain/learning-flow/mapper';
import { resolveRouteByStage } from '@/domain/learning-flow/stage';
import { canEnterStage } from '@/domain/learning-flow/guards';
import type { LearningFlowSnapshot, LearningStage, FlowStatus } from '@/domain/learning-flow/types';
import { useSessionStore } from '@/stores/session';
import { useTaskStore } from '@/stores/task';
import { useQuizStore } from '@/stores/quiz';
import { useFeedbackStore } from '@/stores/feedback';

export const useLearningFlowStore = defineStore('learningFlow', () => {
  const sessionStore = useSessionStore();
  const taskStore = useTaskStore();
  const quizStore = useQuizStore();
  const feedbackStore = useFeedbackStore();

  const status = ref<FlowStatus>('idle');
  const flowError = ref('');
  const currentSessionId = ref<number | null>(null);

  const snapshot = computed<LearningFlowSnapshot | null>(() => {
    const sid = currentSessionId.value;
    if (sid == null || sid <= 0) return null;
    const overview = sessionStore.overview?.sessionId === sid ? sessionStore.overview : null;
    const taskDetail = taskStore.currentTaskDetail?.sessionId === sid ? taskStore.currentTaskDetail : null;
    const taskResult = taskStore.currentTaskResult;
    const quizSnapshot = quizStore.quiz?.sessionId === sid ? quizStore.quiz : null;
    const quizViewStatus = typeof quizStore.status === 'string' ? quizStore.status : 'idle';
    const report = feedbackStore.report?.sessionId === sid ? feedbackStore.report : null;
    return toLearningFlowSnapshot({
      sessionId: sid,
      overview,
      taskDetail: taskDetail ?? null,
      taskResult: taskResult ?? null,
      quizSnapshot: quizSnapshot ?? null,
      quizViewStatus,
      report: report ?? null,
    });
  });

  function setStatus(s: FlowStatus, err = '') {
    status.value = s;
    flowError.value = err;
  }

  async function loadSessionFlow(sessionId: number) {
    if (sessionId <= 0) {
      setStatus('error', '无效的 session');
      return null;
    }
    currentSessionId.value = sessionId;
    setStatus('loading');
    try {
      await sessionStore.fetchOverview(sessionId);
      setStatus('ready');
      return sessionStore.overview;
    } catch (e) {
      const msg = e instanceof Error ? e.message : '加载学习流失败';
      setStatus('error', msg);
      throw e;
    }
  }

  function resolveRouteByStageForSession(stage: LearningStage): string {
    const sid = currentSessionId.value ?? 0;
    return resolveRouteByStage(sid, stage);
  }

  function canEnter(stage: LearningStage): boolean {
    return canEnterStage(snapshot.value, stage);
  }

  function goToStage(stage: LearningStage) {
    const path = resolveRouteByStageForSession(stage);
    return router.push(path);
  }

  async function resumeCurrentStage() {
    const s = snapshot.value;
    if (!s) return router.push('/');
    if (s.blocked.blocked) return goToStage('NEXT_ACTION');
    if (s.canEnterTask && (s.currentStage === 'NEXT_ACTION' || s.currentStage === 'LEARNING_TASK')) return goToStage('LEARNING_TASK');
    if (s.canEnterTraining) return goToStage('TRAINING');
    if (s.canEnterEvaluation) return goToStage('EVALUATION');
    return goToStage('NEXT_ACTION');
  }

  /** 为当前 session 加载并设置当前任务（用于 task 页）；返回 taskId */
  async function ensureCurrentTaskLoaded(): Promise<number | null> {
    const s = snapshot.value;
    const taskId = s?.currentTaskId ?? null;
    if (taskId == null || taskId <= 0) return null;
    if (taskStore.currentTaskDetail?.taskId === taskId) return taskId;
    try {
      await taskStore.fetchTaskDetail(taskId);
      await taskStore.runTask(taskId);
      return taskId;
    } catch {
      return null;
    }
  }

  /** 兼容：从 taskId 反查 session 并跳转到 /learn/:sessionId/task */
  async function redirectFromTaskIdToLearn(taskId: number): Promise<boolean> {
    try {
      await taskStore.fetchTaskDetail(taskId);
      const sid = taskStore.currentTaskDetail?.sessionId;
      if (sid != null && sid > 0) {
        currentSessionId.value = sid;
        await sessionStore.fetchOverview(sid);
        await taskStore.runTask(taskId);
        await router.replace(resolveRouteByStage(sid, 'LEARNING_TASK'));
        return true;
      }
    } catch {
      // ignore
    }
    return false;
  }

  return {
    status,
    flowError,
    currentSessionId,
    snapshot,
    loadSessionFlow,
    goToStage,
    resolveRouteByStage: resolveRouteByStageForSession,
    canEnterStage: canEnter,
    resumeCurrentStage,
    ensureCurrentTaskLoaded,
    redirectFromTaskIdToLearn,
    setStatus,
  };
});
