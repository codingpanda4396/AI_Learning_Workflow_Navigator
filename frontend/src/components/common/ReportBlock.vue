<script setup lang="ts">
defineProps<{
  title: string;
  items?: string[];
  description?: string;
  tone?: 'default' | 'highlight';
}>();
</script>

<template>
  <section
    class="rounded-[1.8rem] border p-6"
    :class="tone === 'highlight' ? 'border-slate-900 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-950'"
  >
    <h3 class="text-lg font-semibold">{{ title }}</h3>
    <p v-if="description" class="mt-3 text-sm leading-7" :class="tone === 'highlight' ? 'text-slate-300' : 'text-slate-600'">
      {{ description }}
    </p>
    <ul v-if="items?.length" class="mt-4 space-y-3">
      <li
        v-for="(item, index) in items"
        :key="`${title}-${index}`"
        class="flex gap-3 text-sm leading-7"
        :class="tone === 'highlight' ? 'text-slate-200' : 'text-slate-700'"
      >
        <span class="mt-2 inline-block h-2 w-2 shrink-0 rounded-full" :class="tone === 'highlight' ? 'bg-amber-400' : 'bg-slate-900'" />
        <span>{{ item }}</span>
      </li>
    </ul>
    <div v-else class="mt-4">
      <slot />
    </div>
  </section>
</template>
