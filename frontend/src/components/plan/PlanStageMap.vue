<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
          Stage Map
        </p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-text-primary">
          四阶段主视觉
        </h2>
      </div>
      <p class="max-w-xl text-sm leading-6 text-text-secondary">
        四张卡分别回答同一件事：这一阶段要解决什么、产出什么、Tutor 怎样介入，以及什么信号表示可以进入下一段。
      </p>
    </div>

    <div class="grid gap-4 xl:grid-cols-4 md:grid-cols-2">
      <article
        v-for="card in cards"
        :key="card.code"
        class="relative overflow-hidden rounded-[24px] border bg-white p-5 shadow-[0_16px_40px_rgba(15,23,42,0.08)]"
        :class="cardClass(card)"
      >
        <div
          class="absolute inset-x-0 top-0 h-1.5"
          :class="barClass(card)"
        />
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-500">
              Stage {{ card.stageIndex }}
            </p>
            <h3 class="mt-2 text-lg font-semibold tracking-tight text-text-primary">
              {{ card.title }}
            </h3>
            <p class="mt-1 text-sm font-medium text-text-secondary">
              {{ card.label }}
            </p>
          </div>
          <div class="flex flex-col items-end gap-2">
            <span
              v-if="card.isRecommended"
              class="rounded-full bg-amber-100 px-2.5 py-1 text-[11px] font-semibold text-amber-700"
            >
              推荐切入
            </span>
            <span
              v-if="card.isCurrent"
              class="rounded-full bg-emerald-100 px-2.5 py-1 text-[11px] font-semibold text-emerald-700"
            >
              当前进度
            </span>
          </div>
        </div>

        <dl class="mt-5 space-y-4">
          <div>
            <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
              阶段目标
            </dt>
            <dd class="mt-1 text-sm leading-6 text-text-primary">
              {{ card.objective }}
            </dd>
          </div>
          <div>
            <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
              阶段产出
            </dt>
            <dd class="mt-1 text-sm leading-6 text-text-primary">
              {{ card.deliverable }}
            </dd>
          </div>
          <div>
            <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
              Tutor 角色
            </dt>
            <dd class="mt-1 text-sm leading-6 text-text-primary">
              {{ card.tutorRole }}
            </dd>
          </div>
          <div>
            <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
              检查点
            </dt>
            <dd class="mt-1 text-sm leading-6 text-text-primary">
              {{ card.checkpoint }}
            </dd>
          </div>
        </dl>

        <div class="mt-5 flex items-center justify-between border-t border-slate-100 pt-4 text-sm">
          <span class="font-medium text-text-primary">{{ card.estimatedTime }}</span>
          <span class="text-text-secondary">{{ card.taskCount }} 个动作</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PlanStageCardView } from '@/utils/planPresentationModel'

defineProps<{
  cards: PlanStageCardView[]
}>()

function cardClass(card: PlanStageCardView): string {
  if (card.isRecommended) {
    return 'border-amber-300 bg-[linear-gradient(180deg,rgba(255,251,235,1)_0%,rgba(255,255,255,1)_70%)]'
  }
  if (card.isCurrent) {
    return 'border-emerald-300 bg-[linear-gradient(180deg,rgba(236,253,245,1)_0%,rgba(255,255,255,1)_70%)]'
  }
  return 'border-slate-200'
}

function barClass(card: PlanStageCardView): string {
  if (card.isRecommended) return 'bg-[linear-gradient(90deg,#f59e0b_0%,#fbbf24_100%)]'
  if (card.isCurrent) return 'bg-[linear-gradient(90deg,#10b981_0%,#34d399_100%)]'
  switch (card.code) {
    case 'STRUCTURE':
      return 'bg-[linear-gradient(90deg,#38bdf8_0%,#2563eb_100%)]'
    case 'UNDERSTANDING':
      return 'bg-[linear-gradient(90deg,#8b5cf6_0%,#6366f1_100%)]'
    case 'TRAINING':
      return 'bg-[linear-gradient(90deg,#22c55e_0%,#14b8a6_100%)]'
    case 'REFLECTION':
      return 'bg-[linear-gradient(90deg,#f97316_0%,#ef4444_100%)]'
  }
}
</script>
