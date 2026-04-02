<template>
  <span
    class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
    :class="variantClass"
  >
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  variant?: 'default' | 'success' | 'warning' | 'error'
  label?: string
}>()

const variantClass = computed(() => {
  const m: Record<string, string> = {
    default: 'bg-gray-100 text-text-secondary',
    success: 'bg-green-100 text-green-800',
    warning: 'bg-accent-muted text-accent-hover ring-1 ring-accent/20',
    error: 'bg-red-100 text-red-800',
  }
  return m[props.variant ?? 'default']
})
</script>
