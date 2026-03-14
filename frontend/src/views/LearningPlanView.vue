<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import PreviewHeroDecisionCard from '@/components/learning-plan/PreviewHeroDecisionCard.vue';
import PreviewKickoffPanel from '@/components/learning-plan/PreviewKickoffPanel.vue';
import PreviewSignalsPanel from '@/components/learning-plan/PreviewSignalsPanel.vue';
import PreviewStrategyComparison from '@/components/learning-plan/PreviewStrategyComparison.vue';
import StrategyAdjustPanel from '@/components/plan/StrategyAdjustPanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import InfoHint from '@/components/ui/InfoHint.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import type { PlanAdjustments, StrategyAdjustAction } from '@/types/learningPlan';
import { buildPreviewViewModel } from '@/utils/usePreviewViewModel';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const strategyNote = computed(() => learningPlanStore.strategyNote);
const previewVm = computed(() => (preview.value ? buildPreviewViewModel(preview.value) : null));

const adjustPanelOpen = ref(false);
const adjustPanelMode = ref<'strategy' | 'disagree'>('strategy');

const explainPrompts = [
  { key: 'why-first', label: '为什么现在先学这个？' },
  { key: 'fast', label: '给我一个10分钟压缩版' },
];

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

function openAdjustPanel(mode: 'strategy' | 'disagree') {
  adjustPanelMode.value = mode;
  adjustPanelOpen.value = true;
}

function resolveAdjustments(action: StrategyAdjustAction): PlanAdjustments | null {
  const current = preview.value?.adjustments ?? learningPlanStore.adjustments;
  switch (action) {
    case 'faster':
      return { ...current, intensity: 'LIGHT', prioritizeFoundation: false };
    case 'steadier':
      return { ...current, intensity: 'STANDARD', prioritizeFoundation: true };
    case 'practice-first':
      return { ...current, learningMode: 'LEARN_BY_DOING' };
    case 'ten-minute':
      return { ...current, intensity: 'LIGHT' };
    case 'already-know':
      return { ...current, prioritizeFoundation: false, learningMode: 'LEARN_BY_DOING' };
    case 'not-enough-time':
      return { ...current, intensity: 'LIGHT' };
    case 'not-clear':
    default:
      return null;
  }
}

async function regenerateWithAdjustments(nextAdjustments: PlanAdjustments | null) {
  if (!nextAdjustments || !learningPlanStore.request) {
    return;
  }
  learningPlanStore.setAdjustments(nextAdjustments);
  try {
    await learningPlanStore.regeneratePreview();
  } catch {
    return;
  }
}

async function onSelectStrategy(action: StrategyAdjustAction) {
  await learningPlanStore.submitStrategyFeedback({
    action,
    intent: adjustPanelMode.value,
  });
  await regenerateWithAdjustments(resolveAdjustments(action));
  adjustPanelOpen.value = false;
}

async function onMastered() {
  adjustPanelMode.value = 'disagree';
  await learningPlanStore.submitStrategyFeedback({
    action: 'already-know',
    intent: 'disagree',
  });
  await regenerateWithAdjustments(resolveAdjustments('already-know'));
}

async function onAskAi(prompt: string) {
  await learningPlanStore.submitStrategyFeedback({
    action: 'ask-ai',
    intent: 'ai-explain',
    prompt,
  });
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
          <PreviewHeroDecisionCard
            :title="previewVm.hero.title"
            :strongest-reason="previewVm.hero.strongestReason"
            :risk-if-skip="previewVm.hero.riskIfSkip"
            :estimate="previewVm.hero.estimate"
            :cta-label="previewVm.hero.ctaLabel"
            :loading="learningPlanStore.confirming"
            @start="startLearning"
          />

          <PreviewSignalsPanel
            :items="previewVm.signals"
          />

          <PreviewStrategyComparison
            :recommended-reason="previewVm.strategy.recommendedReason"
            :recommended="previewVm.strategy.recommended"
            :others="previewVm.strategy.others"
          />

          <PreviewKickoffPanel :kickoff="previewVm.kickoff" />

          <section class="app-card app-card-padding rounded-[24px]">
            <p class="text-sm font-semibold text-slate-900">需要微调推荐时</p>
            <div class="mt-3 flex flex-wrap gap-3">
              <AppButton variant="secondary" @click="openAdjustPanel('strategy')">调整学习节奏</AppButton>
              <AppButton variant="ghost" @click="openAdjustPanel('disagree')">我不认同这个建议</AppButton>
              <button
                type="button"
                class="rounded-full border border-slate-200 bg-slate-50 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-100"
                @click="onMastered"
              >
                我已经会了
              </button>
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button
                v-for="item in explainPrompts"
                :key="item.key"
                type="button"
                class="rounded-full border border-slate-200 px-3 py-2 text-xs text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
                @click="onAskAi(item.label)"
              >
                {{ item.label }}
              </button>
            </div>
            <p v-if="strategyNote" class="mt-3 text-sm leading-7 text-slate-600">{{ strategyNote }}</p>
          </section>

          <InfoHint v-if="error" tone="danger">{{ error }}</InfoHint>
        </section>

        <StrategyAdjustPanel
          :open="adjustPanelOpen"
          :mode="adjustPanelMode"
          :loading="learningPlanStore.strategySubmitting || learningPlanStore.regenerating"
          :pending-note="strategyNote"
          @close="adjustPanelOpen = false"
          @select="onSelectStrategy"
        />
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
