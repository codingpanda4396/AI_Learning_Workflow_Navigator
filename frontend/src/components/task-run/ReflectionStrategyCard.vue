<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">迁移策略</p>
    <div class="mt-2 space-y-2">
      <button
        v-for="s in strategies"
        :key="s.id"
        type="button"
        class="w-full rounded-xl border px-3 py-2 text-left text-sm font-medium transition"
        :class="selected === s.id ? 'border-accent bg-accent-muted/80' : 'border-slate-200 bg-slate-50'"
        @click="$emit('select', s.id)"
      >
        {{ s.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ReflectionStrategy } from '@/types/phaseWorkbench'

defineProps<{
  strategies: ReflectionStrategy[]
  selected: string | null
}>()

defineEmits<{
  select: [strategyId: string]
}>()
</script>
