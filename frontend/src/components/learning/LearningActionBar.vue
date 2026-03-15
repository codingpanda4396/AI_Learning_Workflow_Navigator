<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';
</script>

<template>
  <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
    <div class="w-full sm:w-auto">
      <slot name="primary" />
    </div>
    <div v-if="$slots.secondary" class="w-full sm:w-auto">
      <slot name="secondary" />
    </div>
  </div>
</template>
