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
    <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-sky-600">诊断</p>
        <h1 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">能力诊断</h1>
        <p class="mt-3 text-sm leading-6 text-slate-600">
          这不是考试，而是为了更准确地制定学习路径。你的回答会帮助系统理解你的基础、卡点和学习节奏。
        </p>
      </div>
      <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">会话 ID：{{ sessionId }}</div>
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
