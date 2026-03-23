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
          你可以把二叉树理解成一个「家族结构」：
        </p>
        <ul class="mt-2 list-inside list-disc space-y-1.5 text-sm leading-relaxed text-text-primary">
          <li>最上面是祖先（根节点）</li>
          <li>每个人最多有两个孩子</li>
        </ul>
        <p class="mt-3 text-sm leading-relaxed text-text-secondary">
          👉 这样你就能理解「最多两个分支」的特点
        </p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  /** 来自 R0003 /api/ai-tutor/explain；为空时用内置二叉树兜底示例 */
  explanation?: string | null
}>()

const trimmedExplanation = computed(() => props.explanation?.trim() ?? '')

const useGenerated = computed(() => trimmedExplanation.value.length > 0)
</script>
