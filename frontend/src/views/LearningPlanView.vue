<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import AiExplainEntry from '@/components/plan/AiExplainEntry.vue';
import DecisionReasonPanel from '@/components/plan/DecisionReasonPanel.vue';
import LearningPathStatusCard from '@/components/plan/LearningPathStatusCard.vue';
import RecommendationHeroCard from '@/components/plan/RecommendationHeroCard.vue';
import StrategyAdjustPanel from '@/components/plan/StrategyAdjustPanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import InfoHint from '@/components/ui/InfoHint.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import type { PlanAdjustments, StrategyAdjustAction } from '@/types/learningPlan';
import { adaptLearningPlanPreview } from '@/utils/learningPlanAdapter';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const strategyNote = computed(() => learningPlanStore.strategyNote);
const navigatorVm = computed(() => (preview.value ? adaptLearningPlanPreview(preview.value) : null));

const adjustPanelOpen = ref(false);
const adjustPanelMode = ref<'strategy' | 'disagree'>('strategy');

const explainPrompts = [
  { key: 'harder', label: '为什么我现在不适合直接学更难的内容？' },
  { key: 'goal', label: '这一步和我的目标有什么关系？' },
  { key: 'fast', label: '给我一个 5 分钟极速版' },
  { key: 'example', label: '用一个例子解释为什么先学这个' },
];

const context = computed(() => {
  const sessionId = Number(route.query.sessionId ?? 0);
  const diagnosisId = String(route.query.diagnosisId ?? '').trim();
  return {
    sessionId: Number.isFinite(sessionId) && sessionId > 0 ? sessionId : undefined,
    diagnosisId,
    goalText: String(route.query.goal ?? '').trim() || '先把当前阶段最值得推进的一步学明白',
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
    <div class="mx-auto max-w-[1180px] space-y-6 pb-8">
      <div v-if="viewState === 'loading'" class="app-stack-md">
        <section class="app-hero">
          <div class="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_320px]">
            <div class="space-y-4">
              <div class="flex gap-2">
                <SkeletonBlock width="88px" :height="28" rounded="999px" />
                <SkeletonBlock width="88px" :height="28" rounded="999px" />
              </div>
              <SkeletonBlock width="48%" :height="18" rounded="10px" />
              <SkeletonBlock width="72%" :height="44" rounded="16px" />
              <SkeletonBlock width="84%" :height="18" rounded="10px" />
              <SkeletonBlock :height="220" rounded="28px" />
            </div>
            <SkeletonBlock :height="360" rounded="28px" />
          </div>
        </section>
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习规划生成失败。'" />
        <AppButton @click="loadPlan">重新生成学习规划</AppButton>
      </div>

      <template v-else-if="preview && navigatorVm">
        <RecommendationHeroCard
          :source-label="navigatorVm.decisionCard.sourceLabel"
          :source-type="navigatorVm.decisionCard.sourceType"
          :confidence-label="navigatorVm.decisionCard.confidenceLabel"
          :confidence-level="navigatorVm.decisionCard.confidenceLevel"
          :recommendation-headline="navigatorVm.decisionCard.recommendationHeadline"
          :recommendation-subtitle="navigatorVm.decisionCard.recommendationSubtitle"
          :current-task-title="navigatorVm.decisionCard.currentTaskTitle"
          :estimated-minutes="navigatorVm.decisionCard.estimatedMinutes"
          :priority="navigatorVm.decisionCard.priority"
          :current-status="navigatorVm.decisionCard.currentStatus"
          :loading="learningPlanStore.confirming"
          @start="startLearning"
          @adjust="openAdjustPanel('strategy')"
          @mastered="onMastered"
        />

        <div class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
          <div class="space-y-5">
            <SectionCard
              strong
              title="学完这一步你会立刻获得"
              description="只保留最直接、最能感知到的变化。"
            >
              <div class="grid gap-3 md:grid-cols-2">
                <div v-for="item in navigatorVm.benefits" :key="item.key" class="app-option app-option-selected">
                  <p class="text-sm font-semibold text-slate-950">{{ item.title }}</p>
                  <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
                </div>
              </div>

              <div class="app-divider my-5" />

              <div>
                <p class="text-sm font-semibold text-slate-950">解锁后续</p>
                <div class="mt-3 grid gap-3 md:grid-cols-2">
                  <div v-for="item in navigatorVm.nextUnlocks" :key="item.key" class="rounded-[20px] border border-slate-200 bg-slate-50 px-4 py-4">
                    <p class="text-sm font-semibold text-slate-950">{{ item.title }}</p>
                    <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
                  </div>
                </div>
              </div>
            </SectionCard>

            <DecisionReasonPanel
              :learner-goal="navigatorVm.learnerGoal"
              :current-weaknesses="navigatorVm.currentWeaknesses"
              :mastery-score="navigatorVm.masteryScore"
              :based-on-current-state="navigatorVm.basedOnCurrentState"
              :decision-reasons="navigatorVm.decisionReasons"
              :alternatives="navigatorVm.alternatives"
            />

            <SectionCard
              title="不想照单全收，也可以立刻改策略"
              description="你可以换一种学法，也可以直接表达不同意这个判断。"
            >
              <div class="flex flex-col gap-3 sm:flex-row">
                <AppButton size="lg" @click="openAdjustPanel('strategy')">换一种学法</AppButton>
                <AppButton size="lg" variant="secondary" @click="openAdjustPanel('disagree')">我不认同这个建议</AppButton>
              </div>
              <InfoHint v-if="strategyNote" class="mt-4">{{ strategyNote }}</InfoHint>
            </SectionCard>
          </div>

          <LearningPathStatusCard
            :current-focus="navigatorVm.currentFocus"
            :current-status="navigatorVm.decisionCard.currentStatus"
            :next-step="navigatorVm.nextStep"
            :path-risk="navigatorVm.pathRisk"
            :stages="navigatorVm.pathStages"
          />
        </div>

        <AiExplainEntry :prompts="explainPrompts" @ask="onAskAi" />

        <InfoHint v-if="error" tone="danger">{{ error }}</InfoHint>

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
