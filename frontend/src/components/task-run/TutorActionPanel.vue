<template>
  <aside class="space-y-4">
    <section
      class="rounded-[28px] border border-amber-200 bg-[radial-gradient(circle_at_top_right,_rgba(245,158,11,0.22),_transparent_42%),linear-gradient(180deg,_rgba(255,251,235,1),_rgba(255,255,255,1))] p-5 shadow-card"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.24em] text-amber-700">
        AI Tutor
      </p>
      <h2 class="mt-3 text-lg font-semibold text-slate-900">
        {{ guidancePhaseLabel ? `${guidancePhaseLabel} · 受约束辅助` : '受约束辅助' }}
      </h2>
      <p class="mt-2 text-sm leading-6 text-slate-700">
        {{ description }}
      </p>
      <div
        v-if="guidanceTitle || guidanceBullets.length"
        class="mt-4 rounded-[20px] border border-white/70 bg-white/80 p-4"
      >
        <p v-if="guidanceTitle" class="text-sm font-semibold text-slate-900">
          {{ guidanceTitle }}
        </p>
        <ul
          v-if="guidanceBullets.length"
          class="mt-2 list-disc space-y-2 pl-5 text-sm leading-6 text-slate-700"
        >
          <li v-for="(bullet, index) in guidanceBullets" :key="index">
            {{ bullet }}
          </li>
        </ul>
      </div>
    </section>

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
        系统推荐动作
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
    guidancePhaseLabel?: string
    guidanceTitle?: string
    guidanceBullets?: string[]
    description?: string
    actions: TutorActionConfig[]
    recommendedActions?: RecommendedUserActionItem[]
    disabled?: boolean
  }>(),
  {
    guidancePhaseLabel: '',
    guidanceTitle: '',
    guidanceBullets: () => [],
    description: '不直接给完整答案，只帮助你把当前任务继续推进。',
    recommendedActions: () => [],
    disabled: false,
  }
)

defineEmits<{
  select: [id: string]
}>()
</script>
