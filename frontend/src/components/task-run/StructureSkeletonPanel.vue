<template>
  <div
    class="min-h-[280px] rounded-2xl border border-slate-200/90 bg-white p-5 shadow-[0_8px_30px_-12px_rgba(15,23,42,0.1)] md:p-6"
    data-testid="structure-skeleton-panel"
  >
    <div v-if="loading" class="flex flex-col items-center justify-center gap-3 py-16 text-sm text-slate-600">
      <span
        class="inline-block h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent"
        aria-hidden="true"
      />
      <span>{{ loadingMessage }}</span>
    </div>
    <div v-else-if="error" class="rounded-xl border border-rose-200 bg-rose-50/80 px-4 py-3 text-sm text-rose-900">
      {{ error }}
    </div>
    <div v-else-if="!skeleton" class="py-12 text-center text-sm leading-relaxed text-slate-600">
      {{ emptyMessage }}
    </div>
    <div v-else class="space-y-5">
      <section>
        <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ labels.module }}</h3>
        <p class="mt-1.5 text-sm font-medium leading-relaxed text-slate-900">{{ skeleton.module }}</p>
      </section>
      <section v-if="skeleton.prerequisites?.length">
        <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ labels.prerequisites }}</h3>
        <ul class="mt-2 list-inside list-disc space-y-1 text-sm text-slate-800">
          <li v-for="(line, i) in skeleton.prerequisites" :key="'p-' + i">{{ line }}</li>
        </ul>
      </section>
      <section v-if="skeleton.connections?.length">
        <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ labels.connections }}</h3>
        <ul class="mt-2 list-inside list-disc space-y-1 text-sm text-slate-800">
          <li v-for="(line, i) in skeleton.connections" :key="'c-' + i">{{ line }}</li>
        </ul>
      </section>
      <section v-if="skeleton.deferTopics?.length">
        <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ labels.defer }}</h3>
        <ul class="mt-2 list-inside list-disc space-y-1 text-sm text-slate-800">
          <li v-for="(line, i) in skeleton.deferTopics" :key="'d-' + i">{{ line }}</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { StructureSkeletonBlock } from '@/types/scaffoldEngine'

defineProps<{
  loading: boolean
  error: string | null
  skeleton: StructureSkeletonBlock | null
  emptyMessage: string
  loadingMessage: string
  labels: { module: string; prerequisites: string; connections: string; defer: string }
}>()
</script>
