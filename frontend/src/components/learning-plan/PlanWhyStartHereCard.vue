<script setup lang="ts">
import { computed } from 'vue';
import type { PlanPriorityNode } from '@/types/learningPlan';

const props = defineProps<{
  whyStartHere?: string;
  keyWeaknesses: string[];
  priorityNodes: PlanPriorityNode[];
}>();

const whyStartHereText = computed(() => props.whyStartHere?.trim() || '系统会从当前最稳妥的知识点切入，帮助你先补齐关键薄弱点。');

const weaknessTags = computed(() => props.keyWeaknesses.map((item) => item.trim()).filter(Boolean).slice(0, 4));

const priorityList = computed(() =>
  props.priorityNodes
    .map((item) => ({
      nodeId: String(item.nodeId || ''),
      title: item.title?.trim() || '当前推荐节点',
      reason: item.reason?.trim() || '这是当前更适合作为起点的节点。',
    }))
    .filter((item) => item.title || item.reason)
    .slice(0, 3),
);
</script>

<template>
  <section class="rounded-[1.6rem] border border-sky-100 bg-sky-50/70 px-5 py-5">
    <p class="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">AI 个性化解释</p>
    <h3 class="mt-3 text-lg font-semibold text-slate-950">为什么从这里开始</h3>
    <p class="mt-3 text-sm leading-7 text-slate-700">
      {{ whyStartHereText }}
    </p>

    <div v-if="weaknessTags.length" class="mt-4">
      <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">当前最需要补强</p>
      <div class="mt-2 flex flex-wrap gap-2">
        <span
          v-for="tag in weaknessTags"
          :key="tag"
          class="rounded-full border border-sky-200 bg-white px-3 py-1 text-xs font-medium text-slate-700"
        >
          {{ tag }}
        </span>
      </div>
    </div>

    <div v-if="priorityList.length" class="mt-4">
      <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">优先学习节点</p>
      <ul class="mt-2 space-y-2">
        <li
          v-for="node in priorityList"
          :key="`${node.nodeId}-${node.title}`"
          class="rounded-xl border border-sky-100 bg-white px-3 py-2 text-sm leading-6 text-slate-700"
        >
          <span class="font-semibold text-slate-900">{{ node.title }}</span>
          <span>：{{ node.reason }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>
