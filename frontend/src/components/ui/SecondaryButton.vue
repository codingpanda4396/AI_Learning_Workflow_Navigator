<template>
  <button
    type="button"
    class="inline-flex items-center justify-center rounded-input border border-border bg-white px-5 py-2.5 text-sm font-medium text-text-primary transition-colors hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  disabled?: boolean
}>()
defineEmits<{
  click: []
}>()
</script>
