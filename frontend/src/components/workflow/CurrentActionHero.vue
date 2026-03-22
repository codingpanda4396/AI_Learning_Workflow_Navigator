<template>
  <div
    class="rounded-card border-2 border-primary/30 bg-gradient-to-br from-primary/5 to-white p-6 shadow-card md:p-8"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-primary">
      {{ eyebrow }}
    </p>
    <h2 class="mt-2 text-xl font-bold leading-snug text-text-primary md:text-2xl">
      {{ title }}
    </h2>
    <p
      v-if="minutes != null && minutes > 0"
      class="mt-3 text-sm text-text-secondary"
    >
      约 {{ minutes }} 分钟
    </p>
    <div class="mt-6">
      <PrimaryButton
        class="w-full justify-center py-3 text-base md:w-auto md:min-w-[200px]"
        :loading="loading"
        :disabled="disabled"
        @click="$emit('action')"
      >
        {{ ctaLabel }}
      </PrimaryButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'

withDefaults(
  defineProps<{
    title: string
    eyebrow?: string
    minutes?: number
    ctaLabel: string
    loading?: boolean
    disabled?: boolean
  }>(),
  {
    eyebrow: '当前最优起点',
    loading: false,
    disabled: false,
  }
)

defineEmits<{
  action: []
}>()
</script>
