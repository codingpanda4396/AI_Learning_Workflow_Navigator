<template>
  <div class="mx-auto w-full max-w-2xl space-y-6" :data-phase="stageKey">
    <!-- 页面主标题区 -->
    <header class="space-y-1">
      <template v-if="isStructurePositionFlow">
        <p class="text-lg font-semibold tracking-tight text-slate-950">
          {{ MICRO.STRUCTURE_MICRO_HEADER.phaseLine }}
        </p>
        <p class="text-base font-medium text-slate-800">
          {{ MICRO.STRUCTURE_MICRO_HEADER.heroLine }}
        </p>
        <p class="text-sm leading-relaxed text-slate-600">
          {{ MICRO.STRUCTURE_MICRO_HEADER.actionLine }}
        </p>
      </template>
      <template v-else>
        <p class="text-lg font-semibold tracking-tight text-slate-950">
          {{ phaseTitleLine }}
        </p>
        <p class="text-base font-medium text-slate-800">
          {{ heroSecondLine }}
        </p>
        <p class="text-sm leading-relaxed text-slate-600">
          {{ actionThirdLine }}
        </p>
      </template>
    </header>

    <!-- 唯一主卡 -->
    <section
      class="rounded-[28px] border border-slate-200/90 bg-white p-6 shadow-sm ring-1 ring-slate-100/80 md:p-7"
      data-testid="scaffold-single-main-card"
    >
      <template v-if="isStructurePositionFlow">
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
          {{ MICRO.STRUCTURE_MAIN_TASK_CARD.eyebrow }}
        </p>
        <h2 class="mt-3 text-xl font-semibold tracking-tight text-slate-950">
          {{ MICRO.STRUCTURE_MAIN_TASK_CARD.title }}
        </h2>
        <p class="mt-4 text-[15px] leading-7 text-slate-800">
          {{ MICRO.STRUCTURE_MAIN_TASK_CARD.body }}
        </p>
        <p class="mt-3 text-sm leading-relaxed text-slate-600">
          {{ MICRO.STRUCTURE_MAIN_TASK_CARD.helper }}
        </p>
        <p
          class="mt-5 border-t border-slate-100 pt-4 text-sm leading-relaxed text-slate-600"
        >
          {{ MICRO.STRUCTURE_MAIN_TASK_CARD.passLine }}
        </p>
      </template>
      <template v-else>
        <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
          {{ phaseLabel }}
        </p>
        <h2 class="mt-3 text-xl font-semibold tracking-tight text-slate-950">
          {{ WIREFRAME.mainCardTitle }}
        </h2>
        <p class="mt-4 text-[15px] leading-7 text-slate-800">
          {{ taskBody }}
        </p>
        <p
          v-if="passStandardLine"
          class="mt-5 border-t border-slate-100 pt-4 text-sm leading-relaxed text-slate-600"
        >
          <span class="font-medium text-slate-700">{{ WIREFRAME.passLabel }}</span>{{ passStandardLine }}
        </p>
      </template>
    </section>

    <!-- 渐进式轻判断（STRUCTURE · 位置卡） -->
    <div v-if="isStructurePositionFlow" class="space-y-5">
      <transition name="fade-step" mode="out-in">
        <div v-if="structureMicroStep === 'classify'" key="classify" class="space-y-4">
          <p class="text-base font-medium text-slate-900">
            {{ MICRO.STRUCTURE_CLASSIFY.title }}
          </p>
          <div class="space-y-2" role="radiogroup" :aria-label="MICRO.STRUCTURE_CLASSIFY.title">
            <button
              v-for="opt in MICRO.STRUCTURE_CLASSIFY.options"
              :key="opt.id"
              type="button"
              role="radio"
              :aria-checked="selectedClassifyId === opt.id"
              class="flex w-full rounded-2xl border px-4 py-3.5 text-left text-sm font-medium leading-relaxed transition"
              :class="
                selectedClassifyId === opt.id
                  ? 'border-indigo-400 bg-indigo-50/90 text-indigo-950 ring-2 ring-indigo-500/20'
                  : 'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50/80'
              "
              @click="selectedClassifyId = opt.id"
            >
              {{ opt.label }}
            </button>
          </div>
          <div class="pt-1">
            <PrimaryButton
              data-testid="structure-micro-continue"
              :disabled="loading || submitting || !selectedClassifyId"
              @click="structureMicroStep = 'function'"
            >
              {{ MICRO.STRUCTURE_MICRO_CTA.continue }}
            </PrimaryButton>
          </div>
        </div>

        <div v-else-if="structureMicroStep === 'function'" key="function" class="space-y-4">
          <p class="text-base font-medium text-slate-900">
            {{ MICRO.STRUCTURE_FUNCTION.title }}
          </p>
          <div class="space-y-2" role="radiogroup" :aria-label="MICRO.STRUCTURE_FUNCTION.title">
            <button
              v-for="opt in MICRO.STRUCTURE_FUNCTION.options"
              :key="opt.id"
              type="button"
              role="radio"
              :aria-checked="selectedFunctionId === opt.id"
              class="flex w-full rounded-2xl border px-4 py-3.5 text-left text-sm font-medium leading-relaxed transition"
              :class="
                selectedFunctionId === opt.id
                  ? 'border-indigo-400 bg-indigo-50/90 text-indigo-950 ring-2 ring-indigo-500/20'
                  : 'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50/80'
              "
              @click="selectedFunctionId = opt.id"
            >
              {{ opt.label }}
            </button>
          </div>
          <div class="flex flex-wrap gap-3 pt-1">
            <PrimaryButton
              data-testid="structure-micro-continue"
              :disabled="loading || submitting || !selectedFunctionId"
              @click="structureMicroStep = 'confirm'"
            >
              {{ MICRO.STRUCTURE_MICRO_CTA.continue }}
            </PrimaryButton>
          </div>
        </div>

        <div v-else key="confirm" class="space-y-3">
          <p class="text-base font-medium text-slate-900">
            {{ MICRO.STRUCTURE_CONFIRM.title }}
          </p>
          <label class="sr-only" for="scaffold-structure-confirm-input">{{ inputLabel }}</label>
          <textarea
            id="scaffold-structure-confirm-input"
            v-model="draftValue"
            data-testid="driving-seat-input"
            rows="3"
            class="max-h-36 w-full resize-y rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500/20"
            :disabled="loading || submitting"
            :placeholder="MICRO.STRUCTURE_CONFIRM.placeholder"
          />
          <div class="flex flex-wrap items-center gap-3">
            <PrimaryButton
              :loading="submitting"
              :disabled="loading || submitting || !draftValue.trim()"
              @click="emitStructurePositionSubmit"
            >
              {{ MICRO.STRUCTURE_CONFIRM.submit }}
            </PrimaryButton>
            <button
              type="button"
              class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="loading || submitting"
              @click="onStarterClick"
            >
              {{ WIREFRAME.submitSecondary }}
            </button>
          </div>

          <!-- 脚手架：轻量按钮（收口阶段再给） -->
          <div class="space-y-2 pt-1">
            <div class="flex flex-wrap gap-2">
              <button
                v-for="hint in scaffoldHints"
                :key="hint.id"
                type="button"
                class="rounded-full border border-slate-200 bg-slate-50/90 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-slate-300 hover:bg-white"
                :class="openHintId === hint.id ? 'border-indigo-300 bg-indigo-50 text-indigo-900' : ''"
                @click="toggleHint(hint.id)"
              >
                {{ hint.label }}
              </button>
            </div>
            <div
              v-if="openHintId && activeHintBody"
              class="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm leading-6 whitespace-pre-line text-slate-800"
              role="region"
              aria-live="polite"
            >
              {{ activeHintBody }}
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 默认：大输入区 -->
    <div v-else class="space-y-3">
      <label class="sr-only" for="scaffold-wireframe-input">{{ inputLabel }}</label>
      <textarea
        id="scaffold-wireframe-input"
        v-model="draftValue"
        data-testid="driving-seat-input"
        rows="5"
        class="w-full resize-y rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500/20"
        :disabled="loading || submitting"
        :placeholder="inputPlaceholder"
      />
      <div class="flex flex-wrap items-center gap-3">
        <PrimaryButton
          :loading="submitting"
          :disabled="loading || submitting || !draftValue.trim()"
          @click="emit('submit')"
        >
          {{ WIREFRAME.submitPrimary }}
        </PrimaryButton>
        <button
          type="button"
          class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-800 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="loading || submitting"
          @click="onStarterClick"
        >
          {{ WIREFRAME.submitSecondary }}
        </button>
      </div>
    </div>

    <!-- 脚手架提示（非渐进位置卡） -->
    <div v-if="!isStructurePositionFlow" class="space-y-2">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="hint in scaffoldHints"
          :key="hint.id"
          type="button"
          class="rounded-full border border-slate-200 bg-slate-50/90 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-slate-300 hover:bg-white"
          :class="openHintId === hint.id ? 'border-indigo-300 bg-indigo-50 text-indigo-900' : ''"
          @click="toggleHint(hint.id)"
        >
          {{ hint.label }}
        </button>
      </div>
      <div
        v-if="openHintId && activeHintBody"
        class="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm leading-6 whitespace-pre-line text-slate-800"
        role="region"
        aria-live="polite"
      >
        {{ activeHintBody }}
      </div>
    </div>

    <!-- 提交后反馈 -->
    <div
      v-if="lastResult"
      class="rounded-2xl border px-4 py-4 text-sm"
      :class="
        lastResult.validation.passed
          ? 'border-emerald-200 bg-emerald-50/90 text-emerald-950'
          : 'border-accent/30 bg-accent-muted/85 text-accent-hover'
      "
      data-testid="scaffold-wireframe-feedback"
    >
      <p class="text-xs font-semibold uppercase tracking-wide opacity-90">
        {{ lastResult.validation.passed ? WIREFRAME.feedbackPass : WIREFRAME.feedbackFail }}
      </p>
      <p class="mt-2 leading-7">
        {{ feedbackOneLiner }}
      </p>
      <p v-if="!lastResult.validation.passed && fixHintLine" class="mt-2 text-sm font-medium leading-6">
        {{ fixHintLine }}
      </p>
      <div
        v-if="isTraining && !lastResult.validation.passed && firstTrainingProblem"
        class="mt-3 rounded-xl border border-accent/25 bg-white/70 px-3 py-2 text-xs leading-5"
      >
        {{ firstTrainingProblem }}
      </div>
      <div v-if="lastResult.validation.passed && nextStepLabel" class="mt-4">
        <PrimaryButton
          :disabled="submitting"
          @click="emit('continue-next')"
        >
          {{ nextStepLabel }}
        </PrimaryButton>
      </div>
    </div>

    <!-- 折叠说明 -->
    <details class="group rounded-2xl border border-slate-200/80 bg-white/60">
      <summary
        class="cursor-pointer list-none px-4 py-3 text-sm font-medium text-slate-700 marker:content-none [&::-webkit-details-marker]:hidden"
      >
        <span class="inline-flex items-center gap-2">
          {{ collapseTitle }}
          <span class="text-slate-400 transition group-open:rotate-180">▼</span>
        </span>
      </summary>
      <div
        v-if="isStructurePositionFlow"
        class="space-y-5 border-t border-slate-100 px-4 py-4 text-sm leading-6 text-slate-700"
      >
        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ MICRO.STRUCTURE_POSITION_COLLAPSE.whyTitle }}
          </p>
          <p class="mt-2">{{ MICRO.STRUCTURE_POSITION_COLLAPSE.whyBody }}</p>
        </section>
        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ MICRO.STRUCTURE_POSITION_COLLAPSE.avoidTitle }}
          </p>
          <p class="mt-2">{{ MICRO.STRUCTURE_POSITION_COLLAPSE.avoidBody }}</p>
        </section>
        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ MICRO.STRUCTURE_POSITION_COLLAPSE.outcomeTitle }}
          </p>
          <p class="mt-2">{{ MICRO.STRUCTURE_POSITION_COLLAPSE.outcomeBody }}</p>
        </section>
      </div>
      <div v-else class="space-y-5 border-t border-slate-100 px-4 py-4 text-sm leading-6 text-slate-700">
        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ WIREFRAME.whySectionTitle }}
          </p>
          <p class="mt-2">{{ whyParagraph }}</p>
        </section>
        <section v-if="forbiddenLines.length">
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ WIREFRAME.avoidSectionTitle }}
          </p>
          <p class="mt-2">这一步先不要进入：</p>
          <ul class="mt-2 list-disc space-y-1 pl-5">
            <li v-for="(line, i) in forbiddenLines" :key="i">{{ line }}</li>
          </ul>
        </section>
        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ WIREFRAME.progressSectionTitle }}
          </p>
          <p class="mt-2 font-medium text-slate-900">{{ stageMini.roundLabel }}</p>
          <p class="mt-1 text-slate-600">还差：{{ stageMini.untilNextPhase }}</p>
          <p v-if="nextPhaseHint" class="mt-2 text-slate-600">{{ nextPhaseHint }}</p>
        </section>
        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {{ WIREFRAME.phaseInfoSectionTitle }}
          </p>
          <p class="mt-2">{{ phaseInfoLine }}</p>
        </section>
      </div>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import { DFS_BFS_ACTION } from '@/constants/dfsBfsStructureWorkbenchCopy'
