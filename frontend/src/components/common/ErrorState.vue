<script setup lang="ts">
defineProps<{
  message?: string;
}>();
</script>

<template>
  <div class="rounded-[1.8rem] border border-rose-200 bg-rose-50 p-6 text-sm leading-7 text-rose-700">
    {{ message || '这一步暂时没有加载成功，请稍后再试。' }}
  </div>
</template>
