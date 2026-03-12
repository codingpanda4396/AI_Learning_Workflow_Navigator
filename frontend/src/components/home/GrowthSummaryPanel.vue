<script setup lang="ts">
import { useRouter } from 'vue-router';
import type { LearningSummary } from '@/mocks/home';

defineProps<{
  summary: LearningSummary;
}>();

const router = useRouter();

function goToGrowth() {
  router.push('/growth');
}
</script>

<template>
  <section class="rounded-[2rem] bg-white p-6 shadow-[0_18px_50px_rgba(15,23,42,0.06)] ring-1 ring-slate-200/70 md:p-7">
    <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Growth Summary</p>
    <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">成长摘要</h2>
    <p class="mt-2 text-sm leading-6 text-slate-600">系统会根据你的学习记录，持续跟踪能力变化。</p>

    <div class="mt-6 grid gap-3 md:grid-cols-3">
      <div v-for="metric in summary.metrics" :key="metric.key" class="rounded-[1.4rem] bg-slate-50 px-4 py-4">
        <p class="text-xs uppercase tracking-[0.18em] text-slate-400">{{ metric.label }}</p>
        <p class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ metric.value }}</p>
      </div>
    </div>

    <div class="mt-6 space-y-4">
      <div class="rounded-[1.6rem] bg-slate-50 p-4">
        <div class="flex items-center justify-between gap-3">
          <h3 class="text-sm font-semibold text-slate-900">最近学习记录</h3>
          <span class="text-xs text-slate-400">{{ summary.recentSessions.length }} 条</span>
        </div>
        <ul class="mt-4 space-y-3">
          <li v-for="session in summary.recentSessions" :key="session.id" class="rounded-2xl bg-slate-50 px-4 py-3">
            <p class="text-sm font-medium text-slate-900">{{ session.title }}</p>
            <p class="mt-1 text-xs leading-5 text-slate-500">{{ session.progressText }}</p>
          </li>
        </ul>
      </div>

      <div class="rounded-[1.6rem] bg-slate-50 p-4">
        <h3 class="text-sm font-semibold text-slate-900">最近评估结果</h3>
        <div v-if="summary.recentEvaluation" class="mt-3 rounded-2xl bg-white px-4 py-3">
          <p class="text-sm font-medium text-slate-900">{{ summary.recentEvaluation.title }}</p>
          <p class="mt-1 text-xs leading-5 text-slate-500">{{ summary.recentEvaluation.result }}</p>
        </div>
        <p v-else class="mt-3 text-sm text-slate-500">暂无评估结果</p>
      </div>

      <div class="rounded-[1.6rem] bg-slate-50 p-4">
        <h3 class="text-sm font-semibold text-slate-900">最近新增知识点</h3>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="knowledge in summary.newKnowledge"
            :key="knowledge"
            class="rounded-full bg-white px-3 py-2 text-xs font-medium text-sky-700"
          >
            {{ knowledge }}
          </span>
        </div>
      </div>
    </div>

    <button
      type="button"
      class="mt-6 w-full rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
      @click="goToGrowth"
    >
      查看成长轨迹
    </button>
  </section>
</template>
