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
  <div class="rounded-[1.8rem] border border-slate-200 bg-white p-6 shadow-sm md:p-7">
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">问题 {{ index + 1 }}</p>
        <h3 class="mt-3 text-lg font-semibold leading-8 text-slate-950">{{ question.stem }}</h3>
      </div>
      <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
        {{ question.type || '自动题型' }}
      </span>
    </div>

    <div v-if="(normalizedType === 'SINGLE_CHOICE' || normalizedType === 'CHOICE' || normalizedType === 'TRUE_FALSE') && question.options.length" class="mt-5 space-y-3">
      <label
        v-for="option in question.options"
        :key="option"
        class="flex cursor-pointer items-center gap-3 rounded-2xl border border-slate-200 px-4 py-4 text-sm transition hover:border-slate-400 hover:bg-slate-50"
      >
        <input
          class="h-4 w-4"
          type="radio"
          :name="`question-${question.questionId}`"
          :checked="modelValue === option"
          @change="update(option)"
        />
        <span>{{ option }}</span>
      </label>
    </div>

    <textarea
      v-else
      class="mt-5 min-h-36 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm leading-7 outline-none transition focus:border-slate-400"
      :placeholder="question.evaluationFocus || '请输入你的答案'"
      :value="modelValue"
      @input="update(($event.target as HTMLTextAreaElement).value)"
    />
  </div>
</template>
