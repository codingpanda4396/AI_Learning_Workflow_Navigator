<template>
  <div
    class="rounded-xl border-2 border-accent/30 bg-white p-6 shadow-[0_8px_28px_rgba(234,88,12,0.07)] ring-1 ring-accent/10"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-text-muted">当前动作 · 表达训练</p>
    <p class="mt-2 text-lg font-semibold text-text-primary">{{ title }}</p>
    <p class="mt-1.5 text-sm leading-relaxed text-text-secondary">{{ subtitle }}</p>

    <div v-if="requirements.length" class="mt-4 space-y-1.5">
      <div
        v-for="(req, idx) in requirements"
        :key="idx"
        class="flex items-start gap-2 text-sm text-text-primary"
      >
        <span class="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-accent/70" />
        <span>{{ req }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle: string
  requirements: string[]
}>()
</script>
