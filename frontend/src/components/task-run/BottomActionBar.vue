<template>
  <div
    data-testid="bottom-action-bar"
    class="sticky bottom-0 z-10 mt-6 border-t border-slate-200/90 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_92%,white)] px-4 py-3 backdrop-blur md:px-6"
  >
    <div class="mx-auto flex max-w-[1260px] flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap items-center gap-2">
        <button
          type="button"
          class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          :disabled="saving"
          @click="$emit('save-draft')"
        >
          保存草稿
        </button>
        <button
          type="button"
          class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
          @click="$emit('open-tutor')"
        >
          AI Tutor
        </button>
      </div>
      <div class="flex flex-wrap items-center justify-end gap-2">
        <SecondaryButton
          v-if="showAdvance"
          :disabled="!canAdvance || advancing"
          @click="$emit('advance')"
        >
          <span
            v-if="advancing"
            class="mr-2 inline-block h-4 w-4 animate-spin rounded-full border-2 border-slate-400 border-t-transparent"
          />
          {{ advanceLabel }}
        </SecondaryButton>
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
    primaryLoading?: boolean
    primaryDisabled?: boolean
    saving?: boolean
    showAdvance?: boolean
    canAdvance?: boolean
    advancing?: boolean
    advanceLabel?: string
  }>(),
  {
    primaryLoading: false,
    primaryDisabled: false,
    saving: false,
    showAdvance: false,
    canAdvance: false,
    advancing: false,
    advanceLabel: '进入下一阶段',
  }
)

defineEmits<{
  'save-draft': []
  'open-tutor': []
  primary: []
  advance: []
}>()
</script>
