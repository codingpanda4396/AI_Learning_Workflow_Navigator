<script setup lang="ts">
import { useRouter } from 'vue-router';
import type { ActiveSession } from '@/types/home';

defineProps<{
  session: ActiveSession | null;
}>();

const router = useRouter();

function continueLearning(session: ActiveSession | null) {
  if (!session) {
    return;
  }
  router.push(`/sessions/${session.id}`);
}
</script>

<template>
  <section class="overflow-hidden rounded-[2rem] bg-white shadow-[0_22px_60px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/70">
    <div class="bg-[linear-gradient(135deg,#0f172a_0%,#1e293b_60%,#0f172a_100%)] px-6 py-6 text-white md:px-7">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">当前会话</p>
          <h2 class="mt-3 text-2xl font-semibold tracking-tight">真实会话入口</h2>
          <p class="mt-2 text-sm leading-6 text-slate-300">这里只展示当前活跃会话，不再保留占位卡片或虚假快捷入口。</p>
        </div>
        <div class="rounded-full bg-emerald-400/15 px-3 py-1 text-xs font-medium text-emerald-200">
          {{ session ? '进行中' : '空闲中' }}
        </div>
      </div>
    </div>

    <div v-if="session" class="space-y-5 p-6 md:p-7">
      <dl class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">目标</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.goal || '暂无' }}</dd>
        </div>
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">课程</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.course || '暂无' }}</dd>
        </div>
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">章节</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.chapter || '暂无' }}</dd>
        </div>
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">状态</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.phase || '暂无' }}</dd>
        </div>
      </dl>

      <div class="flex flex-wrap gap-3">
        <button
          type="button"
          class="rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
          @click="continueLearning(session)"
        >
          打开会话
        </button>
      </div>
    </div>

    <div v-else class="m-6 rounded-[1.6rem] bg-slate-50 px-5 py-10 text-center text-sm text-slate-500 md:m-7">
      当前没有活跃会话，请从上方入口开始新的学习会话。
    </div>
  </section>
</template>
