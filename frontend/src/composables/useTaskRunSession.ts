import type { Ref } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { getErrorMessage } from '@/api/request'
import { getCurrentTask } from '@/api/task'
import { useWorkflowStore } from '@/stores/workflow'
import type { CurrentTaskData, ProgressItem } from '@/types/dto'

type WorkflowStore = ReturnType<typeof useWorkflowStore>

/** 脚手架引擎上用于对齐 stageKey 的最小形状 */
export type ScaffoldEngineStageSource = {
  stage?: { stageKey?: string | null } | null
}

/**
 * 任务执行页：封装拉取 current-task、与路由/store 同步（refs 由调用方持有，以便在 useLearningScaffoldEngine 之前声明 task）。
 */
export function useTaskRunSession(opts: {
  route: RouteLocationNormalizedLoaded
  router: Router
  store: WorkflowStore
  scaffoldEngine: ScaffoldEngineStageSource
  task: Ref<CurrentTaskData | null>
  progress: Ref<ProgressItem | null>
  engineStageKeyHint: Ref<string | null>
  taskStartedAt: Ref<number>
  loading: Ref<boolean>
  error: Ref<string | null>
  onTaskIdentityChange: () => void
}) {
  const {
    route,
    router,
    store,
    scaffoldEngine,
    task,
    progress,
    engineStageKeyHint,
    taskStartedAt,
    loading,
    error,
    onTaskIdentityChange,
  } = opts

  async function fetchTask() {
    if (!store.sessionId) return
    if (route.name !== 'task' && route.name !== 'taskRun') return
    const paramId = typeof route.params.taskId === 'string' ? route.params.taskId : null
    if (task.value?.taskId && paramId && paramId !== task.value.taskId) {
      onTaskIdentityChange()
    }
    loading.value = true
    error.value = null
    try {
      const data = await getCurrentTask(store.sessionId)
      const previousTaskId = task.value?.taskId ?? null
      const taskChanged = !!previousTaskId && !!data.taskId && data.taskId !== previousTaskId

      if (taskChanged) {
        onTaskIdentityChange()
      }

      store.currentTask = data
      store.progress = data.progress
      task.value = data
      progress.value = data.progress
      engineStageKeyHint.value = scaffoldEngine.stage?.stageKey ?? data.currentStage
      if (taskChanged || !previousTaskId) {
        taskStartedAt.value = Date.now()
      }

      if (!data.taskId) {
        store.currentTaskId = null
        router.push('/report')
        return
      }

      store.currentTaskId = data.taskId
      const routeTaskId = typeof route.params.taskId === 'string' ? route.params.taskId : ''
      if (
        route.name === 'task' ||
        (route.name === 'taskRun' && routeTaskId && routeTaskId !== data.taskId)
      ) {
        router.replace({ name: 'taskRun', params: { taskId: data.taskId } })
      }
    } catch (err) {
      error.value = getErrorMessage(err)
    } finally {
      loading.value = false
    }
  }

  return { fetchTask }
}
