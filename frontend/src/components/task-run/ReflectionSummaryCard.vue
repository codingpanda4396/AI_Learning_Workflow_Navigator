<template>
  <section
    v-if="summary?.record"
    class="rounded-2xl border border-emerald-200/80 bg-gradient-to-br from-emerald-50/90 to-white p-5 shadow-sm ring-1 ring-emerald-100/80"
    data-testid="reflection-summary-card"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-emerald-800">反思沉淀</p>
    <h3 class="mt-1 text-lg font-semibold text-slate-900">本轮可带走的认知资产</h3>
    <p v-if="summary.systemObservation" class="mt-2 text-sm text-emerald-900/90">
      {{ summary.systemObservation }}
    </p>

    <dl class="mt-4 space-y-3 text-sm">
      <div v-if="summary.record.errorPattern">
        <dt class="font-medium text-slate-600">我的典型错误</dt>
        <dd class="mt-1 leading-relaxed text-slate-900">{{ summary.record.errorPattern }}</dd>
      </div>
      <div v-if="summary.record.rootCause">
        <dt class="font-medium text-slate-600">错误根因</dt>
        <dd class="mt-1 leading-relaxed text-slate-900">{{ summary.record.rootCause }}</dd>
      </div>
      <div v-if="summary.record.decisionRule">
        <dt class="font-medium text-slate-600">我的判断规则</dt>
        <dd class="mt-1 leading-relaxed text-slate-900">{{ summary.record.decisionRule }}</dd>
      </div>
      <div v-if="summary.record.capabilityName">
        <dt class="font-medium text-slate-600">我获得的能力</dt>
        <dd class="mt-1 leading-relaxed text-slate-900">{{ summary.record.capabilityName }}</dd>
      </div>
      <div v-if="summary.record.futureStrategy">
        <dt class="font-medium text-slate-600">下一步建议</dt>
        <dd class="mt-1 leading-relaxed text-slate-900">{{ summary.record.futureStrategy }}</dd>
      </div>
    </dl>

    <div
      v-if="insightLines.length"
      class="mt-4 rounded-xl border border-slate-200/80 bg-white/70 px-3 py-2 text-xs text-slate-600"
    >
      <p class="font-semibold text-slate-700">系统补充观察</p>
      <ul class="mt-1 list-inside list-disc space-y-0.5">
        <li v-for="(line, i) in insightLines" :key="i">{{ line }}</li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ReflectionSummary } from '@/types/scaffoldEngine'

const props = defineProps<{
  summary: ReflectionSummary | null | undefined
}>()

const insightLines = computed(() => {
  const ins = props.summary?.insight
  if (!ins) return []
  const lines: string[] = []
  if (ins.mostDifficultActionId) {
    lines.push(`训练中耗时较多的动作：${ins.mostDifficultActionId}`)
  }
  if (ins.repeatedErrorTypes?.length) {
    lines.push(`反复出现的问题类型：${ins.repeatedErrorTypes.join('、')}`)
  }
  if (ins.totalAttempts != null && ins.totalAttempts > 0) {
    lines.push(`训练阶段累计提交 ${ins.totalAttempts} 次`)
  }
  if (ins.improvedAspects?.length) {
    lines.push(...ins.improvedAspects)
  }
  return lines
})
</script>
