<script setup lang="ts">
import { computed } from 'vue';
import { resolveDiagnosisQuestionCopy } from '@/types/diagnosis';
import type { DiagnosisAnswerValue, DiagnosisQuestion } from '@/types/diagnosis';

const props = defineProps<{
  question: DiagnosisQuestion;
  modelValue?: DiagnosisAnswerValue;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: DiagnosisAnswerValue): void;
}>();

const selectedList = computed(() => (Array.isArray(props.modelValue) ? props.modelValue : []));
const textValue = computed(() => (typeof props.modelValue === 'string' ? props.modelValue : ''));
const questionCopy = computed(() => resolveDiagnosisQuestionCopy(props.question));
const title = computed(() => props.question.title || questionCopy.value.title);
const placeholder = computed(() => props.question.placeholder || questionCopy.value.placeholder);

function updateSingleChoice(option: string) {
  emit('update:modelValue', option);
}

function updateMultipleChoice(option: string, checked: boolean) {
  const nextValues = checked
    ? [...selectedList.value, option]
    : selectedList.value.filter((item) => item !== option);
  emit('update:modelValue', nextValues);
}

function updateText(value: string) {
  emit('update:modelValue', value);
}
</script>

<template>
  <section class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
    <h2 class="text-xl font-semibold leading-8 text-slate-950">
      {{ title }}
    </h2>

    <div class="mt-6">
      <div v-if="question.type === 'SINGLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          class="flex cursor-pointer items-start gap-3 rounded-2xl border border-slate-200 px-4 py-3 transition hover:border-slate-300"
        >
          <input
            class="mt-1"
            type="radio"
            :name="question.questionId"
            :checked="modelValue === option.code"
            @change="updateSingleChoice(option.code)"
          />
          <span class="text-sm leading-6 text-slate-700">{{ option.label }}</span>
        </label>
      </div>

      <div v-else-if="question.type === 'MULTIPLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          class="flex cursor-pointer items-start gap-3 rounded-2xl border border-slate-200 px-4 py-3 transition hover:border-slate-300"
        >
          <input
            class="mt-1"
            type="checkbox"
            :checked="selectedList.includes(option.code)"
            @change="updateMultipleChoice(option.code, ($event.target as HTMLInputElement).checked)"
          />
          <span class="text-sm leading-6 text-slate-700">{{ option.label }}</span>
        </label>
      </div>

      <label v-else class="block">
        <textarea
          :value="textValue"
          rows="6"
          class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm leading-6 text-slate-800 outline-none transition focus:border-slate-300"
          :placeholder="placeholder"
          @input="updateText(($event.target as HTMLTextAreaElement).value)"
        />
      </label>
    </div>
  </section>
</template>
