<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';

interface PathStage {
  key: string;
  title: string;
  state: 'CURRENT' | 'LOCKED' | 'PENDING' | 'COMPLETED' | 'OPTIONAL' | 'REVIEW';
  stateLabel: string;
  description: string;
}

defineProps<{
  currentFocus: string;
  currentStatus: string;
  nextStep: string;
  pathRisk: string;
  stages: PathStage[];
}>();

function stateClass(state: PathStage['state']) {
  switch (state) {
    case 'CURRENT':
      return 'border-slate-900 bg-slate-900 text-white';
    case 'COMPLETED':
      return 'border-emerald-200 bg-emerald-50 text-emerald-700';
    case 'OPTIONAL':
      return 'border-sky-200 bg-sky-50 text-sky-700';
    case 'REVIEW':
      return 'border-amber-200 bg-amber-50 text-amber-700';
    case 'LOCKED':
      return 'border-slate-200 bg-slate-100 text-slate-600';
    case 'PENDING':
    default:
      return 'border-slate-200 bg-white text-slate-600';
  }
}
</script>

<template>
  <SectionCard strong title="你的当前学习地图" description="不是静态阶段条，而是你现在这次规划的推进状态。">
    <div class="grid gap-3">
      <div class="rounded-[22px] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前主攻</p>
        <p class="mt-2 text-base font-semibold text-slate-950">{{ currentFocus }}</p>
      </div>
      <div class="rounded-[22px] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前状态</p>
        <p class="mt-2 text-base font-semibold text-slate-950">{{ currentStatus }}</p>
      </div>
      <div class="rounded-[22px] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">下一跳</p>
        <p class="mt-2 text-base font-semibold text-slate-950">{{ nextStep }}</p>
      </div>
      <div class="rounded-[22px] border border-amber-100 bg-amber-50/80 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-amber-700">跳过风险</p>
        <p class="mt-2 text-sm leading-7 text-amber-800">{{ pathRisk }}</p>
      </div>
    </div>

    <div class="app-divider my-5" />

    <div class="space-y-3">
      <div
        v-for="(item, index) in stages"
        :key="item.key"
        class="rounded-[22px] border px-4 py-4"
        :class="stateClass(item.state)"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.16em]" :class="item.state === 'CURRENT' ? 'text-slate-300' : 'text-current/70'">阶段 {{ index + 1 }}</p>
            <p class="mt-2 text-base font-semibold">{{ item.title }}</p>
          </div>
          <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="item.state === 'CURRENT' ? 'bg-white text-slate-900' : 'bg-slate-900 text-white'">
            {{ item.stateLabel }}
          </span>
        </div>
        <p class="mt-2 text-sm leading-7" :class="item.state === 'CURRENT' ? 'text-slate-200' : 'text-current/80'">{{ item.description }}</p>
      </div>
    </div>
  </SectionCard>
</template>
