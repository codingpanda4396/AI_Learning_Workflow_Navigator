<template>
  <div class="space-y-3">
    <p class="text-xs font-semibold uppercase tracking-wide text-accent">{{ eyebrow }}</p>
    <h2 class="text-lg font-semibold tracking-tight text-slate-950">{{ sectionTitle }}</h2>
    <p class="text-sm leading-relaxed text-slate-600">{{ sectionHint }}</p>
    <ul class="space-y-3">
      <li
        v-for="card in cards"
        :key="card.promptKey"
        class="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm transition hover:border-accent/30 hover:shadow-md"
      >
        <h3 class="text-sm font-semibold text-slate-900">{{ card.title }}</h3>
        <p class="mt-1 text-xs leading-relaxed text-slate-600">{{ card.blurb }}</p>
        <button
          type="button"
          class="mt-3 w-full rounded-xl bg-accent px-3 py-2 text-center text-sm font-semibold text-white transition hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="disabled"
          @click="$emit('learn', card.promptKey)"
        >
          {{ learnLabel }}
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { StructureScaffoldCardVm } from '@/constants/dfsBfsStructureWorkbenchCopy'

defineProps<{
  eyebrow: string
  sectionTitle: string
  sectionHint: string
  cards: StructureScaffoldCardVm[]
  learnLabel: string
  disabled?: boolean
}>()

defineEmits<{
  learn: [promptKey: string]
}>()
</script>
