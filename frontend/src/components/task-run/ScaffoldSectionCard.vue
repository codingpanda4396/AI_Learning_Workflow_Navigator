<template>
  <section
    class="rounded-[28px] border bg-white p-5 shadow-card transition md:p-6"
    :class="sectionClass"
  >
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.22em]" :class="eyebrowClass">
          {{ eyebrow }}
        </p>
        <h3 class="mt-2 text-lg font-semibold text-text-primary">
          {{ title }}
        </h3>
        <p v-if="description" class="mt-2 max-w-2xl text-sm leading-6 text-text-secondary">
          {{ description }}
        </p>
      </div>
      <StatusBadge v-if="badge" :label="badge" :variant="badgeVariant" />
    </div>
    <div class="mt-5">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'

const props = withDefaults(
  defineProps<{
    title: string
    eyebrow: string
    description?: string
    badge?: string
    badgeVariant?: 'default' | 'success' | 'warning' | 'error'
    active?: boolean
    completed?: boolean
  }>(),
  {
    description: '',
    badge: '',
    badgeVariant: 'default',
    active: false,
    completed: false,
  }
)

const sectionClass = computed(() => {
  if (props.active) {
    return 'border-primary/35 bg-gradient-to-br from-primary/5 via-white to-white'
  }
  if (props.completed) {
    return 'border-emerald-200 bg-emerald-50/40'
  }
  return 'border-border'
})

const eyebrowClass = computed(() => {
  if (props.active) return 'text-primary'
  if (props.completed) return 'text-emerald-700'
  return 'text-text-secondary'
})
</script>
