<script setup lang="ts">
import { computed } from 'vue'
import type { LearningActionCard as Card } from '@/types/scaffoldEngine'
import type { LearningScaffoldActionResult } from '@/types/scaffoldEngine'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'

const props = defineProps<{
  /** 后端 stageKey，如 TRAINING */
  stageKey?: string
  /** 顶部阶段标签，如「STRUCTURE · 结构建立」 */
  phaseLabel?: string
  stageTitle: string
  stageGoal: string
  stageDescription: string
  card: Card | null
  loading: boolean
  submitting: boolean
  lastResult: LearningScaffoldActionResult | null
  inputLabel: string
}>()

const headerEyebrow = computed(() => props.phaseLabel ?? `STRUCTURE · ${props.stageTitle}`)

const aspectFeedback = computed(() => {
  const v = props.lastResult?.validation
  if (!v || v.passed) return null
  const matched = v.matchedAspects?.filter(Boolean) ?? []
  const missing = v.missingAspects?.filter(Boolean) ?? []
  if (matched.length === 0 && missing.length === 0) return null
  return { matched, missing }
})

const isTraining = computed(() => props.stageKey === 'TRAINING')
const isReflection = computed(() => props.stageKey === 'REFLECTION')

const roundHint = computed(() => {
  const r = props.lastResult
  if (!r?.actionRuntime?.attemptNo) return '第 1 轮表达'
  const n = r.actionRuntime.attemptNo
  if (r.validation.passed) return `第 ${n} 轮已通过`
  return `第 ${n} 轮反馈 · 请继续第 ${n + 1} 轮重构`
})

const trainingProblems = computed(() => {
  const tf = props.lastResult?.trainingFeedback
  if (!tf?.detectedProblems?.length) return []
  return tf.detectedProblems.slice(0, 2)
})

const errorTypeLabels: Record<string, string> = {
  MECHANISM_ERROR: '机制',
  CAUSAL_GAP: '因果',
  VAGUE_EXPRESSION: '表达',
  MISSING_STEP: '环节',
}

const draftValue = defineModel<string>('draftValue', { default: '' })

const emit = defineEmits<{
  submit: []
}>()

function appendChip(chip: string) {
  const cur = draftValue.value.trim()
  draftValue.value = cur ? `${cur}\n${chip}` : chip
}
</script>

