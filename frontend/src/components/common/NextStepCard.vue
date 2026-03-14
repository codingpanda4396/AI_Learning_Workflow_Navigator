<script setup lang="ts">
defineProps<{
  title: string;
  actionLabel: string;
  reason?: string;
  secondaryLabel?: string;
  tertiaryLabel?: string;
  loading?: boolean;
}>();

defineEmits<{
  primary: [];
  secondary: [];
  tertiary: [];
}>();
</script>

<template>
  <div class="rounded-[2rem] bg-slate-950 p-7 text-white shadow-[0_24px_80px_rgba(15,23,42,0.28)]">
    <p class="text-sm font-medium text-slate-300">Recommended next step</p>
    <h3 class="mt-3 text-2xl font-semibold">{{ title }}</h3>
    <p v-if="reason" class="mt-4 text-sm leading-7 text-slate-300">{{ reason }}</p>
    <div class="mt-8 flex flex-col gap-3">
      <button
        class="rounded-2xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="loading"
        @click="$emit('primary')"
      >
        {{ actionLabel }}
      </button>
      <button
        v-if="secondaryLabel"
        class="rounded-2xl border border-white/20 px-5 py-3 text-sm font-medium text-white transition hover:bg-white/8"
        @click="$emit('secondary')"
      >
        {{ secondaryLabel }}
      </button>
      <button
        v-if="tertiaryLabel"
        class="text-left text-sm text-slate-400 transition hover:text-white"
        @click="$emit('tertiary')"
      >
        {{ tertiaryLabel }}
      </button>
    </div>
  </div>
</template>
