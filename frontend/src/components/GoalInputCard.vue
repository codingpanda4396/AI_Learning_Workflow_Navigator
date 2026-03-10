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
  <section class="goal-card animate-fade-in-up">
    <label for="goal-input" class="goal-label">学习目标</label>
    <textarea
      id="goal-input"
      class="goal-textarea"
      :value="props.modelValue"
      placeholder="例如：在一周内掌握 TCP 可靠传输机制，并能完成 3 道相关训练题。"
      rows="4"
      @input="onInput"
    />
    <p v-if="error" class="goal-error">{{ error }}</p>
    <p v-else-if="hint" class="goal-hint">{{ hint }}</p>
  </section>
</template>

<style scoped>
.goal-card {
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.goal-card:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.goal-label {
  display: block;
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--space-md);
}

.goal-textarea {
  width: 100%;
  min-height: 120px;
  padding: var(--space-lg);
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text);
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  resize: vertical;
  transition: all var(--duration-normal) var(--ease-smooth);
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
  margin-top: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.goal-error {
  margin-top: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--color-error);
}
</style>
