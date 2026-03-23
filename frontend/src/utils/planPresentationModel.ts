import type { PlanStepIconKey, StepShell } from '@/constants/planStepShell'
import {
  mergeShellWithTaskTitle,
  shellRowForFourTaskSequence,
} from '@/constants/planStepShell'
import type { ShowcaseHeroConfig } from '@/constants/showcaseKnowledgeConfig'
import {
  resolveShowcaseKnowledge,
  type ShowcaseStepConfig,
} from '@/constants/showcaseKnowledgeConfig'
import type {
  LearnerProfileSnapshot,
  PlanPreviewData,
  PlanStage,
  ProgressItem,
  StructuredLearningGoal,
  TaskBlueprint,
} from '@/types/dto'

/** 卡片内 1️⃣2️⃣3️⃣ 行动引导 */
export type PlanActionGuide = {
  aiPrompt: string
  reflectionQuestions: string[]
  closingLine: string
}

/** 单步展示用 1-based，与 StepFlow 序号一致 */
export type PlanStep = {
  stepIndex: number
  /** StepFlow 短标题 */
  title: string
  /** 当前步卡片主标题 */
  headline: string
  /** 兼容旧字段：与 subtitle 一致 */
  description: string
  /** 已弃用展示，保留空数组避免误用 taskMethod */
  actions: string[]
  estimatedTime?: string
  taskId?: string
  subtitle: string
  icon: PlanStepIconKey
  whyThisStep: string
  oneLineObjective: string
  objectiveIntro?: string
  objectiveBullets?: string[]
  actionGuide: PlanActionGuide
  /** 演示知识点：口语化时长句，优先于 estimatedTime 展示 */
  timeLabel?: string
  uiVariant?: 'default' | 'showcase'
}

export type StepFlowStatus = 'CURRENT' | 'TODO' | 'DONE'

export type StepFlowItem = {
  stepIndex: number
  title: string
  subtitle: string
  icon: PlanStepIconKey
  status: StepFlowStatus
}

export type PlanShowcaseView = {
  hero: ShowcaseHeroConfig
  optionalTips?: string[]
}

export type PlanViewModel = {
  steps: PlanStep[]
  /** 指向 steps 数组的下标（0-based） */
  currentStepIndex: number
  pathSummaryLine: string
  optionalTips: string[]
  totalSteps: number
  /** 非空时表示演示知识点特化（规划页 Hero / 当前步卡片样式） */
  showcase: PlanShowcaseView | null
}

export type PlanViewModelContext = {
  structuredGoal?: StructuredLearningGoal | null
  learnerProfileSnapshot?: LearnerProfileSnapshot | null
  sessionId?: string | null
  progress?: ProgressItem | null
  taskSequence?: string[] | null
}

function truncate(s: string, max: number): string {
  if (s.length <= max) return s
  return s.slice(0, max) + '…'
}

function formatEstimated(minutes?: number): string | undefined {
  if (minutes == null || minutes <= 0) return undefined
  return `约 ${minutes} 分钟`
}

export function resolveTopicLabel(
  plan: PlanPreviewData,
  ctx: PlanViewModelContext
): string {
  const g = ctx.structuredGoal
  const fromTopic = g?.topics?.find((t) => t?.trim())?.trim()
  if (fromTopic) return truncate(fromTopic, 32)
  const intent = g?.intentDescription?.trim()
  if (intent) return truncate(intent, 32)
  const norm = g?.normalizedGoalText?.trim()
  if (norm) return truncate(norm, 32)
  const raw = g?.rawGoalText?.trim()
  if (raw) return truncate(raw, 32)
  const pg = plan.goal?.trim()
  if (pg) return truncate(pg, 32)
  return '当前主题'
}

/** 将后端模板中的占位符替换为主题词（不展示原始字段名） */
export function applyPromptTemplate(
  template: string | undefined,
  topic: string
): string {
  if (!template?.trim()) return ''
  let s = template.trim()
  s = s.replace(/【主题】/g, topic)
  s = s.replace(/【主题 A】/g, topic)
  s = s.replace(/【主题 B】/g, topic)
  return s
}

