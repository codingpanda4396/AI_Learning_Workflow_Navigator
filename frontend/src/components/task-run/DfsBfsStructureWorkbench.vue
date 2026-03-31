<template>
  <section
    class="rounded-[28px] border border-slate-200/90 bg-white p-5 shadow-[0_24px_60px_-28px_rgba(15,23,42,0.22)] ring-1 ring-slate-900/[0.04] md:p-8"
    aria-label="DFS BFS 判断骨架工作台"
  >
    <div class="border-b border-slate-100 pb-5">
      <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">当前例题</p>
      <p class="mt-2 text-2xl font-semibold leading-snug tracking-tight text-slate-950 md:text-3xl">
        {{ exampleTitle }}
      </p>
    </div>

    <!-- 第一步：仅选方向 -->
    <div v-if="modelValue.flowStep === 'direction'" class="mt-8">
      <p class="text-sm font-semibold text-slate-900">选方向</p>
      <div class="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4" role="radiogroup" aria-label="DFS 或 BFS">
        <button
          type="button"
          class="min-h-[3.5rem] rounded-2xl border-2 px-6 py-4 text-center text-lg font-bold transition sm:min-h-[4rem] sm:text-xl"
          :class="directionBtnClass(modelValue.direction === 'DFS')"
          :aria-pressed="modelValue.direction === 'DFS'"
          :disabled="disabled"
          @click="onPickDfs()"
        >
          DFS
        </button>
        <button
          type="button"
          class="min-h-[3.5rem] rounded-2xl border-2 px-6 py-4 text-center text-lg font-bold transition sm:min-h-[4rem] sm:text-xl"
          :class="directionBtnClass(modelValue.direction === 'BFS')"
          :aria-pressed="modelValue.direction === 'BFS'"
          :disabled="disabled"
          @click="onPickBfs()"
        >
          BFS
        </button>
      </div>
    </div>

    <!-- DFS 纠偏页 -->
    <div v-else-if="modelValue.flowStep === 'dfs_correction'" class="mt-8 space-y-8">
      <div class="rounded-2xl border border-slate-200/90 bg-slate-50/60 px-4 py-5 md:px-6 md:py-6">
        <p class="text-xl font-bold tracking-tight text-slate-950 md:text-2xl">你选了 DFS</p>
        <p class="mt-2 text-xl font-bold tracking-tight text-primary md:text-2xl">这题更适合 BFS</p>
        <div class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="tag in reasonTags"
            :key="tag"
            class="inline-flex rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-800"
          >
            {{ tag }}
          </span>
        </div>
      </div>

      <div>
        <p class="text-base font-semibold text-slate-900">错在哪一层？</p>
        <div class="mt-5 space-y-6">
          <div v-for="row in correctionRows" :key="row.key">
            <p class="text-sm font-medium text-slate-600">{{ row.label }}</p>
            <div class="mt-2 flex flex-wrap gap-2" role="radiogroup" :aria-label="row.label">
              <button
                v-for="opt in row.options"
                :key="opt.id"
                type="button"
                class="rounded-2xl border px-4 py-3 text-sm font-semibold transition"
                :class="correctionChipClass(row.key, opt.id)"
                :disabled="disabled"
                @click="setCorrectionPick(row.key, opt.id)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-1 rounded-2xl border border-slate-100 bg-white px-4 py-4 text-sm text-slate-700">
        <p><span class="font-semibold text-slate-900">DFS：</span>先走深</p>
        <p><span class="font-semibold text-slate-900">BFS：</span>先扩层</p>
        <p class="pt-1 font-medium text-slate-800">要最短步数，先看 BFS</p>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <button
          type="button"
          class="text-sm font-medium text-slate-600 underline-offset-2 hover:text-slate-900 hover:underline"
          :disabled="disabled"
          @click="showWhyBfsHint"
        >
          为什么是 BFS？
        </button>
      </div>
    </div>

    <!-- BFS 就绪：轻量确认（预填，可点选调整） -->
    <div v-else class="mt-8 space-y-8">
      <div class="rounded-2xl border border-slate-200/90 bg-slate-50/50 px-4 py-5 md:px-6">
        <p class="text-xl font-bold text-slate-950 md:text-2xl">你选了 BFS</p>
        <p class="mt-1 text-sm text-slate-600">对齐下面三项后即可提交。</p>
        <div class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="tag in reasonTags"
            :key="tag"
            class="inline-flex rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-800"
          >
            {{ tag }}
          </span>
        </div>
      </div>

      <div class="space-y-6">
        <div v-for="key in slotKeys" :key="key">
          <p class="text-sm font-semibold text-slate-900">{{ slotSectionTitle(key) }}</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <button
              v-for="id in DFS_BFS_SLOT_BLOCKS[key]"
              :key="id"
              type="button"
              class="rounded-2xl border px-4 py-3 text-sm font-semibold transition"
              :class="chipClass(slotFilled(key) === id, key)"
              :disabled="disabled || isBlockTakenElsewhere(id, key)"
              @click="placeInSlot(key, id)"
            >
              {{ DFS_BFS_BLOCK_LABELS[id] }}
            </button>
          </div>
        </div>
      </div>

      <div class="space-y-1 rounded-2xl border border-slate-100 bg-slate-50/40 px-4 py-4 text-sm text-slate-700">
        <p><span class="font-semibold text-slate-900">DFS：</span>先走深</p>
        <p><span class="font-semibold text-slate-900">BFS：</span>先扩层</p>
        <p class="pt-1 font-medium text-slate-800">要最短步数，先看 BFS</p>
      </div>
    </div>

    <p
      v-if="errorMessage"
      class="mt-6 rounded-xl border border-amber-200/90 bg-amber-50/80 px-4 py-3 text-sm text-amber-950"
      role="alert"
    >
      {{ errorMessage }}
    </p>
  </section>
