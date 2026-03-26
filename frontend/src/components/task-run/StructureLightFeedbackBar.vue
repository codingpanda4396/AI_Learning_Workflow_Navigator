<template>
  <div class="flex flex-wrap gap-2 pt-1">
    <button
      type="button"
      class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
      :disabled="disabled"
      @click="$emit('got-next')"
    >
      {{ labels.gotNext }}
    </button>
    <button
      type="button"
      class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
      :disabled="disabled || !hasSkeleton"
      @click="$emit('clarify')"
    >
      {{ labels.clarify }}
    </button>
    <button
      type="button"
      class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
      :disabled="disabled || !hasSkeleton"
      @click="$emit('adjacent')"
    >
      {{ labels.adjacent }}
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  disabled?: boolean
  hasSkeleton: boolean
  labels: { gotNext: string; clarify: string; adjacent: string }
}>()

defineEmits<{
  'got-next': []
  clarify: []
  adjacent: []
}>()
</script>
