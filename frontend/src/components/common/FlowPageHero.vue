<script setup lang="ts">
export interface FlowHeroContextItem {
  label: string;
  value: string;
}

withDefaults(defineProps<{
  stepLabel: string;
  title: string;
  description: string;
  metaLabel?: string;
  metaValue?: string;
  contextItems?: FlowHeroContextItem[];
}>(), {
  metaLabel: '',
  metaValue: '',
  contextItems: () => [],
});
</script>

<template>
  <section class="rounded-[2.2rem] border border-slate-200 bg-[linear-gradient(180deg,rgba(248,250,252,0.96),rgba(255,255,255,0.98))] p-6 shadow-[0_24px_70px_rgba(15,23,42,0.08)] md:p-8">
    <div class="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">{{ stepLabel }}</p>
        <h1 class="mt-3 text-3xl font-semibold tracking-tight text-slate-950 md:text-4xl">{{ title }}</h1>
        <p class="mt-4 text-sm leading-7 text-slate-600 md:text-base">{{ description }}</p>
      </div>
      <div v-if="metaLabel || metaValue" class="rounded-[1.4rem] border border-slate-200 bg-white/90 px-4 py-3 text-left text-xs leading-5 text-slate-500 md:min-w-[10rem] md:text-right">
        <p>{{ metaLabel }}</p>
        <p class="mt-1 font-medium text-slate-700">{{ metaValue || '--' }}</p>
      </div>
    </div>

    <div v-if="contextItems.length || $slots.context" class="mt-6">
      <div v-if="contextItems.length" class="grid gap-3 md:grid-cols-3">
        <div
          v-for="item in contextItems"
          :key="`${item.label}-${item.value}`"
          class="rounded-[1.4rem] border border-slate-200 bg-white/92 p-4"
        >
          <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">{{ item.label }}</p>
          <p class="mt-2 text-sm font-medium leading-6 text-slate-800">{{ item.value }}</p>
        </div>
      </div>
      <slot v-else name="context" />
    </div>
  </section>
</template>
