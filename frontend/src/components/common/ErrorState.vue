<script setup lang="ts">
defineProps<{
  message?: string;
}>();
</script>

<template>
  <div class="rounded-3xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700">
    {{ message || '加载失败，请稍后重试。' }}
  </div>
</template>
