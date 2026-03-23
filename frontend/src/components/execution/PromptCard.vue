<template>
  <FormCard>
    <p class="text-xs font-medium text-text-secondary">👨‍🏫 AI导师</p>
    <p class="mt-2 text-sm leading-relaxed text-text-primary">
      咱们先不急着背定义。我带你先把要学的东西放进一个画面里。
    </p>
    <p class="mt-3 text-xs font-medium text-text-secondary">👉 先试试这样开口：</p>
    <blockquote
      class="mt-2 border-l-4 border-emerald-600/40 pl-3 text-base leading-relaxed text-text-primary"
    >
      「{{ prompt }}」
    </blockquote>
    <p class="mt-4 text-xs leading-relaxed text-text-secondary">
      你可以照着说，也可以改一改说法。点右下角「AI导师」打开侧栏；若看不到悬浮钮，也可点下面按钮直接打开。在系统里聊即可，不必跳到外部工具。聊完再点「问过了，继续」。
    </p>
    <button
      type="button"
      class="mt-3 w-full rounded-input border border-primary/40 bg-primary/5 py-2.5 text-sm font-medium text-primary transition hover:bg-primary/10"
      @click="aiTutor.openPanel()"
    >
      打开 AI 导师侧栏
    </button>
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
import { useAiTutorStore } from '@/stores/aiTutor'

const aiTutor = useAiTutorStore()

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
