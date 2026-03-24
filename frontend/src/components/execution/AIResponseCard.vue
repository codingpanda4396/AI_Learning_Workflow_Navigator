<template>
  <div
    class="flex gap-3 rounded-card border border-indigo-100 bg-indigo-50/60 p-4 shadow-sm md:gap-4 md:p-5"
  >
    <div
      class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-2xl md:h-12 md:w-12"
      aria-hidden="true"
    >
      👨‍🏫
    </div>
    <div class="min-w-0 flex-1">
      <p class="text-xs font-semibold text-indigo-900">👨‍🏫 AI导师 · 导师讲解</p>
      <template v-if="useGenerated">
        <p class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-text-primary">
          {{ trimmedExplanation }}
        </p>
      </template>
      <template v-else>
        <p class="mt-2 text-sm leading-relaxed text-text-primary">
          先把「{{ displayTopic }}」想成一块有层次的知识：上层是总览，下层是细节。
        </p>
        <ul class="mt-2 list-inside list-disc space-y-1.5 text-sm leading-relaxed text-text-primary">
          <li>用最直白的话说出：它解决什么问题？</li>
          <li>和相邻概念相比，它的边界在哪里？</li>
        </ul>
        <p class="mt-3 text-sm leading-relaxed text-text-secondary">
          👉 先建立画面，再补术语，会比死记定义更稳
        </p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** 来自 R0003 /api/ai-tutor/explain；为空时用通用兜底示例 */
  explanation?: string | null
  /** 当前步知识点标签，用于兜底文案 */
  topicLabel?: string | null
}>()

const trimmedExplanation = computed(() => props.explanation?.trim() ?? '')

const useGenerated = computed(() => trimmedExplanation.value.length > 0)

const displayTopic = computed(() => props.topicLabel?.trim() || '当前主题')
</script>
