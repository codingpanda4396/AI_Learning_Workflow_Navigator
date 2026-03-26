<template>
  <label class="block">
    <span class="text-xs font-medium text-slate-500">{{ label }}</span>
    <textarea
      :value="modelValue"
      rows="2"
      class="mt-1.5 w-full resize-y rounded-xl border border-slate-200 bg-slate-50/80 px-3 py-2 text-sm text-slate-900 outline-none ring-primary/0 transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
  </label>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  label: string
  placeholder: string
  disabled?: boolean
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>
