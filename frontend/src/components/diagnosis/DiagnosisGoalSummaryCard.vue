<script setup lang="ts">
defineProps<{
  sessionId: string;
  goal: string;
  course: string;
  chapter: string;
}>();
</script>

<template>
  <section class="rounded-[1.6rem] border border-slate-200 bg-slate-50/85 p-4 shadow-sm">
    <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">能力诊断</p>
        <h1 class="mt-1 text-xl font-semibold tracking-tight text-slate-900 md:text-2xl">能力快照</h1>
        <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
          这不是考试结果，而是系统用来理解你当前起点的一次快速整理。完成后，我们会基于这份画像继续安排更贴合你的学习路径。
        </p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white/80 px-3 py-2 text-left text-xs leading-5 text-slate-500 md:text-right">
        <p>本次诊断记录</p>
        <p class="font-medium text-slate-600">{{ sessionId || '--' }}</p>
      </div>
    </div>

    <div class="mt-4 grid gap-2.5 md:grid-cols-3">
      <div class="rounded-2xl border border-slate-200 bg-white/90 p-3.5">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">学习目标</p>
        <p class="mt-1.5 text-sm font-medium leading-6 text-slate-800">{{ goal }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white/90 p-3.5">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">课程</p>
        <p class="mt-1.5 text-sm font-medium leading-6 text-slate-800">{{ course }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white/90 p-3.5">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">章节</p>
        <p class="mt-1.5 text-sm font-medium leading-6 text-slate-800">{{ chapter }}</p>
      </div>
    </div>
  </section>
</template>
