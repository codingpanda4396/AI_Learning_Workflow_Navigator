<template>
  <nav class="flex items-center gap-1 text-sm text-text-secondary">
    <template v-for="(step, i) in order" :key="step">
      <div
        v-if="i > 0"
        class="mx-0.5 h-px w-4 flex-shrink-0"
        :class="
          isDone(order[i - 1])
            ? 'bg-primary'
            : 'bg-border'
       "
      />
      <div
        class="flex flex-col items-center"
        :class="
          current === step
            ? 'text-accent font-medium'
            : isDone(step)
              ? 'text-primary/80'
              : ''
       "
      >
        <div
          class="flex h-6 w-6 items-center justify-center rounded-full text-xs"
          :class="
            current === step
              ? 'bg-accent text-white shadow-sm ring-2 ring-accent/25'
              : isDone(step)
                ? 'bg-primary/20'
                : 'bg-border'
         "
        >
          {{ isDone(step) ? '✓' : i + 1 }}
        </div>
        <span class="mt-0.5 hidden sm:inline">{{ stepLabels[step] }}</span>
      </div>
    </template>
  </nav>
</template>

<script setup lang="ts">
const order: Array<'goal' | 'diagnosis' | 'plan' | 'task' | 'report'> = [
  'goal',
  'diagnosis',
  'plan',
  'task',
  'report',
]

const stepLabels: Record<string, string> = {
  goal: '目标',
  diagnosis: '诊断',
  plan: '规划',
  task: '执行',
  report: '报告',
}

const props = defineProps<{
  current: 'goal' | 'diagnosis' | 'plan' | 'task' | 'report'
}>()

function isDone(step: string): boolean {
  const idx = order.indexOf(step as (typeof order)[number])
  const curIdx = order.indexOf(props.current)
  return idx < curIdx
}
</script>
