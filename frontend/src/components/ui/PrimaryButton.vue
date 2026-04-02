<template>
  <button
    type="button"
    class="inline-flex h-11 min-w-[7rem] items-center justify-center rounded-md bg-accent px-5 text-[15px] font-semibold text-white shadow-sm transition hover:bg-accent-hover hover:shadow-card-hover disabled:cursor-not-allowed disabled:opacity-50"
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
