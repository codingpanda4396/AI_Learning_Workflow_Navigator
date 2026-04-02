<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-primary">4 Steps</p>
        <h2 class="mt-2 text-xl font-semibold text-text-primary">
          这一轮会分 4 步走
        </h2>
      </div>
      <p class="max-w-2xl text-sm leading-6 text-text-secondary">
        先知道顺序就够了，不用现在把全部内容都看完。
      </p>
    </div>

    <div class="grid gap-3 lg:grid-cols-4">
      <article
        v-for="item in items"
        :key="item.code"
        class="rounded-[24px] border p-5 transition"
        :class="itemClass(item.code)"
      >
        <div class="flex flex-wrap items-center gap-2">
          <p class="font-mono text-[11px] font-bold tracking-tight text-slate-500">
            {{ item.code }}
          </p>
          <span
            v-if="item.code === props.expandedStageCode"
            class="rounded-full bg-accent-muted px-2 py-0.5 text-[10px] font-semibold text-accent-hover"
          >
            当前重点
          </span>
        </div>
        <p class="mt-3 text-sm font-semibold text-text-primary">
          {{ item.title }}
        </p>
        <p class="mt-3 text-sm leading-6 text-text-secondary">
          {{ item.strategy }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PlanStageCode } from '@/utils/planPresentationModel'

const props = defineProps<{
  expandedStageCode: PlanStageCode
}>()

const items: { code: PlanStageCode; title: string; strategy: string }[] = [
  {
    code: 'STRUCTURE',
    title: '先搭起来',
    strategy: '先把这块内容放进整体框架里。',
  },
  {
    code: 'UNDERSTANDING',
    title: '再讲明白',
    strategy: '先把为什么会这样讲清楚。',
  },
  {
    code: 'TRAINING',
    title: '再动手练',
    strategy: '先做一小题或一小步，把理解变成动作。',
  },
  {
    code: 'REFLECTION',
    title: '最后检查',
    strategy: '最后看哪里已经站稳，哪里还要再补。',
  },
]

function itemClass(code: PlanStageCode) {
  if (code === props.expandedStageCode) {
    return 'border-accent/35 bg-gradient-to-br from-accent-muted/70 via-white to-white shadow-[0_16px_36px_rgba(217,119,6,0.1)]'
  }
  return 'border-slate-200 bg-white shadow-sm'
}
</script>
