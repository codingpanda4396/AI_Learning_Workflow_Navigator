<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  disabled?: boolean;
  loading?: boolean;
}>();

defineEmits<{
  continue: [];
}>();
</script>

<template>
  <footer class="mt-8 flex justify-center">
    <AppButton size="lg" :disabled="disabled" :loading="loading" class="min-w-[220px]" @click="$emit('continue')">
      继续下一步
    </AppButton>
  </footer>
</template>
