<template>
  <aside
    data-testid="execution-progress-rail"
    class="space-y-5 rounded-[28px] border border-slate-200 bg-white p-5 shadow-card"
  >
    <section>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {{ model.progressSectionTitle }}
      </p>
      <ul class="mt-3 space-y-2 text-sm leading-6 text-slate-700">
        <li v-for="(line, i) in model.progressLines" :key="`p-${i}`">
          {{ line }}
        </li>
      </ul>
    </section>

    <section class="rounded-[20px] border border-slate-100 bg-slate-50/80 p-4">
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {{ model.criteriaSectionTitle }}
      </p>
      <ul class="mt-3 list-disc space-y-1.5 pl-4 text-sm leading-6 text-slate-700">
        <li v-for="(line, i) in model.completionLines" :key="`c-${i}`">
          {{ line }}
        </li>
      </ul>
    </section>

    <section class="rounded-[20px] border border-sky-100 bg-sky-50/50 p-4">
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-sky-800">
        {{ model.nextSectionTitle }}
      </p>
      <p class="mt-2 text-sm leading-6 text-slate-700">
        {{ model.nextPreview }}
      </p>
    </section>

    <section v-if="model.knowledgeOutline?.length" class="border-t border-slate-100 pt-4">
      <p class="text-xs font-medium text-slate-500">目录</p>
      <ul class="mt-2 space-y-1 text-xs leading-5 text-slate-600">
        <li v-for="p in model.knowledgeOutline" :key="p.id">
          <span class="text-slate-400">{{ p.index }}.</span>
          {{ p.title }}
        </li>
      </ul>
    </section>
  </aside>
</template>

<script setup lang="ts">
import type { ExecutionGuideProgressRailModel } from '@/types/executionGuide'

defineProps<{
  model: ExecutionGuideProgressRailModel
}>()
</script>
