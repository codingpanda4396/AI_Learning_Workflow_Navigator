<template>
  <div class="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm">
    <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
      提问脚手架
    </p>

    <div v-for="group in groupedButtons" :key="group.name" class="mb-4 last:mb-0">
      <p class="mb-2 text-[11px] font-medium text-slate-500">{{ group.name }}</p>
      <div class="space-y-1.5">
        <button
          v-for="btn in group.items"
          :key="btn.id"
          type="button"
          class="group relative flex w-full items-center gap-2 rounded-lg border border-slate-200 px-3 py-2.5 text-left text-sm transition-all hover:border-primary/40 hover:bg-primary/5"
          :disabled="busy"
          @click="handleInject(btn)"
        >
          <svg
            class="h-4 w-4 shrink-0 text-slate-400 transition group-hover:text-primary"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-11a1 1 0 10-2 0v3.586L7.707 9.293a1 1 0 00-1.414 1.414l3 3a1 1 0 001.414 0l3-3a1 1 0 00-1.414-1.414L11 10.586V7z"
              clip-rule="evenodd"
            />
          </svg>
          <span class="font-medium text-slate-700 group-hover:text-primary">{{ btn.title }}</span>

          <div
            class="pointer-events-none absolute bottom-full left-0 z-10 mb-2 w-64 rounded-lg border border-slate-200 bg-white p-3 text-xs leading-relaxed text-slate-600 opacity-0 shadow-lg transition-opacity group-hover:opacity-100"
          >
            {{ truncatePrompt(btn.injectPrompt) }}
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ScaffoldButton } from '@/types/executionWorkbench'

const props = defineProps<{
  buttons: ScaffoldButton[]
  busy: boolean
}>()

const emit = defineEmits<{
  inject: [prompt: string, scaffoldKey: string]
}>()

interface ButtonGroup {
  name: string
  items: ScaffoldButton[]
}

const groupedButtons = computed<ButtonGroup[]>(() => {
  const map = new Map<string, ScaffoldButton[]>()
  for (const btn of props.buttons) {
    const list = map.get(btn.group) ?? []
    list.push(btn)
    map.set(btn.group, list)
  }
  return Array.from(map.entries()).map(([name, items]) => ({ name, items }))
})

function handleInject(btn: ScaffoldButton) {
  emit('inject', btn.injectPrompt, btn.id)
}

function truncatePrompt(text: string): string {
  return text.length > 80 ? text.slice(0, 77) + '…' : text
}
</script>
