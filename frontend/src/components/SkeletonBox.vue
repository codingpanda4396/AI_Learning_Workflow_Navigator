<script setup lang="ts">
defineProps<{
  width?: string
  height?: string
  variant?: 'text' | 'rect' | 'circle'
}>()
</script>

<template>
  <div
    class="skeleton"
    :class="`variant-${variant || 'rect'}`"
    :style="{
      width: width || '100%',
      height: height || '1rem'
    }"
  ></div>
</template>

<style scoped>
.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-border) 25%,
    var(--color-bg-elevated) 50%,
    var(--color-border) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.variant-circle {
  border-radius: 50%;
}

.variant-text {
  border-radius: 4px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
