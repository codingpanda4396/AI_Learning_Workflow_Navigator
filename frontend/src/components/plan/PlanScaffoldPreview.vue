<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-primary">
          Learning Protocol
        </p>
        <h2 class="mt-2 text-xl font-semibold text-text-primary">
          本轮 AI 会这样带你学
        </h2>
      </div>
      <p class="max-w-2xl text-sm leading-6 text-text-secondary">
        这四段不是换名字，而是换教学策略。系统会根据阶段切换提问方式、纠偏力度和通过标准。
      </p>
    </div>

    <div class="grid gap-3 lg:grid-cols-4">
      <article
        v-for="item in items"
        :key="item.code"
        class="rounded-[24px] border p-5 transition"
        :class="itemClass(item.code)"
      >
        <div class="flex flex-wrap items-center gap-2">
          <p class="font-mono text-[11px] font-bold tracking-tight text-slate-500">
            {{ item.code }}
          </p>
          <span
            v-if="item.code === props.expandedStageCode"
            class="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-800"
          >
            当前重点
          </span>
        </div>
        <p class="mt-3 text-sm font-semibold text-text-primary">
          {{ item.title }}
        </p>
        <p class="mt-3 text-sm leading-6 text-text-secondary">
          {{ item.strategy }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PlanStageCode } from '@/utils/planPresentationModel'

const props = defineProps<{
  expandedStageCode: PlanStageCode
}>()

const items: { code: PlanStageCode; title: string; strategy: string }[] = [
  {
    code: 'STRUCTURE',
    title: '先搭结构',
    strategy: '先让你说出整体图景，再补关键节点，让你先看到这块知识在全局里的位置。',
  },
  {
    code: 'UNDERSTANDING',
    title: '再问为什么',
    strategy: '先追问机制和因果，再澄清关键概念，不会一上来就把定义整段讲完。',
  },
  {
    code: 'TRAINING',
    title: '再做最小练习',
    strategy: '先让你自己判断，再按错误类型给最小提示，帮助你把理解变成动作。',
  },
  {
    code: 'REFLECTION',
    title: '最后收束漏洞',
    strategy: '先让你自己总结哪里还不稳，再给修正建议，明确下一轮从哪里继续。',
  },
]

function itemClass(code: PlanStageCode) {
  if (code === props.expandedStageCode) {
    return 'border-primary/35 bg-gradient-to-br from-primary/8 via-white to-white shadow-[0_16px_36px_rgba(79,70,229,0.12)]'
  }
  return 'border-slate-200 bg-white shadow-sm'
}
</script>
