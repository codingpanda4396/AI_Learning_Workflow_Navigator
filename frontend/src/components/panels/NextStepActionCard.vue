<script setup lang="ts">
defineProps<{
  suggestion: string;
  disabled?: boolean;
}>();

defineEmits<{
  action: [];
}>();
</script>

<template>
  <section
    class="rounded-[2rem] bg-[linear-gradient(135deg,#0f172a_0%,#162033_52%,#1f2937_100%)] px-6 py-7 text-white shadow-[0_28px_90px_rgba(15,23,42,0.24)] md:px-8 md:py-8"
  >
    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">下一步行动</p>
    <p class="mt-4 max-w-2xl text-base leading-8 text-slate-200">
      {{ suggestion }}
    </p>
    <button
      type="button"
      class="mt-6 inline-flex min-h-[3.5rem] w-full items-center justify-center rounded-[1.25rem] bg-emerald-400 px-6 py-4 text-base font-semibold text-slate-950 transition hover:bg-emerald-300 disabled:cursor-not-allowed disabled:opacity-60 md:w-auto md:min-w-[20rem]"
      :disabled="disabled"
      @click="$emit('action')"
    >
      查看 AI 为你生成的学习路径
    </button>
  </section>
</template>
