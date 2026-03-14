<script setup lang="ts">
defineProps<{
  disabled?: boolean;
  loading?: boolean;
}>();

defineEmits<{
  continue: [];
}>();
</script>

<template>
  <footer class="mt-8 flex justify-center">
    <button
      type="button"
      class="rounded-2xl bg-slate-950 px-10 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
      :disabled="disabled || loading"
      @click="$emit('continue')"
    >
      {{ loading ? '处理中...' : '继续' }}
    </button>
  </footer>
</template>
