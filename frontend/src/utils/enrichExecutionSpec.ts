import { defaultStuckActions, resolveStartModes } from '@/constants/executionKpTemplates'
import type { KnowledgeDemoConfig, KnowledgeDemoStageCode } from '@/constants/KnowledgeConfig'
import { getKnowledgeDemoConfig } from '@/constants/KnowledgeConfig'
import type { KnowledgePack, KnowledgePointExecutionTemplate } from '@/types/knowledgePack'
import type { ExecutionScaffoldCardModel } from '@/types/executionGuide'
import type {
  CurrentTaskItem,
  RecommendedUserActionItem,
  TaskScaffoldResponse,
} from '@/types/dto'

const STAGE_DISPLAY_ZH: Record<string, string> = {
  STRUCTURE: '结构建立',
  UNDERSTANDING: '机制理解',
  TRAINING: '应用训练',
  REFLECTION: '反思校准',
}

const DELIVERABLE_BY_STATE: Record<string, string> = {
  ORIENT: '产出：一句能站住脚的框架（是什么 / 解决什么）',
  EXPLORE: '产出：补上一处关键因果或机制',
  SELF_EXPLAIN: '产出：用自己的话串起来的小段表述',
  REMEDIAL: '产出：补上刚才缺的那一句判断',
  CHECK: '产出：独立写出的检查题答案',
  PASS: '产出：一句总结 + 两个可复用要点',
}

const INPUT_PLACEHOLDER_SOFT = '不确定也没关系，先写一版。'

function trimText(text?: string | null) {
  return text?.trim() || ''
}

function firstLine(text: string, max = 120) {
  const line = text
    .split(/\n/)
    .map((s) => s.trim())
    .find(Boolean)
  if (!line) return ''
  return line.length <= max ? line : `${line.slice(0, max - 1)}…`
}

function inferKnowledgePointType(
  pack: KnowledgePack | null,
  scaffold: TaskScaffoldResponse | null,
  task: CurrentTaskItem
): KnowledgePointExecutionTemplate {
  if (pack?.executionTemplate) return pack.executionTemplate
  const kt = scaffold?.knowledgeType
  if (kt === 'COMPARE' || kt === 'CHOICE') return 'CONCEPT'
  if (kt === 'SEQUENCE') return 'PROCESS'
  if (kt === 'MECHANISM') return 'PROCESS'
  const tt = task.taskType || ''
  if (tt.includes('MICRO_PRACTICE') || tt === 'CHECKPOINT_REVIEW') return 'PROBLEM'
  const blob = `${task.title || ''}${task.goal || ''}`
  if (/链表|树|表|目录|栈|队列|堆|图|结构|页表|段表/.test(blob)) return 'STRUCTURE'
  return 'CONCEPT'
}

function buildCoreQuestion(
  task: CurrentTaskItem,
  scaffold: TaskScaffoldResponse | null,
  pack: KnowledgePack | null
): string {
  const fromTutor = pack?.tutor.suggestedQuestions?.[0]
  if (fromTutor) return firstLine(fromTutor, 100)
  const selfQ = task.selfEvaluationQuestions?.map(trimText).find(Boolean)
  if (selfQ) return firstLine(selfQ, 100)
  const g = trimText(task.goal) || trimText(scaffold?.learningObjective)
  if (g) return firstLine(g, 100)
  return '这个知识点主要解决什么问题？'
}

function buildAskOpenCard(kpName: string): ExecutionScaffoldCardModel {
  return {
    id: 'scaffold-ask-open',
    title: '提问',
    hint: '',
    actionLabel: '我有一个具体问题',
    prompt: `关于「${kpName}」，我想先问一个具体问题：`,
    behavior: 'prefill',
  }
}

function augmentPromptWithPhase(base: string, phasePrompts: string[]): string {
  const lines = phasePrompts.map((p) => p.trim()).filter(Boolean)
  if (!lines.length) return base
  const block = lines.map((p, i) => `${i + 1}. ${p}`).join('\n')
  return `${base}\n\n【本轮参考线索】\n${block}`
}

