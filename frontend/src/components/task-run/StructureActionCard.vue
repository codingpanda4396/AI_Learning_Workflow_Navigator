<template>
  <section
    data-testid="structure-action-card"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ring-2 ring-slate-300/80 md:p-6"
  >
    <p class="text-lg font-semibold leading-snug text-slate-950 md:text-xl">
      {{ taskTitle }}
    </p>

    <div v-if="objectiveLine" class="mt-4">
      <p class="text-xs font-semibold text-slate-500">{{ TASKRUN_COPY.structureActionRequirement }}</p>
      <p class="mt-1.5 text-sm leading-6 text-slate-700">{{ objectiveLine }}</p>
    </div>

    <div v-if="outputRequirements.length" class="mt-5">
      <p class="text-xs font-semibold text-slate-500">{{ TASKRUN_COPY.structureActionOutputs }}</p>
      <ol class="mt-2 list-decimal space-y-1.5 pl-4 text-sm leading-6 text-slate-800">
        <li v-for="(line, i) in outputRequirements" :key="`req-${i}`">{{ line }}</li>
      </ol>
    </div>

    <div v-if="completionLines.length" class="mt-5 border-t border-slate-100 pt-4">
      <p class="text-xs font-semibold text-slate-500">{{ TASKRUN_COPY.structureActionCriteria }}</p>
      <ul class="mt-2 space-y-1 text-sm leading-6 text-slate-700">
        <li v-for="(line, i) in completionLines" :key="`done-${i}`" class="flex gap-2">
          <span class="shrink-0 text-slate-400">·</span>
          <span>{{ line }}</span>
        </li>
      </ul>
    </div>

    <div v-if="avoidLines.length" class="mt-4 border-t border-slate-100 pt-4">
      <p class="text-xs font-semibold text-slate-500">{{ TASKRUN_COPY.structureActionAvoid }}</p>
      <ul class="mt-2 space-y-1 text-sm leading-6 text-slate-600">
        <li v-for="(line, i) in avoidLines" :key="`avoid-${i}`" class="flex gap-2">
          <span class="shrink-0 text-slate-400">·</span>
          <span>{{ line }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { TASKRUN_COPY } from '@/constants/uiCopy'

defineProps<{
  taskTitle: string
  objectiveLine: string
  outputRequirements: string[]
  completionLines: string[]
  avoidLines: string[]
}>()
</script>
