<script setup lang="ts">
defineProps<{
  title: string;
  whatISaw: string[];
  whyThisPlanFitsYou: string;
}>();
</script>

<template>
  <section class="rounded-[28px] border border-slate-200 bg-white px-6 py-6 shadow-sm sm:px-8">
    <h2 class="text-xl font-semibold text-slate-950">{{ title }}</h2>
    <ul class="mt-4 space-y-2">
      <li
        v-for="(item, index) in whatISaw.slice(0, 4)"
        :key="`saw-${index}`"
        class="rounded-2xl bg-slate-50 px-4 py-3 text-sm leading-7 text-slate-700"
      >
        {{ item }}
      </li>
    </ul>
    <p class="mt-4 rounded-2xl border border-sky-100 bg-sky-50/60 px-4 py-3 text-sm leading-7 text-sky-900">
      {{ whyThisPlanFitsYou }}
    </p>
  </section>
</template>
