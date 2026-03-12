<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import CurrentSessionPanel from '@/components/home/CurrentSessionPanel.vue';
import GrowthSummaryPanel from '@/components/home/GrowthSummaryPanel.vue';
import ModuleNavPanel from '@/components/home/ModuleNavPanel.vue';
import StartLearningEntry from '@/components/home/StartLearningEntry.vue';
import WorkflowPipeline from '@/components/home/WorkflowPipeline.vue';
import { useSessionStore } from '@/stores/session';
import { formatStage } from '@/utils/format';
import { emitInfo } from '@/utils/message';
import {
  homeLearningSummary,
  homeModuleEntries,
  homeWorkflowNodes,
  type ActiveSession,
  type StartLearningForm,
} from '@/mocks/home';

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
    goal: current.goalText || '继续当前学习目标',
    course: current.courseId || '当前课程',
    chapter: current.chapterId || '当前章节',
    phase: formatStage(current.currentStage),
    currentTask: '继续当前学习流程，系统会根据你的阶段进度安排下一步任务。',
    progress: 0,
  };
});

const hasActiveSession = computed(() => Boolean(activeSession.value));

async function startLearning() {
  if (hasActiveSession.value || sessionStore.loading) {
    return;
  }
  if (!form.goal.trim()) {
    emitInfo('先告诉我你这一轮想解决什么学习目标。');
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

onMounted(async () => {
  try {
    await sessionStore.fetchCurrentSession();
  } catch {
    // Keep the home entry available when current session fetch fails.
  }
});
</script>

<template>
  <AppShell>
    <div class="space-y-6 pb-10">
      <section class="relative overflow-hidden rounded-[2.2rem] bg-[linear-gradient(135deg,#ffffff_0%,#f8fbff_55%,#eef6ff_100%)] px-6 py-8 shadow-[0_24px_70px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/70 md:px-8 md:py-10">
        <div class="absolute -right-12 -top-12 h-48 w-48 rounded-full bg-sky-100/70 blur-3xl" />
        <div class="absolute bottom-0 right-20 h-24 w-24 rounded-full bg-emerald-100/60 blur-2xl" />
        <div class="relative">
          <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
            <div class="max-w-3xl">
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-sky-600">AI Personalized Learning System</p>
              <h1 class="mt-3 text-3xl font-semibold tracking-tight text-slate-950 md:text-5xl">让 AI 帮你诊断、规划并推进学习</h1>
              <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
                从学习目标到能力诊断，再到个性化规划与任务执行，你可以在同一条链路里持续推进这一轮学习。
              </p>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <div class="rounded-[1.4rem] bg-white/80 px-4 py-4 shadow-sm ring-1 ring-slate-200/70 backdrop-blur">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-400">Active Session</p>
                <p class="mt-2 text-xl font-semibold tracking-tight text-slate-950">{{ hasActiveSession ? '当前有 1 个学习任务' : '当前没有学习任务' }}</p>
              </div>
              <div class="rounded-[1.4rem] bg-white/80 px-4 py-4 shadow-sm ring-1 ring-slate-200/70 backdrop-blur">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-400">Current Phase</p>
                <p class="mt-2 text-xl font-semibold tracking-tight text-slate-950">{{ activeSession?.phase || '尚未开始' }}</p>
              </div>
              <div class="rounded-[1.4rem] bg-white/80 px-4 py-4 shadow-sm ring-1 ring-slate-200/70 backdrop-blur">
                <p class="text-xs uppercase tracking-[0.18em] text-slate-400">Knowledge Gain</p>
                <p class="mt-2 text-xl font-semibold tracking-tight text-slate-950">新增 {{ homeLearningSummary.newKnowledge.length }} 个知识点</p>
              </div>
            </div>
          </div>

          <div class="mt-8">
            <StartLearningEntry
              v-model="form"
              :disabled="hasActiveSession"
              :loading="sessionStore.loading"
              @submit="startLearning"
            />
          </div>
        </div>
      </section>

      <section>
        <CurrentSessionPanel :session="activeSession" />
      </section>

      <section>
        <WorkflowPipeline :nodes="homeWorkflowNodes" />
      </section>

      <section>
        <ModuleNavPanel :entries="homeModuleEntries" />
      </section>

      <section>
        <GrowthSummaryPanel :summary="homeLearningSummary" />
      </section>
    </div>
  </AppShell>
</template>
