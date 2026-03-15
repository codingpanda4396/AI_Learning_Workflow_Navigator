<script setup lang="ts">
import { computed, ref } from 'vue';
import AppButton from '@/components/ui/AppButton.vue';
import type {
  CapabilityProfile,
  DiagnosisEvidenceSource,
  DiagnosisInsights,
  DiagnosisReasoningStep,
  DiagnosisSubmitResponse,
} from '@/types/diagnosis';
import {
  mapDimensionToLabel,
  mapDiagnosisGoalToLabel,
  mapDiagnosisLevelToLabel,
  mapDiagnosisPreferenceToLabel,
  mapDiagnosisTimeBudgetToLabel,
} from '@/utils/diagnosisDisplay';

const props = defineProps<{
  result: DiagnosisSubmitResponse;
}>();

defineEmits<{
  viewPlan: [];
}>();

const reasoningExpanded = ref(false);

const profile = computed(() => props.result.capabilityProfile);
const insights = computed(() => props.result.insights);

const mainConclusion = computed(() => {
  const level = profile.value.currentLevel;
  return mapDiagnosisLevelToLabel(level?.code, level?.label);
});

const summaryText = computed(() => insights.value?.summary?.trim() || '系统已根据你的回答整理出当前状态，接下来会据此安排学习路径。');

const planExplanation = computed(
  () => insights.value?.planExplanation?.trim() || '后续规划会基于这次判断继续细化，从你当前起点更顺畅地推进。',
);

const strengthsDisplay = computed(() => {
  const list = profile.value.strengths?.filter((s) => s?.trim()) ?? [];
  if (list.length === 0) return [];
  if (list.length === 1 && list[0].length < 8) return []; // 智能收敛：单条且过短不单独成块
  return list.slice(0, 4);
});

const weaknessesDisplay = computed(() => {
  const list = profile.value.weaknesses?.filter((w) => w?.trim()) ?? [];
  return list.slice(0, 4);
});

const showStrengthsCard = computed(() => strengthsDisplay.value.length > 0);
const showWeaknessesCard = computed(() => weaknessesDisplay.value.length > 0);

const reasoningSteps = computed(() => props.result.reasoningSteps ?? []);
const reasoningPreview = computed(() =>
  reasoningSteps.value
    .slice(0, 2)
    .map((s) => (s.inferredConclusion?.trim() || ''))
    .filter(Boolean),
);
const hasMoreReasoning = computed(() => reasoningSteps.value.length > 2);

function formatReasoningStep(step: DiagnosisReasoningStep): string {
  const title = step.questionTitle?.trim() || mapDimensionToLabel(step.dimension);
  const answer = step.selectedAnswerLabel?.trim();
  const conclusion = step.inferredConclusion?.trim();
  if (!conclusion) return '';
  if (answer) return `根据你在「${title}」中的选择，系统判断：${conclusion}`;
  return `根据你在「${title}」中的回答，系统判断：${conclusion}`;
}

function formatSourceHint(source: DiagnosisEvidenceSource): string {
  const dim = mapDimensionToLabel(source.dimension);
  const label = source.label?.trim();
  return label ? `依据：你在「${dim}」中的回答支持了这一判断。` : '';
}

const strengthHints = computed(() => {
  const sources = props.result.strengthSources ?? [];
  return sources.slice(0, 2).map(formatSourceHint).filter(Boolean);
});

const weaknessHints = computed(() => {
  const sources = props.result.weaknessSources ?? [];
  return sources.slice(0, 2).map(formatSourceHint).filter(Boolean);
});

const preferenceText = computed(() => {
  const p = profile.value.learningPreference;
  return mapDiagnosisPreferenceToLabel(p?.code, p?.label);
});

const timeBudgetText = computed(() => {
  const t = profile.value.timeBudget;
  return mapDiagnosisTimeBudgetToLabel(t?.code, t?.label);
});

const goalText = computed(() => {
  const g = profile.value.goalOrientation;
  return mapDiagnosisGoalToLabel(g?.code, g?.label);
});
</script>

