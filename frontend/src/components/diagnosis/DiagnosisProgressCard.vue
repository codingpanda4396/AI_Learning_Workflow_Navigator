<script setup lang="ts">
defineProps<{
  current: number;
  total: number;
}>();
</script>

<template>
  <aside class="rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-sky-600">进度</p>
    <h2 class="mt-2 text-lg font-semibold text-slate-950">当前诊断进度</h2>
    <p class="mt-3 text-sm text-slate-600">第 {{ current }} / {{ total }} 题</p>

    <div class="mt-4 h-2 rounded-full bg-slate-100">
      <div
        class="h-2 rounded-full bg-sky-500 transition-all"
        :style="{ width: `${total > 0 ? Math.max((current / total) * 100, 8) : 0}%` }"
      />
    </div>

    <div class="mt-5 space-y-3 text-sm leading-6 text-slate-600">
      <p>你的回答会被整理成能力快照，用来帮助系统理解你目前更适合从哪里开始学。</p>
      <p>这份快照也会继续影响后续内容节奏、讲解方式和练习难度。</p>
    </div>
  </aside>
</template>
