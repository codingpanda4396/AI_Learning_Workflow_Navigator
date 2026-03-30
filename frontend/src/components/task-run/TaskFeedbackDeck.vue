<template>
  <section
    v-if="model.visible"
    data-testid="feedback-panel"
    class="space-y-3"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">本轮反馈</p>

    <div class="grid gap-3 md:grid-cols-2">
      <article
        v-if="strengthsLine"
        class="rounded-xl border border-emerald-200/90 bg-emerald-50/70 px-4 py-3 shadow-sm"
      >
        <p class="text-xs font-semibold text-emerald-900">{{ correctTitle }}</p>
        <p class="mt-1 line-clamp-4 text-sm leading-relaxed text-slate-800">{{ strengthsLine }}</p>
      </article>

      <article
        v-if="gapBlock"
        class="rounded-xl border border-amber-200/90 bg-amber-50/70 px-4 py-3 shadow-sm"
      >
        <p class="text-xs font-semibold text-amber-950">{{ missingTitle }}</p>
        <p v-if="gapBlock.single" class="mt-1 line-clamp-4 text-sm text-slate-800">{{ gapBlock.single }}</p>
        <ul
          v-else-if="gapBlock.list?.length"
          class="mt-1 list-inside list-decimal space-y-1 text-sm text-slate-800"
        >
          <li v-for="(issue, i) in gapBlock.list" :key="i" class="line-clamp-3">{{ issue }}</li>
        </ul>
      </article>

      <article
        v-if="confusedLine"
        class="rounded-xl border border-violet-200/90 bg-violet-50/60 px-4 py-3 shadow-sm md:col-span-2"
      >
        <p class="text-xs font-semibold text-violet-950">{{ confusedTitle }}</p>
        <p class="mt-1 line-clamp-4 text-sm text-slate-800">{{ confusedLine }}</p>
      </article>

      <article
        v-if="nextBlock"
        class="rounded-xl border border-slate-200 bg-slate-50/90 px-4 py-3 shadow-sm md:col-span-2"
      >
        <p class="text-xs font-semibold text-slate-800">{{ nextFixTitle }}</p>
        <p class="mt-1 line-clamp-4 text-sm text-slate-800">{{ nextBlock }}</p>
      </article>
    </div>

    <div v-if="model.actions.length" class="flex flex-wrap gap-2 pt-1">
      <button
        v-for="action in model.actions"
        :key="action.id"
        type="button"
        :data-testid="`feedback-action-${action.id}`"
        class="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-900 transition hover:border-primary/40 hover:bg-primary/5"
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
const correctTitle = computed(() => props.schema?.correctTitle || '你说对了什么')
const missingTitle = computed(() => props.schema?.missingTitle || '你漏了什么')
const confusedTitle = computed(() => props.schema?.confusedTitle || '你混淆了什么')
const nextFixTitle = computed(() => props.schema?.nextFixTitle || '下一步该修什么')

const keyIssues = computed(() => {
  const k = props.model.keyIssues?.filter(Boolean) ?? []
  if (k.length) return k.slice(0, 2)
  const g = gapLine.value
  if (g) return [g]
  return []
})

const gapBlock = computed(() => {
  const list = keyIssues.value
  if (list.length > 1) return { single: undefined as string | undefined, list }
  if (list.length === 1) return { single: list[0] as string, list: undefined as string[] | undefined }
  const g = gapLine.value
  if (g) return { single: g, list: undefined }
  return null
})

const nextRestateLine = computed(() => props.model.nextRestateAsk?.trim() || '')
const nextBlock = computed(() => nextRestateLine.value || props.model.nextStep?.trim() || '')
</script>
