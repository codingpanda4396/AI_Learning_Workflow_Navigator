import { computed, onBeforeUnmount, onMounted, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { useTutorStore } from '@/stores/tutor'
import type { TutorContext } from '@/utils/buildTutorContext'
import { buildTutorRequestContent } from '@/utils/buildTutorContext'

const DEFAULT_QUICK_PROMPTS = [
  '这一步关键是什么？',
  '请给我一个提示，不要直接告诉答案',
  '用更简单的话解释',
  '举个例子',
  '我哪里容易错？',
]

interface UseTutorPanelOptions {
  sessionId: MaybeRefOrGetter<number | null>
  taskId: MaybeRefOrGetter<number | null>
  context: MaybeRefOrGetter<TutorContext>
  quickPrompts?: string[]
}

export function useTutorPanel(options: UseTutorPanelOptions) {
  const tutorStore = useTutorStore()
  const isOpen = ref(false)
  const input = ref('')
  const loadedKey = ref<string | null>(null)

  const sessionId = computed(() => toValue(options.sessionId))
  const taskId = computed(() => toValue(options.taskId))
  const context = computed(() => toValue(options.context))
  const quickPrompts = options.quickPrompts ?? DEFAULT_QUICK_PROMPTS
  const canUseTutor = computed(() => sessionId.value !== null && taskId.value !== null && taskId.value > 0)
  const contextKey = computed(() => `${sessionId.value ?? 'na'}:${taskId.value ?? 'na'}`)

  async function ensureMessagesLoaded(force = false) {
    if (!canUseTutor.value) {
      return
    }
    if (!force && loadedKey.value === contextKey.value) {
      return
    }
    await tutorStore.load(sessionId.value as number, taskId.value as number)
    loadedKey.value = contextKey.value
  }

  async function openPanel() {
    isOpen.value = true
    await ensureMessagesLoaded()
  }

  function closePanel() {
    isOpen.value = false
  }

  async function togglePanel() {
    if (isOpen.value) {
      closePanel()
      return
    }
    await openPanel()
  }

  async function sendMessage(message = input.value) {
    const content = message.trim()
    if (!content || !canUseTutor.value || tutorStore.sendingMessage) {
      return
    }

    const requestContent = buildTutorRequestContent(content, context.value)
    try {
      try {
        await tutorStore.sendStream(sessionId.value as number, taskId.value as number, requestContent)
      } catch {
        await tutorStore.send(sessionId.value as number, taskId.value as number, requestContent)
      }
      input.value = ''
    } catch (error) {
      console.error('Tutor send failed:', error)
    }
  }

  async function retrySend() {
    try {
      await tutorStore.retryLastSend()
      input.value = ''
    } catch (error) {
      console.error('Tutor retry failed:', error)
    }
  }

  async function useQuickPrompt(prompt: string) {
    input.value = prompt
    await sendMessage(prompt)
  }

  function setInput(value: string) {
    input.value = value
  }

  function handleEscape(event: KeyboardEvent) {
    if (event.key === 'Escape' && isOpen.value) {
      closePanel()
    }
  }

  watch(
    contextKey,
    async (nextKey, prevKey) => {
      if (nextKey === prevKey) {
        return
      }
      tutorStore.reset()
      loadedKey.value = null
      if (isOpen.value) {
        await ensureMessagesLoaded()
      }
    },
    { immediate: true },
  )

  onMounted(() => {
    window.addEventListener('keydown', handleEscape)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleEscape)
  })

  return {
    isOpen,
    input,
    setInput,
    quickPrompts,
    canUseTutor,
    messages: computed(() => tutorStore.messages),
    loading: computed(() => tutorStore.loadingMessages),
    sending: computed(() => tutorStore.sendingMessage),
    loadError: computed(() => tutorStore.loadError),
    sendError: computed(() => tutorStore.sendError),
    openPanel,
    closePanel,
    togglePanel,
    ensureMessagesLoaded,
    sendMessage,
    retrySend,
    useQuickPrompt,
  }
}
