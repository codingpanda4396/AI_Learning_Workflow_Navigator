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
import { buildPreviewViewModel } from '@/utils/usePreviewViewModel';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const previewVm = computed(() => (preview.value ? buildPreviewViewModel(preview.value) : null));

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
        <section class="space-y-5">
          <section class="app-hero rounded-[24px] p-6">
            <p class="text-sm font-medium text-indigo-600">下一步学什么</p>
            <h2 class="mt-2 text-3xl font-semibold text-slate-900">{{ previewVm.hero.title }}</h2>
            <p class="mt-3 text-base leading-7 text-slate-700">{{ previewVm.hero.reason }}</p>
            <p class="mt-3 text-sm text-slate-500">预计耗时：{{ previewVm.hero.estimate }}</p>
            <div class="mt-5">
              <AppButton :loading="learningPlanStore.confirming" @click="startLearning">{{ previewVm.hero.ctaLabel }}</AppButton>
            </div>
          </section>

          <section class="app-card app-card-padding rounded-[24px]">
            <h3 class="text-lg font-semibold text-slate-900">AI 看到了什么</h3>
            <p class="mt-2 text-sm leading-7 text-slate-700">{{ previewVm.aiObserved.currentState }}</p>
            <ul class="mt-3 space-y-2 text-sm leading-7 text-slate-600">
              <li v-for="(item, index) in previewVm.aiObserved.evidence" :key="`evidence-${index}`">- {{ item }}</li>
            </ul>
          </section>

          <section class="app-card app-card-padding rounded-[24px]">
            <h3 class="text-lg font-semibold text-slate-900">为什么推荐这条路</h3>
            <p class="mt-2 text-sm text-slate-700">
              当前推荐：<span class="font-medium text-slate-900">{{ previewVm.strategy.recommendedLabel }}</span>
            </p>
            <p class="mt-2 text-sm leading-7 text-slate-600">{{ previewVm.strategy.explanation }}</p>
            <div v-if="previewVm.strategy.alternatives.length" class="mt-4 space-y-3">
              <div
                v-for="(item, index) in previewVm.strategy.alternatives"
                :key="`alt-${index}`"
                class="rounded-xl border border-slate-200 bg-slate-50 p-3"
              >
                <p class="text-sm font-medium text-slate-900">{{ item.label }}</p>
                <p class="mt-1 text-sm text-slate-600">{{ item.reason }}</p>
              </div>
            </div>
          </section>

          <section class="app-card app-card-padding rounded-[24px]">
            <h3 class="text-lg font-semibold text-slate-900">确认后怎么开始</h3>
            <ul class="mt-3 space-y-2 text-sm leading-7 text-slate-700">
              <li v-for="(action, index) in previewVm.kickoff.actions" :key="`next-${index}`">{{ index + 1 }}. {{ action }}</li>
            </ul>
            <p class="mt-4 text-sm leading-7 text-slate-600">{{ previewVm.kickoff.systemGuide }}</p>
          </section>
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
