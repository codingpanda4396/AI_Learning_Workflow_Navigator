<template>
  <FormCard>
    <p class="text-xs font-medium text-text-secondary">导师建议你这样问</p>
    <p class="mt-3 text-base leading-relaxed text-text-primary">
      {{ prompt }}
    </p>
    <p class="mt-4 text-xs text-text-secondary">
      复制或改写上面这句话，到你的学习渠道（教材、视频、大模型等）里实际问一遍；重点是练习「会问」，而不是背答案。问完后点下面按钮继续。
    </p>
    <div
      v-if="whyTitle && whyBullets?.length"
      class="mt-5 rounded-input border border-border bg-slate-50 px-3 py-3"
    >
      <p class="text-xs font-semibold text-text-primary">💡 {{ whyTitle }}</p>
      <p class="mt-2 text-xs text-text-secondary">这样可以帮助你：</p>
      <ul class="mt-1.5 list-inside list-disc space-y-1 text-xs text-text-secondary">
        <li v-for="(b, i) in whyBullets" :key="i">{{ b }}</li>
      </ul>
    </div>
  </FormCard>
</template>

<script setup lang="ts">
import FormCard from '@/components/ui/FormCard.vue'

withDefaults(
  defineProps<{
    prompt: string
    whyTitle?: string
    whyBullets?: string[]
  }>(),
  {
    whyTitle: '',
    whyBullets: () => [],
  }
)
</script>
