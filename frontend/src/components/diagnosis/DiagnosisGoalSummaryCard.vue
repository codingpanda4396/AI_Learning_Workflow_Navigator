<script setup lang="ts">
defineProps<{
  sessionId: string;
  goal: string;
  course: string;
  chapter: string;
}>();
</script>

<template>
  <section class="rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-sm">
    <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-sky-600">能力诊断</p>
        <h1 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">能力快照</h1>
        <p class="mt-3 text-sm leading-6 text-slate-600">
          这不是考试结果，而是系统用来理解你当前起点的一次快速整理。完成后，我们会基于这份画像继续安排更贴合你的学习路径。
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 px-3 py-2 text-right text-xs leading-5 text-slate-500">
        <p>本次诊断记录</p>
        <p class="font-medium text-slate-600">{{ sessionId || '--' }}</p>
      </div>
    </div>

    <div class="mt-5 grid gap-3 md:grid-cols-3">
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">学习目标</p>
        <p class="mt-2 text-sm font-medium leading-6 text-slate-900">{{ goal }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">课程</p>
        <p class="mt-2 text-sm font-medium leading-6 text-slate-900">{{ course }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">章节</p>
        <p class="mt-2 text-sm font-medium leading-6 text-slate-900">{{ chapter }}</p>
      </div>
    </div>
  </section>
</template>
