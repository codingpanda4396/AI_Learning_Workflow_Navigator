<template>
  <Teleport to="body">
    <div
      class="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-white/92 px-6 backdrop-blur-[2px]"
      role="status"
      aria-live="polite"
    >
      <div
        class="h-10 w-10 animate-spin rounded-full border-2 border-primary border-t-transparent"
      />
      <p class="mt-5 max-w-sm text-center text-base font-medium leading-relaxed text-text-primary">
        {{ message }}
      </p>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  message: string
}>()
</script>
