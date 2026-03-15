<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  disabled?: boolean;
  loading?: boolean;
  isLast?: boolean;
}>();

defineEmits<{
  continue: [];
}>();
</script>

<template>
  <footer class="mt-8 flex flex-col items-center gap-2">
    <AppButton size="lg" :disabled="disabled" :loading="loading" class="min-w-[220px]" @click="$emit('continue')">
      {{ isLast ? '完成诊断，生成学习路径' : '继续下一题' }}
    </AppButton>
    <p v-if="isLast" class="text-xs text-slate-500">提交后将进入你的个性化学习路径</p>
  </footer>
</template>
