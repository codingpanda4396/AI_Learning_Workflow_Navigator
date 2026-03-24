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
        <h3
          class="mt-2 font-semibold text-text-primary"
          :class="emphasis ? 'text-xl tracking-tight md:text-2xl' : 'text-lg'"
        >
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
    /** 主行动区：更大标题与更强边框 */
    emphasis?: boolean
  }>(),
  {
    description: '',
    badge: '',
    badgeVariant: 'default',
    active: false,
    completed: false,
    emphasis: false,
  }
)

const emphasis = computed(() => props.emphasis)

const sectionClass = computed(() => {
  if (props.emphasis && props.active) {
    return 'border-primary/50 bg-gradient-to-br from-primary/[0.08] via-white to-white shadow-[0_20px_48px_rgba(79,70,229,0.12)] ring-1 ring-primary/15'
  }
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
