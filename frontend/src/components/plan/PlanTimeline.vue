<template>
  <section class="space-y-4">
    <div class="space-y-1">
      <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-400">
        Timeline
      </p>
      <h2 class="text-2xl font-semibold tracking-tight text-slate-950">
        这一轮怎么走
      </h2>
    </div>

    <div class="grid gap-3 lg:grid-cols-4">
      <article
        v-for="item in items"
        :key="item.code"
        class="relative overflow-hidden rounded-[26px] border p-5 transition-colors"
        :class="cardClass(item.status)"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-lg font-semibold text-slate-950">
              {{ item.titleZh }}
            </p>
            <p class="mt-1 text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-400">
              {{ item.titleEn }}
            </p>
          </div>
          <span
            class="rounded-full px-2.5 py-1 text-[11px] font-semibold"
            :class="badgeClass(item.status)"
          >
            {{ badgeText(item.status) }}
          </span>
        </div>

        <p class="mt-4 text-sm leading-6 text-slate-600">
          {{ item.objective }}
        </p>
        <p class="mt-5 text-sm font-medium text-slate-900">
          {{ item.estimatedLabel }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PlanTimelineItemView, PlanTimelineStatus } from '@/utils/planPresentationModel'

defineProps<{
  items: PlanTimelineItemView[]
}>()

function cardClass(status: PlanTimelineStatus): string {
  if (status === 'current') {
    return 'border-sky-300 bg-[linear-gradient(160deg,#eff8ff_0%,#ffffff_55%,#f8fafc_100%)] shadow-[0_18px_40px_rgba(14,116,144,0.16)]'
  }
  if (status === 'done') {
    return 'border-emerald-200 bg-emerald-50/60'
  }
  if (status === 'recommended') {
    return 'border-indigo-200 bg-indigo-50/60'
  }
  return 'border-slate-200 bg-white'
}

function badgeClass(status: PlanTimelineStatus): string {
  if (status === 'current') return 'bg-sky-100 text-sky-700'
  if (status === 'done') return 'bg-emerald-100 text-emerald-700'
  if (status === 'recommended') return 'bg-indigo-100 text-indigo-700'
  return 'bg-slate-100 text-slate-500'
}

function badgeText(status: PlanTimelineStatus): string {
  if (status === 'current') return '当前步骤'
  if (status === 'done') return '已完成'
  if (status === 'recommended') return '推荐起步'
  return '待开始'
}
</script>
