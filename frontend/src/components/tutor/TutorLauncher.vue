<script setup lang="ts">
defineProps<{
  open: boolean
  disabled?: boolean
  subtle?: boolean
  label?: string
}>()

const emit = defineEmits<{
  toggle: []
}>()
</script>

<template>
  <button
    type="button"
    class="tutor-launcher"
    :class="{ open, subtle }"
    :disabled="disabled"
    @click="emit('toggle')"
  >
    <span class="tutor-launcher__label">{{ label || (open ? '收起 Tutor' : '问 Tutor') }}</span>
  </button>
</template>

<style scoped>
.tutor-launcher {
  position: fixed;
  right: clamp(16px, 2vw, 28px);
  bottom: clamp(16px, 2vw, 28px);
  z-index: 48;
  min-height: 52px;
  padding: 0 18px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(107, 159, 255, 0.28);
  background:
    linear-gradient(135deg, rgba(17, 28, 44, 0.96), rgba(11, 18, 28, 0.96)),
    rgba(11, 18, 28, 0.96);
  color: var(--color-text);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
  transition:
    transform var(--duration-normal) var(--ease-smooth),
    border-color var(--duration-fast) var(--ease-smooth),
    background var(--duration-fast) var(--ease-smooth);
}

.tutor-launcher:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: rgba(138, 182, 255, 0.46);
}

.tutor-launcher.open {
  border-color: rgba(138, 182, 255, 0.46);
}

.tutor-launcher.subtle {
  min-height: 48px;
  opacity: 0.88;
}

.tutor-launcher:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tutor-launcher__label {
  font-size: var(--font-size-sm);
  font-weight: 600;
}

@media (max-width: 768px) {
  .tutor-launcher {
    right: 14px;
    bottom: 14px;
  }
}
</style>
