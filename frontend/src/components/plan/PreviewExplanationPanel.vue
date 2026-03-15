<script setup lang="ts">
defineProps<{
  whyRecommended: string;
  whyThisStepFirst: string;
  learnerProfile: string;
  systemDecision: string;
  expanded: boolean;
}>();

const emit = defineEmits<{
  (event: 'toggle'): void;
}>();
</script>

<template>
  <section class="app-card rounded-[24px] p-6">
    <button type="button" class="flex w-full items-center justify-between text-left" @click="emit('toggle')">
      <div class="space-y-1">
        <p class="text-sm font-medium text-slate-500">AI 解释（默认收起）</p>
        <h3 class="text-lg font-semibold text-slate-900">为什么推荐这一步</h3>
      </div>
      <span class="text-sm font-semibold text-slate-600">{{ expanded ? '收起' : '展开' }}</span>
    </button>

    <div v-if="expanded" class="mt-5 space-y-5 text-sm leading-7 text-slate-700">
      <div class="rounded-2xl bg-slate-50 px-4 py-3">
        <p class="font-semibold text-slate-900">为什么推荐这一步</p>
        <p class="mt-1">{{ whyRecommended }}</p>
        <p class="mt-1">{{ whyThisStepFirst }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 px-4 py-3">
        <p class="font-semibold text-slate-900">AI 如何理解你的当前状态</p>
        <p class="mt-1">{{ learnerProfile }}</p>
        <p class="mt-1">{{ systemDecision }}</p>
      </div>
    </div>
  </section>
</template>
