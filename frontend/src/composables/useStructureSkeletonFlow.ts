import { reactive, ref, watch } from 'vue'
import { postCompleteStructureStage, postStructureSkeleton } from '@/api/learningScaffold'
import type { StructureSkeletonBlock } from '@/types/scaffoldEngine'

export function useStructureSkeletonFlow(opts: {
  taskId: () => string | undefined
  sessionId: () => string | null
  enabled: () => boolean
  reloadStage: () => Promise<void>
}) {
  const skeleton = ref<StructureSkeletonBlock | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastPromptKey = ref<string | null>(null)

  watch(
    () => opts.enabled(),
    (on) => {
      if (!on) {
        skeleton.value = null
        error.value = null
        lastPromptKey.value = null
      }
    }
  )

  async function fetchSkeleton(promptKey: string, followUpKind?: string) {
    const tid = opts.taskId()
    const sid = opts.sessionId()
    if (!tid || !sid || !opts.enabled()) return
    loading.value = true
    error.value = null
    try {
      const res = await postStructureSkeleton(tid, {
        sessionId: sid,
        promptKey,
        followUpKind: followUpKind || undefined,
      })
      skeleton.value = res.skeleton
      lastPromptKey.value = res.lastPromptKey
      await opts.reloadStage()
    } catch (e) {
      error.value = e instanceof Error ? e.message : '生成骨架失败'
      skeleton.value = null
    } finally {
      loading.value = false
    }
  }

  async function completeStage(optionalOneLiner?: string) {
    const tid = opts.taskId()
    const sid = opts.sessionId()
    if (!tid || !sid || !opts.enabled()) return null
    loading.value = true
    error.value = null
    try {
      const res = await postCompleteStructureStage(tid, {
        sessionId: sid,
        optionalOneLiner: optionalOneLiner?.trim() || undefined,
      })
      skeleton.value = null
      lastPromptKey.value = null
      await opts.reloadStage()
      return res
    } catch (e) {
      error.value = e instanceof Error ? e.message : '无法进入下一阶段'
      return null
    } finally {
      loading.value = false
    }
  }

  function clearPanel() {
    skeleton.value = null
    error.value = null
  }

  return reactive({
    skeleton,
    loading,
    error,
    lastPromptKey,
    fetchSkeleton,
    completeStage,
    clearPanel,
  })
}
