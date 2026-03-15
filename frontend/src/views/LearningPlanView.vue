<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import PreviewExplanationPanel from '@/components/plan/PreviewExplanationPanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import { getPreviewMetricsSnapshot, trackPreviewAccepted, trackPreviewShown } from '@/utils/previewMetrics';
import { buildLearningPlanPreviewView } from '@/utils/useLearningPlanPreviewView';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const previewVm = computed(() => (preview.value ? buildLearningPlanPreviewView(preview.value) : null));
const explanationExpanded = ref(false);

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
    explanationExpanded.value = false;
    await loadPlan();
  },
  { immediate: true },
);

watch(
  () => preview.value?.id,
  (previewId) => {
    if (!previewId) {
      return;
    }
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
    <div class="mx-auto max-w-[1120px] space-y-8 pb-12">
      <div v-if="viewState === 'loading'" class="space-y-6">
        <section class="app-hero min-h-[320px]">
          <div class="grid h-full gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
            <div class="space-y-4">
              <SkeletonBlock width="96px" :height="16" rounded="999px" />
              <SkeletonBlock width="72%" :height="56" rounded="14px" />
              <SkeletonBlock width="88%" :height="20" rounded="10px" />
              <SkeletonBlock width="70%" :height="20" rounded="10px" />
            </div>
            <SkeletonBlock :height="220" rounded="20px" />
          </div>
        </section>
        <SkeletonBlock :height="180" rounded="24px" />
        <SkeletonBlock :height="220" rounded="24px" />
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习规划生成失败。'" />
        <AppButton @click="loadPlan">重新生成学习规划</AppButton>
      </div>

      <template v-else-if="preview && previewVm">
        <section class="space-y-6">
          <header class="space-y-2">
            <p class="text-sm font-medium text-slate-500">学习规划</p>
            <h1 class="text-3xl font-semibold tracking-tight text-slate-900">AI 学习导航</h1>
            <p class="text-sm text-slate-600">根据你的当前状态，系统为你安排了下一步任务。</p>
          </header>

          <section class="app-card rounded-[26px] p-6 md:p-8">
            <p class="text-sm font-semibold text-slate-500">个性化摘要</p>
            <h2 class="mt-2 text-2xl font-semibold text-slate-900">{{ previewVm.summary.title }}</h2>
            <p class="mt-2 text-sm leading-7 text-slate-600">{{ previewVm.summary.description }}</p>
            <div class="mt-4 flex flex-wrap gap-2">
              <span v-for="tag in previewVm.summary.tags" :key="tag" class="app-badge">
                {{ tag }}
              </span>
            </div>
          </section>

          <section class="app-card app-card-strong rounded-[30px] p-6 shadow-[0_20px_56px_rgba(15,23,42,0.12)] md:p-9">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <p class="text-sm font-semibold tracking-wide text-slate-500">下一步任务</p>
              <span class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-semibold text-slate-600">
                预计 {{ previewVm.taskCard.estimatedTime }}
              </span>
            </div>
            <h2 class="mt-3 text-3xl font-semibold tracking-tight text-slate-900">{{ previewVm.taskCard.title }}</h2>
            <p class="mt-3 text-sm leading-7 text-slate-600">{{ previewVm.taskCard.goal }}</p>

            <div class="mt-8 grid gap-6 md:grid-cols-2">
              <div>
                <h3 class="text-base font-semibold text-slate-900">你需要做什么</h3>
                <ol class="mt-3 space-y-2 text-sm leading-7 text-slate-700">
                  <li v-for="(item, index) in previewVm.taskCard.tasks" :key="`task-${index}`">{{ index + 1 }}. {{ item }}</li>
                </ol>
              </div>
              <div>
                <h3 class="text-base font-semibold text-slate-900">完成后你将掌握</h3>
                <ul class="mt-3 space-y-2 text-sm leading-7 text-slate-700">
                  <li v-for="(item, index) in previewVm.taskCard.completionGains" :key="`gain-${index}`">- {{ item }}</li>
                </ul>
              </div>
            </div>

            <div class="mt-9">
              <AppButton
                variant="primary"
                block
                :loading="learningPlanStore.confirming"
                class="!min-h-[48px] !rounded-2xl text-base font-semibold transition-transform duration-200 hover:-translate-y-0.5"
                @click="startLearning"
              >
                开始学习
              </AppButton>
            </div>
          </section>

          <PreviewExplanationPanel
            :expanded="explanationExpanded"
            :why-recommended="previewVm.explanation.whyRecommended"
            :why-this-step-first="previewVm.explanation.whyThisStepFirst"
            :learner-profile="previewVm.explanation.learnerProfile"
            :system-decision="previewVm.explanation.systemDecision"
            @toggle="explanationExpanded = !explanationExpanded"
          />
        </section>
      </template>

      <EmptyStatePanel
        v-else
        title="还没有拿到这次学习规划"
        description="先完成诊断，系统才能判断你现在最该先学哪一步。"
      >
        <AppButton @click="router.push('/')">返回首页</AppButton>
      </EmptyStatePanel>
    </div>
  </AppShell>
</template>
