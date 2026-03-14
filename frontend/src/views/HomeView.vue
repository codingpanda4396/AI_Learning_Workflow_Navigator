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
import type { SessionBusinessStatus } from '@/types/session';
import { emitInfo } from '@/utils/message';

type FlowNode = {
  id: 'goal' | 'diagnosis' | 'plan' | 'learn' | 'feedback' | 'growth';
  title: string;
  summary: string;
  tooltip: string;
  icon: string;
};

const FLOW_NODES: FlowNode[] = [
  {
    id: 'goal',
    title: '目标',
    summary: '先说清楚你想学什么',
    tooltip: '从一个明确目标开始，系统才知道后面该怎样接管。',
    icon: '01',
  },
  {
    id: 'diagnosis',
    title: '能力诊断',
    summary: 'AI 会判断你的当前基础',
    tooltip: '系统会根据你的输入和回答，判断当前掌握程度与起点。',
    icon: '02',
  },
  {
    id: 'plan',
    title: '学习规划',
    summary: 'AI 会生成个性化任务',
    tooltip: '系统会把目标拆成更合理的学习路径和执行顺序。',
    icon: '03',
  },
  {
    id: 'learn',
    title: '任务学习',
    summary: '按当前任务持续推进',
    tooltip: '你只需要处理眼前这一小步，系统会维护整体节奏。',
    icon: '04',
  },
  {
    id: 'feedback',
    title: '练习反馈',
    summary: '通过练习得到即时反馈',
    tooltip: '系统会根据练习结果调整难度、节奏和下一步重点。',
    icon: '05',
  },
  {
    id: 'growth',
    title: '能力成长',
    summary: '把结果沉淀成能力变化',
    tooltip: '每一轮学习都会回流到你的能力画像，帮助后续演化。',
    icon: '06',
  },
];

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
    phase: current.currentStage || mapSessionLabel(current.sessionStatus),
  };
});

const currentFlowIndex = computed(() => mapStatusToIndex(sessionStore.currentSession?.sessionStatus));
const currentFlowLabel = computed(() => {
  if (currentFlowIndex.value < 0) {
    return '从目标开始';
  }

  return `当前主线进行到 ${FLOW_NODES[currentFlowIndex.value]?.title ?? '当前步骤'}`;
});

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

function mapSessionLabel(status?: SessionBusinessStatus) {
  switch (status) {
    case 'ANALYZING':
      return '能力诊断';
    case 'PLANNING':
      return '学习规划';
    case 'LEARNING':
      return '任务学习';
    case 'PRACTICING':
      return '练习反馈';
    case 'REPORT_READY':
    case 'COMPLETED':
      return '能力成长';
    case 'FAILED':
      return '流程中断';
    default:
      return '目标';
  }
}

function mapStatusToIndex(status?: SessionBusinessStatus) {
  switch (status) {
    case 'ANALYZING':
      return 1;
    case 'PLANNING':
      return 2;
    case 'LEARNING':
      return 3;
    case 'PRACTICING':
      return 4;
    case 'REPORT_READY':
    case 'COMPLETED':
      return 5;
    default:
      return -1;
  }
}

function getNodeState(index: number) {
  if (currentFlowIndex.value < 0) {
    return index === 0 ? 'start' : 'idle';
  }

  if (index < currentFlowIndex.value) {
    return 'complete';
  }

  if (index === currentFlowIndex.value) {
    return 'active';
  }

  return 'idle';
}

onMounted(loadCurrentSession);
</script>