<template>
  <div class="space-y-4">
    <header class="rounded-2xl border border-slate-200/80 bg-white/90 p-5 shadow-sm">
      <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ headerEyebrow }}</p>
      <h2 class="mt-1 text-lg font-semibold text-slate-900">当前目标</h2>
      <p class="mt-2 text-sm leading-relaxed text-slate-600">{{ stageGoal }}</p>
      <p class="mt-2 text-sm text-slate-500">{{ stageDescription }}</p>
    </header>

    <div
      v-if="card"
      class="rounded-2xl border border-indigo-200/60 bg-gradient-to-br from-indigo-50/90 to-white p-5 shadow-sm ring-1 ring-indigo-100"
    >
      <p class="text-xs font-medium text-indigo-700">
        {{ isTraining ? '当前训练动作' : isReflection ? '当前反思动作' : '当前动作' }}
      </p>
      <h3 class="mt-1 text-xl font-semibold text-slate-900">{{ card.title }}</h3>
      <p class="mt-2 text-sm font-medium text-slate-800">{{ card.goal }}</p>
      <p class="mt-2 text-sm leading-relaxed text-slate-600">{{ card.instructions }}</p>

      <p
        v-if="isTraining"
        class="mt-3 rounded-lg border border-indigo-200/80 bg-white/80 px-3 py-2 text-xs font-medium text-indigo-900"
      >
        {{ roundHint }}
      </p>

      <label class="mt-4 block text-sm font-medium text-slate-700">{{ inputLabel }}</label>
      <textarea
        v-model="draftValue"
        class="mt-2 min-h-[132px] w-full resize-y rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 shadow-inner outline-none ring-indigo-500/0 transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-500/30"
        :disabled="loading || submitting"
        :placeholder="
          isReflection ? '写具体，可对照右侧通过标准；避免泛泛总结。' : '先写完整再提交，避免一句话带过。'
        "
      />
      <div class="mt-4 flex flex-wrap gap-2">
        <SecondaryButton
          v-for="(chip, i) in card.allowedPrompts"
          :key="'allow-' + i"
          class="!px-3 !py-1.5 text-xs"
          @click="appendChip(chip)"
        >
          {{ chip }}
        </SecondaryButton>
      </div>
    </div>

    <div v-else-if="!loading" class="rounded-xl border border-amber-200 bg-amber-50/80 p-4 text-sm text-amber-900">
      当前没有可用的动作卡，请刷新页面重试。
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
      <p class="font-semibold">{{ lastResult.validation.passed ? '校验通过' : '需要调整' }}</p>

      <div
        v-if="isReflection && !lastResult.validation.passed"
        class="mt-3 space-y-2 border-t border-current/10 pt-3 text-xs"
      >
        <p class="font-semibold opacity-90">轻量反馈</p>
        <p v-if="lastResult.validation.message" class="opacity-90">{{ lastResult.validation.message }}</p>
      </div>

      <div
        v-else-if="isTraining && !lastResult.validation.passed && trainingProblems.length"
        class="mt-3 space-y-3 border-t border-current/10 pt-3"
      >
        <p class="text-xs font-semibold opacity-90">本轮问题（最多 2 条）</p>
        <ul class="space-y-2">
          <li
            v-for="(p, i) in trainingProblems"
            :key="'tp-' + i"
            class="rounded-lg border border-rose-200/60 bg-white/60 px-3 py-2 text-xs"
          >
            <span
              class="mr-2 inline-block rounded bg-rose-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase text-rose-700"
            >
              {{ errorTypeLabels[p.errorType] ?? p.errorType }}
            </span>
            {{ p.problemText }}
          </li>
        </ul>
        <p v-if="lastResult.trainingFeedback?.revisionInstruction" class="text-xs font-medium">
          请重构表达：{{ lastResult.trainingFeedback.revisionInstruction }}
        </p>
      </div>

      <div
        v-else-if="!isReflection && aspectFeedback && (!lastResult.validation.passed)"
        class="mt-3 grid gap-3 border-t border-current/10 pt-3 sm:grid-cols-2"
      >
        <div v-if="aspectFeedback.matched.length">
          <p class="text-xs font-semibold opacity-80">已覆盖</p>
          <ul class="mt-1 list-inside list-disc text-xs opacity-90">
            <li v-for="(m, i) in aspectFeedback.matched" :key="'m-' + i">{{ m }}</li>
          </ul>
        </div>
        <div v-if="aspectFeedback.missing.length">
          <p class="text-xs font-semibold opacity-80">待补充</p>
          <ul class="mt-1 list-inside list-disc text-xs opacity-90">
            <li v-for="(m, i) in aspectFeedback.missing" :key="'x-' + i">{{ m }}</li>
          </ul>
        </div>
      </div>
      <p class="mt-1 opacity-90">{{ lastResult.tutor.content }}</p>
      <p v-if="lastResult.tutor.nextPrompt" class="mt-2 text-xs opacity-90">
        提示：{{ lastResult.tutor.nextPrompt }}
      </p>
    </div>

    <div class="flex justify-end">
      <button
        type="button"
        class="inline-flex items-center justify-center rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="loading || submitting || !draftValue.trim()"
        @click="emit('submit')"
      >
        {{
          submitting
            ? '提交中…'
            : isTraining
              ? '提交本轮表达'
              : isReflection
                ? '提交反思'
                : '提交本轮'
        }}
      </button>
    </div>
  </div>
</template>