import * as MICRO from '@/constants/structurePositionMicroStepsCopy'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import {
  SCAFFOLD_WIREFRAME_PAGE as WIREFRAME,
  getScaffoldWireframeHints,
  getStarterInsertText,
  mergePassCriteriaLines,
  shortFeedbackSentence,
  wireframeActionThirdLine,
  wireframeHeroSecondLine,
  type ScaffoldHintId,
} from '@/constants/executionScaffoldWireframeCopy'
import type { LearningActionCard, LearningScaffoldActionResult } from '@/types/scaffoldEngine'
import type {
  StageProgressMiniModel,
  WhyThisStepModel,
  WorkbenchPhaseProgressModel,
} from '@/types/taskExecutionWorkbench'
import type { StructureMicroStep } from '@/constants/structurePositionMicroStepsCopy'

const props = defineProps<{
  stageKey: string
  stageTitle: string
  stageGoal: string
  phaseGoal?: string
  stageDescription: string
  card: LearningActionCard | null
  phaseLabel: string
  loading: boolean
  submitting: boolean
  lastResult: LearningScaffoldActionResult | null
  whyThisStep: WhyThisStepModel
  stageMini: StageProgressMiniModel
  phaseProgress: WorkbenchPhaseProgressModel
  packId: string | null
  inputLabel: string
}>()

