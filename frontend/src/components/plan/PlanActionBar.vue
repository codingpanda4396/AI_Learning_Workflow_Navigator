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
  <section class="rounded-[2.4rem] border border-slate-950 bg-[linear-gradient(135deg,#0f172a_0%,#111827_52%,#1e293b_100%)] p-6 text-white shadow-[0_32px_100px_rgba(15,23,42,0.28)]">
    <div class="flex flex-col gap-6">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">确认开始</p>
        <h3 class="mt-2 text-2xl font-semibold tracking-tight text-white md:text-3xl">确认后会正式创建学习会话，并直接进入第一步任务。</h3>
        <p class="mt-3 text-sm leading-7 text-slate-300">
          主按钮会按当前规划启动学习流程；如果你还想微调节奏或重看诊断，可以使用下面这些次级操作。
        </p>
      </div>

      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-[1.5rem] border border-white/10 bg-white/6 p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">确认后发生什么</p>
          <p class="mt-2 text-sm leading-6 text-slate-100">系统会正式创建一条新的学习会话。</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/10 bg-white/6 p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">会进入哪里</p>
          <p class="mt-2 text-sm leading-6 text-slate-100">页面会直接跳转到第一步学习任务。</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/10 bg-white/6 p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">何时需要重生成</p>
          <p class="mt-2 text-sm leading-6 text-slate-100">只有想换学习节奏或起点策略时才需要。</p>
        </div>
        <div class="rounded-[1.5rem] border border-white/10 bg-white/6 p-4">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">返回上一步</p>
          <p class="mt-2 text-sm leading-6 text-slate-100">你仍然可以回诊断或目标页重新确认。</p>
        </div>
      </div>

      <div class="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
        <div class="flex flex-wrap gap-3">
          <button
            type="button"
            class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-200 ring-1 ring-white/12 transition hover:bg-white/8"
            :disabled="disabled || regenerating || confirming"
            @click="$emit('diagnosis')"
          >
            返回诊断
          </button>
          <button
            type="button"
            class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-200 ring-1 ring-white/12 transition hover:bg-white/8"
            :disabled="disabled || regenerating || confirming"
            @click="$emit('back')"
          >
            返回目标
          </button>
          <button
            type="button"
            class="rounded-2xl px-5 py-3 text-sm font-semibold text-slate-200 ring-1 ring-white/12 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="disabled || regenerating || confirming"
            @click="$emit('regenerate')"
          >
            {{ regenerating ? '重新生成中...' : '重新生成规划' }}
          </button>
        </div>

        <button
          type="button"
          class="min-w-[15rem] rounded-2xl bg-white px-6 py-4 text-sm font-semibold text-slate-950 shadow-[0_22px_60px_rgba(255,255,255,0.16)] transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled || regenerating || confirming"
          @click="$emit('confirm')"
        >
          {{ confirming ? '正在创建学习会话...' : '确认并开始学习' }}
        </button>
      </div>
    </div>
  </section>
</template>
