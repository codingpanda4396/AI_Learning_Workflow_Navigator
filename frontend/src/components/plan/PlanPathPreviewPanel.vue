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

function getNodeRole(node: PlanPathNode, index: number) {
  if (node.isStartingPoint) return '本轮起点';
  if (node.isPrerequisite) return '前置补齐';
  if (node.isFocus) return '当前重点';
  if (index === props.nodes.length - 1) return '后续进阶';
  return '路径衔接';
}

function getNodeName(node: PlanPathNode) {
  return node.node.displayName || node.node.nodeName;
}
</script>

<template>
  <PageSection
    eyebrow="学习路径"
    title="本轮学习路线"
    description="你会从当前更适合的起点开始，按顺序进入后续知识内容和学习阶段。"
  >
    <div v-if="props.focuses?.length" class="mb-4 flex flex-wrap gap-2">
      <span v-for="focus in props.focuses" :key="focus" class="rounded-full bg-sky-50 px-3 py-1 text-xs font-medium text-sky-700">
        {{ focus }}
      </span>
    </div>

    <div v-if="currentNode" class="mb-5 grid gap-3 md:grid-cols-3">
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">从哪里开始</p>
        <p class="mt-3 text-base font-semibold text-slate-950">{{ getNodeName(currentNode) }}</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">这是系统判断当前最适合先进入的起点节点。</p>
      </div>
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">当前要解决什么</p>
        <p class="mt-3 text-base font-semibold text-slate-950">{{ currentNode.reasonTags[0] || '先补齐本轮最关键的薄弱点' }}</p>
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

    <div class="overflow-hidden rounded-[2rem] border border-slate-200 bg-[linear-gradient(180deg,#ffffff_0%,#f8fafc_100%)] p-5 shadow-[0_18px_60px_rgba(15,23,42,0.05)] md:p-6">
      <div class="flex flex-col gap-4 xl:flex-row xl:items-stretch">
        <article
          v-for="(node, index) in props.nodes"
          :key="node.node.id"
          class="relative flex-1 rounded-[1.8rem] border border-slate-200/90 bg-white px-5 py-5 shadow-[0_14px_40px_rgba(15,23,42,0.05)]"
        >
          <div
            v-if="index < props.nodes.length - 1"
            class="pointer-events-none absolute -bottom-2 left-1/2 h-5 w-[2px] -translate-x-1/2 bg-slate-200 xl:-right-3 xl:bottom-auto xl:left-auto xl:top-1/2 xl:h-[2px] xl:w-6 xl:-translate-y-1/2 xl:translate-x-0"
          />
          <div class="flex items-center justify-between gap-3">
            <span class="rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold text-white">
              {{ getNodeRole(node, index) }}
            </span>
            <span class="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">步骤 {{ index + 1 }}</span>
          </div>

          <h3 class="mt-5 text-lg font-semibold tracking-tight text-slate-950">{{ getNodeName(node) }}</h3>
          <p class="mt-3 text-sm leading-6 text-slate-600">
            {{ node.reasonTags[0] || (index === 0 ? '这是本轮学习路线的起点节点。' : '这是路径中的后续学习内容。') }}
          </p>

          <div class="mt-5 flex flex-wrap gap-2">
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
              {{ PATH_STATUS_LABELS[node.masteryStatus] }}
            </span>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
              {{ PATH_DIFFICULTY_LABELS[node.difficulty] }}
            </span>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
              {{ node.estimatedNodeMinutes }} 分钟
            </span>
          </div>
        </article>
      </div>
    </div>
  </PageSection>
</template>
