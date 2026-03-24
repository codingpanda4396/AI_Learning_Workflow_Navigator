<template>
  <section class="space-y-4">
    <h2 class="text-base font-semibold text-text-primary">接下来具体会做什么</h2>
    <p v-if="activeGroup" class="text-sm text-text-secondary">
      每张卡只保留三件事：你要做什么、系统怎么帮、过关看什么。
    </p>

    <div class="space-y-3">
      <article
        v-for="group in orderedGroups"
        :key="group.stageCode"
        class="overflow-hidden rounded-2xl border bg-white shadow-sm"
        :class="
          group.stageCode === expandedStageCode
            ? 'border-indigo-200/90'
            : 'border-slate-200/80'
        "
      >
        <template v-if="group.stageCode === expandedStageCode">
          <header
            class="flex flex-wrap items-center justify-between gap-2 border-b border-indigo-100 bg-indigo-50/50 px-4 py-3"
          >
            <div>
              <p class="font-mono text-[11px] font-semibold text-indigo-700">
                {{ group.stageCode }}
              </p>
              <h3 class="text-lg font-semibold text-text-primary">
                {{ group.stageLabel }}
              </h3>
            </div>
            <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold text-indigo-800">
              {{ group.items.length }} 个任务
            </span>
          </header>
          <div class="space-y-3 p-4">
            <div
              v-for="item in group.items"
              :key="item.taskId"
              class="flex flex-col gap-4 rounded-[20px] border border-slate-100 bg-slate-50/70 p-4"
            >
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <h4 class="font-semibold text-text-primary">
                    {{ item.title }}
                  </h4>
                  <p class="mt-2 text-xs text-slate-500">
                    {{ item.estimatedTime }}
                  </p>
                </div>
                <SecondaryButton
                  class="shrink-0"
                  :disabled="loading"
                  @click="$emit('start')"
                >
                  进入任务
                </SecondaryButton>
              </div>
              <div class="grid gap-3 md:grid-cols-3">
                <div class="rounded-2xl border border-white bg-white px-4 py-3">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                    你要做什么
                  </p>
                  <p class="mt-2 text-sm leading-6 text-text-primary">
                    {{ oneLineGoal(item.actionGoal, 80) }}
                  </p>
                </div>
                <div class="rounded-2xl border border-white bg-white px-4 py-3">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                    系统怎么帮
                  </p>
                  <p class="mt-2 text-sm leading-6 text-text-primary">
                    {{ oneLineGoal(item.tutorSupport, 80) }}
                  </p>
                </div>
                <div class="rounded-2xl border border-white bg-white px-4 py-3">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                    过关看什么
                  </p>
                  <p class="mt-2 text-sm leading-6 text-text-primary">
                    {{ item.completionChecks[0] ?? '按这一阶段要求完成最小产出。' }}
                  </p>
                </div>
              </div>
            </div>
            <div
              v-if="group.items.length === 0"
              class="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 p-4 text-sm text-text-secondary"
            >
              本阶段没有单独拆任务，跟着上一步的引导继续即可。
            </div>
          </div>
        </template>
        <div
          v-else
          class="flex w-full flex-wrap items-center justify-between gap-2 px-4 py-3 text-left opacity-[0.92]"
        >
          <div>
            <p class="font-mono text-[10px] font-semibold text-slate-400">
              {{ group.stageCode }}
            </p>
            <p class="text-sm font-semibold text-text-primary">
              {{ group.stageLabel }}
            </p>
            <p class="mt-0.5 text-xs text-text-secondary">
              {{ collapsedSummary(group.stageCode) }}
            </p>
          </div>
          <span class="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
            {{ group.items.length }} 个任务
          </span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import type {
  PlanStageCode,
  PlanTaskGroupView,
} from '@/utils/planPresentationModel'

const props = defineProps<{
  groups: PlanTaskGroupView[]
  expandedStageCode: PlanStageCode
  loading?: boolean
}>()

defineEmits<{
  start: []
}>()

const SCAN: Record<PlanStageCode, string> = {
  STRUCTURE: '先把整体框架搭起来',
  UNDERSTANDING: '再理解关键机制',
  TRAINING: '再做题验证理解',
  REFLECTION: '最后复盘薄弱点',
}

function collapsedSummary(code: PlanStageCode): string {
  return SCAN[code]
}

function oneLineGoal(text: string, max = 52): string {
  const t = text.trim()
  if (!t) return '按引导完成本步即可。'
  if (t.length <= max) return t
  return t.slice(0, max) + '…'
}

const activeGroup = computed(() =>
  props.groups.find((g) => g.stageCode === props.expandedStageCode)
)

/** 当前展开阶段置顶，其余保持原顺序 */
const orderedGroups = computed(() => {
  const ex = props.expandedStageCode
  const head = props.groups.filter((g) => g.stageCode === ex)
  const tail = props.groups.filter((g) => g.stageCode !== ex)
  return [...head, ...tail]
})
</script>
