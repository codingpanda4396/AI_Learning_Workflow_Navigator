<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import PageSection from '@/components/common/PageSection.vue';
import PlanWhyStartHereCard from '@/components/learning-plan/PlanWhyStartHereCard.vue';
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

const learningContent = computed(() =>
  [nextTask.value?.learningGoal, nextTask.value?.learnerAction, nextTask.value?.aiSupport]
    .map((item) => item?.trim())
    .filter((item): item is string => Boolean(item))
    .slice(0, 3),
);

const taskTitle = computed(() => nextTask.value?.title?.trim() || preview.value?.nextStepNote?.trim() || '从推荐起点开始学习');

const estimatedTime = computed(() => {
  const minutes = nextTask.value?.estimatedTaskMinutes ?? 0;
  if (!minutes) return '约 15 分钟';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`;
  }
  return `${minutes} 分钟`;
});

const stagePath = [
  { key: 'STRUCTURE', label: 'Foundation' },
  { key: 'UNDERSTANDING', label: 'Algorithm' },
  { key: 'TRAINING', label: 'Implementation' },
  { key: 'REFLECTION', label: 'Advanced' },
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
    <div class="mx-auto max-w-5xl space-y-4 pb-8">
      <div v-if="viewState === 'loading'" class="space-y-4">
        <PageSection eyebrow="Learning Plan" title="正在整理你的下一步学习任务" compact>
          <div class="space-y-3 animate-pulse">
            <div class="h-5 w-40 rounded-full bg-slate-200" />
            <div class="h-24 rounded-[1.5rem] bg-slate-100" />
          </div>
        </PageSection>
        <div class="animate-pulse rounded-[2rem] bg-slate-950 p-8 shadow-[0_28px_80px_rgba(15,23,42,0.18)]">
          <div class="mx-auto h-5 w-28 rounded-full bg-white/20" />
          <div class="mx-auto mt-5 h-12 max-w-xl rounded-2xl bg-white/15" />
          <div class="mx-auto mt-4 h-28 max-w-3xl rounded-[1.5rem] bg-white/10" />
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
        <div v-if="fallbackBannerText" class="rounded-[1.4rem] border border-amber-100 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {{ fallbackBannerText }}
        </div>

        <div v-if="error" class="rounded-[1.4rem] border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {{ error }}
        </div>

        <PageSection eyebrow="SECTION 1" title="学习目标" compact>
          <div class="rounded-[1.6rem] border border-slate-200 bg-slate-50 px-5 py-4">
            <p class="text-lg font-semibold text-slate-950">{{ preview.context.goalText }}</p>
            <p class="mt-2 text-sm text-slate-500">{{ preview.context.courseName }} · {{ preview.context.chapterName }}</p>
          </div>
        </PageSection>

        <section class="overflow-hidden rounded-[2.2rem] bg-[linear-gradient(135deg,#0f172a_0%,#13263f_46%,#1f6b7a_100%)] px-6 py-8 text-white shadow-[0_32px_90px_rgba(15,23,42,0.22)] md:px-10 md:py-10">
          <div class="mx-auto max-w-3xl text-center">
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-300">SECTION 2</p>
            <h1 class="mt-4 text-3xl font-semibold tracking-tight md:text-5xl">下一步学习</h1>
            <p class="mt-3 text-sm leading-7 text-slate-200 md:text-base">你现在该做的只有一件事：开始这一步。</p>
          </div>

          <div class="mx-auto mt-8 max-w-3xl rounded-[2rem] bg-white px-6 py-6 text-slate-950 shadow-[0_22px_70px_rgba(255,255,255,0.12)] md:px-8">
            <p class="text-sm font-semibold uppercase tracking-[0.22em] text-slate-400">任务名称</p>
            <h2 class="mt-3 text-2xl font-semibold tracking-tight">{{ taskTitle }}</h2>

            <div class="mt-6 rounded-[1.5rem] bg-slate-50 p-5">
              <p class="text-sm font-semibold text-slate-950">学习内容</p>
              <ul class="mt-3 space-y-3 text-sm leading-6 text-slate-700">
                <li v-for="item in learningContent" :key="item" class="flex gap-3">
                  <span class="mt-2 h-2 w-2 shrink-0 rounded-full bg-slate-950" />
                  <span>{{ item }}</span>
                </li>
                <li v-if="!learningContent.length" class="flex gap-3">
                  <span class="mt-2 h-2 w-2 shrink-0 rounded-full bg-slate-950" />
                  <span>系统会围绕当前推荐起点，带你完成这一轮的首个学习任务。</span>
                </li>
              </ul>
            </div>

            <div class="mt-5 flex items-center justify-center gap-3 text-sm text-slate-500">
              <span class="rounded-full bg-slate-100 px-4 py-2 font-medium">预计时间 {{ estimatedTime }}</span>
              <span
                v-if="viewState === 'confirming'"
                class="rounded-full bg-sky-100 px-4 py-2 font-medium text-sky-700"
              >
                正在进入任务
              </span>
            </div>

            <div class="mt-7 flex justify-center">
              <button
                type="button"
                class="min-w-[14rem] rounded-2xl bg-slate-950 px-8 py-4 text-base font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="learningPlanStore.confirming"
                @click="startLearning"
              >
                开始学习
              </button>
            </div>
          </div>
        </section>

        <PageSection eyebrow="SECTION 3" title="学习路径概览" compact>
          <div class="grid gap-3 md:grid-cols-4">
            <article
              v-for="(item, index) in stagePath"
              :key="item.key"
              class="rounded-[1.6rem] border px-4 py-4 transition"
              :class="index === currentPathIndex ? 'border-slate-950 bg-slate-950 text-white shadow-[0_18px_40px_rgba(15,23,42,0.14)]' : 'border-slate-200 bg-slate-50 text-slate-500'"
            >
              <p class="text-xs font-semibold uppercase tracking-[0.2em]" :class="index === currentPathIndex ? 'text-slate-300' : 'text-slate-400'">
                {{ index === currentPathIndex ? 'Current' : `Step ${index + 1}` }}
              </p>
              <p class="mt-3 text-base font-semibold">{{ item.label }}</p>
            </article>
          </div>
        </PageSection>

        <PageSection eyebrow="SECTION 4" compact>
          <div class="space-y-3">
            <PlanWhyStartHereCard
              :why-start-here="preview.whyStartHere"
              :key-weaknesses="preview.keyWeaknesses"
              :priority-nodes="preview.priorityNodes"
            />
            <details class="group rounded-[1.6rem] border border-slate-200 bg-slate-50 px-5 py-4">
              <summary class="cursor-pointer list-none text-base font-semibold text-slate-950">
                为什么这样安排
              </summary>
              <div class="mt-4 space-y-3 text-sm leading-7 text-slate-600">
                <p v-if="preview.context.diagnosisSummary">{{ preview.context.diagnosisSummary }}</p>
                <p v-for="reason in preview.reasons" :key="reason.key">
                  <span class="font-semibold text-slate-900">{{ reason.title || reason.label || 'AI 规划依据' }}：</span>
                  {{ reason.description }}
                </p>
                <p v-if="!preview.context.diagnosisSummary && !preview.reasons.length">
                  AI 已根据当前诊断结果选择最合适的起点，并优先给出能立即开始的任务。
                </p>
              </div>
            </details>
          </div>
        </PageSection>
      </template>
    </div>
  </AppShell>
</template>
