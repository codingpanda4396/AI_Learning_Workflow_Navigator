<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import PageSection from '@/components/common/PageSection.vue';
import PlanActionBar from '@/components/plan/PlanActionBar.vue';
import PlanAdjustPanel from '@/components/plan/PlanAdjustPanel.vue';
import PlanPathPreviewPanel from '@/components/plan/PlanPathPreviewPanel.vue';
import PlanReasonPanel from '@/components/plan/PlanReasonPanel.vue';
import PlanSummaryPanel from '@/components/plan/PlanSummaryPanel.vue';
import PlanTaskPreviewPanel from '@/components/plan/PlanTaskPreviewPanel.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import type { PlanAdjustments } from '@/types/learningPlan';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const adjustments = computed({
  get: () => learningPlanStore.adjustments,
  set: (value: PlanAdjustments) => learningPlanStore.setAdjustments(value),
});

const context = computed(() => {
  const sessionId = Number(route.query.sessionId ?? 0);
  const diagnosisId = String(route.query.diagnosisId ?? '').trim();
  return {
    sessionId: Number.isFinite(sessionId) && sessionId > 0 ? sessionId : undefined,
    diagnosisId,
    goalText: String(route.query.goal ?? '').trim() || 'Master the current chapter focus',
    courseName: String(route.query.course ?? '').trim() || 'General Course',
    chapterName: String(route.query.chapter ?? '').trim() || 'Current Chapter',
  };
});

const viewState = computed(() => {
  if (learningPlanStore.loading && !preview.value) {
    return 'loading';
  }
  if (error.value && !preview.value) {
    return 'error';
  }
  if (learningPlanStore.confirming) {
    return 'confirming';
  }
  if (learningPlanStore.regenerating) {
    return 'regenerating';
  }
  return 'ready';
});

async function loadPlan() {
  if (!context.value.diagnosisId || !context.value.goalText) {
    learningPlanStore.error = 'Missing diagnosis or goal parameters for plan preview.';
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

async function regeneratePlan() {
  try {
    await learningPlanStore.regeneratePreview();
  } catch {
    return;
  }
}

async function confirmPlan() {
  let sessionId: number | undefined;
  try {
    sessionId = await learningPlanStore.confirmPlan();
  } catch {
    return;
  }
  if (sessionId) {
    await router.push(`/sessions/${sessionId}`);
  }
}

async function goBackToGoal() {
  await router.push({
    path: '/',
    query: {
      goal: context.value.goalText,
      course: context.value.courseName,
      chapter: context.value.chapterName,
      diagnosisId: context.value.diagnosisId,
    },
  });
}

async function openDiagnosis() {
  if (context.value.sessionId) {
    await router.push({
      path: `/diagnosis/${context.value.sessionId}`,
      query: {
        goal: context.value.goalText,
        course: context.value.courseName,
        chapter: context.value.chapterName,
        diagnosisId: context.value.diagnosisId,
      },
    });
    return;
  }
  await router.push('/diagnosis');
}

function focusConfirmSection() {
  document.getElementById('plan-confirm-anchor')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
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
    <div class="space-y-6 pb-12">
      <div v-if="viewState === 'loading'" class="space-y-6">
        <section class="overflow-hidden rounded-[2.4rem] bg-[linear-gradient(135deg,#0f172a_0%,#13263f_48%,#1d4f6f_100%)] px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.22)] md:px-8">
          <div class="animate-pulse">
            <div class="h-3 w-32 rounded-full bg-white/20" />
            <div class="mt-5 h-10 max-w-4xl rounded-2xl bg-white/18" />
            <div class="mt-3 h-5 max-w-3xl rounded-2xl bg-white/14" />
            <div class="mt-8 grid gap-3 md:grid-cols-5">
              <div v-for="item in 5" :key="item" class="h-24 rounded-[1.5rem] bg-white/10" />
            </div>
          </div>
        </section>

        <PageSection
          eyebrow="Generating"
          title="Turning diagnosis results into an actionable learning path"
          description="The system is combining your goal, weak points, and current foundation to decide where this round should start."
        >
          <div class="grid gap-4 md:grid-cols-3">
            <div v-for="item in 3" :key="item" class="animate-pulse rounded-[1.7rem] border border-slate-200 bg-white p-5">
              <div class="h-3 w-24 rounded-full bg-slate-200" />
              <div class="mt-4 h-5 rounded-full bg-slate-200" />
              <div class="mt-3 h-4 rounded-full bg-slate-100" />
              <div class="mt-2 h-4 w-5/6 rounded-full bg-slate-100" />
            </div>
          </div>
        </PageSection>
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || 'Failed to generate the learning plan preview.'" />
        <div class="flex justify-start">
          <button
            type="button"
            class="rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            @click="loadPlan"
          >
            Retry Preview
          </button>
        </div>
      </div>

      <template v-else-if="preview">
        <div v-if="error" class="rounded-[1.6rem] border border-rose-100 bg-rose-50 px-5 py-4 text-sm leading-7 text-rose-700">
          {{ error }}
        </div>

        <PlanSummaryPanel :preview="preview" />

        <div
          v-if="viewState === 'regenerating' || viewState === 'confirming'"
          class="rounded-[1.6rem] border border-sky-100 bg-sky-50 px-5 py-4 text-sm leading-7 text-sky-700"
        >
          {{
            viewState === 'confirming'
              ? 'Creating a learning session from this preview.'
              : 'Regenerating the path with your updated adjustments.'
          }}
        </div>

        <PlanReasonPanel :reasons="preview.reasons" :diagnosis-summary="preview.context.diagnosisSummary" />
        <PlanPathPreviewPanel :nodes="preview.pathNodes" />
        <PlanTaskPreviewPanel
          :tasks="preview.taskPreviews"
          :next-step-note="preview.nextStepNote"
          :busy="learningPlanStore.confirming || learningPlanStore.regenerating"
          @focus-confirm="focusConfirmSection"
        />
        <PlanAdjustPanel
          v-model="adjustments"
          :disabled="learningPlanStore.confirming"
          :regenerating="learningPlanStore.regenerating"
          @regenerate="regeneratePlan"
        />
        <div id="plan-confirm-anchor" class="pt-2" />
        <PlanActionBar
          :confirming="learningPlanStore.confirming"
          :regenerating="learningPlanStore.regenerating"
          @confirm="confirmPlan"
          @regenerate="regeneratePlan"
          @back="goBackToGoal"
          @diagnosis="openDiagnosis"
        />
      </template>
    </div>
  </AppShell>
</template>
