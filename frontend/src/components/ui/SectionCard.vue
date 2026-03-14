<script setup lang="ts">
withDefaults(defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
  muted?: boolean;
  strong?: boolean;
  interactive?: boolean;
}>(), {
  eyebrow: '',
  title: '',
  description: '',
  muted: false,
  strong: false,
  interactive: false,
});
</script>

<template>
  <section
    :class="[
      'app-card app-card-padding',
      strong ? 'app-card-strong' : '',
      muted ? 'app-card-muted' : '',
      interactive ? 'app-card-interactive' : '',
    ]"
  >
    <div v-if="eyebrow || title || description" class="mb-4">
      <p v-if="eyebrow" class="app-eyebrow">{{ eyebrow }}</p>
      <h2 v-if="title" class="mt-2 text-[24px] font-semibold tracking-[-0.03em] text-slate-950">{{ title }}</h2>
      <p v-if="description" class="mt-2 text-sm leading-7 text-slate-600">{{ description }}</p>
    </div>
    <slot />
  </section>
</template>
