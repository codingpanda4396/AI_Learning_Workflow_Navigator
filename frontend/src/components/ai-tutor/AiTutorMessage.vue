<template>
  <div
    class="flex w-full"
    :class="role === 'user' ? 'justify-end' : 'justify-start'"
  >
    <div
      class="max-w-[85%] rounded-input px-3 py-2 text-sm leading-relaxed"
      :class="
        role === 'user'
          ? 'bg-primary text-white'
          : 'border border-border bg-slate-50 text-text-primary'
      "
    >
      <p class="whitespace-pre-wrap break-words">{{ content }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  role: 'user' | 'assistant'
  content: string
}>()
</script>
