<script setup lang="ts">
defineProps<{
  size?: 'sm' | 'md' | 'lg'
  text?: string
}>()
</script>

<template>
  <div class="loading-container" :class="`size-${size || 'md'}`">
    <div class="spinner">
      <div class="spinner-ring"></div>
      <div class="spinner-ring"></div>
      <div class="spinner-ring"></div>
    </div>
    <p v-if="text" class="loading-text">{{ text }}</p>
  </div>
</template>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
}

.spinner {
  position: relative;
}

.size-sm .spinner {
  width: 20px;
  height: 20px;
}

.size-md .spinner {
  width: 40px;
  height: 40px;
}

.size-lg .spinner {
  width: 60px;
  height: 60px;
}

.spinner-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: var(--color-primary);
  animation: spin 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-ring:nth-child(1) {
  animation-delay: -0.45s;
}

.spinner-ring:nth-child(2) {
  animation-delay: -0.3s;
  opacity: 0.8;
}

.spinner-ring:nth-child(3) {
  animation-delay: -0.15s;
  opacity: 0.6;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>
