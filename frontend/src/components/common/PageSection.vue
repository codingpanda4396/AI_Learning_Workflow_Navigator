<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
  compact?: boolean;
}>();
</script>

<template>
  <section
    class="rounded-[2rem] border border-slate-200/80 bg-white/92 shadow-[0_20px_60px_rgba(15,23,42,0.06)] backdrop-blur"
    :class="compact ? 'p-5 md:p-6' : 'p-6 md:p-8'"
  >
    <div v-if="eyebrow || title || description || $slots.actions" class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div class="max-w-3xl">
        <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">{{ eyebrow }}</p>
        <h2 v-if="title" class="mt-2 text-2xl font-semibold tracking-tight text-slate-950 md:text-3xl">{{ title }}</h2>
        <p v-if="description" class="mt-3 text-sm leading-7 text-slate-600 md:text-base">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="shrink-0">
        <slot name="actions" />
      </div>
    </div>
    <div :class="eyebrow || title || description || $slots.actions ? 'mt-6' : ''">
      <slot />
    </div>
  </section>
</template>
