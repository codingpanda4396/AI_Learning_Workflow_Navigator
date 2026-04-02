<template>
  <section
    v-if="model.visible"
    data-testid="feedback-panel"
    class="rounded-2xl border border-emerald-200/90 bg-emerald-50/50 px-4 py-4 shadow-sm md:px-5"
  >
    <p class="text-xs font-semibold text-emerald-900">{{ model.title }}</p>

    <div class="mt-3 space-y-3 text-sm leading-relaxed">
      <div v-if="strengthsLine">
        <span class="font-medium text-emerald-900">{{ correctTitle }}</span>
        <p class="mt-1 line-clamp-4 text-slate-800">{{ strengthsLine }}</p>
      </div>

      <div v-if="keyIssues.length">
        <span class="font-medium text-accent-hover">{{ missingTitle }}</span>
        <ul class="mt-1 list-inside list-decimal space-y-1 text-slate-800">
          <li v-for="(issue, i) in keyIssues" :key="i" class="line-clamp-3">{{ issue }}</li>
        </ul>
      </div>
      <div v-else-if="gapLine">
        <span class="font-medium text-accent-hover">{{ missingTitle }}</span>
        <p class="mt-1 line-clamp-4 text-slate-800">{{ gapLine }}</p>
      </div>

      <div v-if="confusedLine" class="rounded-xl border border-slate-200/90 bg-white/70 px-3 py-3">
        <span class="font-medium text-slate-800">{{ confusedTitle }}</span>
        <p class="mt-1 line-clamp-4 text-slate-700">{{ confusedLine }}</p>
      </div>

      <div v-if="nextRestateLine">
        <span class="font-medium text-slate-800">{{ nextFixTitle }}</span>
        <p class="mt-1 line-clamp-4 text-slate-700">{{ nextRestateLine }}</p>
      </div>
      <div v-else-if="model.nextStep" class="line-clamp-3 text-slate-600">
        <span class="font-medium text-slate-700">{{ nextFixTitle }}：</span>
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
import type { WorkbenchFeedbackSchemaModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  model: ExecutionGuideFeedbackModel
  schema?: WorkbenchFeedbackSchemaModel
}>()

defineEmits<{
  action: [actionId: string]
}>()

const strengthsLine = computed(
  () => props.model.strengths?.trim() || props.model.mastered?.trim() || ''
)

const gapLine = computed(() => props.model.gap?.trim() || '')
const confusedLine = computed(() => props.model.errorTags?.filter(Boolean).join('、') || '')
const correctTitle = computed(() => props.schema?.correctTitle || '你已经说对了什么')
const missingTitle = computed(() => props.schema?.missingTitle || '你漏了什么')
const confusedTitle = computed(() => props.schema?.confusedTitle || '你混淆了什么')
const nextFixTitle = computed(() => props.schema?.nextFixTitle || '下一步该怎么修')

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
