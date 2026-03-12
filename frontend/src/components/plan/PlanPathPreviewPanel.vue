<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import { PATH_DIFFICULTY_LABELS, PATH_STATUS_LABELS } from '@/constants/learningPlan';
import type { PlanPathNode } from '@/types/learningPlan';

defineProps<{
  nodes: PlanPathNode[];
}>();
</script>

<template>
  <PageSection
    eyebrow="学习路径预览"
    title="这一轮会沿着什么路径推进"
    description="不是把所有内容平铺给你，而是把前置节点、推荐起点和本轮主攻方向串成一条可执行路径。"
  >
    <div class="grid gap-4 xl:grid-cols-4">
      <article
        v-for="(node, index) in nodes"
        :key="node.id"
        class="relative rounded-[1.7rem] border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.05)]"
      >
        <div
          v-if="index < nodes.length - 1"
          class="pointer-events-none absolute -right-3 top-1/2 hidden h-[2px] w-6 -translate-y-1/2 bg-slate-200 xl:block"
        />
        <div class="flex flex-wrap items-center gap-2">
          <span v-if="node.isStartingPoint" class="rounded-full bg-sky-100 px-3 py-1 text-xs font-semibold text-sky-700">
            推荐起点
          </span>
          <span v-if="node.isPrerequisite" class="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-700">
            前置节点
          </span>
          <span v-if="node.isFocus" class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
            主攻方向
          </span>
        </div>

        <h3 class="mt-4 text-lg font-semibold tracking-tight text-slate-950">{{ node.name }}</h3>

        <div class="mt-4 flex flex-wrap gap-2">
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            {{ PATH_STATUS_LABELS[node.masteryStatus] }}
          </span>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            {{ PATH_DIFFICULTY_LABELS[node.difficulty] }}
          </span>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            约 {{ node.estimatedMinutes }} 分钟
          </span>
        </div>

        <div class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="tag in node.reasonTags"
            :key="tag"
            class="rounded-full border border-slate-200 px-3 py-1 text-xs font-medium text-slate-500"
          >
            {{ tag }}
          </span>
        </div>
      </article>
    </div>
  </PageSection>
</template>
