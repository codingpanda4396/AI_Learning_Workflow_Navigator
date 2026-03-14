<script setup lang="ts">
import { computed } from 'vue';
import { resolveCapabilityProfileCopy } from '@/types/diagnosis';
import type { CapabilityProfile, DiagnosisInsights, DiagnosisMetadata, DiagnosisNextAction } from '@/types/diagnosis';

const props = defineProps<{
  profile: CapabilityProfile;
  insights?: DiagnosisInsights | null;
  nextAction?: DiagnosisNextAction | null;
  status?: string;
  fallbackText?: string;
  sourceText?: string;
  metadata?: DiagnosisMetadata | null;
}>();

const profileCopy = computed(() => resolveCapabilityProfileCopy(props.insights ?? undefined));
const strengths = computed(() => (props.profile.strengths.length ? props.profile.strengths : ['Current strengths will be shown after enough signal is collected.']));
const weaknesses = computed(() => (props.profile.weaknesses.length ? props.profile.weaknesses : ['No explicit weak area was returned in this response.']));
</script>

<template>
  <section class="rounded-[1.9rem] border border-slate-200 bg-white p-6 shadow-sm">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-600">Profile</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Capability snapshot</h2>
        <p class="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
          {{ profileCopy.summary }}
        </p>
      </div>
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="rounded-2xl bg-slate-50 p-4 text-sm text-slate-700">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Status</p>
          <p class="mt-2 font-medium text-slate-900">{{ status || 'SUBMITTED' }}</p>
        </div>
        <div class="rounded-2xl bg-slate-50 p-4 text-sm text-slate-700">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Next action</p>
          <p class="mt-2 font-medium text-slate-900">{{ nextAction?.label || 'Plan preview' }}</p>
        </div>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Current level</p>
        <p class="mt-2 text-lg font-semibold text-slate-950">{{ profile.currentLevel.label }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Learning preference</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.learningPreference?.label || 'The system will keep the learning flow gradual and readable.' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Time budget</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.timeBudget?.label || 'A stable weekly rhythm is recommended.' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Goal orientation</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.goalOrientation?.label || 'The next plan will start from the most relevant prerequisite.' }}
        </p>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div>
        <p class="text-sm font-semibold text-slate-900">Strengths</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in strengths"
            :key="item"
            class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div>
        <p class="text-sm font-semibold text-slate-900">Watchouts</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in weaknesses"
            :key="item"
            class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-3">
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Source</p>
        <p class="mt-2 font-medium text-slate-900">{{ sourceText || 'Contract response' }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Fallback</p>
        <p class="mt-2 font-medium text-slate-900">{{ fallbackText || 'No fallback applied' }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Metadata</p>
        <p class="mt-2 font-medium text-slate-900">
          {{ metadata?.questionCount || 0 }} questions / {{ metadata?.answerCount || 0 }} answers / v{{ metadata?.profileVersion || 1 }}
        </p>
      </div>
    </div>

    <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-700">
      {{ profileCopy.planExplanation }}
    </div>
  </section>
</template>
