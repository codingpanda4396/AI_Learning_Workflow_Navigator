import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/session/:id',
      name: 'session',
      component: () => import('@/views/SessionView.vue'),
    },
    {
      path: '/task/:id/run',
      name: 'task-run',
      component: () => import('@/views/TaskRunView.vue'),
    },
    {
      path: '/task/:id/submit',
      name: 'task-submit',
      component: () => import('@/views/TaskSubmitView.vue'),
    },
  ],
})

export default router
