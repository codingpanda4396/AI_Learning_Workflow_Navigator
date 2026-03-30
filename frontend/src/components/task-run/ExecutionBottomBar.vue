<template>
  <div
    data-testid="execution-bottom-bar"
    class="sticky bottom-0 z-10 mt-6 border-t border-slate-200/90 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_92%,white)] px-4 py-3 backdrop-blur md:px-6"
  >
    <div class="mx-auto flex max-w-[1260px] flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap items-center gap-3">
        <button
          type="button"
          class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          :disabled="secondaryDisabled"
          @click="$emit('secondary')"
        >
          {{ secondaryLabel }}
        </button>
        <button
          type="button"
          class="text-xs font-medium text-slate-500 underline-offset-2 hover:text-slate-800 hover:underline"
          :disabled="saveDisabled"
          @click="$emit('save-draft')"
        >
          {{ TASKRUN_COPY.saveDraftInline }}
        </button>
      </div>
      <PrimaryButton
        :loading="primaryLoading"
        :disabled="primaryDisabled"
        @click="$emit('primary')"
      >
        {{ primaryLabel }}
      </PrimaryButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import { TASKRUN_COPY } from '@/constants/uiCopy'

withDefaults(
  defineProps<{
    primaryLabel: string
    primaryLoading?: boolean
    primaryDisabled?: boolean
    secondaryLabel: string
    secondaryDisabled?: boolean
    saveDisabled?: boolean
  }>(),
  {
    primaryLoading: false,
    primaryDisabled: false,
    secondaryDisabled: false,
    saveDisabled: false,
  }
)

defineEmits<{
  primary: []
  secondary: []
  'save-draft': []
}>()
</script>
