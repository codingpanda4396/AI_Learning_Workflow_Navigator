import { computed, reactive, ref, watch } from 'vue'
import { getLearningScaffoldStage, submitLearningScaffoldAction } from '@/api/learningScaffold'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import type { LearningScaffoldActionResult, StageScaffold } from '@/types/scaffoldEngine'

/** 四阶段展示名（与后端 stageKey 对齐） */
export function scaffoldStageLabel(stageKey: string | undefined): string {
  if (!stageKey) return '脚手架'
  const zh = phaseCodeToFullZh(stageKey)
  return zh || stageKey
}

export function useLearningScaffoldEngine(opts: {
  taskId: () => string | null | undefined
  sessionId: () => string | null | undefined
  enabled: () => boolean
}) {
  const stage = ref<StageScaffold | null>(null)
  const loading = ref(false)
  const submitting = ref(false)
  const error = ref<string | null>(null)
  const lastResult = ref<LearningScaffoldActionResult | null>(null)

  const structureStageComplete = computed(() => stage.value?.structureStageComplete === true)
  const understandingStageComplete = computed(() => stage.value?.understandingStageComplete === true)

  /** STRUCTURE → REFLECTION（占位）全部完成后再退出动作卡主区 */
  const scaffoldEngineComplete = computed(() => {
    const keys = stage.value?.completedStageKeys
    if (keys?.length) {
      return keys.includes('REFLECTION')
    }
    return (
      stage.value?.structureStageComplete === true &&
      stage.value?.understandingStageComplete === true &&
      stage.value?.trainingStageComplete === true &&
      stage.value?.reflectionStageComplete === true
    )
  })

  const currentCard = computed(() => {
    const id = stage.value?.currentActionId
    if (!id || !stage.value?.actionCards?.length) return null
    return stage.value.actionCards.find((c) => c.actionId === id) ?? null
  })

  async function loadStage() {
    const tid = opts.taskId()
    const sid = opts.sessionId()
    if (!tid || !sid || !opts.enabled()) return
    loading.value = true
    error.value = null
    try {
      stage.value = await getLearningScaffoldStage(tid, sid)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载脚手架失败'
      stage.value = null
    } finally {
      loading.value = false
    }
  }

  async function submit(userInput: string) {
    const tid = opts.taskId()
    const sid = opts.sessionId()
    const card = currentCard.value
    const sk = stage.value?.stageKey
    if (!tid || !sid || !card || !sk) return null
    submitting.value = true
    error.value = null
    try {
      const res = await submitLearningScaffoldAction(tid, {
        sessionId: sid,
        stageKey: sk,
        actionId: card.actionId,
        userInput,
      })
      lastResult.value = res
      // 同一 stageKey 内仍会切换 currentActionId（如 STRUCTURE 多卡 MCQ），仅依赖 stageComplete 会漏刷新，
      // 导致后续提交仍用旧 actionId，后端拒绝且引擎状态不完整。
      stage.value = res.updatedStage ?? (await getLearningScaffoldStage(tid, sid))
      return res
    } catch (e) {
      error.value = e instanceof Error ? e.message : '提交失败'
      return null
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => [opts.enabled(), opts.taskId(), opts.sessionId()] as const,
    () => {
      if (opts.enabled()) void loadStage()
      else {
        stage.value = null
        lastResult.value = null
      }
    },
    { immediate: true }
  )

  watch(
    () => currentCard.value?.actionId,
    (id, prev) => {
      if (id != null && id !== prev && prev != null) {
        lastResult.value = null
      }
    }
  )

  return reactive({
    stage,
    loading,
    submitting,
    error,
    lastResult,
    structureStageComplete,
    understandingStageComplete,
    scaffoldEngineComplete,
    currentCard,
    loadStage,
    submit,
  })
}
