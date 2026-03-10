<script setup lang="ts">
defineProps<{
  message: string
  title?: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="error-container">
    <div class="error-icon">!</div>
    <h3 class="error-title">{{ title || '出错了' }}</h3>
    <p class="error-message">{{ message }}</p>
    <button class="error-retry" @click="emit('retry')">
      重试
    </button>
  </div>
</template>

<style scoped>
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  padding: var(--space-xxl);
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid rgba(255, 122, 138, 0.3);
  border-radius: var(--radius-xl);
  text-align: center;
}

.error-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-error);
  background: rgba(255, 122, 138, 0.15);
  border-radius: 50%;
}

.error-title {
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.error-message {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  margin: 0;
  max-width: 300px;
}

.error-retry {
  padding: 10px 20px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.error-retry:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}
</style>
