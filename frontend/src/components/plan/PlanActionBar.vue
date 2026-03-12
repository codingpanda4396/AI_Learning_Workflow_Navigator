<script setup lang="ts">
defineProps<{
  confirming?: boolean;
  regenerating?: boolean;
  disabled?: boolean;
}>();

defineEmits<{
  confirm: [];
  regenerate: [];
  back: [];
  diagnosis: [];
}>();
</script>

<template>
  <section class="sticky bottom-4 z-20 rounded-[2rem] border border-slate-200/80 bg-white/95 p-5 shadow-[0_24px_80px_rgba(15,23,42,0.12)] backdrop-blur">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div class="max-w-2xl">
        <p class="text-sm font-semibold text-slate-950">确认后会直接进入本轮学习执行链路。</p>
        <p class="mt-1 text-sm leading-6 text-slate-600">系统会基于这份规划创建 session，并按四阶段任务流继续推进。</p>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('diagnosis')"
        >
          查看能力诊断摘要
        </button>
        <button
          type="button"
          class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('back')"
        >
          返回修改目标
        </button>
        <button
          type="button"
          class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('regenerate')"
        >
          {{ regenerating ? '重新生成中...' : '重新生成方案' }}
        </button>
        <button
          type="button"
          class="rounded-2xl bg-slate-950 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('confirm')"
        >
          {{ confirming ? '正在创建学习会话...' : '确认方案，开始学习' }}
        </button>
      </div>
    </div>
  </section>
</template>
