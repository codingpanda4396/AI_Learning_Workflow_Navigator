<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import AdaptationHintCard from '@/components/plan/AdaptationHintCard.vue';
import AlternativeStrategiesPanel from '@/components/plan/AlternativeStrategiesPanel.vue';
import CurrentStepCard from '@/components/plan/CurrentStepCard.vue';
import PersonalizedInsightCard from '@/components/plan/PersonalizedInsightCard.vue';
import StrategyAdjustPanel from '@/components/plan/StrategyAdjustPanel.vue';
import WhyThisPlanCard from '@/components/plan/WhyThisPlanCard.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import InfoHint from '@/components/ui/InfoHint.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import type { PlanAdjustments, StrategyAdjustAction } from '@/types/learningPlan';
import { buildLearningPlanViewModel } from '@/utils/buildLearningPlanViewModel';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const strategyNote = computed(() => learningPlanStore.strategyNote);
const planVm = computed(() => (preview.value ? buildLearningPlanViewModel(preview.value) : null));

const adjustPanelOpen = ref(false);
const adjustPanelMode = ref<'strategy' | 'disagree'>('strategy');
const alternativesOpen = ref(false);

const explainPrompts = [
  { key: 'why-first', label: '为什么我要先学这个？' },
  { key: 'goal-link', label: '这一步和目标有什么关系？' },
  { key: 'fast', label: '给我一个5分钟极速版' },
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
    <div class="mx-auto max-w-[1180px] space-y-10 pb-10">
      <div v-if="viewState === 'loading'" class="space-y-6">
        <section class="min-h-[calc(100vh-128px)] rounded-[36px] border border-slate-200 bg-slate-50 px-6 py-8 sm:px-8">
          <div class="grid h-full gap-8 lg:grid-cols-[minmax(0,1.4fr)_320px] lg:items-end">
            <div class="space-y-4 self-center">
              <SkeletonBlock width="90px" :height="18" rounded="999px" />
              <SkeletonBlock width="68%" :height="64" rounded="16px" />
              <SkeletonBlock width="52%" :height="18" rounded="10px" />
              <SkeletonBlock width="78%" :height="18" rounded="10px" />
            </div>
            <SkeletonBlock :height="320" rounded="28px" />
          </div>
        </section>
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习规划生成失败。'" />
        <AppButton @click="loadPlan">重新生成学习规划</AppButton>
      </div>

      <template v-else-if="preview && planVm">
        <section class="space-y-5">
          <PersonalizedInsightCard
            :title="planVm.learnerInsightTitle"
            :learner-state="planVm.learnerState"
          />

          <WhyThisPlanCard
            :title="planVm.whySectionTitle"
            :what-i-saw="planVm.whatISaw"
            :why-this-plan-fits-you="planVm.whyThisPlanFitsYou"
          />

          <CurrentStepCard
            :title="planVm.stepSectionTitle"
            :task-title="planVm.taskTitle"
            :estimated-minutes-text="planVm.estimatedMinutesText"
            :this-round-boundary="planVm.thisRoundBoundary"
            :next-step-label="planVm.nextStepLabel"
            :cta-hint="planVm.ctaHint"
            :has-alternatives="planVm.alternatives.length > 0"
            :loading="learningPlanStore.confirming"
            @start="startLearning"
            @show-alternatives="alternativesOpen = true"
          />

          <AdaptationHintCard
            :title="planVm.riskTitle"
            :main-risk-if-skip="planVm.mainRiskIfSkip"
            :adaptation-hint="planVm.adaptationHint"
          />

          <AlternativeStrategiesPanel
            :alternatives="planVm.alternatives"
            :open-by-default="alternativesOpen"
          />

          <section class="rounded-[24px] border border-slate-200 bg-white px-5 py-5">
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