function mergeScaffoldCards(
  kpType: KnowledgePointExecutionTemplate,
  kpName: string,
  pack: KnowledgePack | null,
  scaffold: TaskScaffoldResponse | null,
  knowledgeDemo: KnowledgeDemoConfig | null,
  stageMachineLabel: string
): ExecutionScaffoldCardModel[] {
  const phaseKey = stageMachineLabel as KnowledgeDemoStageCode
  const phasePrompts =
    knowledgeDemo?.execution[phaseKey]?.prompts?.map((p) => p.trim()).filter(Boolean) ?? []

  const defaults = resolveStartModes(kpType, kpName)
  const packCards = pack?.execution.scaffoldCards
  const templates = scaffold?.recommendedAskTemplates?.filter(Boolean).slice(0, 3) ?? []

  if (knowledgeDemo && phasePrompts.length) {
    const core = defaults.map((m, i) => ({
      id: m.id,
      title: m.title,
      hint: m.hint,
      actionLabel: m.actionLabel,
      prompt: augmentPromptWithPhase(templates[i] || m.prompt, phasePrompts),
      behavior: m.behavior ?? 'send',
    }))
    const ask = buildAskOpenCard(kpName)
    return [
      ...core,
      {
        ...ask,
        prompt: augmentPromptWithPhase(ask.prompt, phasePrompts),
      },
    ]
  }

  if (packCards?.length === 3) {
    const mapped = packCards.map((c, i) => ({
      id: c.id,
      title: c.title,
      hint: c.hint,
      prompt: templates[i] || c.prompt,
      actionLabel: c.actionLabel || defaults[i]!.actionLabel,
      behavior: (defaults[i]?.behavior as 'send' | 'prefill' | undefined) ?? 'send',
    }))
    return [...mapped, buildAskOpenCard(kpName)]
  }

  const core = defaults.map((m, i) => ({
    id: m.id,
    title: m.title,
    hint: m.hint,
    actionLabel: m.actionLabel,
    prompt: templates[i] || m.prompt,
    behavior: m.behavior ?? 'send',
  }))
  return [...core, buildAskOpenCard(kpName)]
}

function mergeStuckActions(
  kpType: KnowledgePointExecutionTemplate,
  recommended: RecommendedUserActionItem[]
): string[] {
  const base = defaultStuckActions(kpType)
  if (recommended.length >= 3) {
    return recommended
      .slice(0, 3)
      .map((r) => r.label)
      .filter(Boolean)
  }
  if (recommended.length > 0) {
    const labels = recommended.map((r) => r.label).filter(Boolean)
    return [...labels, ...base].slice(0, 3)
  }
  return base
}

function estimatedLabel(
  task: CurrentTaskItem,
  state: string,
  stepTimes: Record<string, string>
): string {
  const m = task.estimatedMinutes
  if (typeof m === 'number' && m > 0) return `${m} 分钟`
  return stepTimes[state] || '约几分钟'
}

export interface EnrichExecutionSpecInput {
  task: CurrentTaskItem
  scaffold: TaskScaffoldResponse | null
  pack: KnowledgePack | null
  taskState: string
  legacyComplete: boolean
  checkpointQuestion: string
  activeKnowledgePointTitle: string
  recommendedUserActions: RecommendedUserActionItem[]
  /** 来自 STEP_COPY[state].stageLabel，如 STRUCTURE */
  stageMachineLabel: string
  stepTimes: Record<string, string>
}

export interface EnrichedExecutionSpec {
  knowledgePointName: string
  knowledgePointType: KnowledgePointExecutionTemplate
  roundGoal: string
  completionStandardLines: string[]
  estimatedTimeLabel: string
  currentDirective: string
  inputPlaceholderSoft: string
  stageDisplay: string
  currentDeliverable: string
  stuckActions: string[]
  scaffoldCards: ExecutionScaffoldCardModel[]
  phasePromptChips: string[]
}

function normalizeState(taskState: string, legacyComplete: boolean) {
  if (legacyComplete) return 'PASS'
  return taskState
}

