<template>
  <div class="rounded-2xl border border-accent/25 bg-accent-muted/50 p-6 shadow-sm">
    <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-accent">
      最容易混淆的点
    </p>

    <div class="space-y-2.5">
      <div
        v-for="(point, idx) in points"
        :key="idx"
        class="flex items-start gap-2 text-sm leading-relaxed text-slate-700"
      >
        <svg class="mt-0.5 h-4 w-4 shrink-0 text-accent" viewBox="0 0 20 20" fill="currentColor">
          <path
            fill-rule="evenodd"
            d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
            clip-rule="evenodd"
          />
        </svg>
        <span>{{ point }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  points: string[]
}>()
</script>
