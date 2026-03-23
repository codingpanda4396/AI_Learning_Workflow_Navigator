<template>
  <FormCard>
    <p class="text-sm font-medium text-text-primary">用你自己的话写几句</p>
    <p class="mt-1 text-xs text-text-secondary">
      {{ hint }}
    </p>
    <textarea
      :value="modelValue"
      rows="4"
      class="mt-4 w-full rounded-input border border-border px-3 py-2 text-sm text-text-primary"
      :placeholder="placeholder"
      @input="onInput"
    />
  </FormCard>
</template>

<script setup lang="ts">
import FormCard from '@/components/ui/FormCard.vue'

withDefaults(
  defineProps<{
    modelValue: string
    hint?: string
    placeholder?: string
  }>(),
  {
    hint: '结合上面的提示，把你在问答后脑子里留下的理解写下来（几句话即可）。',
    placeholder: '例如：我理解它是……',
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}
</script>
