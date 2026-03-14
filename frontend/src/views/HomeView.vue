<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import CurrentSessionPanel from '@/components/home/CurrentSessionPanel.vue';
import StartLearningEntry from '@/components/home/StartLearningEntry.vue';
import AppButton from '@/components/ui/AppButton.vue';
import { useSessionStore } from '@/stores/session';
import type { ActiveSession, StartLearningForm } from '@/types/home';
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
    phase: current.currentStage || formatSessionStatus(current.sessionStatus),
  };
});

const heroStatus = computed(() => (activeSession.value ? '进行中' : '暂无进行中的主线'));
const heroNextStep = computed(() => (activeSession.value ? '回到当前进度，继续下一步' : '开始一个新的学习目标'));

async function startLearning() {
  if (activeSession.value || sessionStore.loading) {
    return;
  }

  if (!form.goal.trim()) {
    emitInfo('先写下这次最想解决的学习目标。');
    return;
  }

  const goal = form.goal.trim();
  const course = form.course.trim();
  const chapter = form.chapter.trim();

  const sessionId = await sessionStore.createSession({
    goalText: goal,
    courseId: course || '通用课程',
    chapterId: chapter || '当前章节',
  });

  await router.push({
    path: `/diagnosis/${sessionId}`,
    query: {
      goal,
      course,
      chapter,
    },
  });
}

async function continueCurrentSession() {
  if (!activeSession.value) {
    return;
  }

  await router.push(`/sessions/${activeSession.value.id}`);
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
    <div class="home-page">
      <section class="home-hero">
        <div class="home-hero__copy">
          <p class="app-eyebrow">AI LEARNING SYSTEM</p>
          <h1 class="app-title-xl home-hero__title">让 AI 带你把一条学习主线走完</h1>
          <p class="app-text-lead home-hero__desc">
            输入目标后，系统会自动完成诊断、规划、学习与反馈。
          </p>
        </div>

        <div class="home-hero__status">
          <div class="home-status-card">
            <p class="app-stat-label">当前会话</p>
            <p class="home-status-card__value">{{ heroStatus }}</p>
            <p class="home-status-card__hint">
              {{ activeSession ? '系统会从你上次停下来的地方继续推进。' : '先说清楚想学什么，就能开始第一步。' }}
            </p>
          </div>

          <div class="home-status-card">
            <p class="app-stat-label">下一步</p>
            <p class="home-status-card__text">{{ heroNextStep }}</p>
            <button
              class="home-status-link"
              type="button"
              @click="activeSession ? continueCurrentSession() : startLearning()"
            >
              {{ activeSession ? '继续当前主线' : '开始新的主线' }}
            </button>
          </div>
        </div>
      </section>

      <StartLearningEntry
        v-model="form"
        :session="activeSession"
        :loading="sessionStore.loading"
        @submit="startLearning"
        @continue="continueCurrentSession"
      />

      <LoadingState v-if="sessionStore.loading && !activeSession && !sessionStore.error" />

      <div v-else-if="sessionStore.error && !activeSession" class="home-feedback">
        <ErrorState :message="sessionStore.error" />
        <div>
          <AppButton variant="secondary" @click="loadCurrentSession">重新加载当前会话</AppButton>
        </div>
      </div>

      <CurrentSessionPanel v-if="activeSession" :session="activeSession" />

      <section class="home-flow" aria-label="系统闭环">
        <span>目标</span>
        <span>诊断</span>
        <span>规划</span>
        <span>学习</span>
        <span>反馈</span>
        <span>演化</span>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.home-hero {
  position: relative;
  display: grid;
  gap: 28px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(148, 163, 184, 0.11), transparent 34%),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.9), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94));
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.06);
  padding: 32px;
}

.home-hero::after {
  content: '';
  position: absolute;
  right: 4%;
  top: 10%;
  width: 180px;
  height: 180px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  filter: blur(44px);
  pointer-events: none;
}

.home-hero__copy,
.home-hero__status {
  position: relative;
  z-index: 1;
}

.home-hero__title {
  max-width: 720px;
  margin-top: 16px;
}

.home-hero__desc {
  max-width: 560px;
  margin-top: 18px;
}

.home-hero__status {
  display: grid;
  gap: 14px;
}

.home-status-card {
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.04);
  padding: 20px;
  backdrop-filter: blur(10px);
}

.home-status-card__value {
  margin-top: 12px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 650;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.home-status-card__text {
  margin-top: 12px;
  font-size: 15px;
  line-height: 1.7;
  font-weight: 600;
  color: #0f172a;
}

.home-status-card__hint {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.home-status-link {
  margin-top: 14px;
  border: none;
  background: transparent;
  padding: 0;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  cursor: pointer;
  transition: color 160ms ease, opacity 160ms ease;
}

.home-status-link:hover {
  color: #334155;
}

.home-status-link:focus-visible {
  outline: none;
  box-shadow: 0 0 0 4px rgba(15, 23, 42, 0.08);
  border-radius: 10px;
}

.home-feedback {
  display: grid;
  gap: 16px;
}

.home-flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.home-flow span {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.home-flow span:not(:last-child)::after {
  content: '';
  width: 18px;
  height: 1px;
  background: rgba(100, 116, 139, 0.28);
}

@media (min-width: 1024px) {
  .home-hero {
    grid-template-columns: minmax(0, 1.3fr) 320px;
    align-items: end;
    padding: 40px;
  }
}

@media (max-width: 767px) {
  .home-hero {
    padding: 24px;
  }

  .home-status-card__value {
    font-size: 24px;
  }
}
</style>
