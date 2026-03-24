<template>
  <section
    class="overflow-hidden rounded-[28px] border border-indigo-200/80 bg-[linear-gradient(145deg,#eef2ff_0%,#ffffff_42%,#f8fafc_100%)] p-6 shadow-[0_24px_56px_rgba(79,70,229,0.12)] md:p-8"
  >
    <p class="text-xs font-semibold tracking-wide text-indigo-600">
      系统已经为你排好顺序
    </p>
    <h1 class="mt-2 text-2xl font-semibold tracking-tight text-text-primary md:text-3xl">
      这轮学习怎么开始
    </h1>

    <div class="mt-6 rounded-2xl border border-white/80 bg-white/90 p-5 shadow-sm md:p-6">
      <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
        当前知识点
      </p>
      <p class="mt-1 text-lg font-semibold text-text-primary md:text-xl">
        {{ knowledge }}
      </p>

      <div class="mt-5 flex flex-wrap items-center gap-2">
        <span class="text-sm text-text-secondary">推荐起点</span>
        <span
          class="rounded-full bg-indigo-600 px-3 py-1 text-sm font-semibold text-white"
        >
          {{ recommendedStart }}
        </span>
      </div>

      <p class="mt-4 text-sm leading-relaxed text-text-secondary">
        {{ whyLine }}
      </p>

      <div class="mt-5 flex flex-wrap items-center gap-4 border-t border-slate-100 pt-5 text-sm">
        <div>
          <span class="text-text-secondary">总预计时长</span>
          <span class="ml-2 font-semibold text-text-primary">{{ totalTime }}</span>
        </div>
      </div>

      <p class="mt-4 text-xs text-slate-500">
        本轮会依次经过：结构 → 理解 → 训练 → 复盘
      </p>
    </div>

    <div class="mt-6">
      <PrimaryButton
        class="w-full justify-center py-3.5 text-base font-semibold shadow-[0_14px_30px_rgba(79,70,229,0.35)] md:w-auto md:min-w-[200px]"
        :loading="loading"
        @click="$emit('start')"
      >
        开始第 1 步
      </PrimaryButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'

defineProps<{
  knowledge: string
  recommendedStart: string
  whyLine: string
  totalTime: string
  loading?: boolean
}>()

defineEmits<{
  start: []
}>()
</script>