export function buildCurrentDirective(input: {
  state: string
  task: CurrentTaskItem
  pack: KnowledgePack | null
  checkpointQuestion: string
  coreQuestion: string
  /** KnowledgeConfig 当前阶段一句 */
  knowledgeTaskLine?: string
}): string {
  const { state, task, pack, checkpointQuestion, coreQuestion, knowledgeTaskLine } = input
  if (state === 'CHECK') {
    const q = trimText(checkpointQuestion) || pack?.checkpoint.checkpointPrompt
    if (q) return `现在请你用一两句话回答：${q}`
    return '现在请你用一两句话完成这道检查题。'
  }
  if (knowledgeTaskLine) {
    if (state === 'ORIENT' || state === 'EXPLORE' || state === 'SELF_EXPLAIN' || state === 'REMEDIAL') {
      return `当前任务：${knowledgeTaskLine}`
    }
    if (state === 'PASS') {
      return `当前任务：${knowledgeTaskLine}`
    }
  }
  if (state === 'SELF_EXPLAIN') {
    return '现在请你用自己的话把关系讲顺（三四句即可）。'
  }
  if (state === 'REMEDIAL') {
    return '现在请把刚才缺的那一句补上，写清你的判断依据。'
  }
  if (state === 'PASS') {
    return '现在请写一句能带走的总结，并留下两个关键判断。'
  }
  if (state === 'ORIENT') {
    return `现在请你先用一句话回答：${coreQuestion}`
  }
  if (state === 'EXPLORE') {
    return `顺着上一步，再补一句：${coreQuestion}`
  }
  return `现在请你继续完成：${trimText(task.goal) || coreQuestion}`
}

export function enrichExecutionSpec(input: EnrichExecutionSpecInput): EnrichedExecutionSpec {
  const state = normalizeState(input.taskState, input.legacyComplete)
  const kpName = trimText(input.activeKnowledgePointTitle) || trimText(input.task.title) || '当前知识点'
  const kpType = inferKnowledgePointType(input.pack, input.scaffold, input.task)
  const coreQuestion = buildCoreQuestion(input.task, input.scaffold, input.pack)

  const knowledgeDemo = getKnowledgeDemoConfig(input.pack?.id ?? null)
  const phaseKey = input.stageMachineLabel as KnowledgeDemoStageCode
  const knowledgeTaskLine = knowledgeDemo?.execution[phaseKey]?.currentTaskLine

  const roundGoal =
    firstLine(trimText(input.scaffold?.learningObjective) || trimText(input.task.goal), 140) ||
    `弄懂「${kpName}」在当前任务里要求你掌握的最小闭环。`

  const crit = [
    ...(input.task.completionCriteria ?? []).map(trimText).filter(Boolean),
    ...(input.scaffold?.completionSignals ?? []).map(trimText).filter(Boolean),
  ]
  const completionStandardLines = crit.length ? crit.slice(0, 2) : ['能独立说出关键判断', '能对应到当前知识点']

  const phaseChips =
    knowledgeDemo?.execution[phaseKey]?.prompts?.map((p) => p.trim()).filter(Boolean).slice(0, 4) ?? []

  return {
    knowledgePointName: kpName,
    knowledgePointType: kpType,
    roundGoal,
    completionStandardLines,
    estimatedTimeLabel: estimatedLabel(input.task, state, input.stepTimes),
    currentDirective: buildCurrentDirective({
      state,
      task: input.task,
      pack: input.pack,
      checkpointQuestion: input.checkpointQuestion,
      coreQuestion,
      knowledgeTaskLine,
    }),
    inputPlaceholderSoft: INPUT_PLACEHOLDER_SOFT,
    stageDisplay: stageDisplayFromMachineLabel(input.stageMachineLabel),
    currentDeliverable: DELIVERABLE_BY_STATE[state] || '产出：当前步骤要求的最小结果',
    stuckActions: mergeStuckActions(kpType, input.recommendedUserActions),
    scaffoldCards: mergeScaffoldCards(
      kpType,
      kpName,
      input.pack,
      input.scaffold,
      knowledgeDemo,
      input.stageMachineLabel
    ),
    phasePromptChips: phaseChips,
  }
}

/** 由调用方传入 stageLabel（STRUCTURE 等），再映射为中文阶段名 */
export function stageDisplayFromMachineLabel(stageLabel: string | undefined): string {
  if (!stageLabel) return '结构建立'
  return STAGE_DISPLAY_ZH[stageLabel] || stageLabel
}
