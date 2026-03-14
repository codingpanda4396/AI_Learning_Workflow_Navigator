<script setup lang="ts">
interface SignalItem {
  key: string;
  label: string;
  value: string;
}

defineProps<{
  items: SignalItem[];
}>();
</script>

<template>
  <section class="app-card app-card-strong app-card-padding">
    <div class="flex items-center justify-between gap-4">
      <h2 class="text-xl font-semibold text-slate-950">AI 看到了什么</h2>
      <span class="app-badge">实时判断</span>
    </div>

    <div class="mt-4 grid gap-3 md:grid-cols-2">
      <article
        v-for="item in items.slice(0, 6)"
        :key="item.key"
        class="rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{{ item.label }}</p>
        <p class="mt-2 text-sm leading-7 text-slate-800">{{ item.value }}</p>
      </article>
    </div>
  </section>
</template>
