<script setup lang="ts">
defineProps<{
  title: string;
  mainRiskIfSkip: string;
  adaptationHint: string;
}>();
</script>

<template>
  <section class="rounded-[28px] border border-slate-200 bg-white px-6 py-6 shadow-sm sm:px-8">
    <h2 class="text-xl font-semibold text-slate-950">{{ title }}</h2>
    <div class="mt-4 grid gap-3 sm:grid-cols-2">
      <article class="rounded-2xl border border-rose-100 bg-rose-50/60 px-4 py-4">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-rose-500">如果跳过</p>
        <p class="mt-2 text-sm leading-7 text-rose-900">{{ mainRiskIfSkip }}</p>
      </article>
      <article class="rounded-2xl border border-emerald-100 bg-emerald-50/60 px-4 py-4">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-500">做完后会怎样</p>
        <p class="mt-2 text-sm leading-7 text-emerald-900">{{ adaptationHint }}</p>
      </article>
    </div>
  </section>
</template>
