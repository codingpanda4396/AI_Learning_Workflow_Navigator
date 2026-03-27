<script setup lang="ts">
import { computed } from 'vue'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import type { LearningActionCard as Card, LearningScaffoldActionResult } from '@/types/scaffoldEngine'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'

const props = defineProps<{
  stageKey?: string
  phaseLabel?: string
  stageTitle: string
  stageGoal: string
  stageDescription: string
  phaseGoal?: string
  card: Card | null
  loading: boolean
  submitting: boolean
  lastResult: LearningScaffoldActionResult | null
  inputLabel: string
}>()

const draftValue = defineModel<string>('draftValue', { default: '' })

const emit = defineEmits<{
  submit: []
}>()

const headerEyebrow = computed(() => {
  if (props.phaseLabel?.trim()) return props.phaseLabel.trim()
  const zh = props.stageKey ? phaseCodeToFullZh(props.stageKey) : ''
  return zh || props.stageTitle
})

const displayPhaseGoal = computed(() => props.phaseGoal?.trim() || props.stageGoal)
const displaySingleAction = computed(() => props.card?.singleAction?.trim() || props.card?.goal || '')
const displaySystemPrompt = computed(() => props.card?.systemPrompt?.trim() || props.card?.instructions || '')
const displayRole = computed(() => props.card?.llmRole?.trim() || '阶段反馈器')
const forbiddenItems = computed(() => props.card?.forbiddenActions?.length ? props.card.forbiddenActions : (props.card?.forbiddenPrompts ?? []))
const completionItems = computed(() => props.card?.completionCriteria?.length ? props.card.completionCriteria : (props.card?.passCriteria ?? []))

const isTraining = computed(() => props.stageKey === 'TRAINING')
const isReflection = computed(() => props.stageKey === 'REFLECTION')

const roundHint = computed(() => {
  const attemptNo = props.lastResult?.actionRuntime?.attemptNo
  if (!attemptNo) return '第 1 轮表达'
  if (props.lastResult?.validation.passed) return `第 ${attemptNo} 轮已通过`
  return `第 ${attemptNo} 轮需重写`
})

const aspectFeedback = computed(() => {
  const validation = props.lastResult?.validation
  if (!validation || validation.passed) return null
  const matched = validation.matchedAspects?.filter(Boolean) ?? []
  const missing = validation.missingAspects?.filter(Boolean) ?? []
  if (matched.length === 0 && missing.length === 0) return null
  return { matched, missing }
})

const trainingProblems = computed(() => props.lastResult?.trainingFeedback?.detectedProblems?.slice(0, 2) ?? [])

const errorTypeLabels: Record<string, string> = {
  MECHANISM_ERROR: '机制错位',
  CAUSAL_GAP: '因果断裂',
  VAGUE_EXPRESSION: '表达空泛',
  MISSING_STEP: '缺步骤',
}

function appendChip(chip: string) {
  const cur = draftValue.value.trim()
  draftValue.value = cur ? `${cur}\n${chip}` : chip
}
</script>