const draftValue = defineModel<string>('draftValue', { default: '' })

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'continue-next'): void
}>()

const openHintId = ref<ScaffoldHintId | null>(null)
const structureMicroStep = ref<StructureMicroStep>('classify')
const selectedClassifyId = ref<string | null>(null)
const selectedFunctionId = ref<string | null>(null)

const isStructurePositionFlow = computed(
  () =>
    props.stageKey === 'STRUCTURE' &&
    props.packId === 'ds_dfs_bfs' &&
    props.card?.actionId === DFS_BFS_ACTION.POSITION
)

const phaseTitleLine = computed(() => phaseCodeToFullZh(props.stageKey) || props.phaseLabel)

const heroSecondLine = computed(() => wireframeHeroSecondLine(props.stageKey, props.stageTitle))

const actionThirdLine = computed(() =>
  wireframeActionThirdLine(props.stageKey, props.packId, props.stageDescription)
)

const taskBody = computed(() => {
  const c = props.card
  if (!c) return '加载中…'
  return (c.singleAction || c.goal || c.title).trim()
})

const passStandardLine = computed(() => mergePassCriteriaLines(props.card))

const collapseTitle = computed(() =>
  isStructurePositionFlow.value ? MICRO.STRUCTURE_POSITION_COLLAPSE.title : WIREFRAME.collapseTitle
)