<template>
  <AppShell>
    <div class="guide-page">
      <section class="guide-hero">
        <div>
          <p class="app-eyebrow">AI LEARNING SYSTEM</p>
          <h1 class="guide-hero__title">把一个学习目标交给 AI，后面的流程会自己展开</h1>
        </div>

        <div class="guide-hero__actions">
          <p class="guide-hero__status">{{ activeSession ? currentFlowLabel : '1 秒看懂结构，3 秒开始第一步' }}</p>
          <AppButton v-if="activeSession" size="lg" @click="continueCurrentSession">继续当前主线</AppButton>
        </div>
      </section>

      <section class="flow-board" aria-label="AI 学习流程图">
        <div class="flow-board__header">
          <div>
            <p class="app-eyebrow">AI Workflow</p>
            <h2 class="flow-board__title">AI 学习流程</h2>
          </div>
          <p class="flow-board__hint">{{ currentFlowLabel }}</p>
        </div>

        <div class="flow-track">
          <template v-for="(node, index) in FLOW_NODES" :key="node.id">
            <article
              class="flow-node"
              :class="`flow-node--${getNodeState(index)}`"
              tabindex="0"
            >
              <div class="flow-node__icon">{{ node.icon }}</div>
              <h3 class="flow-node__title">{{ node.title }}</h3>
              <p class="flow-node__summary">{{ node.summary }}</p>
              <div class="flow-node__tooltip">{{ node.tooltip }}</div>
            </article>

            <div v-if="index < FLOW_NODES.length - 1" class="flow-connector" aria-hidden="true"></div>
          </template>
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

      <div v-else-if="sessionStore.error && !activeSession" class="guide-feedback">
        <ErrorState :message="sessionStore.error" />
        <div>
          <AppButton variant="secondary" @click="loadCurrentSession">重新加载当前会话</AppButton>
        </div>
      </div>

      <CurrentSessionPanel v-if="activeSession" :session="activeSession" />
    </div>
  </AppShell>
</template>

<style scoped>
.guide-page {
  display: grid;
  gap: 24px;
}

.guide-hero,
.flow-board {
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.95));
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.05);
}

.guide-hero {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 28px 32px;
}

.guide-hero__title {
  margin-top: 12px;
  max-width: 760px;
  font-size: clamp(30px, 4.2vw, 48px);
  line-height: 1.06;
  font-weight: 650;
  letter-spacing: -0.045em;
  color: #0f172a;
}

.guide-hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.guide-hero__status {
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.flow-board {
  padding: 28px;
}

.flow-board__header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.flow-board__title {
  margin-top: 10px;
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.08;
  font-weight: 650;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.flow-board__hint {
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.flow-track {
  display: grid;
  gap: 14px;
}

.flow-node {
  position: relative;
  display: grid;
  gap: 12px;
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.035);
  padding: 20px;
  outline: none;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease,
    background-color 180ms ease;
}

.flow-node:hover,
.flow-node:focus-visible {
  transform: translateY(-2px);
  border-color: rgba(15, 23, 42, 0.12);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.flow-node__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.05);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #0f172a;
}

.flow-node__title {
  font-size: 18px;
  line-height: 1.3;
  font-weight: 650;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.flow-node__summary {
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.flow-node__tooltip {
  pointer-events: none;
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: calc(100% + 10px);
  opacity: 0;
  transform: translateY(6px);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  transition: opacity 180ms ease, transform 180ms ease;
}

.flow-node:hover .flow-node__tooltip,
.flow-node:focus-visible .flow-node__tooltip {
  opacity: 1;
  transform: translateY(0);
}

.flow-connector {
  display: none;
  align-self: center;
  height: 1px;
  background: linear-gradient(90deg, rgba(148, 163, 184, 0.32), rgba(148, 163, 184, 0.6));
}

.flow-node--start,
.flow-node--active {
  border-color: rgba(15, 23, 42, 0.16);
  background: linear-gradient(180deg, #ffffff, rgba(241, 245, 249, 0.96));
  box-shadow: 0 20px 46px rgba(15, 23, 42, 0.08);
}

.flow-node--start .flow-node__icon,
.flow-node--active .flow-node__icon {
  background: #0f172a;
  color: #fff;
}

.flow-node--complete {
  border-color: rgba(148, 163, 184, 0.12);
  background: rgba(248, 250, 252, 0.94);
}

.flow-node--complete .flow-node__icon {
  background: rgba(15, 23, 42, 0.08);
}

.guide-feedback {
  display: grid;
  gap: 16px;
}

@media (min-width: 900px) {
  .flow-board__header {
    flex-direction: row;
    align-items: end;
    justify-content: space-between;
  }

  .flow-track {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    align-items: center;
  }

  .flow-connector {
    display: block;
    min-width: 28px;
  }

  .flow-track {
    grid-template-columns:
      minmax(0, 1fr) 28px
      minmax(0, 1fr) 28px
      minmax(0, 1fr) 28px
      minmax(0, 1fr) 28px
      minmax(0, 1fr) 28px
      minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .guide-hero,
  .flow-board {
    padding: 24px;
  }

  .flow-node__tooltip {
    position: static;
    opacity: 1;
    transform: none;
    box-shadow: none;
    border-style: dashed;
    margin-top: 2px;
  }
}
</style>
