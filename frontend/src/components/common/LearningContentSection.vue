<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  items?: string[];
  numbered?: boolean;
}>();
</script>

<template>
  <section class="rounded-[1.75rem] border border-slate-200 bg-white p-6 shadow-sm">
    <h3 class="text-lg font-semibold text-slate-950">{{ title }}</h3>
    <p v-if="description" class="mt-3 text-sm leading-7 text-slate-600">{{ description }}</p>
    <ol v-if="items?.length && numbered" class="mt-4 space-y-3">
      <li v-for="(item, index) in items" :key="`${title}-${index}`" class="flex gap-3 text-sm leading-7 text-slate-700">
        <span class="mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-slate-900 text-xs font-semibold text-white">
          {{ index + 1 }}
        </span>
        <span>{{ item }}</span>
      </li>
    </ol>
    <ul v-else-if="items?.length" class="mt-4 space-y-3">
      <li v-for="(item, index) in items" :key="`${title}-${index}`" class="flex gap-3 text-sm leading-7 text-slate-700">
        <span class="mt-2 inline-block h-2 w-2 shrink-0 rounded-full bg-amber-400" />
        <span>{{ item }}</span>
      </li>
    </ul>
    <div v-else class="mt-4 text-sm text-slate-400">
      <slot>暂无内容</slot>
    </div>
  </section>
</template>
