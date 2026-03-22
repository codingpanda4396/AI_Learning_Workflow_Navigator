<template>
  <div class="rounded-card border border-border bg-white p-4 md:p-5">
    <p class="text-xs font-medium text-text-secondary">接下来会怎么学</p>
    <ol class="mt-3 list-decimal space-y-2 pl-5 text-sm text-text-primary">
      <li v-for="(line, i) in lines" :key="i" class="leading-relaxed">
        {{ line }}
      </li>
    </ol>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  lines: string[]
}>()
</script>
