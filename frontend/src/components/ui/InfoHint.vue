<script setup lang="ts">
withDefaults(defineProps<{
  tone?: 'default' | 'danger';
}>(), {
  tone: 'default',
});
</script>

<template>
  <div :class="['app-hint', tone === 'danger' ? 'app-hint-danger' : '']">
    <slot />
  </div>
</template>
