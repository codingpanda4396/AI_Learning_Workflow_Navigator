<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import { PATH_DIFFICULTY_LABELS, PATH_STATUS_LABELS } from '@/constants/learningPlan';
import type { PlanPathNode } from '@/types/learningPlan';

const props = defineProps<{
  nodes: PlanPathNode[];
  focuses?: string[];
}>();

function getNodeRole(node: PlanPathNode, index: number) {
  if (node.isStartingPoint) return '推荐起点';
  if (node.isPrerequisite) return '前置知识';
  if (node.isFocus) return '当前重点';
  if (index === props.nodes.length - 1) return '后续扩展';
  return '衔接节点';
}

function getNodeName(node: PlanPathNode) {
  return node.node.displayName || node.node.nodeName;
}
</script>

<template>
  <PageSection
    eyebrow="路径"
    title="预览学习路径的编排顺序"
    description="节点名称优先使用 displayName，其次 nodeName。难度与掌握度由 code-label 映射驱动。"
  >
    <div v-if="props.focuses?.length" class="mb-4 flex flex-wrap gap-2">
      <span v-for="focus in props.focuses" :key="focus" class="rounded-full bg-sky-50 px-3 py-1 text-xs font-medium text-sky-700">
        {{ focus }}
      </span>
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
            {{ node.reasonTags[0] || '根据后端路径预览推理定位。' }}
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
