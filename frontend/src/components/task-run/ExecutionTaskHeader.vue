<template>
  <header
    data-testid="execution-task-header"
    class="overflow-hidden rounded-[32px] border border-slate-200 bg-[linear-gradient(135deg,_rgba(255,255,255,0.98),_rgba(240,249,255,0.96)_52%,_rgba(248,250,252,0.98))] p-5 shadow-card md:p-7"
  >
    <!-- 第一层：阶段 + 当前唯一动作锚点 -->
    <div class="flex flex-wrap items-center gap-2">
      <span
        v-if="phaseBadge"
        class="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary"
      >
        {{ phaseBadge }}
      </span>
      <span
        v-if="model.strategyLine"
        class="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-700"
      >
        {{ model.strategyLine }}
      </span>
    </div>

    <p class="mt-4 text-xs font-medium uppercase tracking-[0.16em] text-slate-500">
      你现在要做的事
    </p>
    <p class="mt-2 text-lg font-semibold leading-snug text-slate-950 md:text-xl">
      {{ anchorLine }}
    </p>

    <!-- 第二层：知识点名（弱化为上下文） -->
    <div class="mt-6 flex flex-wrap items-start justify-between gap-6 border-t border-slate-100/90 pt-6">
      <div class="min-w-0 flex-1 space-y-2">
        <p class="text-xs font-medium text-slate-500">知识点</p>
        <h1 class="text-xl font-semibold tracking-tight text-slate-950 md:text-2xl">
          {{ oc.knowledgePointName }}
        </h1>
        <p class="text-sm leading-6 text-slate-600">
          {{ oc.roundGoal }}
        </p>
      </div>

      <div
        class="shrink-0 rounded-[22px] border border-sky-100 bg-white/95 px-5 py-4 shadow-sm md:min-w-[140px]"
      >
        <p class="text-xs font-medium text-slate-500">预计耗时</p>
        <p class="mt-1 text-xl font-semibold text-slate-950">{{ oc.estimatedTimeLabel }}</p>
        <p class="mt-3 text-xs text-slate-500">
          {{ typeLabel }}
        </p>
      </div>
    </div>

    <details
      v-if="completionLines.length"
      class="mt-6 rounded-[20px] border border-slate-200/90 bg-white/70"
    >
      <summary
        class="cursor-pointer px-4 py-3 text-xs font-medium text-slate-600 transition hover:text-slate-900"
      >
        完成标准（{{ completionLines.length }}）
      </summary>
      <ul class="border-t border-slate-100 px-4 py-3 text-sm leading-6 text-slate-600">
        <li v-for="(line, i) in completionLines" :key="i" class="border-b border-slate-50 py-2 last:border-0">
          {{ line }}
        </li>
      </ul>
    </details>

    <details
      v-if="model.knowledgePoints?.length"
      class="mt-3 rounded-[20px] border border-slate-200/90 bg-white/70"
    >
      <summary
        class="cursor-pointer px-4 py-3 text-xs font-medium text-slate-600 transition hover:text-slate-900"
      >
        知识点目录（{{ model.knowledgePoints.length }}）
      </summary>
      <ul class="border-t border-slate-100 px-4 py-3 text-sm text-slate-600">
        <li
          v-for="point in model.knowledgePoints"
          :key="point.id"
          class="flex flex-wrap items-baseline gap-2 border-b border-slate-50 py-2 last:border-0"
        >
          <span class="text-xs text-slate-400">{{ point.index }}.</span>
          <span :class="point.status === 'active' ? 'font-semibold text-slate-900' : ''">
            {{ point.title }}
          </span>
          <span v-if="point.status === 'done'" class="text-xs text-emerald-600">已完成</span>
          <span v-else-if="point.status === 'active'" class="text-xs text-sky-600">当前</span>
        </li>
      </ul>
    </details>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import type { ExecutionGuideHeaderModel, ExecutionOperationConsoleModel } from '@/types/executionGuide'
import type { KnowledgePointExecutionTemplate } from '@/types/knowledgePack'

const props = withDefaults(
  defineProps<{
    model: ExecutionGuideHeaderModel
  }>(),
  {}
)

const TYPE_LABELS: Record<KnowledgePointExecutionTemplate, string> = {
  CONCEPT: '概念辨析',
  PROCESS: '机制过程',
  STRUCTURE: '结构对象',
  PROBLEM: '题目应用',
}

const phaseBadge = computed(() => {
  const zh = props.model.phaseDisplayZh?.trim()
  if (zh) return zh
  const code = props.model.phaseCode?.trim()
  return code ? phaseCodeToFullZh(code) : ''
})

const oc = computed<ExecutionOperationConsoleModel>(() => {
  const m = props.model.operationConsole
  if (m) return m
  return {
    knowledgePointName: props.model.heroTitle || props.model.title,
    knowledgePointType: 'CONCEPT',
    roundGoal: props.model.heroSubtitle || '',
    completionStandardLines: props.model.completionCriteria?.length
      ? props.model.completionCriteria
      : ['做到关键判断清晰'],
    estimatedTimeLabel: props.model.estimatedTime || '',
  }
})

const typeLabel = computed(() => {
  const t = oc.value.knowledgePointType
  return `类型 · ${TYPE_LABELS[t] ?? t}`
})

const anchorLine = computed(() => {
  const a = props.model.anchorActionLine?.trim()
  if (a) return a
  return oc.value.roundGoal || '完成当前这一步的最小产出。'
})

const completionLines = computed(() => {
  const fromOc = oc.value.completionStandardLines?.filter(Boolean) ?? []
  if (fromOc.length) return fromOc.slice(0, 3)
  return (props.model.completionCriteria ?? []).filter(Boolean).slice(0, 3)
})
</script>
