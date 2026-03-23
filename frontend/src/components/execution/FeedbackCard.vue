<template>
  <FormCard>
    <div class="flex items-center gap-2">
      <span
        class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold"
        :class="
          feedback.correct
            ? 'bg-emerald-100 text-emerald-900'
            : 'bg-amber-100 text-amber-950'
        "
      >
        {{ feedback.correct ? '🟢 方向对' : '⚠️ 再调整一下' }}
      </span>
    </div>

    <template v-if="hasLayered">
      <p v-if="feedback.praise" class="mt-3 text-sm font-medium text-text-primary">
        {{ feedback.praise }}
      </p>
      <p v-if="feedback.gap" class="mt-2 text-sm text-amber-950">
        {{ feedback.gap }}
      </p>
      <p v-if="feedback.nextHint" class="mt-2 text-sm text-text-secondary">
        👉 {{ feedback.nextHint }}
      </p>
    </template>
    <template v-else>
      <p class="mt-3 text-sm font-medium text-text-primary">
        {{ feedback.comment }}
      </p>
      <p class="mt-2 text-sm text-text-secondary">{{ feedback.suggestion }}</p>
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
