<template>
  <section
    v-if="blocks.length"
    data-testid="scaffold-prompt-panel"
    class="rounded-2xl border-2 border-accent/30 bg-gradient-to-b from-accent/[0.07] to-white p-5 shadow-md ring-1 ring-accent/15 md:p-6"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-accent">脚手架 · 当前怎么写</p>
    <div class="mt-4 space-y-4">
      <article
        v-for="block in blocks"
        :key="block.id"
        class="rounded-xl border border-slate-200/90 bg-white/90 px-4 py-3 shadow-sm"
        :class="block.kind === 'readonly' ? 'bg-slate-50/90' : ''"
      >
        <p class="text-xs font-semibold text-slate-700">{{ block.title }}</p>
        <p v-if="block.prompt" class="mt-2 text-sm leading-relaxed text-slate-900">{{ block.prompt }}</p>
        <p v-if="block.constraint" class="mt-2 text-xs leading-relaxed text-accent-hover/95">
          边界：{{ block.constraint }}
        </p>
      </article>
    </div>
    <p v-if="submitConstraint" class="mt-3 text-xs text-slate-600">{{ submitConstraint }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StageScaffoldWorkbenchPayload } from '@/types/scaffoldEngine'

const props = defineProps<{
  workbench: StageScaffoldWorkbenchPayload | null | undefined
}>()

const blocks = computed(() => props.workbench?.promptScaffold?.blocks ?? [])

const submitConstraint = computed(() => props.workbench?.submitConstraint?.trim() || '')
</script>
