<template>
  <article
    class="rounded-[28px] border border-slate-200/90 bg-white p-5 shadow-[0_8px_30px_-12px_rgba(15,23,42,0.12)] md:p-8"
    data-testid="structure-workbench-main-card"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-primary">{{ mainCopy.phaseTag }}</p>
    <h1 class="mt-2 text-xl font-semibold tracking-tight text-slate-950 md:text-2xl">{{ mainCopy.mainTitle }}</h1>

    <p class="mt-4 text-sm leading-relaxed text-slate-600">{{ mainCopy.roundTask }}</p>

    <div class="mt-6 rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3 md:px-5 md:py-4">
      <p class="text-xs font-medium text-slate-500">当前问题</p>
      <p class="mt-1 text-base font-semibold text-slate-900">{{ mainCopy.currentQuestion }}</p>
    </div>

    <div class="mt-5">
      <p class="text-xs font-medium text-slate-500">回答要求</p>
      <div class="mt-2 flex flex-wrap gap-2">
        <span
          v-for="(chip, i) in mainCopy.requirementChips"
          :key="'req-' + i"
          class="inline-flex rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-800"
        >
          {{ chip }}
        </span>
      </div>
    </div>

    <label class="mt-8 block">
      <span class="text-sm font-semibold text-slate-950">{{ mainCopy.inputLabel }}</span>
      <textarea
        :value="draftValue"
        data-testid="structure-workbench-input"
        rows="6"
        class="mt-2 w-full resize-y rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-900 shadow-inner outline-none ring-primary/0 transition focus:border-primary focus:ring-4 focus:ring-primary/10"
        :disabled="loading || submitting"
        :placeholder="mainCopy.placeholder"
        @input="$emit('update:draftValue', ($event.target as HTMLTextAreaElement).value)"
      />
    </label>

    <p v-if="mainCopy.exampleHint" class="mt-2 text-xs leading-relaxed text-slate-500">
      {{ mainCopy.exampleHint }}
    </p>

    <div
      v-if="feedback"
      class="mt-4 rounded-2xl border px-4 py-3 text-sm leading-relaxed"
      :class="feedbackBoxClass"
      data-testid="structure-workbench-feedback"
    >
      <p v-if="feedback.title" class="font-semibold">{{ feedback.title }}</p>
      <p :class="feedback.title ? 'mt-1' : ''">{{ feedback.body }}</p>
    </div>

    <div class="mt-6 flex flex-wrap items-center gap-3">
      <PrimaryButton
        data-testid="structure-workbench-submit"
        :loading="submitting"
        :disabled="loading || submitting || !draftValue.trim()"
        @click="$emit('submit')"
      >
        {{ mainCopy.primaryAction }}
      </PrimaryButton>
      <SecondaryButton :disabled="submitting" @click="$emit('save-draft')">
        {{ mainCopy.secondaryAction }}
      </SecondaryButton>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import type { StructureWorkbenchMainCopy } from '@/constants/dfsBfsStructureWorkbenchCopy'
import type { StructureFeedbackBlock } from '@/utils/structureWorkbenchFeedback'

const props = defineProps<{
  mainCopy: StructureWorkbenchMainCopy
  draftValue: string
  loading: boolean
  submitting: boolean
  feedback: StructureFeedbackBlock | null
}>()

defineEmits<{
  'update:draftValue': [value: string]
  submit: []
  'save-draft': []
}>()

const feedbackBoxClass = computed(() => {
  const t = props.feedback?.tone
  if (!t) return ''
  if (t === 'pass') return 'border-emerald-200 bg-emerald-50/90 text-emerald-950'
  if (t === 'empty') return 'border-slate-200 bg-slate-50 text-slate-700'
  return 'border-amber-200/90 bg-amber-50/85 text-amber-950'
})
</script>
