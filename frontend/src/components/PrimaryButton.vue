<script setup lang="ts">
interface PrimaryButtonProps {
  disabled?: boolean
  loading?: boolean
  type?: 'button' | 'submit'
}

withDefaults(defineProps<PrimaryButtonProps>(), {
  disabled: false,
  loading: false,
  type: 'button',
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function handleClick(event: MouseEvent) {
  emit('click', event)
}
</script>

<template>
  <button class="primary-button" :type="type" :disabled="disabled || loading" @click="handleClick">
    <span v-if="loading" class="spinner" aria-hidden="true"></span>
    <span><slot /></span>
  </button>
</template>

<style scoped>
.primary-button {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-hover));
  color: #fff;
  font-size: var(--font-size-md);
  font-weight: 600;
  letter-spacing: 0.01em;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  transition: transform 120ms ease, filter 160ms ease, box-shadow 160ms ease;
  box-shadow: var(--shadow-sm);
}

.primary-button:hover:not(:disabled) {
  filter: brightness(1.05);
  transform: translateY(-1px);
}

.primary-button:active:not(:disabled) {
  transform: translateY(0);
  background: var(--color-primary-active);
}

.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
