import { createRouter, createWebHistory } from 'vue-router'
import { useLearningFlowStore } from '@/stores/learningFlow'

const routes = [
  {
    path: '/',
    redirect: '/goal-input'
  },
  {
    path: '/goal-input',
    name: 'GoalInput',
    component: () => import('@/views/GoalInputView.vue'),
    meta: { title: '目标输入' }
  },
  {
    path: '/diagnosis',
    name: 'Diagnosis',
    component: () => import('@/views/DiagnosisView.vue'),
    meta: { title: '学习诊断', requiresGoal: true }
  },
  {
    path: '/plan',
    name: 'Plan',
    component: () => import('@/views/LearningPlanView.vue'),
    meta: { title: '学习规划', requiresGoal: true, requiresDiagnosis: true }
  },
  {
    path: '/task-run',
    name: 'TaskRun',
    component: () => import('@/views/TaskRunView.vue'),
    meta: { title: '任务执行', requiresSession: true }
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('@/views/ReportView.vue'),
    meta: { title: '学习报告', requiresSession: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const store = useLearningFlowStore()

  if (to.meta.requiresGoal && !store.goalId) {
    next({ name: 'GoalInput' })
    return
  }

  if (to.meta.requiresDiagnosis && !store.diagnosisId) {
    next({ name: 'GoalInput' })
    return
  }

  if (to.meta.requiresSession && !store.sessionId) {
    next({ name: 'GoalInput' })
    return
  }

  next()
})

export default router
