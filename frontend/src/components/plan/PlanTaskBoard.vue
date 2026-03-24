<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
          Task Breakdown
        </p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-text-primary">
          底部任务拆解
        </h2>
      </div>
      <p class="max-w-xl text-sm leading-6 text-text-secondary">
        这里展示的不是解释，而是把阶段直接落成动作卡。每张卡都告诉你要做什么、Tutor 会怎么帮、完成信号是什么。
      </p>
    </div>

    <div class="space-y-5">
      <article
        v-for="group in groups"
        :key="group.stageCode"
        class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_16px_40px_rgba(15,23,42,0.06)]"
      >
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-400">
              {{ group.stageCode }}
            </p>
            <h3 class="mt-1 text-xl font-semibold tracking-tight text-text-primary">
              {{ group.stageLabel }}
            </h3>
          </div>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
            {{ group.items.length }} 个任务
          </span>
        </div>

        <div class="mt-4 grid gap-4 xl:grid-cols-2">
          <div
            v-for="item in group.items"
            :key="item.taskId"
            class="rounded-[20px] border border-slate-200 bg-slate-50/70 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                  {{ item.stageCode }}
                </p>
                <h4 class="mt-1 text-lg font-semibold leading-7 text-text-primary">
                  {{ item.title }}
                </h4>
              </div>
              <span class="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600">
                {{ item.estimatedTime }}
              </span>
            </div>

            <dl class="mt-4 space-y-3">
              <div>
                <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
                  动作目标
                </dt>
                <dd class="mt-1 text-sm leading-6 text-text-primary">
                  {{ item.actionGoal }}
                </dd>
              </div>
              <div>
                <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
                  Tutor 支持方式
                </dt>
                <dd class="mt-1 text-sm leading-6 text-text-primary">
                  {{ item.tutorSupport }}
                </dd>
              </div>
              <div>
                <dt class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
                  完成检查
                </dt>
                <dd class="mt-1 space-y-1 text-sm leading-6 text-text-primary">
                  <p
                    v-for="check in item.completionChecks"
                    :key="check"
                  >
                    {{ check }}
                  </p>
                </dd>
              </div>
            </dl>
          </div>

          <div
            v-if="group.items.length === 0"
            class="rounded-[20px] border border-dashed border-slate-300 bg-slate-50/55 p-5 text-sm leading-7 text-text-secondary xl:col-span-2"
          >
            当前规划没有把这一阶段单独拆成任务卡，说明系统将它并入了前后动作；你仍然可以从上方阶段卡理解这一段的目标、产出和检查点。
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PlanTaskGroupView } from '@/utils/planPresentationModel'

defineProps<{
  groups: PlanTaskGroupView[]
}>()
</script>
