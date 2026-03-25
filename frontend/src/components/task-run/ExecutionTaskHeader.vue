<template>
  <header
    data-testid="execution-task-header"
    class="overflow-hidden rounded-[32px] border border-slate-200 bg-[linear-gradient(135deg,_rgba(255,255,255,0.98),_rgba(240,249,255,0.96)_52%,_rgba(248,250,252,0.98))] p-5 shadow-card md:p-7"
  >
    <p class="text-xs text-slate-500">
      {{ model.metaLine }}
    </p>

    <div class="mt-4 flex flex-wrap items-start justify-between gap-6">
      <div class="min-w-0 flex-1">
        <h1 class="text-2xl font-semibold tracking-tight text-slate-950 md:text-3xl">
          {{ model.heroTitle }}
        </h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
          {{ model.heroSubtitle }}
        </p>
        <div class="mt-5 grid gap-2 text-sm text-slate-600 md:max-w-xl">
          <template v-if="showScaffoldHints">
            <p class="font-medium text-slate-800">你可以怎么问 AI</p>
            <p class="leading-6">
              下面有三种「开场问法」，点一下就会发给导师；也可以自己在对话框里写。
            </p>
          </template>
          <p class="font-medium text-slate-800">做到什么算完成</p>
          <ul class="list-disc space-y-1 pl-5 leading-6">
            <li v-for="(line, i) in model.completionCriteria" :key="i">
              {{ line }}
            </li>
          </ul>
        </div>
      </div>

      <div
        class="shrink-0 rounded-[22px] border border-sky-100 bg-white/95 px-5 py-4 shadow-sm md:min-w-[160px]"
      >
        <p class="text-xs font-medium text-slate-500">预计耗时</p>
        <p class="mt-1 text-xl font-semibold text-slate-950">{{ model.estimatedTime }}</p>
        <p class="mt-4 text-xs font-medium text-slate-500">完成标准（速览）</p>
        <ul class="mt-2 space-y-1.5 text-xs leading-5 text-slate-600">
          <li v-for="(line, i) in model.completionCriteria.slice(0, 3)" :key="`c-${i}`">
            · {{ line }}
          </li>
        </ul>
      </div>
    </div>

    <details
      v-if="model.knowledgePoints?.length"
      class="mt-6 rounded-[20px] border border-slate-200/90 bg-white/70"
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
import type { ExecutionGuideHeaderModel } from '@/types/executionGuide'

withDefaults(
  defineProps<{
    model: ExecutionGuideHeaderModel
    /** 非驾驶台阶段隐藏「三种开场」说明 */
    showScaffoldHints?: boolean
  }>(),
  { showScaffoldHints: true }
)
</script>
