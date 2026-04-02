<template>
  <Transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="opacity-0 translate-y-2"
    enter-to-class="opacity-100 translate-y-0"
  >
    <div
      v-if="feedback"
      class="rounded-xl border px-5 py-4"
      :class="toneClasses"
    >
      <p class="text-sm leading-relaxed text-slate-800">{{ feedback.body }}</p>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  feedback: { tone: 'positive' | 'neutral' | 'redirect'; body: string } | null
}>()

const toneClasses = computed(() => {
  if (!props.feedback) return ''
  switch (props.feedback.tone) {
    case 'positive':
      return 'border-emerald-200 bg-emerald-50/60'
    case 'redirect':
      return 'border-accent/25 bg-accent-muted/60'
    default:
      return 'border-slate-200 bg-slate-50/60'
  }
})
</script>
