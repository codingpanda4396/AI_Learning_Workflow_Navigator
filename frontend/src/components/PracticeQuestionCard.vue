<script setup lang="ts">
import { computed } from 'vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import { getQuestionMeta } from '@/constants/trainingStage'
import type { PracticeItem, PracticeSubmission } from '@/types'

const props = defineProps<{
  item: PracticeItem
  index: number
  draft: string
  submission: PracticeSubmission | null
  submitting: boolean
}>()

const emit = defineEmits<{
  updateDraft: [value: string]
  submit: []
}>()

const meta = computed(() => getQuestionMeta(props.item, props.index, props.submission))

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
          return `${label}：${value}`
        }
        return label || value
      }
      return ''
    })
    .filter((line) => line.trim().length > 0)
})

const canSubmit = computed(() => props.draft.trim().length > 0 && !props.submitting)
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

    <ul v-if="optionLines.length" class="question-options">
      <li v-for="(line, lineIndex) in optionLines" :key="`${item.itemId}-${lineIndex}`">{{ line }}</li>
    </ul>

    <textarea
      class="answer-input"
      rows="4"
      :value="draft"
      :disabled="submitting"
      placeholder="请输入你的答案"
      @input="emit('updateDraft', ($event.target as HTMLTextAreaElement).value)"
    ></textarea>

    <div class="question-actions">
      <PrimaryButton :disabled="!canSubmit" :loading="submitting" @click="emit('submit')">提交答案</PrimaryButton>
    </div>

    <div v-if="submission" class="result-card">
      <p class="result-title">
        {{ submission.score === null ? '已提交，等待批改' : submission.isCorrect ? '本题回答正确' : '本题还可以再加强' }}
      </p>
      <p v-if="submission.feedback" class="result-copy">{{ submission.feedback }}</p>
      <div v-if="submission.errorTags.length" class="tag-row">
        <span v-for="tag in submission.errorTags" :key="`${submission.submissionId}-${tag}`" class="feedback-tag">{{ tag }}</span>
      </div>
    </div>
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

.question-head,
.question-actions {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: center;
  flex-wrap: wrap;
}

.question-tags,
.tag-row {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.label,
.status-label,
.feedback-tag {
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
.result-copy {
  line-height: 1.75;
  color: var(--color-text);
}

.question-options {
  margin: 0;
  padding-left: 20px;
  color: var(--color-text-secondary);
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

.result-card {
  display: grid;
  gap: 10px;
  padding: var(--space-lg);
  border-radius: var(--radius-md);
  border: 1px solid rgba(93, 212, 166, 0.2);
  background: rgba(14, 38, 33, 0.45);
}

.result-title {
  color: var(--color-success);
  font-weight: 600;
}

.feedback-tag {
  background: rgba(255, 122, 138, 0.14);
  color: #ffc5cd;
}

@media (max-width: 640px) {
  .question-actions :deep(.btn) {
    width: 100%;
  }
}
</style>
