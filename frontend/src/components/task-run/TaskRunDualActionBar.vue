<template>
  <div
    data-testid="task-run-dual-action-bar"
    class="sticky bottom-0 z-10 mt-6 border-t border-slate-200/90 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_92%,white)] px-4 py-3 backdrop-blur md:px-6"
  >
    <div class="mx-auto flex max-w-[720px] flex-wrap items-center justify-between gap-3">
      <div class="flex min-w-0 flex-1 justify-start">
        <SecondaryButton :disabled="secondaryDisabled" class="shrink-0" @click="$emit('secondary')">
          {{ secondaryLabel }}
        </SecondaryButton>
      </div>
      <div class="flex shrink-0 justify-end">
        <PrimaryButton
          :loading="primaryLoading"
          :disabled="primaryDisabled"
          @click="$emit('primary')"
        >
          {{ primaryLabel }}
        </PrimaryButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'

withDefaults(
  defineProps<{
    primaryLabel: string
    secondaryLabel: string
    primaryLoading?: boolean
    primaryDisabled?: boolean
    secondaryDisabled?: boolean
  }>(),
  {
    primaryLoading: false,
    primaryDisabled: false,
    secondaryDisabled: false,
  }
)

defineEmits<{
  primary: []
  secondary: []
}>()
</script>
