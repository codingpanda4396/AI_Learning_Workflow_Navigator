<template>
  <div data-testid="structure-hint-reveal" class="space-y-2">
    <div class="flex flex-wrap gap-2">
      <button
        v-for="item in panels"
        :key="item.key"
        type="button"
        class="rounded-full border px-3 py-1.5 text-xs font-medium transition"
        :class="
          openKey === item.key
            ? 'border-primary/40 bg-primary/10 text-primary'
            : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
        "
        @click="toggle(item.key)"
      >
        {{ item.label }}
      </button>
    </div>
    <div
      v-if="openKey && activeBody"
      class="rounded-xl border border-slate-100 bg-slate-50/90 px-4 py-3 text-sm leading-6 text-slate-700"
    >
      {{ activeBody }}
    </div>
    <div
      v-else-if="openKey && !activeBody"
      class="rounded-xl border border-dashed border-slate-200 bg-slate-50/50 px-4 py-3 text-sm text-slate-500"
    >
      {{ TASKRUN_COPY.hintRevealEmpty }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { TASKRUN_COPY } from '@/constants/uiCopy'
import type { WorkbenchHintRevealModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  hintReveal: WorkbenchHintRevealModel
}>()

type PanelKey = 'tips' | 'example' | 'pitfalls'

const openKey = ref<PanelKey | null>(null)

const panels = computed(() => [
  { key: 'tips' as const, label: TASKRUN_COPY.hintRevealTips },
  { key: 'example' as const, label: TASKRUN_COPY.hintRevealExample },
  { key: 'pitfalls' as const, label: TASKRUN_COPY.hintRevealPitfalls },
])

const activeBody = computed(() => {
  if (!openKey.value) return ''
  return props.hintReveal[openKey.value]?.trim() || ''
})

function toggle(key: PanelKey) {
  openKey.value = openKey.value === key ? null : key
}
</script>
