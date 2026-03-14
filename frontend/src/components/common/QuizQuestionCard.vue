<script setup lang="ts">
import { computed } from 'vue';
import type { QuizQuestion } from '@/types/quiz';

const props = defineProps<{
  index: number;
  question: QuizQuestion;
  modelValue: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const normalizedType = computed(() => (props.question.type || '').toUpperCase());

function update(value: string) {
  emit('update:modelValue', value);
}
</script>

<template>
  <section class="app-card app-card-padding app-card-strong">
    <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
      <div>
        <p class="app-eyebrow">练习 {{ index + 1 }}</p>
        <h3 class="mt-2 text-[24px] font-semibold leading-8 tracking-[-0.03em] text-slate-950">{{ question.stem }}</h3>
      </div>
      <span class="app-badge">{{ question.type || '自动题型' }}</span>
    </div>

    <div
      v-if="(normalizedType === 'SINGLE_CHOICE' || normalizedType === 'CHOICE' || normalizedType === 'TRUE_FALSE') && question.options.length"
      class="mt-5 grid gap-3"
    >
      <label
        v-for="option in question.options"
        :key="option"
        :class="['app-option flex cursor-pointer items-center gap-3', modelValue === option ? 'app-option-selected' : '']"
      >
        <input
          class="h-4 w-4"
          type="radio"
          :name="`question-${question.questionId}`"
          :checked="modelValue === option"
          @change="update(option)"
        />
        <span class="text-sm leading-7 text-slate-700">{{ option }}</span>
      </label>
    </div>

    <textarea
      v-else
      class="app-textarea mt-5"
      :placeholder="question.evaluationFocus || '先写下你的答案'"
      :value="modelValue"
      @input="update(($event.target as HTMLTextAreaElement).value)"
    />
  </section>
</template>
