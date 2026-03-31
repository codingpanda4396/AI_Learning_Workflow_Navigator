<template>
  <header class="space-y-1">
    <p class="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">STRUCTURE</p>
    <h1 class="text-lg font-semibold tracking-tight text-slate-950 md:text-xl">先搭 DFS / BFS 判断骨架</h1>
    <p class="text-sm text-slate-600">{{ taskLine }}</p>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  taskLine: string
}>()
</script>
