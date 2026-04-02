<template>
  <div
    class="overflow-hidden rounded-xl border border-border bg-white shadow-card transition duration-200 ease-out hover:shadow-card-hover"
  >
    <div class="h-1 bg-phase-understanding" aria-hidden="true" />
    <div class="border-b border-border bg-primary-muted/60 px-4 py-3">
      <p class="text-sm font-semibold text-text-primary">学习脚手架</p>
      <p class="mt-1 text-xs leading-relaxed text-text-muted">
        {{ panelHint }}
      </p>
    </div>

    <div class="max-h-[min(520px,62vh)] overflow-y-auto p-4">
      <div v-for="group in groupedButtons" :key="group.name" class="mb-5 last:mb-0">
        <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-text-secondary">
          {{ group.name }}
        </p>
        <div class="space-y-2">
          <button
            v-for="btn in group.items"
            :key="btn.id"
            type="button"
            class="group relative flex w-full items-center gap-2 rounded-md border border-border bg-white px-3 py-2.5 text-left text-sm transition hover:border-accent/35 hover:bg-accent-muted/40 disabled:opacity-50"
            :disabled="busy || injectingId === btn.id"
            @click="handleInject(btn)"
          >
            <span
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-slate-100 text-phase-understanding transition group-hover:bg-white"
            >
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path
                  d="M10.394 2.08a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A2 2 0 009 15.571V11a1 1 0 112 0v4.571a2 2 0 001.087 1.79l5 1.428a1 1 0 001.17-1.408l-7-14z"
                />
              </svg>
            </span>
            <span class="min-w-0 flex-1 font-medium text-text-primary">{{ btn.title }}</span>
            <span v-if="injectingId === btn.id" class="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-accent border-t-transparent" />
            <span
              v-else-if="successId === btn.id"
              class="shrink-0 text-success"
              aria-hidden="true"
            >
              <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fill-rule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ScaffoldButton } from '@/types/executionWorkbench'

const props = withDefaults(
  defineProps<{
    buttons: ScaffoldButton[]
    busy: boolean
    /** 顶栏说明；默认兼容旧文案 */
    panelHint?: string
  }>(),
  {
    panelHint: '点击后将写入左侧输入区，作为你的追问起点（不展示技术指令全文）。',
  },
)

const emit = defineEmits<{
  inject: [prompt: string, scaffoldKey: string]
}>()

const injectingId = ref<string | null>(null)
const successId = ref<string | null>(null)

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
  if (props.busy) return
  injectingId.value = btn.id
  emit('inject', btn.injectPrompt, btn.id)
  window.setTimeout(() => {
    injectingId.value = null
    successId.value = btn.id
    window.setTimeout(() => {
      successId.value = null
    }, 650)
  }, 320)
}
</script>
