<template>
  <div class="space-y-2">
    <button
      v-for="step in steps"
      :key="step.id"
      type="button"
      class="flex w-full items-start gap-3 rounded-input border px-4 py-3 text-left text-sm transition-colors"
      :class="
        step.id === currentStepId
          ? 'border-primary bg-primary/5 font-medium text-text-primary ring-1 ring-primary/20'
          : 'border-border text-text-secondary hover:border-primary/40 hover:bg-gray-50/80'
      "
      @click="$emit('select', step.id)"
    >
      <span
        class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold"
        :class="
          step.id === currentStepId
            ? 'bg-primary text-white'
            : 'bg-gray-100 text-text-secondary'
        "
      >
        {{ step.index }}
      </span>
      <span class="min-w-0 flex-1">
        <span class="block text-text-primary">{{ step.label }}</span>
        <span
          v-if="step.hint"
          class="mt-0.5 block text-xs font-normal text-text-secondary"
        >
          {{ step.hint }}
        </span>
      </span>
    </button>
  </div>
</template>

<script setup lang="ts">
export interface GuidedStepItem {
  id: string
  index: number
  label: string
  hint?: string
}

defineProps<{
  steps: GuidedStepItem[]
  currentStepId: string
}>()

defineEmits<{
  select: [id: string]
}>()
</script>
