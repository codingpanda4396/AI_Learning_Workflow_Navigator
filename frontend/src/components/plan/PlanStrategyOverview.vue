<template>
  <section
    class="overflow-hidden rounded-[28px] border border-slate-200 bg-[linear-gradient(135deg,#0f172a_0%,#13233f_38%,#20456d_100%)] text-white shadow-[0_24px_60px_rgba(15,23,42,0.22)]"
  >
    <div class="grid gap-6 px-5 py-6 md:grid-cols-[1.35fr_0.85fr] md:px-7 md:py-7">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-cyan-200/90">
          Plan Battle Map
        </p>
        <div class="mt-3 flex flex-wrap items-end gap-3">
          <h1 class="text-[30px] font-semibold leading-none tracking-tight md:text-[40px]">
            四阶段作战图
          </h1>
          <span class="rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-medium text-slate-100">
            推荐从 {{ overview.recommendedStageLabel }} 切入
          </span>
        </div>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-200 md:text-base">
          这不是一页策略说明，而是系统为你排好的学习编排台。先定切入点，再看四阶段如何串起来，最后把每一步落成具体任务。
        </p>

        <div class="mt-5 grid gap-3 md:grid-cols-2">
          <article
            v-for="item in summaryItems"
            :key="item.label"
            class="rounded-2xl border border-white/12 bg-white/8 p-4 backdrop-blur-sm"
          >
            <p class="text-[11px] font-semibold uppercase tracking-[0.2em] text-cyan-100/80">
              {{ item.label }}
            </p>
            <p class="mt-2 text-sm font-medium leading-6 text-white">
              {{ item.value }}
            </p>
          </article>
        </div>
      </div>

      <aside class="flex h-full flex-col justify-between rounded-[24px] border border-white/12 bg-white/8 p-5 backdrop-blur-sm">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-100/80">
            Start Mission
          </p>
          <p class="mt-3 text-2xl font-semibold tracking-tight">
            从第 1 步进入执行
          </p>
          <p class="mt-3 text-sm leading-7 text-slate-200">
            执行入口仍保持现有链路，不会直接跳到中间阶段。推荐切入阶段用于解释编排逻辑和视觉高亮。
          </p>
        </div>

        <div class="mt-6">
          <PrimaryButton
            class="w-full justify-center py-3.5 text-base font-semibold shadow-[0_14px_30px_rgba(79,70,229,0.38)]"
            :loading="loading"
            @click="$emit('start')"
          >
            开始执行第 1 步
          </PrimaryButton>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { PlanStrategyOverview } from '@/utils/planPresentationModel'

const props = defineProps<{
  overview: PlanStrategyOverview
  loading?: boolean
}>()

defineEmits<{
  start: []
}>()

const summaryItems = computed(() => [
  { label: '当前知识点', value: props.overview.currentKnowledge },
  { label: '推荐策略', value: props.overview.recommendedStrategy },
  { label: '为什么这样安排', value: props.overview.whyThisArrangement },
  { label: '跳过风险', value: props.overview.skipRisk },
])
</script>
