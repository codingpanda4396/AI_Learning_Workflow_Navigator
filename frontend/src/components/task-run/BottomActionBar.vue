<template>
  <div
    data-testid="bottom-action-bar"
    class="sticky bottom-0 z-10 mt-6 border-t border-border bg-background/95 px-4 py-3 backdrop-blur md:px-6"
  >
    <div class="mx-auto flex max-w-execution flex-wrap items-center justify-between gap-3">
      <p class="text-xs text-slate-500">完成当前阶段并看到反馈后，可进入下一步。</p>
      <PrimaryButton :loading="primaryLoading" :disabled="primaryDisabled" @click="$emit('primary')">
        {{ primaryLabel }}
      </PrimaryButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'

withDefaults(
  defineProps<{
    primaryLabel: string
    primaryLoading?: boolean
    primaryDisabled?: boolean
  }>(),
  {
    primaryLoading: false,
    primaryDisabled: false,
  }
)

defineEmits<{
  primary: []
}>()
</script>
