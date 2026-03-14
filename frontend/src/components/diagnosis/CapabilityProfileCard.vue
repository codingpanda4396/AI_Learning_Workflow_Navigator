<script setup lang="ts">
import { computed } from 'vue';
import { resolveCapabilityProfileCopy } from '@/types/diagnosis';
import type { CapabilityProfile, DiagnosisInsights } from '@/types/diagnosis';

const props = defineProps<{
  profile: CapabilityProfile;
  insights?: DiagnosisInsights | null;
}>();

const profileCopy = computed(() => resolveCapabilityProfileCopy(props.insights ?? undefined));
</script>

<template>
  <section class="rounded-[1.9rem] border border-slate-200 bg-white p-6 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-600">Profile</p>
    <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Your Capability Snapshot</h2>
    <p class="mt-3 text-sm leading-6 text-slate-600">
      {{ profileCopy.summary }}
    </p>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Current Level</p>
        <p class="mt-2 text-lg font-semibold text-slate-950">{{ profile.currentLevel.label }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Learning Preference</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.learningPreference?.label || 'The system will organize content in a clear and gradual way.' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Time Budget</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.timeBudget?.label || 'A steady weekly rhythm is recommended.' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Goal Orientation</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.goalOrientation?.label || 'The system will start from the most relevant foundational steps.' }}
        </p>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div>
        <p class="text-sm font-semibold text-slate-900">Strengths</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in profile.strengths"
            :key="item"
            class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div>
        <p class="text-sm font-semibold text-slate-900">Weaknesses</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in profile.weaknesses"
            :key="item"
            class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>

    <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-700">
      {{ profileCopy.planExplanation }}
    </div>
  </section>
</template>
