<template>
  <div class="mb-8">
    <div class="mb-2 flex justify-between text-xs font-medium text-text-secondary">
      <span>学习路径</span>
      <span>第 {{ current }} / {{ total }} 阶段</span>
    </div>
    <div class="flex flex-wrap items-center gap-x-1 gap-y-2 text-xs sm:gap-x-0.5">
      <template v-for="(label, index) in labels" :key="label">
        <span
          class="whitespace-nowrap rounded-full px-2 py-1 font-medium transition-colors"
          :class="
            index + 1 === current
              ? 'bg-accent text-white shadow-sm ring-1 ring-accent/25'
              : index + 1 < current
                ? 'bg-emerald-100 text-emerald-900'
                : 'bg-slate-100 text-text-secondary'
          "
        >
          {{ label }}
        </span>
        <span
          v-if="index < labels.length - 1"
          class="hidden text-text-secondary sm:inline"
          aria-hidden="true"
        >
          →
        </span>
      </template>
    </div>
    <p class="mt-2 text-[11px] leading-relaxed text-text-secondary">
      当前 MVP 已深度落地「{{ labels[current - 1] ?? labels[0] }}」；后续阶段将逐步开放。
    </p>
    <div class="mt-3 h-2 overflow-hidden rounded-full bg-border">
      <div
        class="h-full rounded-full bg-accent transition-all duration-300"
        :style="{ width: `${Math.min(100, (current / total) * 100)}%` }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    current: number
    total: number
    labels: readonly string[]
  }>(),
  {
    labels: () => [],
  }
)
</script>
