<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  summary?: string;
  strengths: string[];
  weaknesses: string[];
}>();

const reasons = computed(() => {
  const positive = props.strengths.filter(Boolean);
  const improvements = props.weaknesses.filter(Boolean).map((item) => `仍需加强：${item}`);
  const merged = [...positive, ...improvements];
  return merged.length ? merged : ['AI 已根据你的作答形成当前判断，后续会在学习路径中继续细化。'];
});
</script>

<template>
  <section class="rounded-[2rem] border border-slate-200 bg-white px-6 py-7 shadow-[0_18px_50px_rgba(15,23,42,0.06)] md:px-8 md:py-8">
    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">AI 解释</p>
    <p v-if="summary" class="mt-4 text-base leading-8 text-slate-700">
      {{ summary }}
    </p>
    <ul class="mt-5 space-y-3">
      <li
        v-for="item in reasons"
        :key="item"
        class="flex items-start gap-3 rounded-[1.25rem] bg-slate-50 px-4 py-3 text-sm leading-7 text-slate-700 md:text-base"
      >
        <span class="mt-2 h-2.5 w-2.5 shrink-0 rounded-full bg-emerald-500" />
        <span>{{ item }}</span>
      </li>
    </ul>
  </section>
</template>
