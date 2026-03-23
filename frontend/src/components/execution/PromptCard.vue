<template>
  <FormCard>
    <p class="text-xs font-medium text-text-secondary">👨‍🏫 AI导师</p>
    <p class="mt-2 text-sm leading-relaxed text-text-primary">
      咱们先不急着背定义。我带你先把要学的东西放进一个画面里。
    </p>
    <p class="mt-3 text-xs font-medium text-text-secondary">👉 你可以这样问我：</p>
    <blockquote
      class="mt-2 border-l-4 border-emerald-600/40 pl-3 text-base leading-relaxed text-text-primary"
    >
      「{{ prompt }}」
    </blockquote>
    <p class="mt-4 text-xs leading-relaxed text-text-secondary">
      你可以照着说，也可以改一改说法。去教材、视频或大模型里真的问一遍，练的是「会问」。问完点下面按钮，我们继续。
    </p>
    <div
      v-if="whyTitle && whyBullets?.length"
      class="mt-5 rounded-input border border-border bg-slate-50 px-3 py-3"
    >
      <p class="text-xs font-semibold text-text-primary">💡 {{ whyTitle }}</p>
      <p class="mt-2 text-xs text-text-secondary">我这么带你问，是因为：</p>
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
