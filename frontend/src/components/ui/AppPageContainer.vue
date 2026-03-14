<script setup lang="ts">
withDefaults(defineProps<{
  narrow?: boolean;
}>(), {
  narrow: false,
});
</script>

<template>
  <div :class="['app-page', narrow ? 'app-page-narrow' : '']">
    <slot />
  </div>
</template>
