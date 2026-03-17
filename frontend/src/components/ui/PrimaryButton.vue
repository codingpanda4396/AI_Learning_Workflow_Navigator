<template>
  <button
    type="button"
    class="inline-flex items-center justify-center rounded-input bg-primary px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
    :disabled="loading || disabled"
    @click="$emit('click')"
  >
    <span v-if="loading" class="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
    <slot />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  loading?: boolean
  disabled?: boolean
}>()
defineEmits<{
  click: []
}>()
</script>
