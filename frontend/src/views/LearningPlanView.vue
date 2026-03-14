<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import InfoHint from '@/components/ui/InfoHint.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);

const context = computed(() => {
  const sessionId = Number(route.query.sessionId ?? 0);
  const diagnosisId = String(route.query.diagnosisId ?? '').trim();
  return {
    sessionId: Number.isFinite(sessionId) && sessionId > 0 ? sessionId : undefined,
    diagnosisId,
    goalText: String(route.query.goal ?? '').trim() || '掌握当前章节重点',
    courseName: String(route.query.course ?? '').trim() || '通用课程',
    chapterName: String(route.query.chapter ?? '').trim() || '当前章节',
  };
});

const viewState = computed(() => {
  if (learningPlanStore.loading && !preview.value) return 'loading';
  if (error.value && !preview.value) return 'error';
  if (learningPlanStore.confirming) return 'confirming';
  return 'ready';
});

const nextTask = computed(() => preview.value?.taskPreviews[0]);
const taskTitle = computed(() => nextTask.value?.title?.trim() || preview.value?.nextStepNote?.trim() || '从推荐起点开始');
const actionBenefits = computed(() =>
  [nextTask.value?.learningGoal, nextTask.value?.learnerAction, nextTask.value?.aiSupport]
    .map((item) => item?.trim())
    .filter((item): item is string => Boolean(item))
    .slice(0, 3),
);
const estimatedTime = computed(() => {
  const minutes = nextTask.value?.estimatedTaskMinutes ?? 0;
  if (!minutes) return '约 15 分钟';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `约 ${hours} 小时 ${rest} 分钟` : `约 ${hours} 小时`;
  }
  return `约 ${minutes} 分钟`;
});
const stagePath = [
  { key: 'STRUCTURE', label: '基础结构' },
  { key: 'UNDERSTANDING', label: '理解原理' },
  { key: 'TRAINING', label: '练习强化' },
  { key: 'REFLECTION', label: '复盘巩固' },
];
const currentPathIndex = computed(() => {
  const stage = nextTask.value?.stage;
  const index = stagePath.findIndex((item) => item.key === stage);
  return index >= 0 ? index : 0;
});
const reasons = computed(() => (preview.value?.reasons ?? []).slice(0, 2));
const focusPoints = computed(() => (preview.value?.keyWeaknesses ?? []).slice(0, 3));

async function loadPlan() {
  if (!context.value.diagnosisId || !context.value.goalText) {
    learningPlanStore.error = '缺少生成学习规划所需的诊断信息，请先完成诊断。';
    return;
  }
  try {
    await learningPlanStore.generatePreview({
      ...context.value,
      adjustments: learningPlanStore.request?.adjustments ?? DEFAULT_PLAN_ADJUSTMENTS,
    });
  } catch {
    return;
  }
}

async function startLearning() {
  try {
    const result = await learningPlanStore.confirmPlan();
    if (result.firstTaskId) {
      await router.push(`/tasks/${result.firstTaskId}/run`);
      return;
    }
    if (result.nextPage) {
      await router.push(result.nextPage);
      return;
    }
    if (result.sessionId) {
      await router.push(`/sessions/${result.sessionId}`);
    }
  } catch {
    return;
  }
}

watch(
  () => [route.query.sessionId, route.query.diagnosisId, route.query.goal, route.query.course, route.query.chapter],
  async () => {
    await loadPlan();
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  learningPlanStore.reset();
});
</script>

<template>
  <AppShell>
    <div class="mx-auto max-w-[920px] space-y-6 pb-8">
      <div v-if="viewState === 'loading'" class="app-stack-md">
        <section class="app-card app-card-padding app-card-strong">
          <SkeletonBlock width="92px" :height="14" rounded="999px" />
          <div class="mt-4">
            <SkeletonBlock width="68%" :height="42" rounded="22px" />
          </div>
          <div class="mt-6 grid gap-3">
            <SkeletonBlock :height="18" rounded="12px" />
            <SkeletonBlock width="86%" :height="18" rounded="12px" />
            <SkeletonBlock :height="120" rounded="22px" />
          </div>
        </section>
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习规划生成失败。'" />
        <AppButton @click="loadPlan">重新生成学习规划</AppButton>
      </div>

      <template v-else-if="preview">
        <section class="app-hero">
          <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
            <div class="max-w-2xl">
              <p class="app-eyebrow">学习规划</p>
              <h1 class="app-title-lg mt-4">你现在先学这一步</h1>
              <h2 class="mt-4 text-[30px] font-semibold leading-[1.2] tracking-[-0.04em] text-slate-950">{{ taskTitle }}</h2>
              <p class="mt-3 text-sm text-slate-500">{{ estimatedTime }}</p>
            </div>
            <div class="flex flex-col gap-3">
              <AppButton size="lg" :loading="learningPlanStore.confirming" @click="startLearning">
                开始这一小步
              </AppButton>
              <span v-if="viewState === 'confirming'" class="app-badge justify-center">正在进入学习任务</span>
            </div>
          </div>
        </section>

        <SectionCard strong title="完成后你会得到什么" description="这里只保留当前最值得开始的收益，不让说明信息抢走注意力。">
          <div class="grid gap-3">
            <div v-for="item in actionBenefits" :key="item" class="app-option app-option-selected flex items-start gap-3">
              <span class="mt-[10px] h-2 w-2 rounded-full bg-slate-900" />
              <span class="text-sm leading-7 text-slate-700">{{ item }}</span>
            </div>
            <div v-if="!actionBenefits.length" class="app-option app-option-selected text-sm leading-7 text-slate-700">
              先补上这个关键起点，后面的学习会更轻松。
            </div>
          </div>
        </SectionCard>

        <div class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_280px]">
          <SectionCard title="为什么先学这里" description="理由只保留最短解释，帮你确认这一步是合理的。">
            <div class="space-y-3">
              <InfoHint>{{ preview.whyStartHere || '这是当前最稳妥的起点，先把它学会，后面会顺很多。' }}</InfoHint>
              <div v-for="item in reasons" :key="item.key" class="app-hint">
                <p class="text-sm font-semibold text-slate-900">{{ item.title }}</p>
                <p class="mt-1 text-sm text-slate-600">{{ item.description }}</p>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="你现在的重点">
            <div class="flex flex-wrap gap-2">
              <span v-for="item in focusPoints" :key="item" class="app-pill">{{ item }}</span>
              <span v-if="!focusPoints.length" class="app-pill">先把当前建议学会</span>
            </div>
            <div class="app-divider my-5" />
            <p class="app-eyebrow">后续路径</p>
            <div class="mt-3 grid gap-2">
              <div
                v-for="(item, index) in stagePath"
                :key="item.key"
                :class="['rounded-[16px] px-4 py-3 text-sm', index === currentPathIndex ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600']"
              >
                {{ index + 1 }}. {{ item.label }}
              </div>
            </div>
          </SectionCard>
        </div>

        <InfoHint v-if="error" tone="danger">{{ error }}</InfoHint>
      </template>

      <EmptyStatePanel
        v-else
        title="还没有拿到学习规划"
        description="先完成诊断，系统才能给出这次最值得开始的建议。"
      />
    </div>
  </AppShell>
</template>
