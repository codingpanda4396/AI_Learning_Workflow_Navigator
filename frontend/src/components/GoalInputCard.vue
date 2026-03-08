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
    <label for="goal-input" class="label">1. 学习目标</label>
    <textarea
      id="goal-input"
      class="input"
      :value="props.modelValue"
      placeholder="例如：理解 TCP 可靠传输机制，并能分析握手/挥手相关题目。"
      rows="4"
      @input="onInput"
    />
    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="hint" class="hint">{{ hint }}</p>
  </section>
</template>

<style scoped>
.goal-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.label {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text);
}

.input {
  width: 100%;
  resize: vertical;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #0a1225;
  color: var(--color-text);
  padding: var(--space-md);
  line-height: 1.5;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

.hint {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}
</style>
