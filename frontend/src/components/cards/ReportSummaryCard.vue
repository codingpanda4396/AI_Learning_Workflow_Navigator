<script setup lang="ts">
defineProps<{
  title: string;
  content?: string;
}>();
</script>

<template>
  <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <h3 class="text-base font-semibold text-slate-900">{{ title }}</h3>
    <p class="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-600">{{ content || '暂无数据' }}</p>
  </div>
</template>
