import { createRouter, createWebHistory } from 'vue-router'
import { getSessionFlowState } from '@/api/session'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/goal' },
    { path: '/goal', name: 'goal', component: () => import('@/views/GoalInputView.vue'), meta: { step: 'goal' } },
    { path: '/auth/login', name: 'login', component: () => import('@/views/AuthView.vue'), meta: { guestOnly: true } },
    { path: '/auth/register', name: 'register', component: () => import('@/views/AuthView.vue'), meta: { guestOnly: true } },
    { path: '/diagnosis', name: 'diagnosis', component: () => import('@/views/DiagnosisView.vue'), meta: { step: 'diagnosis' } },
    { path: '/plan', name: 'plan', component: () => import('@/views/LearningPlanView.vue'), meta: { step: 'plan' } },
    {
      path: '/execution',
      name: 'execution',
      component: () => import('@/views/ExecutionView.vue'),
      meta: { step: 'task' },
    },
    {
      path: '/tasks/:taskId/run',
      name: 'taskRun',
      component: () => import('@/views/TaskRunView.vue'),
      meta: { step: 'task' },
    },
    { path: '/task', name: 'task', component: () => import('@/views/TaskRunView.vue'), meta: { step: 'task' } },
    { path: '/report', name: 'report', component: () => import('@/views/ReportView.vue'), meta: { step: 'report' } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const store = useWorkflowStore()
  await auth.ensureReady()

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'goal' }
  }

  const step = to.meta.step as string | undefined
  if (step && step !== 'goal' && !auth.isAuthenticated) {
    auth.setPendingRedirect(to.fullPath)
    return { name: 'login' }
  }

  if (step === 'diagnosis' && !store.goalId) {
    return { name: 'goal' }
  }
  if (step === 'plan' && (!store.goalId || !store.diagnosisId)) {
    return store.goalId ? { name: 'diagnosis' } : { name: 'goal' }
  }
  if ((step === 'task' || step === 'report') && !store.sessionId) {
    return { name: 'plan' }
  }

  if (store.sessionId && (to.name === 'execution' || to.name === 'task' || to.name === 'taskRun')) {
    const skipTaskRunRefresh =
      to.name === 'taskRun' &&
      typeof to.params.taskId === 'string' &&
      typeof router.currentRoute.value.params.taskId === 'string' &&
      to.params.taskId === router.currentRoute.value.params.taskId

    if (!skipTaskRunRefresh) {
      try {
        const flow = await getSessionFlowState(store.sessionId)
        store.currentTaskId = flow.currentTaskId ?? null

        if (flow.reportReady) {
          return { name: 'report', replace: true }
        }
        if (flow.currentTaskId) {
          const requestedTaskId = typeof to.params.taskId === 'string' ? to.params.taskId : null
          if (to.name !== 'taskRun' || requestedTaskId !== flow.currentTaskId) {
            return {
              name: 'taskRun',
              params: { taskId: flow.currentTaskId },
              replace: true,
            }
          }
        }
      } catch {
        // Keep existing local fallback if flow-state request fails.
      }
    }
  }

  if (to.name === 'task' && store.sessionId && store.currentTaskId) {
    return {
      name: 'taskRun',
      params: { taskId: store.currentTaskId },
      replace: true,
    }
  }
  if (to.name === 'execution' && store.sessionId) {
    if (store.currentTaskId) {
      return {
        name: 'taskRun',
        params: { taskId: store.currentTaskId },
        replace: true,
      }
    }
    return {
      name: 'task',
      replace: true,
    }
  }
  return true
})

export default router
