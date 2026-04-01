<template>
  <div class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
    <p class="text-lg font-semibold text-slate-900">{{ title }}</p>
    <p class="mt-1.5 text-sm leading-relaxed text-slate-600">{{ subtitle }}</p>

    <div v-if="requirements.length" class="mt-4 space-y-1.5">
      <div
        v-for="(req, idx) in requirements"
        :key="idx"
        class="flex items-start gap-2 text-sm text-slate-700"
      >
        <span class="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary/60" />
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
