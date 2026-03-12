<script setup lang="ts">
defineProps<{
  current: number;
  total: number;
}>();
</script>

<template>
  <aside class="rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-sky-600">进度</p>
    <h2 class="mt-2 text-lg font-semibold text-slate-950">当前进度</h2>
    <p class="mt-3 text-sm text-slate-600">第 {{ current }} / {{ total }} 题</p>

    <div class="mt-4 h-2 rounded-full bg-slate-100">
      <div
        class="h-2 rounded-full bg-sky-500 transition-all"
        :style="{ width: `${total > 0 ? Math.max((current / total) * 100, 8) : 0}%` }"
      />
    </div>

    <div class="mt-5 space-y-3 text-sm leading-6 text-slate-600">
      <p>你的回答会用于生成能力画像，并帮助系统调整后续学习路径。</p>
      <p>系统也会参考这份画像，优化 Tutor 提示方式和训练难度。</p>
    </div>
  </aside>
</template>
