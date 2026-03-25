<template>
  <details
    v-if="sections.length || transcript.length"
    data-testid="execution-help-collapse"
    class="rounded-[28px] border border-slate-200 bg-white shadow-card"
  >
    <summary class="cursor-pointer px-5 py-4 text-sm font-medium text-slate-900">需要时再看</summary>

    <div class="space-y-4 border-t border-slate-200 px-5 py-5">
      <article
        v-for="section in sections"
        :key="section.id"
        class="rounded-[20px] border border-slate-200 bg-slate-50/70 p-4"
      >
        <h3 class="text-sm font-semibold text-slate-950">{{ section.title }}</h3>
        <p v-if="section.body" class="mt-2 text-sm leading-6 text-slate-700">{{ section.body }}</p>
        <ul
          v-if="section.bullets?.length"
          class="mt-2 list-disc space-y-1.5 pl-5 text-sm leading-6 text-slate-700"
        >
          <li v-for="(item, index) in section.bullets" :key="`${section.id}-${index}`">
            {{ item }}
          </li>
        </ul>
      </article>

      <article
        v-if="transcript.length"
        class="rounded-[20px] border border-slate-200 bg-slate-50/70 p-4"
      >
        <h3 class="text-sm font-semibold text-slate-950">本步记录</h3>
        <div class="mt-3 space-y-3">
          <div
            v-for="(item, index) in transcript"
            :key="index"
            class="rounded-[18px] border px-4 py-3"
            :class="
              item.role === 'ASSISTANT'
                ? 'border-primary/20 bg-primary/5'
                : 'border-slate-200 bg-white'
            "
          >
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
              {{ item.speaker }}
            </p>
            <p class="mt-2 whitespace-pre-line text-sm leading-6 text-slate-700">
              {{ item.content }}
            </p>
          </div>
        </div>
      </article>
    </div>
  </details>
</template>

<script setup lang="ts">
import type { ExecutionGuideHelpSection } from '@/types/executionGuide'

interface TranscriptItem {
  role: 'USER' | 'ASSISTANT'
  speaker: string
  content: string
}

defineProps<{
  sections: ExecutionGuideHelpSection[]
  transcript: TranscriptItem[]
}>()
</script>