function defaultAiPromptForTask(task: TaskBlueprint, topic: string): string {
  const type = task.taskType
  switch (type) {
    case 'CONCEPT_EXPLAIN':
      return `请用最简单的话解释「${topic}」的定义，并给我一个最直观的例子。`
    case 'COMPARE_AND_CONNECT':
      return `请用我能听懂的方式，对比「${topic}」和容易混淆的相关概念，各说清一个关键差异。`
    case 'GUIDED_EXAMPLE':
      return `请用一个最小例子，带我把「${topic}」的关键步骤走一遍，并说明每一步在干什么。`
    case 'SELF_EXPLANATION':
      return `我想先自己用白话解释「${topic}」，讲完后再请你指出遗漏或误解。`
    case 'MICRO_PRACTICE':
      return `请出 1 道与「${topic}」相关的小题，我做完后请你检查思路。`
    case 'CHECKPOINT_REVIEW':
      return `请帮我自检：定义是否清楚、易混点能否区分、能否做一道最小题。`
    default:
      return `请围绕「${topic}」用简单步骤带我完成本步学习目标。`
  }
}

function buildAiPromptLine(task: TaskBlueprint, topic: string): string {
  const fromRecommended = applyPromptTemplate(
    task.recommendedPromptTemplate,
    topic
  )
  if (fromRecommended) return fromRecommended
  const fromLegacy = applyPromptTemplate(task.promptScaffold, topic)
  if (fromLegacy) return fromLegacy
  return defaultAiPromptForTask(task, topic)
}

function toQuestionLine(s: string): string {
  const t = s.trim()
  if (!t) return ''
  if (/[？?]|吗$|能否/.test(t)) return t
  return `${t}？`
}

function buildReflectionQuestions(task: TaskBlueprint): string[] {
  const out: string[] = []
  const seen = new Set<string>()
  const push = (line: string) => {
    const t = line.trim()
    if (!t || out.length >= 3 || seen.has(t)) return
    seen.add(t)
    out.push(toQuestionLine(t))
  }
  for (const q of task.selfEvaluationQuestions ?? []) {
    push(q)
  }
  for (const c of task.completionCriteria ?? []) {
    push(c)
  }
  if (out.length === 0) {
    push('试着说说：这一步里你最想先搞懂的一点是什么？')
  }
  if (out.length === 1) {
    push('如果讲给同桌听，你会从哪一句开头？')
  }
  return out.slice(0, 3)
}

function closingLineForTaskType(taskType: string | undefined): string {
  switch (taskType) {
    case 'SELF_EXPLANATION':
      return '写一句就行，不用漂亮，诚实最重要。'
    case 'CHECKPOINT_REVIEW':
      return '把还不太稳的地方随手记下来，下一步我们再一起补上。'
    case 'MICRO_PRACTICE':
      return '做完后，用一句话说说你从这道题里抓到的那一点。'
    case 'COMPARE_AND_CONNECT':
      return '用一句话说说：和你最容易混的那一块比，最关键的差别在哪？'
    case 'GUIDED_EXAMPLE':
      return '先合上材料，再试着自己复述一遍例子里到底发生了什么。'
    default:
      return '用你自己的话收尾一句，你会立刻知道哪里还虚。'
  }
}

function buildActionGuide(
  task: TaskBlueprint,
  topic: string,
  shell: StepShell
): PlanActionGuide {
  const reflectionQuestions =
    shell.reflectionLines && shell.reflectionLines.length
      ? shell.reflectionLines
      : buildReflectionQuestions(task)
  return {
    aiPrompt: buildAiPromptLine(task, topic),
    reflectionQuestions,
    closingLine: closingLineForTaskType(task.taskType),
  }
}

