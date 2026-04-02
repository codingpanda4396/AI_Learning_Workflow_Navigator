import { computed, reactive, ref, watch } from 'vue'
import { getLearningScaffoldStage, submitLearningScaffoldAction } from '@/api/learningScaffold'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import type { LearningScaffoldActionResult, StageScaffold } from '@/types/scaffoldEngine'

/** 四阶段展示名，与后端 stageKey 对齐 */
export function scaffoldStageLabel(stageKey: string | undefined): string {
  if (!stageKey) return '脚手架'
  const zh = phaseCodeToFullZh(stageKey)
  return zh || stageKey
}

type StageMode = 'fast' | 'full'

export function useLearningScaffoldEngine(opts: {
  taskId: () => string | null | undefined
  sessionId: () => string | null | undefined
  /** 后端 STAGE_KEY，需与引擎当前阶段一致，如 STRUCTURE */
  stageApiKey: () => string | undefined
  enabled: () => boolean
}) {
  const stage = ref<StageScaffold | null>(null)
  const loading = ref(false)
  const submitting = ref(false)
  const error = ref<string | null>(null)
  const lastResult = ref<LearningScaffoldActionResult | null>(null)

  const structureStageComplete = computed(() => stage.value?.structureStageComplete === true)
  const understandingStageComplete = computed(() => stage.value?.understandingStageComplete === true)

  /** STRUCTURE -> REFLECTION 全部完成后再退出动作卡主区 */
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

  let loadGeneration = 0
  let activeFastLoadKey: string | null = null
  let activeFastLoadPromise: Promise<StageScaffold | null> | null = null
  let activeFullLoadKey: string | null = null
  let activeFullLoadPromise: Promise<StageScaffold | null> | null = null
  let currentStageLoadKey: string | null = null
  const currentStageMode = ref<StageMode | null>(null)
  const inflightStageRequests = new Map<string, Promise<StageScaffold>>()

  function makeLoadKey(tid: string, sid: string, sk: string) {
    return `${tid}::${sid}::${sk}`
  }

  function makeRequestKey(tid: string, sid: string, sk: string, mode: StageMode) {
    return `${makeLoadKey(tid, sid, sk)}::${mode}`
  }

  function assignStage(nextStage: StageScaffold, mode: StageMode, loadKey: string) {
    stage.value = nextStage
    currentStageMode.value = mode
    currentStageLoadKey = loadKey
  }

  async function requestStage(
    tid: string,
    sid: string,
    sk: string,
    mode: StageMode
  ): Promise<StageScaffold> {
    const requestKey = makeRequestKey(tid, sid, sk, mode)
    const inflight = inflightStageRequests.get(requestKey)
    if (inflight) return inflight

    const requestPromise = getLearningScaffoldStage(tid, sid, sk, mode).finally(() => {
      if (inflightStageRequests.get(requestKey) === requestPromise) {
        inflightStageRequests.delete(requestKey)
      }
    })

    inflightStageRequests.set(requestKey, requestPromise)
    return requestPromise
  }

  async function loadStage(options?: { force?: boolean }) {
    const tid = opts.taskId()
    const sid = opts.sessionId()
    const sk = opts.stageApiKey()
    if (!tid || !sid || !sk || !opts.enabled()) return null

    const force = options?.force === true
    const loadKey = makeLoadKey(tid, sid, sk)

    if (!force && currentStageLoadKey === loadKey && currentStageMode.value === 'full' && stage.value) {
      return stage.value
    }
    if (activeFastLoadKey === loadKey && activeFastLoadPromise) {
      return activeFastLoadPromise
    }
    if (!force && activeFullLoadKey === loadKey && activeFullLoadPromise) {
      return activeFullLoadPromise
    }

    const gen = ++loadGeneration
    loading.value = true
    error.value = null

    const fastPromise = (async () => {
      try {
        const fast = await requestStage(tid, sid, sk, 'fast')
        if (gen === loadGeneration) {
          assignStage(fast, 'fast', loadKey)
        }
        return fast
      } catch (e) {
        if (gen === loadGeneration) {
          error.value = e instanceof Error ? e.message : '加载脚手架失败'
          stage.value = null
          currentStageMode.value = null
          currentStageLoadKey = null
        }
        return null
      } finally {
        if (gen === loadGeneration) {
          loading.value = false
        }
        if (activeFastLoadKey === loadKey) {
          activeFastLoadKey = null
          activeFastLoadPromise = null
        }
      }
    })()

    activeFastLoadKey = loadKey
    activeFastLoadPromise = fastPromise

    const fast = await fastPromise
    if (!fast) return null

    const shouldHydrate =
      force ||
      currentStageLoadKey !== loadKey ||
      currentStageMode.value !== 'full' ||
      stage.value?.currentActionId !== fast.currentActionId

    if (shouldHydrate) {
      void hydrateWorkbenchSoft(gen, tid, sid, sk, loadKey)
    }

    return fast
  }

  async function hydrateWorkbenchSoft(
    gen: number,
    tid: string,
    sid: string,
    sk: string,
    loadKey: string
  ): Promise<void> {
    if (currentStageLoadKey === loadKey && currentStageMode.value === 'full') {
      return
    }
    if (activeFullLoadKey === loadKey && activeFullLoadPromise) {
      await activeFullLoadPromise
      return
    }

    const fullPromise = (async () => {
      try {
        const full = await requestStage(tid, sid, sk, 'full')
        if (gen !== loadGeneration) return null
        assignStage(full, 'full', loadKey)
        return full
      } catch {
        return null
      } finally {
        if (activeFullLoadKey === loadKey) {
          activeFullLoadKey = null
          activeFullLoadPromise = null
        }
      }
    })()

    activeFullLoadKey = loadKey
    activeFullLoadPromise = fullPromise
    await fullPromise
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

      if (res.updatedStage) {
        assignStage(res.updatedStage, 'fast', makeLoadKey(tid, sid, res.updatedStage.stageKey ?? sk))
      } else {
        await loadStage({ force: true })
      }

      return res
    } catch (e) {
      error.value = e instanceof Error ? e.message : '提交失败'
      return null
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => [opts.enabled(), opts.taskId(), opts.sessionId(), opts.stageApiKey()] as const,
    () => {
      if (opts.enabled()) {
        void loadStage()
      } else {
        loadGeneration++
        stage.value = null
        currentStageMode.value = null
        currentStageLoadKey = null
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
