<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import { STAGE_LABELS } from '@/constants/learningPlan';
import type { PlanTaskPreview } from '@/types/learningPlan';

const props = defineProps<{
  tasks: PlanTaskPreview[];
  nextStepNote: string;
  busy?: boolean;
}>();

defineEmits<{
  focusConfirm: [];
}>();

const stageMarkers: Record<string, string> = {
  STRUCTURE: 'Stage 01',
  UNDERSTANDING: 'Stage 02',
  TRAINING: 'Stage 03',
  REFLECTION: 'Stage 04',
};

const stageOutcomes: Record<string, string> = {
  STRUCTURE: 'Frame the chapter before diving into details.',
  UNDERSTANDING: 'Turn key concepts into actual understanding.',
  TRAINING: 'Convert understanding into usable performance.',
  REFLECTION: 'Close the loop and decide the next iteration.',
};
</script>

<template>
  <PageSection
    eyebrow="Execution"
    title="What this preview turns into after confirmation"
    description="Preview and committed status are separated explicitly so the user can tell whether this is still a draft."
  >
    <div class="rounded-[2rem] border border-slate-200 bg-[linear-gradient(180deg,#ffffff_0%,#f8fafc_100%)] p-4 md:p-5">
      <div class="grid gap-4 xl:grid-cols-4">
        <article
          v-for="task in props.tasks"
          :key="task.stage"
          class="relative rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-[0_18px_45px_rgba(15,23,42,0.05)]"
        >
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">{{ stageMarkers[task.stage] || task.stage }}</p>
              <h3 class="mt-2 text-lg font-semibold tracking-tight text-slate-950">{{ STAGE_LABELS[task.stage] }}</h3>
            </div>
            <span class="rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold text-white">
              {{ task.estimatedTaskMinutes }} min
            </span>
          </div>
          <p class="mt-4 text-sm font-medium leading-6 text-slate-900">{{ task.title || task.learningGoal }}</p>
          <dl class="mt-4 space-y-3 text-sm leading-6 text-slate-600">
            <div>
              <dt class="font-semibold text-slate-900">Learner action</dt>
              <dd class="mt-1">{{ task.learnerAction }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">AI support</dt>
              <dd class="mt-1">{{ task.aiSupport }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">Stage outcome</dt>
              <dd class="mt-1">{{ stageOutcomes[task.stage] }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>

    <div class="mt-5 rounded-[2rem] bg-slate-950 px-5 py-5 text-white shadow-[0_26px_70px_rgba(15,23,42,0.22)] md:px-6">
      <p class="text-sm font-semibold text-white">The preview is ready for confirmation</p>
      <p class="mt-2 text-sm leading-7 text-slate-300">{{ props.nextStepNote || 'Confirming will create the formal learning session.' }}</p>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs leading-6 text-slate-400">After confirmation we create the session and start from the first stage returned by the backend.</p>
        <button
          type="button"
          class="rounded-2xl bg-white px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="props.busy"
          @click="$emit('focusConfirm')"
        >
          Review confirmation area
        </button>
      </div>
    </div>
  </PageSection>
</template>
