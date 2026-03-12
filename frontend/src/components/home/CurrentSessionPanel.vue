<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { ActiveSession } from '@/mocks/home';

const props = defineProps<{
  session: ActiveSession | null;
}>();

const router = useRouter();

const progressWidth = computed(() => `${props.session?.progress ?? 0}%`);

function continueLearning() {
  if (!props.session) {
    return;
  }
  router.push(`/session/${props.session.id}`);
}
</script>

<template>
  <section class="overflow-hidden rounded-[2rem] bg-white shadow-[0_22px_60px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/70">
    <div class="bg-[linear-gradient(135deg,#0f172a_0%,#1e293b_60%,#0f172a_100%)] px-6 py-6 text-white md:px-7">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Current Session</p>
          <h2 class="mt-3 text-2xl font-semibold tracking-tight">当前学习</h2>
          <p class="mt-2 text-sm leading-6 text-slate-300">你正在进行的学习任务。</p>
        </div>
        <div class="rounded-full bg-emerald-400/15 px-3 py-1 text-xs font-medium text-emerald-200">进行中</div>
      </div>
    </div>

    <div v-if="session" class="p-6 md:p-7">
      <dl class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">当前目标</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.goal }}</dd>
        </div>
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">当前课程</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.course }}</dd>
        </div>
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">当前章节</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.chapter }}</dd>
        </div>
        <div class="rounded-[1.5rem] bg-slate-50 px-4 py-4">
          <dt class="text-xs uppercase tracking-[0.18em] text-slate-400">当前阶段</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ session.phase }}</dd>
        </div>
      </dl>

      <div class="mt-5 grid gap-5 lg:grid-cols-[1.35fr_0.65fr]">
        <div class="rounded-[1.6rem] bg-slate-50 px-5 py-5">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-400">当前任务</p>
          <p class="mt-3 text-base font-semibold text-slate-950">{{ session.currentTask }}</p>

          <div class="mt-6">
            <div class="flex items-center justify-between text-sm">
              <span class="font-medium text-slate-700">学习进度</span>
              <span class="font-semibold text-slate-950">{{ session.progress }}%</span>
            </div>
            <div class="mt-2 h-3 overflow-hidden rounded-full bg-slate-200">
              <div class="h-full rounded-full bg-sky-500 transition-all" :style="{ width: progressWidth }" />
            </div>
          </div>

          <button
            type="button"
            class="mt-6 rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            @click="continueLearning"
          >
            继续学习
          </button>
        </div>

        <div class="rounded-[1.6rem] bg-[linear-gradient(180deg,#f8fafc_0%,#eef2ff_100%)] px-5 py-5">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-400">Learning Snapshot</p>
          <div class="mt-4 space-y-4">
            <div>
              <p class="text-xs text-slate-500">学习概览</p>
              <p class="mt-1 text-sm font-medium text-slate-900">{{ session.course }} / {{ session.chapter }}</p>
            </div>
            <div>
              <p class="text-xs text-slate-500">当前阶段</p>
              <p class="mt-1 text-sm font-medium text-slate-900">{{ session.phase }}</p>
            </div>
            <div>
              <p class="text-xs text-slate-500">下一步建议</p>
              <p class="mt-1 text-sm font-medium text-slate-900">系统会根据你的学习进度推荐下一步任务。</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="m-6 rounded-[1.6rem] bg-slate-50 px-5 py-10 text-center text-sm text-slate-500 md:m-7">
      你当前还没有进行中的学习，可以直接开始新的学习。
    </div>
  </section>
</template>
