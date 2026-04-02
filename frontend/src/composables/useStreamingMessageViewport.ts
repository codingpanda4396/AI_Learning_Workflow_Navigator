import { nextTick, ref, watch, type Ref } from 'vue'
import type { ChatMessage } from '@/types/executionWorkbench'

const BOTTOM_THRESHOLD_PX = 48

export function useStreamingMessageViewport(
  getMessages: () => ChatMessage[],
  getBusy: () => boolean,
) {
  const messagesRef: Ref<HTMLElement | null> = ref(null)
  const isPinnedToBottom = ref(true)

  function computePinnedState(element: HTMLElement): boolean {
    const distanceFromBottom =
      element.scrollHeight - element.clientHeight - element.scrollTop
    return distanceFromBottom <= BOTTOM_THRESHOLD_PX
  }

  function handleScroll() {
    if (!messagesRef.value) return
    isPinnedToBottom.value = computePinnedState(messagesRef.value)
  }

  async function scrollToBottom() {
    await nextTick()
    if (!messagesRef.value) return
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    isPinnedToBottom.value = true
  }

  watch(
    () => {
      const messages = getMessages()
      const lastMessage = messages[messages.length - 1]
      return [
        messages.length,
        lastMessage?.id ?? '',
        lastMessage?.content ?? '',
        getBusy(),
      ]
    },
    async () => {
      if (!messagesRef.value || !isPinnedToBottom.value) return
      await scrollToBottom()
    },
    { immediate: true },
  )

  return {
    messagesRef,
    handleScroll,
  }
}
