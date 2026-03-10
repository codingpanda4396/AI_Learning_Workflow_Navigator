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
  <button class="btn btn-primary" :type="type" :disabled="disabled || loading" @click="handleClick">
    <span v-if="loading" class="loading-spinner" aria-hidden="true"></span>
    <span><slot /></span>
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: 14px 28px;
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  font-weight: 600;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-smooth);
  min-height: 52px;
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  color: #fff;
  box-shadow:
    0 4px 16px rgba(107, 159, 255, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 8px 24px rgba(107, 159, 255, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow:
    0 2px 8px rgba(107, 159, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
