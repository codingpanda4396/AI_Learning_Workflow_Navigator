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
  <section class="rounded-[2.2rem] border border-slate-200 bg-[linear-gradient(180deg,#ffffff_0%,#f5f7fb_100%)] p-6 shadow-[0_28px_80px_rgba(15,23,42,0.1)]">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div class="max-w-2xl">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">确认开始</p>
        <h3 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">确认后会创建正式学习会话，并从第一步任务开始。</h3>
        <p class="mt-3 text-sm leading-7 text-slate-600">
          现在确认，系统会按这条规划创建正式学习流程；如果你还想微调节奏，也可以先重新生成。
        </p>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('diagnosis')"
        >
          返回诊断
        </button>
        <button
          type="button"
          class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('back')"
        >
          返回目标
        </button>
        <button
          type="button"
          class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('regenerate')"
        >
          {{ regenerating ? '重新生成中...' : '重新生成规划' }}
        </button>
        <button
          type="button"
          class="rounded-2xl bg-slate-950 px-6 py-3 text-sm font-semibold text-white shadow-[0_16px_36px_rgba(15,23,42,0.24)] transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('confirm')"
        >
          {{ confirming ? '正在创建学习会话...' : '开始学习这条路径' }}
        </button>
      </div>
    </div>
  </section>
</template>
