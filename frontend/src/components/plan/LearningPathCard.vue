<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';

interface PathStage {
  key: string;
  title: string;
  isCurrent: boolean;
}

defineProps<{
  currentFocus: string;
  currentStatus: string;
  nextStep: string;
  stages: PathStage[];
}>();
</script>

<template>
  <SectionCard
    strong
    title="你的学习地图"
    description="先看现在主攻哪一步，再看后面怎么接。"
  >
    <div class="grid gap-3 lg:grid-cols-[280px_minmax(0,1fr)]">
      <div class="space-y-3">
        <div class="rounded-[22px] bg-slate-50 px-4 py-4">
          <p class="text-sm text-slate-500">当前主攻</p>
          <p class="mt-2 text-base font-semibold text-slate-950">{{ currentFocus }}</p>
        </div>
        <div class="rounded-[22px] bg-slate-50 px-4 py-4">
          <p class="text-sm text-slate-500">当前状态</p>
          <p class="mt-2 text-base font-semibold text-slate-950">{{ currentStatus }}</p>
        </div>
        <div class="rounded-[22px] bg-slate-50 px-4 py-4">
          <p class="text-sm text-slate-500">下一步</p>
          <p class="mt-2 text-base font-semibold text-slate-950">{{ nextStep }}</p>
        </div>
      </div>

      <div class="rounded-[24px] border border-slate-200 bg-white px-4 py-4">
        <p class="text-sm font-semibold text-slate-950">路径</p>
        <div class="mt-4 space-y-2">
          <div
            v-for="item in stages"
            :key="item.key"
            class="flex items-center gap-3 rounded-[18px] px-4 py-3"
            :class="item.isCurrent ? 'bg-slate-900 text-white' : 'bg-slate-50 text-slate-700'"
          >
            <span class="text-sm font-semibold">{{ item.title }}</span>
            <span v-if="item.isCurrent" class="text-xs text-slate-300">← 当前</span>
          </div>
        </div>
      </div>
    </div>
  </SectionCard>
</template>
