<script setup lang="ts">
withDefaults(defineProps<{
  height?: number;
  width?: string;
  rounded?: string;
}>(), {
  height: 16,
  width: '100%',
  rounded: '16px',
});
</script>

<template>
  <div class="app-skeleton" :style="{ height: `${height}px`, width, borderRadius: rounded }" />
</template>
