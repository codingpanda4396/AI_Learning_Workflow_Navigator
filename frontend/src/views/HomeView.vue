<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import CurrentSessionPanel from '@/components/home/CurrentSessionPanel.vue';
import StartLearningEntry from '@/components/home/StartLearningEntry.vue';
import type { ActiveSession, StartLearningForm } from '@/types/home';
import { useSessionStore } from '@/stores/session';
import { formatSessionStatus } from '@/utils/format';
import { emitInfo } from '@/utils/message';

const router = useRouter();
const sessionStore = useSessionStore();

const form = reactive<StartLearningForm>({
  goal: '',
  course: '',
  chapter: '',
});

const activeSession = computed<ActiveSession | null>(() => {
  const current = sessionStore.currentSession;
  if (!current) {
    return null;
  }
  return {
    id: String(current.sessionId),
    goal: current.goalText || '',
    course: current.courseId || '',
    chapter: current.chapterId || '',
    phase: formatSessionStatus(current.sessionStatus),
  };
});

async function startLearning() {
  if (activeSession.value || sessionStore.loading) {
    return;
  }
  if (!form.goal.trim()) {
    emitInfo('请先输入学习目标。');
    return;
  }

  const sessionId = await sessionStore.createSession({
    goalText: form.goal.trim(),
    courseId: form.course.trim() || '通用课程',
    chapterId: form.chapter.trim() || '当前章节',
  });

  await router.push({
    path: `/diagnosis/${sessionId}`,
    query: {
      goal: form.goal.trim(),
      course: form.course.trim(),
      chapter: form.chapter.trim(),
    },
  });
}

async function loadCurrentSession() {
  try {
    await sessionStore.fetchCurrentSession();
  } catch {
    // The error state is rendered below.
  }
}

onMounted(loadCurrentSession);
</script>

<template>
  <AppShell>
    <div class="space-y-6 pb-10">
      <section class="relative overflow-hidden rounded-[2.2rem] bg-[linear-gradient(135deg,#ffffff_0%,#f8fbff_55%,#eef6ff_100%)] px-6 py-8 shadow-[0_24px_70px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/70 md:px-8 md:py-10">
        <div class="absolute -right-12 -top-12 h-48 w-48 rounded-full bg-sky-100/70 blur-3xl" />
        <div class="absolute bottom-0 right-20 h-24 w-24 rounded-full bg-emerald-100/60 blur-2xl" />
        <div class="relative space-y-8">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div class="max-w-3xl">
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-sky-600">熊猫导航</p>
              <h1 class="mt-3 text-3xl font-semibold tracking-tight text-slate-950 md:text-5xl">从真实会话入口开始你的学习闭环</h1>
              <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
                首页只保留当前活跃会话和真实开始学习入口，去掉了占位导航与虚假摘要。
              </p>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <div class="rounded-[1.4rem] bg-white/80 px-4 py-4 shadow-sm ring-1 ring-slate-200/70 backdrop-blur">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-400">活跃会话</p>
                <p class="mt-2 text-xl font-semibold tracking-tight text-slate-950">{{ activeSession ? '进行中' : '暂无活跃会话' }}</p>
              </div>
              <div class="rounded-[1.4rem] bg-white/80 px-4 py-4 shadow-sm ring-1 ring-slate-200/70 backdrop-blur">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-400">当前阶段</p>
                <p class="mt-2 text-xl font-semibold tracking-tight text-slate-950">{{ activeSession?.phase || '未开始' }}</p>
              </div>
              <div class="rounded-[1.4rem] bg-white/80 px-4 py-4 shadow-sm ring-1 ring-slate-200/70 backdrop-blur">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-400">会话 ID</p>
                <p class="mt-2 text-xl font-semibold tracking-tight text-slate-950">{{ activeSession?.id || '--' }}</p>
              </div>
            </div>
          </div>

          <StartLearningEntry
            v-model="form"
            :disabled="Boolean(activeSession)"
            :loading="sessionStore.loading"
            @submit="startLearning"
          />
        </div>
      </section>

      <LoadingState v-if="sessionStore.loading && !activeSession && !sessionStore.error" />

      <div v-else-if="sessionStore.error && !activeSession" class="space-y-4">
        <ErrorState :message="sessionStore.error" />
        <button
          type="button"
          class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          @click="loadCurrentSession"
        >
          重试加载当前会话
        </button>
      </div>

      <CurrentSessionPanel :session="activeSession" />
    </div>
  </AppShell>
</template>
