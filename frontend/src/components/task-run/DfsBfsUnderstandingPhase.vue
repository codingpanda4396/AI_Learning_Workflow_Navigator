<template>
  <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_300px]">
    <UnderstandingChatPanel
      :messages="state.messages"
      :draft-input="state.draftInput"
      :busy="busy"
      :input-highlight="inputHighlight"
      placeholder="输入你的问题，或点右侧脚手架..."
      class="min-h-[480px]"
      @send="handleSend"
      @update:draft-input="handleDraftUpdate"
    />

    <div class="lg:sticky lg:top-20 lg:self-start">
      <PromptScaffoldInjectPanel
        :buttons="scaffoldButtons"
        :busy="busy"
        @inject="handleInject"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import UnderstandingChatPanel from '@/components/task-run/UnderstandingChatPanel.vue'
import PromptScaffoldInjectPanel from '@/components/task-run/PromptScaffoldInjectPanel.vue'
import type { UnderstandingPhaseState, ScaffoldButton } from '@/types/executionWorkbench'

defineProps<{
  state: UnderstandingPhaseState
  scaffoldButtons: ScaffoldButton[]
  busy: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
  'update:draftInput': [value: string]
  inject: [prompt: string, scaffoldKey: string]
}>()

const inputHighlight = ref(false)

function handleSend(text: string) {
  emit('send', text)
}

function handleDraftUpdate(value: string) {
  emit('update:draftInput', value)
}

function handleInject(prompt: string, scaffoldKey: string) {
  emit('update:draftInput', prompt)
  emit('inject', prompt, scaffoldKey)
  inputHighlight.value = true
  setTimeout(() => {
    inputHighlight.value = false
  }, 1200)
}
</script>