</template>

<script setup lang="ts">
import {
  DFS_BFS_BLOCK_LABELS,
  DFS_BFS_EXAMPLE_TITLE,
  DFS_BFS_SLOT_BLOCKS,
  DFS_BFS_WHY_BFS_ONE_LINER,
  enterBfsReadyFromDirectionPick,
  enterDfsCorrectionFromDirectionPick,
  type DfsBfsBlockId,
  type DfsBfsSkeletonSlotKey,
  type DfsBfsStructureWorkbenchUi,
  type DfsBfsWorkbenchHighlight,
} from '@/constants/dfsBfsStructureSkeleton'
import { showToast } from '@/stores/toast'

const props = withDefaults(
  defineProps<{
    modelValue: DfsBfsStructureWorkbenchUi
    disabled?: boolean
    errorMessage?: string
    highlight?: DfsBfsWorkbenchHighlight
    exampleTitle?: string
  }>(),
  {
    disabled: false,
    errorMessage: '',
    highlight: null,
    exampleTitle: DFS_BFS_EXAMPLE_TITLE,
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: DfsBfsStructureWorkbenchUi]
}>()

const reasonTags = ['最短步数', '按层扩展', '队列推进']

const slotKeys = ['signal', 'advance', 'traverse'] as const

const correctionRows: {
  key: 'signal' | 'advance' | 'traverse'
  label: string
  options: { id: DfsBfsBlockId; label: string }[]
}[] = [
  {
    key: 'signal',
    label: '题目信号',
    options: [
      { id: 'depth_try', label: DFS_BFS_BLOCK_LABELS.depth_try },
      { id: 'shortest_steps', label: DFS_BFS_BLOCK_LABELS.shortest_steps },
    ],
  },
  {
    key: 'advance',
    label: '推进方式',
    options: [
      { id: 'rec_stack', label: DFS_BFS_BLOCK_LABELS.rec_stack },
      { id: 'queue', label: DFS_BFS_BLOCK_LABELS.queue },
    ],
  },
  {
    key: 'traverse',
    label: '遍历方式',
    options: [
      { id: 'one_deep', label: DFS_BFS_BLOCK_LABELS.one_deep },
      { id: 'layer_wide', label: DFS_BFS_BLOCK_LABELS.layer_wide },
    ],
  },
]

