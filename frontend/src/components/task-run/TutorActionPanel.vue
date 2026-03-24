<template>
  <aside class="space-y-4">
    <section
      class="rounded-[28px] border border-emerald-200 bg-[radial-gradient(circle_at_top_right,_rgba(16,185,129,0.18),_transparent_42%),linear-gradient(180deg,_rgba(240,253,250,1),_rgba(255,255,255,1))] p-5 shadow-card"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-700">
        Protocol Status
      </p>
      <h2 class="mt-3 text-lg font-semibold text-slate-900">
        {{ currentStatus }}
      </h2>
      <p class="mt-2 text-sm leading-6 text-slate-700">
        {{ nextStepLine }}
      </p>

      <div class="mt-4 rounded-[20px] border border-white/80 bg-white/85 p-4">
        <p class="text-sm font-semibold text-slate-900">通过条件</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">
          {{ passCondition }}
        </p>
      </div>

      <div class="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-1">
        <div class="rounded-[20px] border border-emerald-100 bg-white/80 p-4">
          <p class="text-sm font-semibold text-slate-900">已完成的点</p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-slate-700">
            <li v-for="(item, index) in completedPoints" :key="`done-${index}`">
              {{ item }}
            </li>
          </ul>
        </div>
        <div class="rounded-[20px] border border-amber-100 bg-amber-50/70 p-4">
          <p class="text-sm font-semibold text-amber-900">还缺少的点</p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-amber-900">
            <li v-for="(item, index) in missingPoints" :key="`miss-${index}`">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>
    </section>

    <details class="rounded-[24px] border border-border bg-white p-5 shadow-card" open>
      <summary class="cursor-pointer text-sm font-semibold text-text-primary">
        查看本步导师策略
      </summary>
      <div class="mt-4 space-y-4 border-t border-border pt-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
            当前教学模式
          </p>
          <p class="mt-2 text-sm font-semibold text-text-primary">
            {{ strategyMode }}
          </p>
        </div>

        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
            提问顺序
          </p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
            <li v-for="(item, index) in strategySequence" :key="`seq-${index}`">
              {{ item }}
            </li>
          </ul>
        </div>

        <div v-if="guidanceTitle || guidanceBullets.length">
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
            当前干预依据
          </p>
          <p v-if="guidanceTitle" class="mt-2 text-sm font-semibold text-text-primary">
            {{ guidanceTitle }}
          </p>
          <ul
            v-if="guidanceBullets.length"
            class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary"
          >
            <li v-for="(bullet, index) in guidanceBullets" :key="`guide-${index}`">
              {{ bullet }}
            </li>
          </ul>
        </div>
      </div>
    </details>

    <section class="rounded-[24px] border border-border bg-white p-5 shadow-card">
      <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
        Tutor Actions
      </p>
      <div class="mt-4 space-y-3">
        <button
          v-for="action in actions"
          :key="action.id"
          type="button"
          class="w-full rounded-[18px] border border-border bg-slate-50/70 px-4 py-3 text-left transition hover:border-primary/35 hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="disabled"
          @click="$emit('select', action.id)"
        >
          <p class="text-sm font-semibold text-text-primary">
            {{ action.label }}
          </p>
          <p class="mt-1 text-xs leading-5 text-text-secondary">
            {{ action.description }}
          </p>
        </button>
      </div>
    </section>

    <section
      v-if="recommendedActions.length"
      class="rounded-[24px] border border-border bg-white p-5 shadow-card"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
        系统建议你这样答
      </p>
      <div class="mt-4 flex flex-wrap gap-2">
        <StatusBadge
          v-for="action in recommendedActions"
          :key="action.code"
          :label="action.label"
        />
      </div>
    </section>
  </aside>
</template>

<script setup lang="ts">
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { RecommendedUserActionItem } from '@/types/dto'

interface TutorActionConfig {
  id: string
  label: string
  description: string
}

withDefaults(
  defineProps<{
    guidanceTitle?: string
    guidanceBullets?: string[]
    currentStatus: string
    completedPoints: string[]
    missingPoints: string[]
    nextStepLine: string
    passCondition: string
    strategyMode: string
    strategySequence: string[]
    actions: TutorActionConfig[]
    recommendedActions?: RecommendedUserActionItem[]
    disabled?: boolean
  }>(),
  {
    guidanceTitle: '',
    guidanceBullets: () => [],
    recommendedActions: () => [],
    disabled: false,
  }
)

defineEmits<{
  select: [id: string]
}>()
</script>
