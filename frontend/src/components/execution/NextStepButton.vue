<template>
  <PrimaryButton
    class="mt-6 w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
    @click="$emit('click')"
  >
    {{ label }}
  </PrimaryButton>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'

withDefaults(
  defineProps<{
    label?: string
  }>(),
  { label: '下一步' }
)

defineEmits<{
  click: []
}>()
</script>
