<script setup lang="ts">
import { computed } from 'vue';
import PageSection from '@/components/common/PageSection.vue';
import { PATH_DIFFICULTY_LABELS, PATH_STATUS_LABELS } from '@/constants/learningPlan';
import type { PlanPathNode } from '@/types/learningPlan';

const props = defineProps<{
  nodes: PlanPathNode[];
  focuses?: string[];
}>();

const currentNode = computed(() => props.nodes.find((node) => node.isStartingPoint || node.isFocus) || props.nodes[0]);
const nextNode = computed(() => {
  if (!props.nodes.length) return undefined;
  const index = props.nodes.findIndex((node) => node.node.id === currentNode.value?.node.id);
  return index >= 0 ? props.nodes[index + 1] : props.nodes[1];
});
const pathSummary = computed(() => props.nodes.map((node) => getNodeName(node)).filter(Boolean).join(' → '));

function getNodeRole(node: PlanPathNode, index: number) {
  if (node.isStartingPoint) return '本轮起点';
  if (node.isPrerequisite) return '前置补齐';
  if (node.isFocus) return '当前重点';
  if (index === props.nodes.length - 1) return '后续进阶';
  return '路径推进';
}

function getNodeName(node: PlanPathNode) {
  return node.node.displayName || node.node.nodeName;
}

function getNodeCardClass(node: PlanPathNode) {
  if (node.isStartingPoint || node.isFocus) {
    return 'border-slate-950 bg-slate-950 text-white shadow-[0_20px_60px_rgba(15,23,42,0.18)]';
  }
  if (node.isPrerequisite) {
    return 'border-sky-200 bg-sky-50/80';
  }
  return 'border-slate-200/90 bg-white';
}

function getTagClass(node: PlanPathNode) {
  return node.isStartingPoint || node.isFocus ? 'bg-white/12 text-white' : 'bg-slate-100 text-slate-600';
}
</script>

<template>
  <PageSection
    eyebrow="学习路径"
    title="这轮学习会按这条路径推进"
    description="先确认从哪里开始，再沿着节点顺序逐步进入后续内容。当前起点和下一步都已经为你标出来了。"
  >
    <div v-if="props.focuses?.length" class="mb-4 flex flex-wrap gap-2">
      <span v-for="focus in props.focuses" :key="focus" class="rounded-full bg-sky-50 px-3 py-1 text-xs font-medium text-sky-700">
        {{ focus }}
      </span>
    </div>

    <div v-if="currentNode" class="mb-5 grid gap-3 md:grid-cols-3">
      <div class="rounded-[1.5rem] border border-slate-950 bg-slate-950 p-4 text-white">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-300">从这里开始</p>
        <p class="mt-3 text-base font-semibold text-white">{{ getNodeName(currentNode) }}</p>
        <p class="mt-2 text-sm leading-6 text-slate-200">这是系统判断当前最适合优先进入的起点节点。</p>
      </div>
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">当前先解决什么</p>
        <p class="mt-3 text-base font-semibold text-slate-950">{{ currentNode.reasonTags[0] || '先补齐这轮最关键的薄弱点' }}</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">确认后会围绕这个节点展开第一步学习任务。</p>
      </div>
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">接下来会进入</p>
        <p class="mt-3 text-base font-semibold text-slate-950">{{ nextNode ? getNodeName(nextNode) : '当前节点后的下一阶段' }}</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          {{ nextNode ? '学完起点内容后，系统会继续带你进入后续阶段。' : '即使当前只返回一个节点，也会以它作为本轮正式起点展开学习。' }}
        </p>
      </div>
    </div>
    <div v-else class="mb-5 rounded-[1.6rem] border border-slate-200 bg-slate-50 px-5 py-5 text-sm leading-7 text-slate-600">
      路径节点还在整理中，确认开始后系统会按你的诊断结果补全起点与推进顺序。
    </div>

    <div class="mb-5 rounded-[1.6rem] border border-slate-200 bg-slate-50 px-4 py-4 text-sm leading-7 text-slate-600 md:px-5">
      <p class="font-semibold text-slate-900">路径总览</p>
      <p class="mt-1 break-words">{{ pathSummary || '系统会根据你的当前起点，依次安排本轮学习路径。' }}</p>
    </div>

    <div class="overflow-hidden rounded-[2rem] border border-slate-200 bg-[linear-gradient(180deg,#ffffff_0%,#f8fafc_100%)] p-5 shadow-[0_18px_60px_rgba(15,23,42,0.05)] md:p-6">
      <div v-if="props.nodes.length" class="flex flex-col gap-4 xl:flex-row xl:items-stretch">
        <article
          v-for="(node, index) in props.nodes"
          :key="node.node.id"
          class="relative flex-1 rounded-[1.8rem] border px-5 py-5 shadow-[0_14px_40px_rgba(15,23,42,0.05)]"
          :class="getNodeCardClass(node)"
        >
          <div
            v-if="index < props.nodes.length - 1"
            class="pointer-events-none absolute -bottom-2 left-1/2 h-5 w-[2px] -translate-x-1/2 bg-slate-200 xl:-right-3 xl:bottom-auto xl:left-auto xl:top-1/2 xl:h-[2px] xl:w-6 xl:-translate-y-1/2 xl:translate-x-0"
          />
          <div class="flex items-center justify-between gap-3">
            <span
              class="rounded-full px-3 py-1 text-xs font-semibold"
              :class="node.isStartingPoint || node.isFocus ? 'bg-white text-slate-950' : 'bg-slate-950 text-white'"
            >
              {{ getNodeRole(node, index) }}
            </span>
            <span class="text-xs font-medium uppercase tracking-[0.18em]" :class="node.isStartingPoint || node.isFocus ? 'text-slate-300' : 'text-slate-400'">步骤 {{ index + 1 }}</span>
          </div>

          <h3 class="mt-5 text-lg font-semibold tracking-tight" :class="node.isStartingPoint || node.isFocus ? 'text-white' : 'text-slate-950'">{{ getNodeName(node) }}</h3>
          <p class="mt-3 text-sm leading-6" :class="node.isStartingPoint || node.isFocus ? 'text-slate-200' : 'text-slate-600'">
            {{ node.reasonTags[0] || (index === 0 ? '这是本轮学习路线的起点节点。' : '这是路径中的后续学习内容。') }}
          </p>

          <div class="mt-5 flex flex-wrap gap-2">
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="getTagClass(node)">
              {{ PATH_STATUS_LABELS[node.masteryStatus] }}
            </span>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="getTagClass(node)">
              {{ PATH_DIFFICULTY_LABELS[node.difficulty] }}
            </span>
            <span class="rounded-full px-3 py-1 text-xs font-medium" :class="getTagClass(node)">
              {{ node.estimatedNodeMinutes }} 分钟
            </span>
          </div>
        </article>
      </div>
      <div v-else class="rounded-[1.6rem] border border-dashed border-slate-200 bg-white/90 px-5 py-6 text-sm leading-7 text-slate-600">
        暂未返回完整路径卡片，本轮仍会从推荐起点开始，后续内容会在学习过程中逐步明确。
      </div>
    </div>
  </PageSection>
</template>
