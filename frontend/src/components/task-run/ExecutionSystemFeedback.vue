<template>
  <section
    v-if="model.visible"
    data-testid="execution-system-feedback"
    class="rounded-[22px] border border-emerald-200/90 bg-emerald-50/50 px-4 py-3 shadow-sm"
  >
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0 flex-1">
        <p class="text-xs font-semibold text-emerald-800">{{ model.title }}</p>
        <p class="mt-1 text-sm leading-6 text-slate-800">
          <span class="font-medium text-emerald-900">已做到：</span>
          {{ model.mastered }}
        </p>
        <p class="mt-1 text-sm leading-6 text-slate-700">
          <span class="font-medium text-amber-900">还要补：</span>
          {{ model.gap }}
          <span v-if="model.nextStep" class="text-slate-600">
            · 接下来：{{ model.nextStep }}
          </span>
        </p>
      </div>
      <div v-if="model.actions.length" class="flex shrink-0 flex-wrap gap-2">
        <button
          v-for="action in model.actions"
          :key="action.id"
          type="button"
          :data-testid="`feedback-action-${action.id}`"
          class="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-900 transition hover:border-primary/40 hover:bg-primary/5"
          @click="$emit('action', action.id)"
        >
          {{ action.label }}
        </button>
      </div>
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
