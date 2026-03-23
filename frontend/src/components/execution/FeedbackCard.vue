<template>
  <FormCard>
    <p class="text-xs font-medium text-text-secondary">👨‍🏫 AI导师反馈</p>

    <div class="mt-2 flex items-center gap-2">
      <span
        class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold"
        :class="
          feedback.correct
            ? 'bg-emerald-100 text-emerald-900'
            : 'bg-amber-100 text-amber-950'
        "
      >
        {{ feedback.correct ? '整体方向不错' : '还可以再收紧一点' }}
      </span>
    </div>

    <template v-if="hasLayered">
      <div v-if="feedback.praise" class="mt-3 space-y-1">
        <p class="text-sm font-medium text-text-primary">🟢 你已经抓住关键点 👍</p>
        <p class="text-sm leading-relaxed text-text-primary">{{ feedback.praise }}</p>
      </div>
      <p
        v-else-if="feedback.correct"
        class="mt-3 text-sm font-medium text-text-primary"
      >
        🟢 你已经抓住关键点 👍
      </p>
      <div v-if="feedback.gap" class="mt-3 space-y-1">
        <p class="text-sm font-medium text-amber-950">⚠️ 但这里可以更清楚：</p>
        <p class="text-sm leading-relaxed text-amber-950">{{ feedback.gap }}</p>
      </div>
      <div v-if="feedback.nextHint" class="mt-3 space-y-1">
        <p class="text-sm font-medium text-text-secondary">👉 建议这样理解：</p>
        <p class="text-sm leading-relaxed text-text-secondary">{{ feedback.nextHint }}</p>
      </div>
    </template>
    <template v-else>
      <div class="mt-3 space-y-1">
        <p class="text-sm font-medium text-text-primary">我帮你看过了，先这么说：</p>
        <p class="text-sm leading-relaxed text-text-primary">{{ feedback.comment }}</p>
      </div>
      <div v-if="feedback.suggestion" class="mt-3 space-y-1">
        <p class="text-sm font-medium text-text-secondary">👉 建议这样理解：</p>
        <p class="text-sm leading-relaxed text-text-secondary">{{ feedback.suggestion }}</p>
      </div>
    </template>
  </FormCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FormCard from '@/components/ui/FormCard.vue'
import type { ExecutionStepFeedback } from '@/types/execution'

const props = defineProps<{
  feedback: ExecutionStepFeedback
}>()

const hasLayered = computed(
  () =>
    !!(props.feedback.praise || props.feedback.gap || props.feedback.nextHint)
)
</script>