<template>
  <div class="mx-auto max-w-[720px] space-y-8 pb-10">
    <section class="app-hero">
      <p class="text-sm font-medium text-slate-500">诊断结果</p>
      <h1 class="mt-1 text-2xl font-semibold tracking-tight text-slate-900">系统对你的当前判断</h1>
      <p class="mt-2 text-sm text-slate-600">
        下面是根据你刚才的回答整理出的学习画像，会直接用来安排接下来的学习路径。
      </p>
    </section>

    <!-- 1. 你的当前状态 -->
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <h2 class="text-sm font-semibold uppercase tracking-wider text-slate-500">你的当前状态</h2>
      <p class="mt-3 text-lg font-semibold text-slate-900">{{ mainConclusion }}</p>
      <p class="mt-2 text-sm leading-7 text-slate-600">{{ summaryText }}</p>
      <div v-if="preferenceText || timeBudgetText || goalText" class="mt-4 flex flex-wrap gap-2">
        <span v-if="preferenceText" class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-700">{{ preferenceText }}</span>
        <span v-if="timeBudgetText" class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-700">{{ timeBudgetText }}</span>
        <span v-if="goalText" class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-700">{{ goalText }}</span>
      </div>
    </section>

    <!-- 2. 关键卡点：简洁卡片 -->
    <section v-if="showStrengthsCard || showWeaknessesCard" class="grid gap-4 sm:grid-cols-2">
      <div v-if="showStrengthsCard" class="rounded-2xl border border-emerald-100 bg-emerald-50/80 p-4">
        <h3 class="text-sm font-semibold text-emerald-800">已具备的优势</h3>
        <ul class="mt-2 space-y-1 text-sm text-emerald-900">
          <li v-for="(item, i) in strengthsDisplay" :key="i">· {{ item }}</li>
        </ul>
        <p v-if="strengthHints.length" class="mt-2 text-xs text-emerald-700">{{ strengthHints[0] }}</p>
      </div>
      <div v-if="showWeaknessesCard" class="rounded-2xl border border-amber-100 bg-amber-50/80 p-4">
        <h3 class="text-sm font-semibold text-amber-800">需要重点补强的部分</h3>
        <ul class="mt-2 space-y-1 text-sm text-amber-900">
          <li v-for="(item, i) in weaknessesDisplay" :key="i">· {{ item }}</li>
        </ul>
        <p v-if="weaknessHints.length" class="mt-2 text-xs text-amber-700">{{ weaknessHints[0] }}</p>
      </div>
    </section>

    <!-- 3. 对学习路径的影响 -->
    <section class="rounded-2xl border border-slate-200 bg-slate-50/80 p-5 md:p-6">
      <h2 class="text-sm font-semibold uppercase tracking-wider text-slate-500">会怎样影响接下来的学习</h2>
      <p class="mt-3 text-sm leading-7 text-slate-700">{{ planExplanation }}</p>
      <p class="mt-2 text-xs text-slate-500">下一步将为你生成可确认的学习路径，你可以先看再决定是否开始。</p>
    </section>

    <!-- 折叠：查看系统如何判断 -->
    <section class="rounded-2xl border border-slate-200 bg-white p-5">
      <button
        type="button"
        class="flex w-full items-center justify-between gap-2 text-left text-sm font-medium text-slate-600 hover:text-slate-900"
        @click="reasoningExpanded = !reasoningExpanded"
      >
        <span>{{ reasoningExpanded ? '收起' : '查看系统如何判断' }}</span>
        <span class="text-slate-400" aria-hidden="true">{{ reasoningExpanded ? '▲' : '▼' }}</span>
      </button>
      <div v-if="reasoningExpanded" class="mt-4 space-y-3 border-t border-slate-100 pt-4">
        <template v-if="reasoningSteps.length">
          <p
            v-for="(step, idx) in reasoningSteps"
            :key="idx"
            class="rounded-xl bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700"
          >
            {{ formatReasoningStep(step) }}
          </p>
        </template>
        <p v-else class="text-sm text-slate-500">系统已根据你的回答完成判断，具体依据会在学习路径中体现。</p>
      </div>
      <p v-else-if="reasoningPreview.length" class="mt-2 text-xs text-slate-500">
        {{ reasoningPreview[0] }}
      </p>
    </section>

    <!-- CTA -->
    <footer class="flex flex-col items-center gap-3 pt-2">
      <AppButton size="lg" class="min-w-[220px]" @click="$emit('viewPlan')">
        查看学习路径
      </AppButton>
      <p class="text-center text-xs text-slate-500">
        系统已根据这次诊断完成起步判断，下一步会给出可确认的学习路径。
      </p>
    </footer>
  </div>
</template>
