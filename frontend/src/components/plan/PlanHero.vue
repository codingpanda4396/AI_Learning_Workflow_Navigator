<template>
  <header class="mb-10 text-center md:mb-12 md:text-left">
    <h1
      class="text-2xl font-bold leading-tight tracking-tight text-text-primary md:text-3xl"
    >
      {{ heroTitle }}
    </h1>
    <p
      class="mx-auto mt-3 max-w-xl text-base font-medium leading-relaxed text-text-primary/90 md:mx-0"
    >
      {{ heroSubtitleLine }}
    </p>
    <p
      v-if="auxiliaryParagraph"
      class="mx-auto mt-4 max-w-xl text-sm leading-relaxed text-text-primary/85 md:mx-0"
    >
      {{ auxiliaryParagraph }}
    </p>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PlanShowcaseView } from '@/utils/planPresentationModel'

const props = defineProps<{
  pathSummaryLine: string
  totalSteps: number
  showcase?: PlanShowcaseView | null
}>()

const heroTitle = computed(() => {
  if (props.showcase?.hero?.title) return props.showcase.hero.title
  return '现在，我们一起把这个知识学会'
})

const heroSubtitleLine = computed(() => {
  if (props.showcase?.hero?.subtitle) return props.showcase.hero.subtitle
  const n = props.totalSteps
  if (n <= 0) return '别急，我们一小步一小步来，你会跟得上的'
  if (n === 4) return '先不用想太远，把这 4 小步走完，你会发现顺很多'
  return `先不用想太远，把这 ${n} 小步走完，你会发现顺很多`
})

/** 演示模式：直接展示辅助说明；默认模式：目标摘要带引导前缀 */
const auxiliaryParagraph = computed(() => {
  if (props.showcase?.hero?.auxiliaryLine) {
    return props.showcase.hero.auxiliaryLine
  }
  if (props.pathSummaryLine?.trim()) {
    return `你现在心里最想推进的是：${props.pathSummaryLine}`
  }
  return ''
})
</script>
