import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { getAccessToken } from '@/auth/storage'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/auth',
      name: 'auth',
      component: () => import('@/views/AuthView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/session/:id',
      name: 'session',
      component: () => import('@/views/SessionView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/task/:id/run',
      name: 'task-run',
      component: () => import('@/views/TaskRunView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/task/:id/submit',
      name: 'task-submit',
      component: () => import('@/views/TaskSubmitView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const token = getAccessToken()
  if (to.meta.requiresAuth && !token) {
    return {
      name: 'auth',
      query: { redirect: to.fullPath },
    }
  }
  if (to.meta.guestOnly && token) {
    return { name: 'home' }
  }
  return true
})

export default router
