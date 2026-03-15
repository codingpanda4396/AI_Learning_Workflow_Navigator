import { createRouter, createWebHistory } from 'vue-router';
import GrowthDashboardView from '@/views/GrowthDashboardView.vue';
import DiagnosisView from '@/views/DiagnosisView.vue';
import HomeView from '@/views/HomeView.vue';
import LearningPlanView from '@/views/LearningPlanView.vue';
import LoginView from '@/views/LoginView.vue';
import QuizView from '@/views/QuizView.vue';
import ReportView from '@/views/ReportView.vue';
import SessionView from '@/views/SessionView.vue';
import TaskRunView from '@/views/TaskRunView.vue';
import { getStoredToken } from '@/utils/storage';
import { useLearningFlowStore } from '@/stores/learningFlow';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
    { path: '/diagnosis/:sessionId?', name: 'diagnosis', component: DiagnosisView },
    { path: '/plan', name: 'plan', component: LearningPlanView },
    // 学习执行主链路：基于 sessionId
    { path: '/learn/:sessionId', name: 'learn', component: SessionView, meta: { learn: true } },
    { path: '/learn/:sessionId/task', name: 'learn-task', component: TaskRunView, meta: { learn: true } },
    { path: '/learn/:sessionId/training', name: 'learn-training', component: QuizView, meta: { learn: true } },
    { path: '/learn/:sessionId/evaluation', name: 'learn-evaluation', component: ReportView, meta: { learn: true } },
    { path: '/learn/:sessionId/next', name: 'learn-next', redirect: (to) => ({ name: 'learn', params: to.params }) },
    // 旧路由兼容：重定向到 /learn
    { path: '/sessions/:sessionId', name: 'session', redirect: (to) => ({ name: 'learn', params: { sessionId: to.params.sessionId } }) },
    { path: '/tasks/:taskId/run', name: 'task-run', component: TaskRunView, meta: { learnCompat: true } },
    { path: '/sessions/:sessionId/quiz', name: 'quiz', redirect: (to) => ({ name: 'learn-training', params: { sessionId: to.params.sessionId } }) },
    { path: '/sessions/:sessionId/report', name: 'report', redirect: (to) => ({ name: 'learn-evaluation', params: { sessionId: to.params.sessionId } }) },
    { path: '/sessions/:sessionId/growth', name: 'growth', component: GrowthDashboardView },
  ],
});

router.beforeEach(async (to) => {
  const token = getStoredToken();
  if (!to.meta.public && !token) {
    return { path: '/login' };
  }
  if (to.path === '/login' && token) {
    return { path: '/' };
  }
  // 旧入口 /tasks/:taskId/run：反查 session 后替换为 /learn/:sessionId/task
  if (to.meta.learnCompat && to.name === 'task-run' && to.params.taskId) {
    const taskId = Number(to.params.taskId);
    if (Number.isFinite(taskId)) {
      const flowStore = useLearningFlowStore();
      const ok = await flowStore.redirectFromTaskIdToLearn(taskId);
      if (ok) return false; // 已 replace，阻止继续
    }
  }
  // 学习主链路：确保 store 已加载当前 session
  if (to.meta.learn && to.params.sessionId) {
    const sessionId = Number(to.params.sessionId);
    if (Number.isFinite(sessionId) && sessionId > 0) {
      const flowStore = useLearningFlowStore();
      try {
        await flowStore.loadSessionFlow(sessionId);
      } catch {
        // 留由页面展示错误
      }
    }
  }
  return true;
});

export default router;
