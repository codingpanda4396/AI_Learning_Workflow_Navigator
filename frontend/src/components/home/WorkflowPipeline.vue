<script setup lang="ts">
import type { WorkflowNode, WorkflowStatus } from '@/mocks/home';

defineProps<{
  nodes: WorkflowNode[];
}>();

const statusLabelMap: Record<WorkflowStatus, string> = {
  DONE: '已完成',
  RUNNING: '进行中',
  PENDING: '待开始',
};

function getNodeClass(status: WorkflowStatus) {
  if (status === 'RUNNING') {
    return 'bg-slate-950 text-white shadow-[0_18px_40px_rgba(15,23,42,0.18)]';
  }
  if (status === 'DONE') {
    return 'bg-emerald-50 text-emerald-800 ring-1 ring-emerald-200';
  }
  return 'bg-slate-100 text-slate-500';
}

function getBadgeClass(status: WorkflowStatus) {
  if (status === 'RUNNING') {
    return 'bg-white/10 text-white';
  }
  if (status === 'DONE') {
    return 'bg-white text-emerald-700';
  }
  return 'bg-white text-slate-500';
}

function getArrowClass(status: WorkflowStatus) {
  if (status === 'DONE') {
    return 'text-emerald-300';
  }
  if (status === 'RUNNING') {
    return 'text-slate-400';
  }
  return 'text-slate-300';
}
</script>

<template>
  <section class="rounded-[2rem] bg-white p-6 shadow-[0_18px_50px_rgba(15,23,42,0.06)] ring-1 ring-slate-200/70 md:p-7">
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Workflow Pipeline</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">学习流程</h2>
        <p class="mt-2 text-sm leading-6 text-slate-600">从能力诊断到知识沉淀，系统会持续优化你的学习路径。</p>
      </div>
      <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">当前学习流程</div>
    </div>

    <div class="mt-6 hidden items-center gap-3 overflow-x-auto pb-2 xl:flex">
      <template v-for="(node, index) in nodes" :key="node.key">
        <div class="min-w-[180px] flex-1 rounded-[1.5rem] px-4 py-4 transition" :class="getNodeClass(node.status)">
          <div class="flex items-center justify-between gap-3">
            <span class="text-sm font-semibold">{{ node.label }}</span>
            <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold tracking-[0.12em]" :class="getBadgeClass(node.status)">
              {{ statusLabelMap[node.status] }}
            </span>
          </div>
        </div>
        <span v-if="index < nodes.length - 1" class="text-xl" :class="getArrowClass(node.status)">→</span>
      </template>
    </div>

    <div class="mt-6 grid gap-3 xl:hidden">
      <div
        v-for="(node, index) in nodes"
        :key="node.key"
        class="rounded-[1.5rem] px-4 py-4 transition"
        :class="getNodeClass(node.status)"
      >
        <div class="flex items-center justify-between gap-3">
          <span class="text-sm font-semibold">{{ index + 1 }}. {{ node.label }}</span>
          <span class="rounded-full px-2.5 py-1 text-[11px] font-semibold tracking-[0.12em]" :class="getBadgeClass(node.status)">
            {{ statusLabelMap[node.status] }}
          </span>
        </div>
      </div>
    </div>
  </section>
</template>
