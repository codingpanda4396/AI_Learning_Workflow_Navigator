<template>
  <section class="space-y-4">
    <div class="space-y-1">
      <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-400">
        Details
      </p>
      <h2 class="text-2xl font-semibold tracking-tight text-slate-950">
        每一步具体做什么
      </h2>
    </div>

    <div class="space-y-3">
      <article
        v-for="panel in panels"
        :key="panel.code"
        class="overflow-hidden rounded-[26px] border bg-white shadow-[0_10px_28px_rgba(15,23,42,0.05)]"
        :class="expandedCode === panel.code ? 'border-sky-200' : 'border-slate-200'"
      >
        <button
          type="button"
          class="flex w-full items-center justify-between gap-4 px-5 py-4 text-left transition-colors hover:bg-slate-50"
          @click="expandedCode = panel.code"
        >
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <h3 class="text-lg font-semibold text-slate-950">
                {{ panel.titleZh }}
              </h3>
              <span class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                {{ panel.titleEn }}
              </span>
              <span
                v-if="panel.isCurrent"
                class="rounded-full bg-sky-100 px-2.5 py-1 text-[11px] font-semibold text-sky-700"
              >
                当前
              </span>
              <span
                v-else-if="panel.isRecommended"
                class="rounded-full bg-indigo-100 px-2.5 py-1 text-[11px] font-semibold text-indigo-700"
              >
                推荐起步
              </span>
            </div>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              {{ panel.objective }}
            </p>
          </div>

          <div class="shrink-0 text-right">
            <p class="text-sm font-medium text-slate-900">
              {{ panel.estimatedLabel }}
            </p>
            <p class="mt-1 text-xs text-slate-400">
              {{ panel.taskCount }} 个任务
            </p>
          </div>
        </button>

        <div
          v-if="expandedCode === panel.code"
          class="border-t border-slate-100 bg-slate-50/60 px-5 py-5"
        >
          <div class="grid gap-4 lg:grid-cols-[1.05fr_0.95fr]">
            <div class="space-y-4">
              <div class="rounded-2xl border border-white bg-white p-4">
                <p class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                  这一步目标
                </p>
                <p class="mt-3 text-sm leading-7 text-slate-700">
                  {{ panel.objective }}
                </p>
              </div>

              <div class="rounded-2xl border border-white bg-white p-4">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                    任务清单
                  </p>
                  <span class="text-xs font-medium text-slate-400">
                    {{ panel.taskCount }} 个任务
                  </span>
                </div>
                <div v-if="panel.tasks.length" class="mt-3 space-y-3">
                  <div
                    v-for="(task, index) in panel.tasks"
                    :key="task.taskId"
                    class="rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <p class="text-sm font-semibold text-slate-900">
                        任务 {{ index + 1 }}：{{ task.title }}
                      </p>
                      <span class="text-xs text-slate-400">
                        {{ task.estimatedLabel }}
                      </span>
                    </div>
                  </div>
                </div>
                <p v-else class="mt-3 text-sm leading-7 text-slate-600">
                  这一阶段没有额外拆分任务，按当前阶段目标推进即可。
                </p>
              </div>
            </div>

            <div class="space-y-4">
              <div class="rounded-2xl border border-white bg-white p-4">
                <p class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                  完成标准
                </p>
                <div class="mt-3 space-y-2">
                  <div
                    v-for="line in panel.completionStandard"
                    :key="line"
                    class="rounded-2xl bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700"
                  >
                    {{ line }}
                  </div>
                </div>
              </div>

              <div class="rounded-2xl border border-white bg-white p-4">
                <p class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                  可直接这样问
                </p>
                <div class="mt-3 space-y-2">
                  <div
                    v-for="prompt in panel.scaffoldPrompts"
                    :key="prompt"
                    class="rounded-2xl border border-sky-100 bg-sky-50/70 px-4 py-3 text-sm leading-6 text-slate-700"
                  >
                    {{ prompt }}
                  </div>
                </div>
              </div>

              <PrimaryButton
                v-if="panel.isCurrent || (!hasCurrentPanel && panel.isRecommended)"
                class="w-full justify-center py-3 text-base font-semibold shadow-[0_14px_30px_rgba(79,70,229,0.18)]"
                :loading="loading"
                @click="$emit('start')"
              >
                {{ actionLabel }}
              </PrimaryButton>
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { PlanStageCode, PlanStagePanelView } from '@/utils/planPresentationModel'

const props = defineProps<{
  panels: PlanStagePanelView[]
  loading?: boolean
  actionLabel: string
}>()

defineEmits<{
  start: []
}>()

const defaultExpanded = computed<PlanStageCode>(() => {
  return props.panels.find((panel) => panel.isExpandedDefault)?.code ?? 'STRUCTURE'
})

const expandedCode = ref<PlanStageCode>(defaultExpanded.value)

watch(
  () => props.panels,
  () => {
    expandedCode.value = defaultExpanded.value
  },
  { deep: true }
)

const hasCurrentPanel = computed(() => props.panels.some((panel) => panel.isCurrent))
</script>
