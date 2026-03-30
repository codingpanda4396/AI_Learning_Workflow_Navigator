<template>
  <section
    data-testid="scaffold-action-card"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"
  >
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h2 class="text-base font-semibold text-slate-950">系统给你的思考支架</h2>
        <p class="mt-1 text-xs text-slate-600">先按支架推进，再提交结构化表达。</p>
      </div>
      <div
        v-if="topicObservationBullets?.length"
        class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600"
      >
        本轮观察点 {{ topicObservationBullets.length }}
      </div>
    </div>

    <div class="mt-4 grid gap-3">
      <article
        v-for="section in renderedSections"
        :key="section.id"
        class="rounded-2xl border border-slate-200/90 bg-slate-50/70 p-4"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
              {{ section.title }}
            </p>
            <p class="mt-2 text-sm font-medium leading-6 text-slate-900">
              {{ section.description }}
            </p>
          </div>
          <button
            type="button"
            class="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-700 transition hover:border-primary/40 hover:text-primary"
            :disabled="sending"
            @click="toggleOpen(section.id)"
          >
            {{ openSectionId === section.id ? '收起' : '展开提示' }}
          </button>
        </div>

        <div v-if="openSectionId === section.id" class="mt-3 grid gap-2 md:grid-cols-3">
          <button
            type="button"
            class="rounded-xl border border-slate-200 bg-white px-3 py-3 text-left text-xs leading-5 text-slate-700 transition hover:border-primary/40 hover:bg-primary/5"
            :disabled="sending"
            @click="$emit('prefill-chip', section.lightHint)"
          >
            <span class="block font-semibold text-slate-900">轻提示</span>
            <span class="mt-1 block">{{ section.lightHint }}</span>
          </button>
          <button
            type="button"
            class="rounded-xl border border-slate-200 bg-white px-3 py-3 text-left text-xs leading-5 text-slate-700 transition hover:border-primary/40 hover:bg-primary/5"
            :disabled="sending"
            @click="$emit('prefill-chip', section.standardHint)"
          >
            <span class="block font-semibold text-slate-900">标准提示</span>
            <span class="mt-1 block">{{ section.standardHint }}</span>
          </button>
          <button
            type="button"
            class="rounded-xl border border-slate-200 bg-white px-3 py-3 text-left text-xs leading-5 text-slate-700 transition hover:border-primary/40 hover:bg-primary/5"
            :disabled="sending"
            @click="$emit('prefill-chip', section.strongHint)"
          >
            <span class="block font-semibold text-slate-900">加强提示</span>
            <span class="mt-1 block">{{ section.strongHint }}</span>
          </button>
        </div>
      </article>
    </div>

    <div
      v-if="topicObservationBullets?.length"
      class="mt-4 rounded-2xl border border-slate-200/90 bg-slate-50 px-4 py-3"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">本轮观察点</p>
      <ul class="mt-2 space-y-1 text-sm leading-6 text-slate-700">
        <li v-for="(bullet, index) in topicObservationBullets" :key="index">- {{ bullet }}</li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { WorkbenchGuideSectionModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  sections?: WorkbenchGuideSectionModel[]
  topicObservationBullets?: string[]
  sending: boolean
}>()

defineEmits<{
  'prefill-chip': [text: string]
}>()

const openSectionId = ref<string | null>(null)

const renderedSections = computed(() =>
  (props.sections ?? []).length
    ? props.sections!
    : [
        {
          id: 'think-first',
          title: '先想什么',
          description: '先确认当前动作真正要你完成什么。',
          lightHint: '先写一个最小判断。',
          standardHint: '用一句话说出它在解决什么问题。',
          strongHint: '如果还是空白，就先对比它和相邻概念的差别。',
        },
        {
          id: 'fill-gap',
          title: '再补什么',
          description: '只补最关键的一处，不要一口气展开全部。',
          lightHint: '补一句因果或边界。',
          standardHint: '想一想：少了它会怎样？',
          strongHint: '换一个最小例子验证刚才的判断。',
        },
        {
          id: 'land-output',
          title: '最后落到哪里',
          description: '把这一步收束成可以提交的一版。',
          lightHint: '对照完成标准看还缺什么。',
          standardHint: '按结构化字段把当前理解写出来。',
          strongHint: '优先修最影响过关的一处，而不是全部重写。',
        },
      ]
)

function toggleOpen(id: string) {
  openSectionId.value = openSectionId.value === id ? null : id
}
</script>
