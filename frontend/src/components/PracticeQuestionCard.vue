<script setup lang="ts">
import { computed } from 'vue'
import { getQuestionMeta } from '@/constants/trainingStage'
import type { PracticeItem } from '@/types'

const props = defineProps<{
  item: PracticeItem
  index: number
  draft: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  updateDraft: [value: string]
}>()

const meta = computed(() => getQuestionMeta(props.item, props.index))

const optionLines = computed(() => {
  if (!Array.isArray(props.item.options)) {
    return []
  }
  return props.item.options
    .map((option) => {
      if (typeof option === 'string') {
        return option
      }
      if (typeof option === 'number' || typeof option === 'boolean') {
        return String(option)
      }
      if (option && typeof option === 'object') {
        const record = option as Record<string, unknown>
        const label = typeof record.label === 'string' ? record.label : ''
        const value = typeof record.value === 'string' ? record.value : ''
        if (label && value) {
          return `${label}: ${value}`
        }
        return label || value
      }
      return ''
    })
    .filter((line) => line.trim().length > 0)
})
</script>

<template>
  <article class="question-card">
    <div class="question-head">
      <div class="question-tags">
        <span class="label order">{{ meta.orderLabel }}</span>
        <span class="label type">{{ meta.typeLabel }}</span>
        <span class="label difficulty">{{ meta.difficultyLabel }}</span>
      </div>
      <span class="status-label">{{ meta.statusLabel }}</span>
    </div>

    <p class="question-stem">{{ item.stem }}</p>
    <p v-if="item.evaluationFocus" class="evaluation-focus">考察点：{{ item.evaluationFocus }}</p>

    <ul v-if="optionLines.length" class="question-options">
      <li v-for="(line, lineIndex) in optionLines" :key="`${item.questionId}-${lineIndex}`">{{ line }}</li>
    </ul>

    <textarea
      class="answer-input"
      rows="4"
      :value="draft"
      :disabled="disabled"
      placeholder="填写你的答案或思路"
      @input="emit('updateDraft', ($event.target as HTMLTextAreaElement).value)"
    ></textarea>
  </article>
</template>

<style scoped>
.question-card {
  display: grid;
  gap: var(--space-md);
  padding: var(--space-xl);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.46);
  background: rgba(14, 20, 31, 0.9);
}

.question-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: center;
  flex-wrap: wrap;
}

.question-tags {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.label,
.status-label {
  padding: 6px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
}

.label.order {
  background: rgba(107, 159, 255, 0.14);
  color: var(--color-primary-hover);
}

.label.type {
  background: rgba(126, 223, 197, 0.14);
  color: var(--color-accent-mint);
}

.label.difficulty {
  background: rgba(255, 184, 140, 0.14);
  color: var(--color-accent-peach);
}

.status-label {
  border: 1px solid rgba(61, 80, 104, 0.48);
  color: var(--color-text-secondary);
}

.question-stem,
.evaluation-focus {
  margin: 0;
  line-height: 1.75;
}

.question-stem {
  color: var(--color-text);
}

.evaluation-focus,
.question-options {
  color: var(--color-text-secondary);
}

.question-options {
  margin: 0;
  padding-left: 20px;
}

.answer-input {
  width: 100%;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(61, 80, 104, 0.5);
  background: rgba(8, 12, 19, 0.82);
  color: var(--color-text);
  resize: vertical;
}
</style>
