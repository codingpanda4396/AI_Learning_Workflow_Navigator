<template>
  <aside
    class="sticky top-20 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm md:p-5"
    data-testid="structure-answer-hints"
  >
    <h2 class="text-sm font-semibold text-slate-950">{{ copy.title }}</h2>

    <section class="mt-4">
      <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.howToTitle }}</h3>
      <ul class="mt-2 list-inside list-disc space-y-1.5 text-sm leading-relaxed text-slate-700">
        <li v-for="(line, i) in copy.howToItems" :key="'h-' + i">{{ line }}</li>
      </ul>
    </section>

    <section class="mt-5">
      <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.avoidTitle }}</h3>
      <ul class="mt-2 list-inside list-disc space-y-1.5 text-sm leading-relaxed text-slate-700">
        <li v-for="(line, i) in copy.avoidItems" :key="'a-' + i">{{ line }}</li>
      </ul>
    </section>

    <section class="mt-5 border-t border-slate-100 pt-4">
      <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.passTitle }}</h3>
      <ul class="mt-2 list-inside list-disc space-y-1.5 text-sm leading-relaxed text-slate-700">
        <li v-for="(line, i) in copy.passItems" :key="'p-' + i">{{ line }}</li>
      </ul>
    </section>
  </aside>
</template>

<script setup lang="ts">
import type { StructureAnswerHintsCopy } from '@/constants/dfsBfsStructureWorkbenchCopy'

defineProps<{
  copy: StructureAnswerHintsCopy
}>()
</script>
