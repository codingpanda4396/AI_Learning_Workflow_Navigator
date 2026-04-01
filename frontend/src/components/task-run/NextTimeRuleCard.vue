<template>
  <div class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
    <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
      下次判断规则
    </p>

    <div class="space-y-2">
      <button
        v-for="strategy in strategies"
        :key="strategy.id"
        type="button"
        class="flex w-full items-center gap-3 rounded-xl border-2 px-4 py-3 text-left text-sm transition-all"
        :class="isSelected(strategy.id)
          ? 'border-primary bg-primary/5 text-primary font-medium'
          : 'border-slate-200 text-slate-700 hover:border-primary/30 hover:bg-primary/5'"
        @click="toggleStrategy(strategy.id)"
      >
        <span
          class="flex h-5 w-5 shrink-0 items-center justify-center rounded-md border-2 transition-colors"
          :class="isSelected(strategy.id)
            ? 'border-primary bg-primary text-white'
            : 'border-slate-300'"
        >
          <svg
            v-if="isSelected(strategy.id)"
            class="h-3 w-3"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clip-rule="evenodd"
            />
          </svg>
        </span>
        <span>{{ strategy.label }}</span>
      </button>
    </div>

    <div class="mt-4">
      <textarea
        v-model="localReflection"
        :placeholder="placeholder"
        rows="2"
        class="w-full rounded-xl border-2 border-slate-200 bg-slate-50/50 px-4 py-3 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-primary/40 focus:bg-white"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ReflectionStrategy } from '@/types/executionWorkbench'

const props = defineProps<{
  strategies: ReflectionStrategy[]
  selectedIds: string[]
  reflectionText: string
  placeholder: string
}>()

const emit = defineEmits<{
  'toggle-strategy': [strategyId: string]
  'update:reflectionText': [value: string]
}>()

const localReflection = ref(props.reflectionText)

watch(
  () => props.reflectionText,
  (v) => {
    if (v !== localReflection.value) localReflection.value = v
  },
)

watch(localReflection, (v) => {
  emit('update:reflectionText', v)
})

function isSelected(id: string): boolean {
  return props.selectedIds.includes(id)
}

function toggleStrategy(id: string) {
  emit('toggle-strategy', id)
}
</script>
