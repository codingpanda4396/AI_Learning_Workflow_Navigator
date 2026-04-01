<template>
  <section class="space-y-3">
    <article
      v-if="blocks.length === 0"
      class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm"
    >
      <p class="text-sm text-slate-600">暂时没有补充说明，可先完成当前主交互。</p>
    </article>
    <article
      v-for="b in blocks"
      :key="b.id"
      class="rounded-2xl border px-4 py-3 shadow-sm"
      :class="toneClass(b.tone)"
    >
      <p class="text-xs font-semibold text-slate-600">{{ b.title }}</p>
      <p class="mt-1 text-sm leading-relaxed text-slate-900">{{ b.body }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { ExplanationBlock } from '@/types/phaseWorkbench'

defineProps<{
  blocks: ExplanationBlock[]
}>()

function toneClass(tone: ExplanationBlock['tone']) {
  if (tone === 'scaffold') return 'border-primary/30 bg-primary/[0.04]'
  if (tone === 'feedback') return 'border-emerald-200 bg-emerald-50/60'
  return 'border-slate-200 bg-white'
}
</script>
