<script setup lang="ts">
interface GoalInputCardProps {
  modelValue: string
  hint?: string
  error?: string
}

const props = defineProps<GoalInputCardProps>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function onInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <section class="goal-card">
    <label for="goal-input" class="goal-label">学习目标</label>
    <textarea
      id="goal-input"
      class="goal-textarea"
      :value="props.modelValue"
      placeholder="例如：这周学会 TCP 可靠传输，并能独立完成几道相关练习。"
      rows="4"
      @input="onInput"
    />
    <p v-if="error" class="goal-error">{{ error }}</p>
    <p v-else-if="hint" class="goal-hint">{{ hint }}</p>
  </section>
</template>

<style scoped>
.goal-card {
  background: rgba(10, 16, 26, 0.82);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  display: grid;
  gap: 14px;
}

.goal-label {
  display: block;
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
}

.goal-textarea {
  width: 100%;
  min-height: 120px;
  padding: var(--space-lg);
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text);
  background: rgba(6, 10, 18, 0.92);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  resize: vertical;
}

.goal-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-alpha);
  outline: none;
}

.goal-textarea::placeholder {
  color: var(--color-text-muted);
}

.goal-hint {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.goal-error {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-error);
}
</style>
