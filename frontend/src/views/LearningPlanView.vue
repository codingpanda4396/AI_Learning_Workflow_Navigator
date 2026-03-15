<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import { useLearningFlowStore } from '@/stores/learningFlow';
import { getPreviewMetricsSnapshot, trackPreviewAccepted, trackPreviewShown } from '@/utils/previewMetrics';
import { buildLearningPlanPreviewView } from '@/utils/useLearningPlanPreviewView';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();
const flowStore = useLearningFlowStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const previewVm = computed(() => (preview.value ? buildLearningPlanPreviewView(preview.value) : null));

const context = computed(() => {
  const sessionId = Number(route.query.sessionId ?? 0);
  const diagnosisId = String(route.query.diagnosisId ?? '').trim();
  return {
    sessionId: Number.isFinite(sessionId) && sessionId > 0 ? sessionId : undefined,
    diagnosisId,
    goalText: String(route.query.goal ?? '').trim() || '先把当前最值得推进的一步学明白',
    courseName: String(route.query.course ?? '').trim() || '当前学习主题',
    chapterName: String(route.query.chapter ?? '').trim() || '当前章节',
  };
});

const viewState = computed(() => {
  if (learningPlanStore.loading && !preview.value) return 'loading';
  if (error.value && !preview.value) return 'error';
  if (learningPlanStore.confirming) return 'confirming';
  return 'ready';
});

async function loadPlan() {
  if (!context.value.diagnosisId || !context.value.goalText) {
    learningPlanStore.error = '还缺少生成规划所需的诊断信息，请先完成诊断。';
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
    if (preview.value?.id) {
      trackPreviewAccepted(preview.value.id, result.firstTaskId);
      console.info('[metrics] preview accepted', getPreviewMetricsSnapshot());
    }
    if (!result.sessionId) return;
    await flowStore.loadSessionFlow(result.sessionId);
    if (result.firstTaskId) {
      await flowStore.goToStage('LEARNING_TASK');
      return;
    }
    if (result.nextPage?.includes('/quiz')) await flowStore.goToStage('TRAINING');
    else if (result.nextPage?.includes('/report')) await flowStore.goToStage('EVALUATION');
    else await flowStore.goToStage('NEXT_ACTION');
  } catch {
    return;
  }
}

watch(
  () => [route.query.sessionId, route.query.diagnosisId, route.query.goal, route.query.course, route.query.chapter],
  async () => { await loadPlan(); },
  { immediate: true },
);

watch(
  () => preview.value?.id,
  (previewId) => {
    if (!previewId) return;
    trackPreviewShown(previewId);
    console.info('[metrics] preview shown', getPreviewMetricsSnapshot());
  },
);

onBeforeUnmount(() => {
  learningPlanStore.reset();
});
</script>

<template>
  <AppShell>
    <div class="mx-auto max-w-[720px] space-y-10 pb-16 pt-2">
      <div v-if="viewState === 'loading'" class="space-y-6">
        <div class="space-y-4">
          <SkeletonBlock width="120px" :height="14" rounded="999px" />
          <SkeletonBlock width="85%" :height="32" rounded="12px" />
          <SkeletonBlock width="70%" :height="20" rounded="10px" />
        </div>
        <SkeletonBlock :height="200" rounded="24px" />
        <SkeletonBlock :height="120" rounded="20px" />
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习规划生成失败。'" />
        <AppButton @click="loadPlan">重新生成</AppButton>
      </div>

      <template v-else-if="preview && previewVm">
        <!-- 1. 现在为什么从这一步开始 -->
        <header class="space-y-3 text-center">
          <p class="text-sm font-medium text-slate-500">你的目标</p>
          <h1 class="text-xl font-semibold text-slate-900">{{ previewVm.hero.goal }}</h1>
          <p class="text-base font-medium text-slate-700">推荐起步：{{ previewVm.hero.startPoint }}</p>
          <p class="text-sm leading-relaxed text-slate-600">{{ previewVm.hero.oneLineReason }}</p>
        </header>

        <!-- 2. 当前这一小步要做什么（页面中心） -->
        <section class="app-card app-card-strong rounded-3xl border border-slate-200/80 bg-white p-6 shadow-[0_20px_56px_rgba(15,23,42,0.08)] sm:p-8">
          <div class="mb-4 flex items-center justify-between gap-3">
            <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">第一步</span>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
              约 {{ previewVm.taskCard.estimatedTime }}
            </span>
          </div>
          <h2 class="text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">{{ previewVm.taskCard.title }}</h2>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ previewVm.taskCard.goal }}</p>

          <div class="mt-6">
            <h3 class="text-sm font-semibold text-slate-800">接下来你会做这几件事</h3>
            <ol class="mt-2 space-y-2 text-sm leading-7 text-slate-700">
              <li v-for="(item, index) in previewVm.taskCard.tasks" :key="index" class="flex gap-2">
                <span class="shrink-0 font-medium text-slate-500">{{ index + 1 }}.</span>
                <span>{{ item }}</span>
              </li>
            </ol>
          </div>

          <div class="mt-6 rounded-2xl bg-slate-50/80 px-4 py-3">
            <h3 class="text-sm font-semibold text-slate-800">做完会得到</h3>
            <ul class="mt-1.5 space-y-1 text-sm text-slate-700">
              <li v-for="(gain, index) in previewVm.taskCard.completionGains" :key="index" class="flex gap-2">
                <span class="text-emerald-600">✓</span>
                <span>{{ gain }}</span>
              </li>
            </ul>
          </div>
        </section>

        <!-- 3. 为什么这一步适合你 -->
        <section v-if="previewVm.whyFitsYou.length" class="rounded-2xl bg-slate-50/60 px-5 py-4">
          <p class="text-sm font-semibold text-slate-700">为什么先做这一步</p>
          <ul class="mt-2 space-y-1.5 text-sm leading-6 text-slate-600">
            <li v-for="(line, index) in previewVm.whyFitsYou" :key="index">{{ line }}</li>
          </ul>
        </section>

        <!-- 4. 确认后会获得什么 + CTA -->
        <section class="space-y-6">
          <div class="rounded-2xl border border-slate-200/80 bg-white px-5 py-4">
            <p class="text-sm font-semibold text-slate-800">确认后你会</p>
            <p class="mt-1 text-sm leading-6 text-slate-600">{{ previewVm.afterConfirm.expectedGain }}</p>
            <p class="mt-2 text-xs text-slate-500">{{ previewVm.afterConfirm.startGuide }}</p>
          </div>
          <AppButton
            variant="primary"
            block
            :loading="learningPlanStore.confirming"
            class="!min-h-[52px] !rounded-2xl text-base font-semibold shadow-lg transition hover:opacity-95"
            @click="startLearning"
          >
            确认并开始第一步
          </AppButton>
        </section>
      </template>

      <EmptyStatePanel
        v-else
        title="还没有学习规划"
        description="先完成诊断，系统会为你推荐最该先学的一步。"
      >
        <AppButton @click="router.push('/')">返回首页</AppButton>
      </EmptyStatePanel>
    </div>
  </AppShell>
</template>
