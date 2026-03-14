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
const description = computed(() => props.question.description || questionCopy.value.description);
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
  <section class="app-card app-card-padding app-card-strong">
    <div class="max-w-2xl">
      <p class="app-eyebrow">{{ questionCopy.sectionLabel }}</p>
      <h2 class="mt-3 text-[30px] font-semibold leading-[1.2] tracking-[-0.04em] text-slate-950">
        {{ title }}
      </h2>
      <p class="mt-3 text-sm leading-7 text-slate-600">{{ description }}</p>
    </div>

    <div class="mt-6">
      <div v-if="question.type === 'SINGLE_CHOICE'" class="grid gap-3">
        <label
          v-for="option in question.options || []"
          :key="option.code"
          :class="['app-option flex cursor-pointer items-start gap-3', modelValue === option.code ? 'app-option-selected' : '']"
        >
          <input
            class="mt-1 h-4 w-4"
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
            class="mt-1 h-4 w-4"
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
          rows="6"
          class="app-textarea"
          :placeholder="placeholder"
          @input="updateText(($event.target as HTMLTextAreaElement).value)"
        />
      </label>
    </div>

    <div class="app-hint mt-5">
      按你现在的真实情况来答就够了，这一步只用来安排更贴近你的学习起点。
    </div>
  </section>
</template>
