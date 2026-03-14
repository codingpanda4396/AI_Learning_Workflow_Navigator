<script setup lang="ts">
defineProps<{
  message?: string;
}>();
</script>

<template>
  <div class="app-hint app-hint-danger">
    {{ message || '这一步暂时没有成功加载，请稍后再试。' }}
  </div>
</template>
