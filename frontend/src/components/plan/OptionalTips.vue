<template>
  <details
    class="group mt-10 rounded-card border border-border bg-white"
  >
    <summary
      class="cursor-pointer list-none p-4 text-sm font-medium text-text-primary marker:hidden [&::-webkit-details-marker]:hidden"
    >
      <span class="flex items-center justify-between gap-2">
        想再稳一点的话：
        <span class="text-xs font-normal text-text-secondary group-open:hidden">
          展开
        </span>
        <span class="hidden text-xs font-normal text-text-secondary group-open:inline">
          收起
        </span>
      </span>
    </summary>
    <ul
      class="space-y-2 border-t border-border p-4 pt-3 text-sm leading-relaxed text-text-secondary"
    >
      <li
        v-for="(tip, i) in tips"
        :key="i"
        class="pl-1"
      >
        {{ tip }}
      </li>
    </ul>
  </details>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    tips?: string[]
  }>(),
  {
    tips: () => [],
  }
)
</script>
