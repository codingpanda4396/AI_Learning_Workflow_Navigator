<script setup lang="ts">
interface StrategyCard {
  key: string;
  title: string;
  fitFor: string;
  tradeoff: string;
  timeShortPlan?: string;
}

defineProps<{
  recommendedReason: string;
  recommended: StrategyCard;
  others: StrategyCard[];
}>();
</script>

<template>
  <section class="app-card app-card-padding">
    <h2 class="text-xl font-semibold text-slate-950">为什么推荐这个，而不是别的</h2>
    <p class="mt-3 text-sm leading-7 text-slate-700">{{ recommendedReason }}</p>

    <div class="mt-5 grid gap-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
      <article class="rounded-3xl border border-slate-900 bg-slate-900 px-5 py-5 text-white">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-300">主推荐</p>
        <h3 class="mt-2 text-lg font-semibold">{{ recommended.title }}</h3>
        <p class="mt-3 text-sm leading-7 text-slate-100">{{ recommended.fitFor }}</p>
        <p class="mt-3 rounded-2xl border border-white/10 bg-white/8 px-3 py-2 text-sm leading-6 text-slate-200">
          代价：{{ recommended.tradeoff }}
        </p>
        <p v-if="recommended.timeShortPlan" class="mt-3 text-sm leading-6 text-slate-300">
          今天时间少：{{ recommended.timeShortPlan }}
        </p>
      </article>

      <div class="grid gap-3">
        <article
          v-for="item in others.slice(0, 3)"
          :key="item.key"
          class="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4"
        >
          <p class="text-sm font-semibold text-slate-900">{{ item.title }}</p>
          <p class="mt-2 text-sm leading-7 text-slate-700">适合：{{ item.fitFor }}</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">代价：{{ item.tradeoff }}</p>
          <p v-if="item.timeShortPlan" class="mt-2 text-xs leading-6 text-slate-500">
            时间少时：{{ item.timeShortPlan }}
          </p>
        </article>
      </div>
    </div>
  </section>
</template>