function emitFull(next: DfsBfsStructureWorkbenchUi) {
  emit('update:modelValue', next)
}

function onPickDfs() {
  emitFull(enterDfsCorrectionFromDirectionPick())
}

function onPickBfs() {
  emitFull(enterBfsReadyFromDirectionPick())
}

function setCorrectionPick(key: 'signal' | 'advance' | 'traverse', id: DfsBfsBlockId) {
  const cur = { ...props.modelValue.correctionPicks }
  const prev = cur[key]
  cur[key] = prev === id ? null : id
  emitFull({ ...props.modelValue, correctionPicks: cur })
}

function correctionChipClass(
  key: 'signal' | 'advance' | 'traverse',
  id: DfsBfsBlockId
): string {
  const on = props.modelValue.correctionPicks[key] === id
  const hi = props.highlight
  const conflict = hi === key
  if (on) {
    return conflict
      ? 'border-amber-500 bg-amber-50 text-amber-950 ring-2 ring-amber-400/35'
      : 'border-primary/60 bg-primary/10 text-slate-950 ring-2 ring-primary/20'
  }
  return 'border-slate-200 bg-white text-slate-800 hover:border-slate-300'
}

function slotFilled(key: DfsBfsSkeletonSlotKey): DfsBfsBlockId | null {
  return props.modelValue.slots[key]
}

function placeInSlot(key: DfsBfsSkeletonSlotKey, id: DfsBfsBlockId) {
  const cur = { ...props.modelValue.slots }
  const prev = cur[key]
  if (prev === id) {
    cur[key] = null
  } else {
    for (const k of Object.keys(cur) as DfsBfsSkeletonSlotKey[]) {
      if (cur[k] === id) cur[k] = null
    }
    cur[key] = id
  }
  emitFull({ ...props.modelValue, slots: cur })
}

function isBlockTakenElsewhere(id: DfsBfsBlockId, slot: DfsBfsSkeletonSlotKey): boolean {
  for (const k of Object.keys(props.modelValue.slots) as DfsBfsSkeletonSlotKey[]) {
    if (k !== slot && props.modelValue.slots[k] === id) return true
  }
  return false
}

function slotSectionTitle(key: DfsBfsSkeletonSlotKey): string {
  if (key === 'signal') return '题目信号'
  if (key === 'advance') return '推进方式'
  return '遍历方式'
}

function chipClass(on: boolean, slot: DfsBfsSkeletonSlotKey): string {
  const hi = props.highlight
  const conflict = hi === slot
  if (on) {
    return conflict
      ? 'border-amber-500 bg-amber-50 text-amber-950 ring-2 ring-amber-400/35'
      : 'border-primary/60 bg-primary/10 text-slate-950 ring-2 ring-primary/20'
  }
  return 'border-slate-200 bg-white text-slate-800 hover:border-slate-300'
}

function directionBtnClass(on: boolean): string {
  const hi = props.highlight
  const conflict = hi === 'direction' || hi === 'd5'
  if (on) {
    return conflict
      ? 'border-amber-500 bg-amber-50 text-amber-950 ring-2 ring-amber-400/40'
      : 'border-primary bg-primary text-white shadow-lg shadow-primary/25'
  }
  return conflict
    ? 'border-amber-300 bg-amber-50/50 text-slate-800'
    : 'border-slate-200 bg-slate-50 text-slate-800 hover:border-slate-300 hover:bg-white'
}

function showWhyBfsHint() {
  showToast(DFS_BFS_WHY_BFS_ONE_LINER)
}
</script>
