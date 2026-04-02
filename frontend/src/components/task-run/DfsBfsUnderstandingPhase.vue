<template>
  <div class="grid gap-5 lg:grid-cols-[minmax(0,1.65fr)_minmax(300px,1fr)] lg:items-start">
    <UnderstandingChatPanel
      :messages="state.messages"
      :draft-input="state.draftInput"
      :busy="busy"
      :input-highlight="inputHighlight"
      :error="state.error"
      :completion-hint="state.completionHint"
      placeholder="补充追问，或点右侧认知动作…"
      class="w-full min-w-0"
      @send="handleSend"
      @update:draft-input="handleDraftUpdate"
    />

    <div class="w-full min-w-0 lg:sticky lg:top-20 lg:max-w-[360px] lg:justify-self-end">
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
import type { ScaffoldButton, UnderstandingPhaseState } from '@/types/executionWorkbench'

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
