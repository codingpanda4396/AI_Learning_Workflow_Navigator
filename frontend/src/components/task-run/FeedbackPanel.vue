<template>
  <section
    v-if="model.visible"
    data-testid="feedback-panel"
    class="rounded-2xl border border-emerald-200/90 bg-emerald-50/40 px-4 py-4 shadow-sm md:px-5"
  >
    <p class="text-xs font-semibold text-emerald-900">{{ model.title }}</p>

    <div class="mt-3 space-y-3 text-sm leading-relaxed">
      <div v-if="strengthsLine">
        <span class="font-medium text-emerald-900">你做对了什么</span>
        <p class="mt-1 text-slate-800">{{ strengthsLine }}</p>
      </div>

      <div v-if="keyIssues.length">
        <span class="font-medium text-accent-hover">当前最关键的问题</span>
        <ul class="mt-1 list-inside list-decimal space-y-1 text-slate-800">
          <li v-for="(issue, i) in keyIssues" :key="i">{{ issue }}</li>
        </ul>
      </div>
      <div v-else-if="gapLine">
        <span class="font-medium text-accent-hover">当前最关键的问题</span>
        <p class="mt-1 text-slate-800">{{ gapLine }}</p>
      </div>

      <div v-if="model.errorTags?.length" class="flex flex-wrap gap-2">
        <span
          v-for="tag in model.errorTags"
          :key="tag"
          class="rounded-full bg-white/90 px-2.5 py-0.5 text-xs font-medium text-slate-700 ring-1 ring-slate-200/90"
        >
          {{ tag }}
        </span>
      </div>

      <div v-if="nextRestateLine">
        <span class="font-medium text-slate-800">下一次重构要求</span>
        <p class="mt-1 text-slate-700">{{ nextRestateLine }}</p>
      </div>
      <div v-else-if="model.nextStep" class="text-slate-600">
        <span class="font-medium text-slate-700">接下来：</span>
        {{ model.nextStep }}
      </div>
    </div>

    <div v-if="model.actions.length" class="mt-4 flex flex-wrap gap-2">
      <button
        v-for="action in model.actions"
        :key="action.id"
        type="button"
        :data-testid="`feedback-action-${action.id}`"
        class="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-900 transition hover:border-accent/40 hover:bg-accent-muted/50"
        @click="$emit('action', action.id)"
      >
        {{ action.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExecutionGuideFeedbackModel } from '@/types/executionGuide'

const props = defineProps<{
  model: ExecutionGuideFeedbackModel
}>()

defineEmits<{
  action: [actionId: string]
}>()

const strengthsLine = computed(
  () => props.model.strengths?.trim() || props.model.mastered?.trim() || ''
)

const gapLine = computed(() => props.model.gap?.trim() || '')

const keyIssues = computed(() => {
  const k = props.model.keyIssues?.filter(Boolean) ?? []
  if (k.length) return k.slice(0, 2)
  const g = gapLine.value
  if (g) return [g]
  return []
})

const nextRestateLine = computed(
  () => props.model.nextRestateAsk?.trim() || ''
)
</script>
