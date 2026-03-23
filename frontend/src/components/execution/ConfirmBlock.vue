<template>
  <FormCard>
    <p class="text-base font-medium text-text-primary">看完了吗？</p>
    <p class="mt-2 text-sm text-text-secondary">
      不用背下来，只要心里能答得上来下面两点，就可以继续。
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