const inputPlaceholder = computed(() => WIREFRAME.inputPlaceholder)

const scaffoldHints = computed(() =>
  getScaffoldWireframeHints(props.packId, props.card?.actionId, props.card)
)

const activeHintBody = computed(() => {
  const id = openHintId.value
  if (!id) return ''
  return scaffoldHints.value.find((h) => h.id === id)?.body ?? ''
})

function toggleHint(id: ScaffoldHintId) {
  openHintId.value = openHintId.value === id ? null : id
}

watch(
  () => props.card?.actionId,
  () => {
    openHintId.value = null
  }
)

function resetStructurePositionFlow() {
  structureMicroStep.value = 'classify'
  selectedClassifyId.value = null
  selectedFunctionId.value = null
  draftValue.value = ''
}

watch(
  () => [isStructurePositionFlow.value, props.card?.actionId] as const,
  ([on]) => {
    if (on) {
      void nextTick(() => resetStructurePositionFlow())
    }
  },
  { immediate: true }
)

const forbiddenLines = computed(() => {
  const c = props.card
  if (!c) return []
  const raw = [...(c.forbiddenActions ?? []), ...(c.forbiddenPrompts ?? [])]
  return raw.map((s) => s.trim()).filter(Boolean)
})

const whyParagraph = computed(() => {
  const w = props.whyThisStep.whyNow?.trim()
  if (w) return w
  return WIREFRAME.fallbackWhyStructure
})

