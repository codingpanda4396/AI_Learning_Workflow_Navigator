import { createRouter, createWebHistory } from 'vue-router';
import GrowthDashboardView from '@/views/GrowthDashboardView.vue';
import HomeView from '@/views/HomeView.vue';
import LoginView from '@/views/LoginView.vue';
import QuizView from '@/views/QuizView.vue';
import ReportView from '@/views/ReportView.vue';
import SessionView from '@/views/SessionView.vue';
import TaskRunView from '@/views/TaskRunView.vue';
import { getStoredToken } from '@/utils/storage';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
    { path: '/sessions/:sessionId', name: 'session', component: SessionView },
    { path: '/tasks/:taskId/run', name: 'task-run', component: TaskRunView },
    { path: '/sessions/:sessionId/quiz', name: 'quiz', component: QuizView },
    { path: '/sessions/:sessionId/report', name: 'report', component: ReportView },
    { path: '/sessions/:sessionId/growth', name: 'growth', component: GrowthDashboardView },
  ],
});

router.beforeEach((to) => {
  const token = getStoredToken();
  if (!to.meta.public && !token) {
    return { path: '/login' };
  }
  if (to.path === '/login' && token) {
    return { path: '/' };
  }
  return true;
});

export default router;
