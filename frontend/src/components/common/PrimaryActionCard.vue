<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title: string;
  description: string;
  helper?: string;
  buttonLabel: string;
  disabled?: boolean;
}>();

defineEmits<{
  action: [];
}>();
</script>

<template>
  <div class="rounded-[2rem] bg-slate-950 p-7 text-white shadow-[0_24px_80px_rgba(15,23,42,0.28)] md:p-8">
    <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">{{ eyebrow }}</p>
    <h3 class="mt-3 text-2xl font-semibold tracking-tight md:text-3xl">{{ title }}</h3>
    <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-300 md:text-base">{{ description }}</p>
    <p v-if="helper" class="mt-4 text-sm text-slate-400">{{ helper }}</p>
    <div class="mt-8">
      <button
        class="inline-flex items-center justify-center rounded-2xl bg-amber-400 px-6 py-3 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="disabled"
        @click="$emit('action')"
      >
        {{ buttonLabel }}
      </button>
    </div>
  </div>
</template>
