<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">反思题</p>
    <p class="mt-1 text-sm font-medium text-slate-900">{{ question.prompt }}</p>
    <div class="mt-3 space-y-2">
      <button
        v-for="o in question.options"
        :key="o.id"
        type="button"
        class="w-full rounded-xl border px-3 py-2 text-left text-sm font-medium transition"
        :class="selected === o.id ? 'border-primary bg-primary/[0.08]' : 'border-slate-200 bg-slate-50'"
        @click="$emit('select', o.id)"
      >
        {{ o.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ReflectionQuestion } from '@/types/phaseWorkbench'

defineProps<{
  question: ReflectionQuestion
  selected: string | null
}>()

defineEmits<{
  select: [optionId: string]
}>()
</script>