<template>
  <div class="space-y-4">
    <header class="rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm">
      <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{{ headerEyebrow }}</p>
      <h2 class="mt-2 text-lg font-semibold text-slate-950">阶段目标</h2>
      <p class="mt-2 text-sm leading-7 text-slate-700">{{ displayPhaseGoal }}</p>
      <p class="mt-2 text-sm text-slate-500">{{ stageDescription }}</p>
    </header>

    <div v-if="card" class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="grid gap-3 lg:grid-cols-[minmax(0,1.45fr)_minmax(280px,0.95fr)]">
        <section class="space-y-4">
          <div class="rounded-2xl border border-indigo-200/80 bg-indigo-50/70 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-indigo-700">当前唯一动作</p>
            <h3 class="mt-2 text-xl font-semibold text-slate-950">{{ card.title }}</h3>
            <p class="mt-3 text-sm leading-7 text-slate-800">{{ displaySingleAction }}</p>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            <div class="flex items-center justify-between gap-3">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-600">系统引导</p>
              <span class="rounded-full border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-600">
                {{ displayRole }}
              </span>
            </div>
            <p class="mt-3 text-sm leading-7 text-slate-700">{{ displaySystemPrompt }}</p>
            <p v-if="isTraining" class="mt-3 text-xs font-medium text-indigo-700">{{ roundHint }}</p>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700">{{ inputLabel }}</label>
            <textarea
              v-model="draftValue"
              class="mt-2 min-h-[168px] w-full resize-y rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-950 outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500/20"
              :disabled="loading || submitting"
              :placeholder="
                isReflection
                  ? '按当前这张卡只回答一个点，越具体越容易过。'
                  : '先写完整，再提交。不要靠一句定义混过去。'
              "
            />
          </div>

          <div v-if="card.allowedPrompts?.length" class="flex flex-wrap gap-2">
            <SecondaryButton
              v-for="(chip, i) in card.allowedPrompts"
              :key="'allow-' + i"
              class="!px-3 !py-1.5 text-xs"
              @click="appendChip(chip)"
            >
              {{ chip }}
            </SecondaryButton>
          </div>
        </section>

        <aside class="space-y-3">
          <div class="rounded-2xl border border-rose-200/80 bg-rose-50/70 p-4">
            <p class="text-sm font-semibold text-rose-900">本阶段禁止行为</p>
            <ul class="mt-3 space-y-2 text-xs leading-6 text-rose-900/90">
              <li v-for="(item, i) in forbiddenItems" :key="'forbid-' + i">• {{ item }}</li>
            </ul>
          </div>

          <div class="rounded-2xl border border-emerald-200/80 bg-emerald-50/70 p-4">
            <p class="text-sm font-semibold text-emerald-900">完成判定</p>
            <ul class="mt-3 space-y-2 text-xs leading-6 text-emerald-900/90">
              <li v-for="(item, i) in completionItems" :key="'completion-' + i">• {{ item }}</li>
            </ul>
          </div>
        </aside>
      </div>
    </div>

    <div v-else-if="!loading" class="rounded-2xl border border-amber-200 bg-amber-50/80 p-4 text-sm text-amber-900">
      当前没有可执行的学习动作，请刷新后重试。
    </div>

    <div
      v-if="lastResult"
      class="rounded-2xl border p-4 text-sm shadow-sm"
      :class="
        lastResult.validation.passed
          ? 'border-emerald-200 bg-emerald-50/90 text-emerald-900'
          : 'border-rose-200 bg-rose-50/90 text-rose-900'
      "
    >
      <p class="font-semibold">{{ lastResult.validation.passed ? '本轮通过' : '系统反馈' }}</p>
      <p class="mt-2 leading-7">{{ lastResult.tutor.content }}</p>

      <div
        v-if="isTraining && !lastResult.validation.passed && trainingProblems.length"
        class="mt-3 space-y-2 border-t border-current/10 pt-3"
      >
        <p class="text-xs font-semibold opacity-90">本轮缺口</p>
        <ul class="space-y-2">
          <li
            v-for="(problem, i) in trainingProblems"
            :key="'problem-' + i"
            class="rounded-xl border border-rose-200/70 bg-white/70 px-3 py-2 text-xs leading-6"
          >
            <span class="mr-2 rounded bg-rose-100 px-1.5 py-0.5 text-[10px] font-semibold text-rose-700">
              {{ errorTypeLabels[problem.errorType] ?? problem.errorType }}
            </span>
            {{ problem.problemText }}
          </li>
        </ul>
        <p v-if="lastResult.trainingFeedback?.revisionInstruction" class="text-xs font-medium">
          重写要求：{{ lastResult.trainingFeedback.revisionInstruction }}
        </p>
      </div>

      <div
        v-else-if="aspectFeedback && !lastResult.validation.passed"
        class="mt-3 grid gap-3 border-t border-current/10 pt-3 sm:grid-cols-2"
      >
        <div v-if="aspectFeedback.matched.length">
          <p class="text-xs font-semibold opacity-80">你已经说到了</p>
          <ul class="mt-2 space-y-1 text-xs leading-6 opacity-90">
            <li v-for="(item, i) in aspectFeedback.matched" :key="'matched-' + i">• {{ item }}</li>
          </ul>
        </div>
        <div v-if="aspectFeedback.missing.length">
          <p class="text-xs font-semibold opacity-80">还没说清</p>
          <ul class="mt-2 space-y-1 text-xs leading-6 opacity-90">
            <li v-for="(item, i) in aspectFeedback.missing" :key="'missing-' + i">• {{ item }}</li>
          </ul>
        </div>
      </div>

      <p v-if="lastResult.tutor.nextPrompt" class="mt-3 text-xs font-medium opacity-90">
        重写提示：{{ lastResult.tutor.nextPrompt }}
      </p>
    </div>

    <div class="flex justify-end">
      <button
        type="button"
        class="inline-flex items-center justify-center rounded-2xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="loading || submitting || !draftValue.trim()"
        @click="emit('submit')"
      >
        {{
          submitting
            ? '提交中...'
            : isTraining
              ? '提交本轮因果链'
              : isReflection
                ? '提交这张反思卡'
                : '提交本轮表达'
        }}
      </button>
    </div>
  </div>
</template>
