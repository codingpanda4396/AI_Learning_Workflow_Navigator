<script setup lang="ts">
import type { StartLearningForm } from '@/types/home';

const model = defineModel<StartLearningForm>({ required: true });

defineProps<{
  disabled: boolean;
  loading?: boolean;
}>();

defineEmits<{
  submit: [];
}>();
</script>

<template>
  <section class="rounded-[1.75rem] bg-white/92 p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)] ring-1 ring-slate-200/70 backdrop-blur md:p-6">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-xl">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-sky-600">开始学习</p>
        <h2 class="mt-2 text-xl font-semibold tracking-tight text-slate-950">创建真实学习会话</h2>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          输入学习目标、课程和章节。系统会先创建真实会话，再带你进入诊断流程。
        </p>
      </div>
      <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">同一时间仅保留一个活跃会话</div>
    </div>

    <div class="mt-5 grid gap-3 lg:grid-cols-[1.4fr_1fr_1fr_auto]">
      <label class="block">
        <span class="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">学习目标</span>
        <input
          v-model="model.goal"
          type="text"
          class="mt-2 w-full rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none ring-1 ring-transparent transition focus:bg-white focus:ring-sky-300"
          placeholder="例如：理解最短路径算法"
        />
      </label>

      <label class="block">
        <span class="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">课程</span>
        <input
          v-model="model.course"
          type="text"
          class="mt-2 w-full rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none ring-1 ring-transparent transition focus:bg-white focus:ring-sky-300"
          placeholder="例如：数据结构"
        />
      </label>

      <label class="block">
        <span class="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">章节</span>
        <input
          v-model="model.chapter"
          type="text"
          class="mt-2 w-full rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none ring-1 ring-transparent transition focus:bg-white focus:ring-sky-300"
          placeholder="例如：图"
        />
      </label>

      <div class="flex items-end">
        <button
          type="button"
          class="w-full rounded-2xl px-5 py-3 text-sm font-semibold transition lg:w-auto"
          :class="disabled || loading ? 'cursor-not-allowed bg-slate-200 text-slate-500' : 'bg-slate-950 text-white hover:bg-slate-800'"
          :disabled="disabled || loading"
          @click="$emit('submit')"
        >
          {{ loading ? '正在创建会话...' : '开始学习' }}
        </button>
      </div>
    </div>

    <p v-if="disabled" class="mt-4 text-sm text-amber-700">请先完成或继续当前活跃会话，再创建新的学习会话。</p>
  </section>
</template>
