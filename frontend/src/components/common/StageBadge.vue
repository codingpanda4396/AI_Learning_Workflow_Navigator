<script setup lang="ts">
import StagePill from '@/components/common/StagePill.vue';

defineProps<{
  stage?: string;
}>();
</script>

<template>
  <StagePill :stage="stage" />
</template>
