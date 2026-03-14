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
  <div class="rounded-[2rem] border border-slate-900/90 bg-slate-950 p-7 text-white shadow-[0_24px_80px_rgba(15,23,42,0.28)] md:p-8">
    <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-2xl">
        <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.28em] text-amber-300/80">{{ eyebrow }}</p>
        <h3 class="mt-3 text-2xl font-semibold tracking-tight md:text-3xl">{{ title }}</h3>
        <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-300 md:text-base">{{ description }}</p>
        <p v-if="helper" class="mt-4 text-sm text-slate-400">{{ helper }}</p>
      </div>
      <div class="rounded-[1.6rem] border border-white/10 bg-white/6 p-4 lg:w-[22rem]">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">建议下一步</p>
        <p class="mt-2 text-sm leading-6 text-slate-200">继续进入学习规划页，系统会把这份诊断结果转换成一条可直接开始的学习路径。</p>
        <button
          class="mt-5 inline-flex w-full items-center justify-center rounded-2xl bg-amber-400 px-6 py-3 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled"
          @click="$emit('action')"
        >
          {{ buttonLabel }}
        </button>
      </div>
    </div>
  </div>
</template>
