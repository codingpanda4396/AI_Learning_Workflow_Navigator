<template>
  <FormCard>
    <p class="text-xs font-medium text-text-secondary">👨‍🏫 AI导师</p>
    <p class="mt-2 text-sm font-medium text-text-primary">
      用你自己的话说一遍就行，不用标准答案。
    </p>
    <p class="mt-1.5 text-xs text-text-secondary">👉 我帮你看哪里需要补一补。</p>
    <p v-if="hint" class="mt-3 text-xs leading-relaxed text-text-secondary">
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
    hint: '就写你刚问完、想完之后，脑子里留下的那几句。不用长，真实就好。',
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
