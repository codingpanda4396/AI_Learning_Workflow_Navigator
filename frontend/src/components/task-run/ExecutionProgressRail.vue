<template>
  <aside
    data-testid="execution-progress-rail"
    class="space-y-5 rounded-[28px] border border-slate-200 bg-white p-5 shadow-card"
  >
    <section>
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {{ model.stageSectionTitle }}
      </p>
      <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">
        {{ model.stageLabel }}
      </p>
    </section>

    <section class="rounded-[20px] border border-slate-100 bg-slate-50/80 p-4">
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {{ model.deliverableSectionTitle }}
      </p>
      <p class="mt-2 text-sm leading-6 text-slate-800">
        {{ model.deliverableLine }}
      </p>
    </section>

    <section class="rounded-[20px] border border-accent/20 bg-accent-muted/45 p-4">
      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-accent-hover/95">
        {{ model.stuckSectionTitle }}
      </p>
      <ul class="mt-3 space-y-2">
        <li v-for="(action, i) in model.stuckActions" :key="`stuck-${i}`">
          <button
            type="button"
            class="w-full rounded-[14px] border border-accent/25 bg-white px-3 py-2 text-left text-sm text-slate-800 transition hover:border-accent/40 hover:bg-accent-muted/45"
            :data-testid="`stuck-action-${i}`"
            @click="$emit('stuck-action', action)"
          >
            {{ action }}
          </button>
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

defineEmits<{
  'stuck-action': [action: string]
}>()
</script>
