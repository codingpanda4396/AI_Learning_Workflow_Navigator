<template>
  <section class="rounded-2xl border border-indigo-100/70 bg-white px-5 py-5 shadow-sm md:px-6">
    <h2 class="text-xl font-semibold tracking-tight text-slate-900">
      {{ model.headline }}
    </h2>

    <dl class="mt-5 space-y-4 text-sm leading-relaxed text-slate-700">
      <div>
        <dt class="text-xs font-medium uppercase tracking-[0.14em] text-slate-500">
          任务目标
        </dt>
        <dd class="mt-1.5">
          {{ model.goalLine }}
        </dd>
      </div>
      <div>
        <dt class="text-xs font-medium uppercase tracking-[0.14em] text-slate-500">
          产出要求
        </dt>
        <dd class="mt-1.5">
          {{ model.deliverableLine }}
        </dd>
      </div>
      <div>
        <dt class="text-xs font-medium uppercase tracking-[0.14em] text-amber-700">
          错误提醒
        </dt>
        <dd class="mt-1.5 text-slate-700">
          {{ model.errorReminder }}
        </dd>
      </div>
    </dl>

    <div class="mt-6 border-t border-slate-100 pt-4">
      <button
        type="button"
        class="text-sm font-medium text-primary underline-offset-4 hover:underline"
        @click="$emit('enter')"
      >
        {{ model.enterTaskLabel }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['firstTask']
}>()

defineEmits<{
  enter: []
}>()
</script>
