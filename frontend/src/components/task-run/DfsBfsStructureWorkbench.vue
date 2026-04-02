<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">结构辨析题</p>
    <p class="mt-1 text-base font-semibold text-slate-900">{{ question.prompt }}</p>
    <div class="mt-3 space-y-2">
      <button
        v-for="option in question.options"
        :key="option.id"
        type="button"
        class="w-full rounded-xl border px-3 py-2 text-left text-sm font-medium transition"
        :class="buttonClass(option.id)"
        :disabled="locked || busy"
        @click="$emit('pick', option.id)"
      >
        {{ option.id }}. {{ option.label }}
      </button>
    </div>
    <p
      v-if="selectedOptionId"
      class="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-700"
    >
      已锁定你的选择：{{ selectedOptionId }}
    </p>
  </section>
</template>

<script setup lang="ts">
import type { StructureQuestion } from '@/types/phaseWorkbench'

const props = withDefaults(
  defineProps<{
    question: StructureQuestion
    selectedOptionId: string | null
    locked: boolean
    busy?: boolean
  }>(),
  {
    busy: false,
  }
)

defineEmits<{
  pick: [optionId: string]
}>()

function buttonClass(id: string) {
  if (props.selectedOptionId === id) return 'border-accent bg-accent-muted/80 text-slate-900'
  return 'border-slate-200 bg-slate-50 text-slate-800 hover:border-slate-300'
}
</script>