function showcaseStepToPlanStep(
  task: TaskBlueprint,
  index: number,
  shell: ShowcaseStepConfig
): PlanStep {
  return {
    stepIndex: index + 1,
    taskId: task.taskId,
    title: shell.flowTitle,
    headline: shell.headline,
    subtitle: shell.flowSubtitle,
    description: shell.flowSubtitle,
    whyThisStep: shell.whyFirst,
    oneLineObjective: '',
    objectiveIntro: shell.objectiveIntro,
    objectiveBullets: shell.objectiveBullets,
    icon: shell.icon,
    actionGuide: {
      aiPrompt: shell.suggestedPrompt,
      reflectionQuestions: shell.reflectionQuestions,
      closingLine: shell.closingLine,
    },
    actions: [],
    estimatedTime: formatEstimated(task.estimatedMinutes),
    timeLabel: shell.timeLabel,
    uiVariant: 'showcase',
  }
}

function taskToPlanStep(
  task: TaskBlueprint,
  index: number,
  topic: string,
  rowShell: ReturnType<typeof shellRowForFourTaskSequence>
): PlanStep {
  const rowStep = rowShell?.[index] ?? null
  const shell = mergeShellWithTaskTitle(
    task.title ?? '',
    task.taskType,
    rowStep
  )

  const actionGuide = buildActionGuide(task, topic, shell)

  return {
    stepIndex: index + 1,
    taskId: task.taskId,
    title: shell.title,
    headline: shell.headline?.trim() || shell.title,
    subtitle: shell.subtitle,
    description: shell.subtitle,
    whyThisStep: shell.whyThisStep,
    oneLineObjective: shell.oneLineObjective,
    objectiveIntro: shell.objectiveIntro,
    objectiveBullets: shell.objectiveBullets,
    icon: shell.icon,
    actionGuide,
    actions: [],
    estimatedTime: formatEstimated(task.estimatedMinutes),
  }
}

function stageToPlanStep(stage: PlanStage, index: number): PlanStep {
  const title = stage.title?.trim() || `第 ${index + 1} 小段`
  const subtitle =
    stage.objective?.trim() || '先知道这一段在帮你干嘛，走起来就不慌'
  return {
    stepIndex: index + 1,
    title,
    headline: title,
    subtitle,
    description: subtitle,
    whyThisStep:
      '我们拆成几小段，是为了让你每一步都能真的走完，而不是一口气被压垮。',
    oneLineObjective: '你能用一句话说清：这一段走完，你最少要多了哪一点把握。',
    icon: 'brain',
    actionGuide: {
      aiPrompt: `请用特别白话的方式，帮我搞懂「${title}」这一段到底在解决什么，我怎样算做到了。`,
      reflectionQuestions: [
        stage.objective?.trim()
          ? '试着用自己的话，把这一段的目标复述一遍。'
          : '这一段结束后，你希望自己能更确定哪一件事？',
      ],
      closingLine: '写一句就行：你打算怎么把这一段轻轻推进一点点。',
    },
    actions: [],
    estimatedTime: formatEstimated(stage.estimatedMinutes),
  }
}

function entryFallbackStep(entryTitle: string | undefined, minutes?: number): PlanStep {
  const title = entryTitle?.trim() || '先从最好下手的那一小步开始'
  return {
    stepIndex: 1,
    title,
    headline: title,
    subtitle: '先走一小步，你会更容易进入状态',
    description: '先走一小步，你会更容易进入状态',
    whyThisStep:
      '不用一上来就完美，我们先从一个顺手的入口开始，你会发现后面顺很多。',
    oneLineObjective: '你能照着引导，把最小的那一下真的做完。',
    icon: 'brain',
    actionGuide: {
      aiPrompt: `我想从「${title}」这一小步开始学，请你带我把步子拆得特别小、特别具体。`,
      reflectionQuestions: ['做完这一点点之后，你心里有没有更踏实一点？是哪里？'],
      closingLine: '用一句话记下你刚才实际做了什么，不用完整。',
    },
    actions: [],
    estimatedTime: formatEstimated(minutes),
  }
}

