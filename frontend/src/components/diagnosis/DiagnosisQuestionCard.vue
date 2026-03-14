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
  <section class="rounded-[1.9rem] border border-slate-200 bg-white p-6 shadow-sm">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">
          {{ questionCopy.sectionLabel }}
        </p>
        <h2 class="mt-2 line-clamp-2 text-xl font-semibold leading-8 text-slate-950">
          {{ questionCopy.title }}
        </h2>
        <p class="mt-3 line-clamp-2 text-sm leading-6 text-slate-600">
          {{ questionCopy.description }}
        </p>
      </div>
      <span v-if="question.required" class="rounded-full bg-rose-50 px-3 py-1 text-xs font-medium text-rose-600">必答</span>
    </div>

    <div class="mt-6">
      <div v-if="question.type.code === 'SINGLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          class="flex cursor-pointer items-start gap-3 rounded-2xl border border-slate-200 px-4 py-3 transition hover:border-sky-300 hover:bg-sky-50/50"
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

      <div v-else-if="question.type.code === 'MULTIPLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          class="flex cursor-pointer items-start gap-3 rounded-2xl border border-slate-200 px-4 py-3 transition hover:border-sky-300 hover:bg-sky-50/50"
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
          class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-800 outline-none transition focus:border-sky-300 focus:bg-white"
          :placeholder="questionCopy.placeholder"
          @input="updateText(($event.target as HTMLTextAreaElement).value)"
        />
      </label>
    </div>

    <p class="mt-4 text-sm leading-6 text-slate-500">
      {{ questionCopy.submitHint }}
    </p>
  </section>
</template>