const phaseInfoLine = computed(() => {
  const g = props.phaseGoal?.trim() || props.stageGoal?.trim()
  if (g) return g
  return props.stageDescription?.trim() || '这一阶段只做结构定位，不做细节展开。'
})

const nextPhaseHint = computed(() => {
  const cur = props.phaseProgress.currentPhase
  if (cur === 'STRUCTURE') return '完成这一轮后，进入「机制理解」。'
  if (cur === 'UNDERSTANDING') return '完成这一轮后，进入「表达训练」。'
  if (cur === 'TRAINING') return '完成这一轮后，进入「反思收敛」。'
  return ''
})

const isTraining = computed(() => props.stageKey === 'TRAINING')

const firstTrainingProblem = computed(() => {
  const p = props.lastResult?.trainingFeedback?.detectedProblems?.[0]
  return p?.problemText?.trim() ?? ''
})

const feedbackOneLiner = computed(() => {
  const lr = props.lastResult
  if (!lr) return ''
  if (lr.validation.passed) {
    return shortFeedbackSentence(lr.tutor.content || lr.validation.message || '可以，继续。')
  }
  return shortFeedbackSentence(
    lr.validation.message || lr.tutor.content || '再补一句关键点。'
  )
})

const fixHintLine = computed(() => {
  const lr = props.lastResult
  if (!lr || lr.validation.passed) return ''
  const sug = lr.validation.suggestions?.[0]?.trim()
  if (sug) return sug
  return lr.tutor.nextPrompt?.trim() || ''
})

const nextStepLabel = computed(() => {
  const hint = props.card?.nextActionHint?.trim()
  if (hint) return `进入下一步：${hint}`
  return WIREFRAME.nextStepCta
})

function classifyLabel(): string {
  const id = selectedClassifyId.value
  const o = MICRO.STRUCTURE_CLASSIFY.options.find((x) => x.id === id)
  return o?.label ?? ''
}

function functionLabel(): string {
  const id = selectedFunctionId.value
  const o = MICRO.STRUCTURE_FUNCTION.options.find((x) => x.id === id)
  return o?.label ?? ''
}

function buildStructurePositionPayload(one: string): string {
  return [`【归类】${classifyLabel()}`, `【作用】${functionLabel()}`, one].join('\n')
}

function emitStructurePositionSubmit() {
  const raw = draftValue.value.trim()
  if (!raw) return
  if (raw.startsWith('【归类】')) {
    emit('submit')
    return
  }
  draftValue.value = buildStructurePositionPayload(raw)
  emit('submit')
}

function onStarterClick() {
  const text = getStarterInsertText(props.packId, props.card?.actionId, props.card)
  draftValue.value = draftValue.value.trim() ? `${draftValue.value.trim()}\n${text}` : text
  void nextTick(() => {
    document.querySelector<HTMLTextAreaElement>('[data-testid="driving-seat-input"]')?.focus()
  })
}
</script>

<style scoped>
.fade-step-enter-active,
.fade-step-leave-active {
  transition: opacity 0.2s ease;
}
.fade-step-enter-from,
.fade-step-leave-to {
  opacity: 0;
}
</style>
