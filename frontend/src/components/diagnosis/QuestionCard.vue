<script setup lang="ts">
import { computed } from 'vue';
import { isTopicRelatedDimension, resolveDiagnosisQuestionCopy } from '@/types/diagnosis';
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
const title = computed(() => (props.question.title || questionCopy.value.title || '').trim());
const description = computed(() => (props.question.description || questionCopy.value.description || '').trim());
const placeholder = computed(() => props.question.placeholder || questionCopy.value.placeholder);
const submitHint = computed(() => (props.question.submitHint || questionCopy.value.submitHint || '').trim());
const sectionLabel = computed(() => (props.question.sectionLabel || questionCopy.value.sectionLabel || '').trim());
const showDescription = computed(() => description.value.length > 0);
const showSubmitHint = computed(() => submitHint.value.length > 0 && submitHint.value.length <= 80);
const isTopicRelated = computed(() => isTopicRelatedDimension(props.question.dimension));

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
  <section class="app-card app-card-padding app-card-strong">
    <div class="max-w-2xl">
      <div class="flex flex-wrap items-center gap-2">
        <span v-if="sectionLabel" class="text-xs font-medium uppercase tracking-wider text-slate-400">
          {{ sectionLabel }}
        </span>
        <span
          v-if="isTopicRelated"
          class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600"
        >
          与当前目标直接相关
        </span>
      </div>
      <h2 class="mt-3 text-[26px] font-semibold leading-[1.25] tracking-[-0.04em] text-slate-950">
        {{ title }}
      </h2>
      <p v-if="showDescription" class="mt-2 text-sm leading-6 text-slate-600">
        {{ description }}
      </p>
    </div>

    <div class="mt-6">
      <div v-if="question.type === 'SINGLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          :class="['app-option flex cursor-pointer items-start gap-3', modelValue === option.code ? 'app-option-selected' : '']"
        >
          <input
            class="mt-1 h-4 w-4 shrink-0"
            type="radio"
            :name="question.questionId"
            :checked="modelValue === option.code"
            @change="updateSingleChoice(option.code)"
          />
          <span class="text-sm leading-7 text-slate-700">{{ option.label }}</span>
        </label>
      </div>

      <div v-else-if="question.type === 'MULTIPLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          :class="['app-option flex cursor-pointer items-start gap-3', selectedList.includes(option.code) ? 'app-option-selected' : '']"
        >
          <input
            class="mt-1 h-4 w-4 shrink-0"
            type="checkbox"
            :checked="selectedList.includes(option.code)"
            @change="updateMultipleChoice(option.code, ($event.target as HTMLInputElement).checked)"
          />
          <span class="text-sm leading-7 text-slate-700">{{ option.label }}</span>
        </label>
      </div>

      <label v-else class="block">
        <textarea
          :value="textValue"
          rows="4"
          class="app-textarea"
          :placeholder="placeholder"
          @input="updateText(($event.target as HTMLTextAreaElement).value)"
        />
      </label>
    </div>

    <p v-if="showSubmitHint" class="mt-4 text-xs text-slate-500">
      {{ submitHint }}
    </p>
  </section>
</template>
