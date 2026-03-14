<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  block?: boolean;
  type?: 'button' | 'submit' | 'reset';
  disabled?: boolean;
  loading?: boolean;
}>(), {
  variant: 'primary',
  size: 'md',
  block: false,
  type: 'button',
  disabled: false,
  loading: false,
});

const classes = computed(() => [
  'app-btn',
  `app-btn-${props.variant}`,
  `app-btn-${props.size}`,
  props.block ? 'w-full' : '',
]);
</script>

<template>
  <button :type="type" :disabled="disabled || loading" :class="classes">
    <span v-if="loading" class="text-xs tracking-[0.2em]" aria-hidden="true">...</span>
    <slot />
  </button>
</template>
