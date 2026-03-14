<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
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

const actionBenefits = computed(() =>
  [nextTask.value?.learningGoal, nextTask.value?.learnerAction, nextTask.value?.aiSupport]
    .map((item) => item?.trim())
    .filter((item): item is string => Boolean(item))
    .slice(0, 3),
);

const taskTitle = computed(() => nextTask.value?.title?.trim() || preview.value?.nextStepNote?.trim() || '从推荐起点开始学习');

const estimatedTime = computed(() => {
  const minutes = nextTask.value?.estimatedTaskMinutes ?? 0;
  if (!minutes) return '15 分钟';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`;
  }
  return `${minutes} 分钟`;
});

const stagePath = [
  { key: 'STRUCTURE', label: '基础框架' },
  { key: 'UNDERSTANDING', label: '算法原理' },
  { key: 'TRAINING', label: '算法实现' },
  { key: 'REFLECTION', label: '进阶应用' },
];

const currentPathIndex = computed(() => {
  const stage = nextTask.value?.stage;
  const index = stagePath.findIndex((item) => item.key === stage);
  return index >= 0 ? index : 0;
});

const fallbackBannerText = computed(() => {
  if (!preview.value?.fallbackApplied) {
    return '';
  }
  return '部分规划说明已切换为精简展示，但不会影响当前学习任务与后续跳转。';
});

const aiAdvice = computed(() => {
  const lines: string[] = [];
  lines.push(`你的目标是：${preview.value?.context.goalText || '完成当前章节学习'}`);

  const weaknessText = preview.value?.keyWeaknesses
    ?.map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 2)
    .join('、');
  if (weaknessText) {
    lines.push(`当前还需要先补稳：${weaknessText}。`);
  }

  const mainReason =
    preview.value?.whyStartHere?.trim() ||
    preview.value?.context.diagnosisSummary?.trim() ||
    preview.value?.reasons?.[0]?.description?.trim();
  if (mainReason) {
    lines.push(mainReason);
  } else {
    lines.push('系统建议先完成当前起步任务，后续学习会更顺畅。');
  }

  return lines.slice(0, 3);
});

async function loadPlan() {
  if (!context.value.diagnosisId || !context.value.goalText) {
    learningPlanStore.error = '缺少生成学习计划所需的诊断信息，请先完成诊断。';
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
    <div class="mx-auto max-w-4xl space-y-5 pb-8">
      <div v-if="viewState === 'loading'" class="space-y-4">
        <div class="rounded-[1.8rem] border border-slate-200 bg-white px-6 py-5 shadow-[0_16px_48px_rgba(15,23,42,0.06)]">
          <div class="space-y-3 animate-pulse">
            <div class="h-4 w-24 rounded-full bg-slate-200" />
            <div class="h-8 w-80 rounded-2xl bg-slate-100" />
            <div class="h-20 rounded-[1.2rem] bg-slate-100" />
          </div>
        </div>
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习计划生成失败。'" />
        <div class="flex justify-start">
          <button
            type="button"
            class="rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            @click="loadPlan"
          >
            重试生成
          </button>
        </div>
      </div>

      <template v-else-if="preview">
        <div v-if="error" class="rounded-[1.4rem] border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {{ error }}
        </div>

        <section class="mx-auto w-full max-w-[720px] rounded-[24px] border border-slate-200 bg-white p-8 shadow-[0_14px_36px_rgba(15,23,42,0.08)]">
          <p class="text-3xl font-semibold tracking-tight text-slate-950 md:text-4xl">当前建议先学</p>
          <h1 class="mt-4 text-2xl font-semibold leading-tight text-slate-950">{{ taskTitle }}</h1>
          <p class="mt-3 text-sm text-slate-500">预计时间：{{ estimatedTime }}</p>

          <div class="mt-7">
            <p class="text-base font-semibold text-slate-900">完成后你会：</p>
            <ul class="mt-3 space-y-2.5 text-sm leading-6 text-slate-700">
              <li v-for="item in actionBenefits" :key="item" class="flex gap-3">
                <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-slate-900" />
                <span>{{ item }}</span>
              </li>
              <li v-if="!actionBenefits.length" class="flex gap-3">
                <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-slate-900" />
                <span>先补这一块，再继续后面的算法学习会更顺。</span>
              </li>
            </ul>
          </div>

          <div class="mt-8 flex flex-wrap items-center gap-3">
            <button
              type="button"
              class="min-w-[12rem] rounded-2xl bg-slate-950 px-6 py-3 text-base font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="learningPlanStore.confirming"
              @click="startLearning"
            >
              开始这一小步
            </button>
            <span
              v-if="viewState === 'confirming'"
              class="rounded-full bg-sky-100 px-3 py-1.5 text-sm font-medium text-sky-700"
            >
              正在进入学习任务
            </span>
          </div>
        </section>

        <section class="mx-auto w-full max-w-[720px] rounded-2xl bg-slate-100 px-4 py-4">
          <p class="text-sm font-semibold text-slate-900">为什么先学这个？</p>
          <p class="mt-2 text-sm leading-6 text-slate-600">
            {{ aiAdvice[0] || '先补这一块，后面会更容易连起来。' }}
          </p>
        </section>

        <section class="mx-auto w-full max-w-[720px] space-y-2">
          <p class="text-sm font-semibold text-slate-900">后续学习路径</p>
          <p class="text-sm leading-6 text-slate-600">
            <span class="mr-2 inline-flex rounded-full bg-slate-900 px-2.5 py-0.5 text-xs font-medium text-white">当前</span>
            <template v-for="(item, index) in stagePath" :key="item.key">
              <span :class="index === currentPathIndex ? 'font-semibold text-slate-900' : ''">{{ item.label }}</span>
              <span v-if="index < stagePath.length - 1" class="px-2 text-slate-400">→</span>
            </template>
          </p>
          <p class="text-xs text-slate-500">先完成当前这一步，后续会自动衔接。</p>
        </section>
      </template>
    </div>
  </AppShell>
</template>
