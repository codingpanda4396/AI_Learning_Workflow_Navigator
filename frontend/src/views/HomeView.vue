<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import CurrentSessionPanel from '@/components/home/CurrentSessionPanel.vue';
import StartLearningEntry from '@/components/home/StartLearningEntry.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
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
    emitInfo('请先写下这一轮最想解决的学习目标。');
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
    return;
  }
}

onMounted(loadCurrentSession);
</script>

<template>
  <AppShell>
    <div class="app-stack-lg">
      <section class="app-hero">
        <div class="relative z-10 grid gap-8 lg:grid-cols-[minmax(0,1.2fr)_320px] lg:items-end">
          <div>
            <p class="app-eyebrow">AI 个性化学习系统</p>
            <h1 class="app-title-xl mt-4">把学习流程收成一条清晰主线</h1>
            <p class="app-text-lead mt-5 max-w-2xl">
              先说出目标，系统会自动完成诊断、规划、任务学习、练习和反馈。你每次只需要看清现在该做什么。
            </p>
          </div>

          <div class="grid gap-3">
            <div class="app-stat">
              <p class="app-stat-label">当前会话</p>
              <p class="app-stat-value">{{ activeSession ? '进行中' : '等待开始' }}</p>
            </div>
            <div class="app-stat">
              <p class="app-stat-label">下一步</p>
              <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">
                {{ activeSession ? '继续当前学习会话' : '创建一个新的学习目标' }}
              </p>
            </div>
            <div class="app-stat">
              <p class="app-stat-label">主入口</p>
              <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">
                {{ activeSession ? '进入进行中的会话' : '开始这一小步' }}
              </p>
            </div>
          </div>
        </div>
      </section>

      <StartLearningEntry
        v-model="form"
        :disabled="Boolean(activeSession)"
        :loading="sessionStore.loading"
        @submit="startLearning"
      />

      <LoadingState v-if="sessionStore.loading && !activeSession && !sessionStore.error" />

      <div v-else-if="sessionStore.error && !activeSession" class="app-stack-md">
        <ErrorState :message="sessionStore.error" />
        <div>
          <AppButton variant="secondary" @click="loadCurrentSession">重新加载当前会话</AppButton>
        </div>
      </div>

      <CurrentSessionPanel :session="activeSession" />
    </div>
  </AppShell>
</template>
