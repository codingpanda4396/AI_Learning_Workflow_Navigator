<template>
  <div class="flex w-full" :class="role === 'user' ? 'justify-end' : 'justify-start'">
    <div
      class="flex max-w-[92%] items-start gap-3"
      :class="role === 'user' ? 'flex-row-reverse' : 'flex-row'"
    >
      <div
        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-semibold"
        :class="role === 'user' ? 'bg-primary/10 text-primary' : 'bg-slate-900 text-white'"
      >
        {{ role === 'user' ? '\u6211' : 'AI' }}
      </div>
      <div class="rounded-input px-3 py-2 text-sm leading-relaxed shadow-sm" :class="bubbleClass">
        <p class="whitespace-pre-wrap break-words">
          {{ content }}<span v-if="showStreamingCursor" class="typing-cursor" aria-hidden="true" />
        </p>
        <p
          v-if="role === 'ai' && source"
          class="mt-2 text-[11px] font-medium uppercase tracking-[0.08em] text-text-secondary/70"
        >
          {{ source }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  role: 'ai' | 'user'
  content: string
  type?: 'prompt' | 'feedback' | 'hint'
  source?: string
  streaming?: boolean
}>()

const bubbleClass = computed(() => {
  if (props.role === 'user') {
    return 'bg-primary text-white'
  }
  if (props.type === 'feedback') {
    return 'border border-amber-200 bg-amber-50 text-text-primary'
  }
  if (props.type === 'hint') {
    return 'border border-sky-200 bg-sky-50 text-text-primary'
  }
  return 'border border-border bg-slate-50 text-text-primary'
})

const showStreamingCursor = computed(
  () => props.role === 'ai' && props.streaming && props.content.trim().length > 0
)
</script>

<style scoped>
.typing-cursor {
  display: inline-block;
  width: 0.6em;
  height: 1.1em;
  margin-left: 0.08em;
  vertical-align: -0.18em;
  border-radius: 999px;
  background: currentColor;
  animation: blink-cursor 1s steps(1, end) infinite;
}

@keyframes blink-cursor {
  0%,
  49% {
    opacity: 1;
  }

  50%,
  100% {
    opacity: 0;
  }
}
</style>
