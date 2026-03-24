<template>
  <section
    v-if="model.visible"
    data-testid="execution-system-feedback"
    class="rounded-[28px] border border-emerald-100 bg-[linear-gradient(180deg,_rgba(236,253,245,0.9),_rgba(255,255,255,0.98))] p-5 shadow-card"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
          {{ model.title }}
        </p>
        <h3 class="mt-2 text-xl font-semibold text-slate-950">系统已经给出下一步判断</h3>
      </div>
      <div class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-800">
        紧接着处理这里
      </div>
    </div>

    <div class="mt-5 grid gap-3 md:grid-cols-3">
      <article class="rounded-[20px] border border-emerald-200 bg-white/90 p-4">
        <p class="text-sm font-semibold text-slate-950">你已经抓到</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">{{ model.mastered }}</p>
      </article>
      <article class="rounded-[20px] border border-amber-200 bg-white/90 p-4">
        <p class="text-sm font-semibold text-slate-950">还差一点</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">{{ model.gap }}</p>
      </article>
      <article class="rounded-[20px] border border-slate-200 bg-white/90 p-4">
        <p class="text-sm font-semibold text-slate-950">下一步建议</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">{{ model.nextStep }}</p>
      </article>
    </div>

    <div v-if="model.actions.length" class="mt-5 flex flex-wrap gap-3">
      <button
        v-for="action in model.actions"
        :key="action.id"
        type="button"
        :data-testid="`feedback-action-${action.id}`"
        class="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-900 transition hover:border-primary/40 hover:bg-primary/5"
        @click="$emit('action', action.id)"
      >
        {{ action.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ExecutionGuideFeedbackModel } from '@/types/executionGuide'

defineProps<{
  model: ExecutionGuideFeedbackModel
}>()

defineEmits<{
  action: [actionId: string]
}>()
</script>
