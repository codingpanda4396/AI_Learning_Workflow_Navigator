<template>
  <section
    class="overflow-hidden rounded-[32px] border border-indigo-200/80 bg-[linear-gradient(145deg,#eef2ff_0%,#ffffff_40%,#f8fafc_100%)] p-6 shadow-[0_24px_56px_rgba(79,70,229,0.12)] md:p-8"
  >
    <p class="text-xs font-semibold tracking-[0.22em] text-indigo-600">Plan</p>
    <h1 class="mt-2 text-2xl font-semibold tracking-tight text-text-primary md:text-3xl">
      这一轮你会这样学
    </h1>
    <p class="mt-3 text-sm leading-6 text-text-secondary">
      先看顺序，不用一次看完全部内容。
    </p>

    <div class="mt-6 grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
      <div class="rounded-[26px] border border-white/80 bg-white/90 p-5 shadow-sm md:p-6">
        <p class="text-[11px] font-semibold uppercase tracking-wider text-slate-400">当前问题</p>
        <p class="mt-1 text-lg font-semibold text-text-primary md:text-xl">
          {{ currentProblem }}
        </p>

        <div class="mt-5 flex flex-wrap items-center gap-2">
          <span class="text-sm text-text-secondary">先从这里开始</span>
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
            <span class="text-text-secondary">这一轮共</span>
            <span class="ml-2 font-semibold text-text-primary">{{ totalTime }}</span>
          </div>
          <div>
            <span class="text-text-secondary">总共</span>
            <span class="ml-2 font-semibold text-text-primary">4 步</span>
          </div>
        </div>
      </div>

      <div class="rounded-[26px] border border-indigo-100 bg-indigo-950 px-5 py-5 text-white shadow-[0_18px_40px_rgba(79,70,229,0.18)] md:px-6">
        <p class="text-[11px] font-semibold uppercase tracking-[0.22em] text-indigo-200/80">这一轮安排</p>
        <div class="mt-4 space-y-4">
          <div>
            <p class="text-xs text-indigo-200/80">接下来这样走</p>
            <p class="mt-1 text-base font-semibold">{{ recommendedLine }}</p>
          </div>
          <div class="rounded-2xl border border-white/12 bg-white/8 p-4">
            <p class="text-sm font-semibold">{{ notYetLine }}</p>
            <p class="mt-2 text-sm text-indigo-100">{{ riskLine }}</p>
            <p class="mt-3 text-xs leading-5 text-indigo-200/80">先把第一步做完，后面再往下看。</p>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6">
      <PrimaryButton
        class="w-full justify-center py-3.5 text-base font-semibold shadow-[0_14px_30px_rgba(79,70,229,0.35)] md:w-auto md:min-w-[200px]"
        :loading="loading"
        @click="$emit('start')"
      >
        开始第一步
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
  currentProblem: string
  recommendedLine: string
  notYetLine: string
  riskLine: string
  loading?: boolean
}>()

defineEmits<{
  start: []
}>()
</script>
