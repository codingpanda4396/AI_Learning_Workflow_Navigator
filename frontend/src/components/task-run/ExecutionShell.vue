<template>
  <div class="execution-shell mx-auto w-full max-w-execution">
    <div v-if="$slots.header" class="execution-shell__header">
      <slot name="header" />
    </div>
    <div class="execution-shell__body min-h-0 transition-opacity duration-200">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 执行页统一宽度与区块层级：顶栏 + 主内容（阶段内容由子组件决定单/双列）
 */
</script>
