<template>
  <aside class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">认知动作库</p>
    <p class="mt-1 text-sm font-semibold text-slate-900">{{ phaseLabel }}</p>
    <div class="mt-3 space-y-2">
      <p v-if="actions.length === 0" class="text-xs text-slate-500">暂无可用动作，先完成左侧主任务。</p>
      <button
        v-for="a in actions"
        :key="a.id"
        type="button"
        class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-left text-sm font-medium text-slate-900 transition hover:border-accent/40 hover:bg-accent-muted/50"
        :disabled="busy"
        @click="$emit('append-explanation', a.id)"
      >
        {{ a.title }}
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WorkbenchScaffoldAction } from '@/types/phaseWorkbench'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

const props = withDefaults(
  defineProps<{
    phase: WorkbenchPhaseCode
    actions: WorkbenchScaffoldAction[]
    busy?: boolean
  }>(),
  { busy: false }
)

defineEmits<{
  'append-explanation': [actionId: string]
}>()

const phaseLabel = computed(() =>
  props.phase === 'TRAINING' || props.phase === 'REFLECTION' ? '表达辅助' : '理解辅助'
)
</script>
