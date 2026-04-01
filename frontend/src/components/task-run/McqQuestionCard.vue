<template>
  <div class="space-y-3">
    <p class="text-base font-semibold leading-relaxed text-slate-800">{{ question.prompt }}</p>

    <div class="space-y-2.5">
      <button
        v-for="opt in question.options"
        :key="opt.id"
        type="button"
        class="group flex w-full items-start gap-3 rounded-xl border-2 px-5 py-4 text-left transition-all duration-200"
        :class="optionClass(opt.id)"
        :disabled="locked"
        @click="handlePick(opt.id)"
      >
        <span
          class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 text-xs font-bold transition-colors"
          :class="badgeClass(opt.id)"
        >
          {{ opt.id }}
        </span>
        <span class="text-sm leading-relaxed">{{ opt.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { McqQuestion } from '@/types/executionWorkbench'

const props = defineProps<{
  question: McqQuestion
  selectedId: string | null
  locked: boolean
}>()

const emit = defineEmits<{
  pick: [optionId: string]
}>()

function handlePick(optionId: string) {
  if (props.locked) return
  emit('pick', optionId)
}

function optionClass(optionId: string): string {
  if (!props.selectedId) {
    return 'border-slate-200 bg-white hover:border-primary/40 hover:bg-primary/5 cursor-pointer'
  }
  if (optionId === props.selectedId && optionId === props.question.correctId) {
    return 'border-emerald-300 bg-emerald-50/60 cursor-default'
  }
  if (optionId === props.selectedId && optionId !== props.question.correctId) {
    return 'border-amber-300 bg-amber-50/50 cursor-default'
  }
  if (optionId === props.question.correctId && props.locked) {
    return 'border-emerald-200 bg-emerald-50/30 cursor-default opacity-70'
  }
  return 'border-slate-100 bg-slate-50/50 cursor-default opacity-60'
}

function badgeClass(optionId: string): string {
  if (!props.selectedId) {
    return 'border-slate-300 text-slate-500 group-hover:border-primary/60 group-hover:text-primary'
  }
  if (optionId === props.selectedId && optionId === props.question.correctId) {
    return 'border-emerald-400 bg-emerald-100 text-emerald-700'
  }
  if (optionId === props.selectedId && optionId !== props.question.correctId) {
    return 'border-amber-400 bg-amber-100 text-amber-700'
  }
  return 'border-slate-200 text-slate-400'
}
</script>
