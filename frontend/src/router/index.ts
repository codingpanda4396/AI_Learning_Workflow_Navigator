import { createRouter, createWebHistory } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/goal' },
    { path: '/goal', name: 'goal', component: () => import('@/views/GoalInputView.vue'), meta: { step: 'goal' } },
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

router.beforeEach((to, _from, next) => {
  const store = useWorkflowStore()
  const step = to.meta.step as string

  if (step === 'diagnosis' && !store.goalId) {
    next({ name: 'goal' })
    return
  }
  if (step === 'plan' && (!store.goalId || !store.diagnosisId)) {
    next(store.goalId ? { name: 'diagnosis' } : { name: 'goal' })
    return
  }
  if ((step === 'task' || step === 'report') && !store.sessionId) {
    next({ name: 'plan' })
    return
  }
  if (to.name === 'task' && store.sessionId && store.currentTaskId) {
    next({
      name: 'taskRun',
      params: { taskId: store.currentTaskId },
      replace: true,
    })
    return
  }
  next()
})

export default router
