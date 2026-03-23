<template>
  <div class="mt-4 rounded-input border border-border bg-slate-50/90 p-4">
    <p class="text-xs font-medium text-text-secondary">带着这两个问题看</p>
    <ul class="mt-2 list-disc space-y-1.5 pl-5 text-sm text-text-primary">
      <li v-for="(q, i) in questions" :key="i">{{ q }}</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  questions: string[]
}>()
</script>
