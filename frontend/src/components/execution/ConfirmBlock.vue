<template>
  <FormCard>
    <p class="text-xs font-medium text-text-secondary">👨‍🏫 AI导师</p>
    <p class="mt-2 text-sm leading-relaxed text-text-primary">
      不用全记住，先抓住一个感觉就行。
    </p>
    <p class="mt-2 text-sm leading-relaxed text-text-primary">
      👉 和这一步的目标相关的画面，你心里有数了吗？
    </p>
    <ReflectionBlock :questions="questions" />
  </FormCard>
</template>

<script setup lang="ts">
import FormCard from '@/components/ui/FormCard.vue'
import ReflectionBlock from '@/components/execution/ReflectionBlock.vue'

defineProps<{
  questions: string[]
}>()
</script>