function buildPathSummaryLine(
  plan: PlanPreviewData,
  ctx: PlanViewModelContext
): string {
  const g = ctx.structuredGoal
  const goalLine =
    g?.intentDescription?.trim() ||
    g?.normalizedGoalText?.trim() ||
    g?.rawGoalText?.trim() ||
    plan.goal?.trim() ||
    ''

  if (goalLine) return truncate(goalLine, 72)
  return '先不着急，我们一步一步来，把眼前这一小步走稳。'
}

function taskSequencesAlign(plan: PlanPreviewData, seq: string[]): boolean {
  const ids = plan.tasks?.map((t) => t.taskId) ?? []
  if (!ids.length) return false
  if (ids.length !== seq.length) return false
  return ids.every((id, i) => id === seq[i])
}

function resolveCurrentStepIndex(
  steps: PlanStep[],
  plan: PlanPreviewData,
  ctx: PlanViewModelContext
): number {
  if (!steps.length) return 0
  if (!ctx.sessionId || !ctx.progress || !ctx.taskSequence?.length) return 0

  const seq = ctx.taskSequence
  if (plan.tasks?.length && !taskSequencesAlign(plan, seq)) return 0

  const idx = ctx.progress.currentIndex - 1
  if (idx < 0) return 0
  if (idx >= steps.length) return steps.length - 1
  return idx
}

const DEFAULT_OPTIONAL_TIPS = [
  '看完后试着在白纸上随手画一遍，画歪了也没关系',
  '合上笔记，用一句大白话讲给「完全不懂的人」听',
]

/**
 * 将 PlanPreview 映射为路径型规划页视图模型（纯前端，不改 API）。
 */
export function buildPlanViewModel(
  plan: PlanPreviewData | null,
  ctx: PlanViewModelContext = {}
): PlanViewModel | null {
  if (!plan) return null

  const topic = resolveTopicLabel(plan, ctx)
  let steps: PlanStep[]
  const showcaseConfig = resolveShowcaseKnowledge(plan, ctx)
  const useShowcase =
    !!showcaseConfig &&
    plan.tasks?.length === 4 &&
    showcaseConfig.steps.length === 4

  if (useShowcase && plan.tasks) {
    steps = plan.tasks.map((t, i) =>
      showcaseStepToPlanStep(t, i, showcaseConfig.steps[i]!)
    )
  } else if (plan.tasks?.length) {
    const types = plan.tasks.map((t) => t.taskType ?? '')
    const rowShell = shellRowForFourTaskSequence(types)
    steps = plan.tasks.map((t, i) => taskToPlanStep(t, i, topic, rowShell))
  } else if (plan.stages?.length) {
    steps = plan.stages.map((s, i) => stageToPlanStep(s, i))
  } else {
    steps = [entryFallbackStep(plan.recommendedEntry?.title, plan.recommendedEntry?.estimatedMinutes)]
  }

  const currentStepIndex = resolveCurrentStepIndex(steps, plan, ctx)

  const showcaseView: PlanShowcaseView | null = useShowcase
    ? {
        hero: showcaseConfig!.hero,
        optionalTips: showcaseConfig!.optionalTips,
      }
    : null

  return {
    steps,
    currentStepIndex,
    pathSummaryLine: useShowcase
      ? showcaseConfig!.hero.auxiliaryLine
      : buildPathSummaryLine(plan, ctx),
    optionalTips:
      useShowcase && showcaseConfig!.optionalTips?.length
        ? [...showcaseConfig!.optionalTips!]
        : [...DEFAULT_OPTIONAL_TIPS],
    totalSteps: steps.length,
    showcase: showcaseView,
  }
}

export function planStepsToFlowItems(vm: PlanViewModel): StepFlowItem[] {
  return vm.steps.map((s, i) => {
    let status: StepFlowStatus
    if (i < vm.currentStepIndex) status = 'DONE'
    else if (i === vm.currentStepIndex) status = 'CURRENT'
    else status = 'TODO'
    return {
      stepIndex: s.stepIndex,
      title: s.title,
      subtitle: s.subtitle,
      icon: s.icon,
      status,
    }
  })
}
