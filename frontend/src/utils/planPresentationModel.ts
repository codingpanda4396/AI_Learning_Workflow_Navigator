import type { PlanStepIconKey, StepShell } from '@/constants/planStepShell'
import {
  mergeShellWithTaskTitle,
  shellRowForFourTaskSequence,
} from '@/constants/planStepShell'
import type {
  ShowcaseFocusType,
  ShowcaseHeroConfig,
  ShowcaseMindImageHint,
} from '@/constants/showcaseKnowledgeConfig'
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
import { workflowTopicLabelFromStructuredGoal } from '@/utils/workflowTopicLabel'

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
  mindImageHint?: ShowcaseMindImageHint
  focusType?: ShowcaseFocusType
  judgmentTips?: string[]
  knowledgeLabel?: { title: string; subtitle?: string }
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
  const fromGoal = workflowTopicLabelFromStructuredGoal(ctx.structuredGoal)
  if (fromGoal) return fromGoal
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
        mindImageHint: showcaseConfig!.mindImageHint,
        focusType: showcaseConfig!.focusType,
        judgmentTips: showcaseConfig!.judgmentTips,
        knowledgeLabel: showcaseConfig!.knowledgeLabel,
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

export type PlanStageCode =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export type PlanStrategyOverview = {
  currentKnowledge: string
  recommendedStrategy: string
  recommendedStageCode: PlanStageCode
  recommendedStageLabel: string
  whyThisArrangement: string
  skipRisk: string
}

export type PlanStageCardView = {
  code: PlanStageCode
  stageIndex: number
  label: string
  title: string
  objective: string
  deliverable: string
  tutorRole: string
  checkpoint: string
  estimatedTime: string
  /** 用于汇总总时长；缺省或 0 时不计入 */
  estimatedMinutesTotal?: number
  taskCount: number
  isRecommended: boolean
  isCurrent: boolean
}

/** 四阶段路径条（扫读区） */
export type PlanPathStripItem = {
  code: PlanStageCode
  title: string
  scanLine: string
  estimatedLabel: string
  isRecommended: boolean
  isCurrent: boolean
}

export type PlanTaskCardView = {
  taskId: string
  title: string
  stageCode: PlanStageCode
  stageLabel: string
  actionGoal: string
  tutorSupport: string
  completionChecks: string[]
  estimatedTime: string
  estimatedMinutes?: number
}

export type PlanTaskGroupView = {
  stageCode: PlanStageCode
  stageLabel: string
  items: PlanTaskCardView[]
}

export type PlanBattleMapView = {
  strategyOverview: PlanStrategyOverview
  stageCards: PlanStageCardView[]
  taskGroupsByStage: PlanTaskGroupView[]
  recommendedStageCode: PlanStageCode
  /** 有会话进度时优先展开该阶段，否则为推荐起点阶段 */
  expandedStageCode: PlanStageCode
  pathStrip: PlanPathStripItem[]
  totalEstimatedMinutes: number
  totalEstimatedLabel: string
  /** 结论卡用：口语化推荐起点 */
  recommendedStartPhrase: string
  /** 结论卡用：一句原因（短） */
  whyShortLine: string
}

type StageMeta = {
  label: string
  title: string
  defaultObjective: string
  deliverable: string
  tutorRole: string
  checkpoint: string
  fallbackRisk: string
}

const PLAN_STAGE_ORDER: PlanStageCode[] = [
  'STRUCTURE',
  'UNDERSTANDING',
  'TRAINING',
  'REFLECTION',
]

/** 路径条上一句扫读说明（与四阶段模型对齐，保持极短） */
const STAGE_PATH_SCAN_LINE: Record<PlanStageCode, string> = {
  STRUCTURE: '先把整体框架搭起来',
  UNDERSTANDING: '再理解关键机制',
  TRAINING: '再做题验证理解',
  REFLECTION: '最后复盘薄弱点',
}

/** 结论卡「推荐起点」口语短句 */
const RECOMMENDED_START_PHRASE: Record<PlanStageCode, string> = {
  STRUCTURE: '先搭结构',
  UNDERSTANDING: '先理解关键机制',
  TRAINING: '先做小题验证',
  REFLECTION: '先快速复盘',
}

const STAGE_META: Record<PlanStageCode, StageMeta> = {
  STRUCTURE: {
    label: '结构建立',
    title: 'STRUCTURE',
    defaultObjective: '先搭出知识框架，知道这个点和前后内容分别是什么关系。',
    deliverable: '一张能说清主线的结构图或最小框架。',
    tutorRole: '帮助拆主线、标关键概念、把复杂主题先压成可导航的骨架。',
    checkpoint: '你能说清“这一块在整个主题里属于哪一层”。',
    fallbackRisk: '跳过结构阶段，后面容易只记零散结论，遇到变形题就断线。',
  },
  UNDERSTANDING: {
    label: '机制理解',
    title: 'UNDERSTANDING',
    defaultObjective: '把概念、边界和机制讲明白，避免只背表面答案。',
    deliverable: '一段自己的解释，能区分易混点并说出因果。',
    tutorRole: '用白话重讲原理，对比易混概念，追问“为什么成立”。',
    checkpoint: '你能不用原话复述概念，还能解释一个关键差异。',
    fallbackRisk: '跳过理解阶段，训练时容易出现“看懂了但不会迁移”的假熟练。',
  },
  TRAINING: {
    label: '应用训练',
    title: 'TRAINING',
    defaultObjective: '把理解落到动作里，通过最小练习建立可重复的方法感。',
    deliverable: '完成一组微练习，并留下稳定的解题动作或操作步骤。',
    tutorRole: '给最小任务脚手架，盯住步骤、纠正动作、帮助形成方法。',
    checkpoint: '你能独立完成一题或一步，并说出自己为什么这样做。',
    fallbackRisk: '跳过训练阶段，理解很难转成稳定表现，执行时会频繁卡壳。',
  },
  REFLECTION: {
    label: '反思校准',
    title: 'REFLECTION',
    defaultObjective: '回看证据，确认学会了什么、还缺什么，以及下一步怎么补。',
    deliverable: '一份简短复盘，包含收获、漏洞和下一次切入点。',
    tutorRole: '帮助总结证据、指出薄弱点，把下一轮学习重新压缩成小步。',
    checkpoint: '你能明确说出“我已经会什么、还差什么、下一步练什么”。',
    fallbackRisk: '跳过反思阶段，问题会重复出现，系统也难以为下一轮做准校准。',
  },
}

const TASK_TYPE_STAGE_MAP: Record<string, PlanStageCode> = {
  CONCEPT_EXPLAIN: 'UNDERSTANDING',
  COMPARE_AND_CONNECT: 'UNDERSTANDING',
  GUIDED_EXAMPLE: 'TRAINING',
  SELF_EXPLANATION: 'REFLECTION',
  MICRO_PRACTICE: 'TRAINING',
  CHECKPOINT_REVIEW: 'REFLECTION',
}

function formatStageMinutes(minutes?: number): string {
  if (!minutes || minutes <= 0) return '约 10-15 分钟'
  if (minutes < 60) return `约 ${minutes} 分钟`
  const hour = Math.floor(minutes / 60)
  const remain = minutes % 60
  if (remain === 0) return `约 ${hour} 小时`
  return `约 ${hour} 小时 ${remain} 分钟`
}

function chooseCurrentKnowledge(
  plan: PlanPreviewData,
  ctx: PlanViewModelContext
): string {
  const structured = ctx.structuredGoal
  const topic = structured?.topics?.find((item) => item?.trim())?.trim()
  if (topic) return topic
  const intent = structured?.intentDescription?.trim()
  if (intent) return truncate(intent, 32)
  const normalized = structured?.normalizedGoalText?.trim()
  if (normalized) return truncate(normalized, 32)
  const raw = structured?.rawGoalText?.trim()
  if (raw) return truncate(raw, 32)
  const goal = plan.goal?.trim()
  if (goal) return truncate(goal, 32)
  return '当前知识点'
}

function keywordStageMatch(text: string): PlanStageCode | null {
  const normalized = text.toLowerCase()
  if (
    /structure|框架|结构|关系|主线|地图|骨架|搭建|定位|切入/.test(normalized)
  ) {
    return 'STRUCTURE'
  }
  if (
    /understanding|理解|概念|澄清|原理|机制|对比|辨析|why|为什么|自解释/.test(
      normalized
    )
  ) {
    return 'UNDERSTANDING'
  }
  if (/training|练习|应用|例题|题型|快练|演练|操作|实践|示范/.test(normalized)) {
    return 'TRAINING'
  }
  if (/reflection|复盘|检查|校准|总结|回顾|checkpoint|检验/.test(normalized)) {
    return 'REFLECTION'
  }
  return null
}

function inferRecommendedStageCode(
  plan: PlanPreviewData,
  ctx: PlanViewModelContext
): PlanStageCode {
  const entryText = [plan.recommendedEntry?.title, plan.recommendedEntry?.reason]
    .filter(Boolean)
    .join(' ')
  const fromEntry = keywordStageMatch(entryText)
  if (fromEntry) return fromEntry

  const strategy = `${plan.recommendedStrategy?.code ?? ''} ${
    plan.recommendedStrategy?.label ?? ''
  } ${plan.recommendedStrategy?.reason ?? ''}`.toUpperCase()
  const risks = [
    ...(plan.risks ?? []),
    ...(ctx.learnerProfileSnapshot?.riskTags ?? []),
    ...(ctx.learnerProfileSnapshot?.blockerTags ?? []),
  ].join(' ')

  if (/FOUNDATION|FRAMEWORK|BUILD|PATCH|PREREQUISITE|STRUCTURE/.test(strategy)) {
    return 'STRUCTURE'
  }
  if (/CLARIFICATION|CONCEPT|UNDERSTANDING/.test(strategy)) {
    return 'UNDERSTANDING'
  }
  if (/DRILL|PRACTICE|SPRINT|TRAIN/.test(strategy)) {
    return 'TRAINING'
  }
  if (/REFLECT|REVIEW|CHECKPOINT/.test(strategy)) {
    return 'REFLECTION'
  }
  if (/PREREQUISITE_GAP|RELATIONSHIP_GAP/.test(risks)) return 'STRUCTURE'
  if (/SHALLOW_UNDERSTANDING_RISK|CONCEPT_GAP|EXPRESSION_GAP/.test(risks)) {
    return 'UNDERSTANDING'
  }
  if (/PROCEDURE_GAP|QUESTION_TYPE_RECOGNITION_GAP/.test(risks)) {
    return 'TRAINING'
  }
  return 'STRUCTURE'
}

function summarizeWhy(plan: PlanPreviewData): string {
  const reason = plan.recommendedStrategy?.reason?.trim()
  if (reason) return truncate(reason, 88)
  const evidence = plan.keyEvidence?.filter((item) => item?.trim()) ?? []
  if (evidence.length) return truncate(evidence.slice(0, 2).join('；'), 88)
  return '系统先根据你的起点和风险，决定从最稳的阶段切入，再把后续动作串成闭环。'
}

/** 结论卡专用：更短、偏用户口吻的一句原因 */
function summarizeWhyShort(plan: PlanPreviewData): string {
  const entry = plan.recommendedEntry?.reason?.trim()
  if (entry) return truncate(entry, 46)
  const reason = plan.recommendedStrategy?.reason?.trim()
  if (reason) return truncate(reason, 46)
  const evidence = plan.keyEvidence?.filter((item) => item?.trim()) ?? []
  if (evidence.length) return truncate(evidence[0]!, 46)
  return '从你最顺手的阶段切入，后面再自动串起来。'
}

function summarizeRisk(
  plan: PlanPreviewData,
  recommendedStageCode: PlanStageCode
): string {
  const risks = plan.risks?.filter((item) => item?.trim()) ?? []
  if (risks.length) return truncate(risks.slice(0, 2).join('；'), 88)
  return STAGE_META[recommendedStageCode].fallbackRisk
}

function buildStrategyOverview(
  plan: PlanPreviewData,
  ctx: PlanViewModelContext,
  recommendedStageCode: PlanStageCode
): PlanStrategyOverview {
  return {
    currentKnowledge: chooseCurrentKnowledge(plan, ctx),
    recommendedStrategy:
      plan.recommendedStrategy?.label?.trim() || '分阶段推进',
    recommendedStageCode,
    recommendedStageLabel: `${STAGE_META[recommendedStageCode].title} / ${STAGE_META[recommendedStageCode].label}`,
    whyThisArrangement: summarizeWhy(plan),
    skipRisk: summarizeRisk(plan, recommendedStageCode),
  }
}

function mapStagesByCode(plan: PlanPreviewData): Partial<Record<PlanStageCode, PlanStage>> {
  const result: Partial<Record<PlanStageCode, PlanStage>> = {}
  const remaining = [...PLAN_STAGE_ORDER]

  for (const stage of plan.stages ?? []) {
    const text = `${stage.stageCode ?? ''} ${stage.title ?? ''} ${stage.objective ?? ''}`
    let code = keywordStageMatch(text)
    if (!code && plan.stages.length === 4) {
      code = PLAN_STAGE_ORDER[Object.keys(result).length] ?? null
    }
    if (!code) {
      code = remaining.find((item) => !result[item]) ?? null
    }
    if (code && !result[code]) {
      result[code] = stage
      const idx = remaining.indexOf(code)
      if (idx >= 0) remaining.splice(idx, 1)
    }
  }

  return result
}

function mapTaskToStageCode(task: TaskBlueprint, index: number, total: number): PlanStageCode {
  if (total === 4) return PLAN_STAGE_ORDER[index] ?? 'REFLECTION'

  const mapped = TASK_TYPE_STAGE_MAP[task.taskType]
  if (mapped) return mapped

  const text = `${task.title ?? ''} ${task.goal ?? ''} ${task.taskMethod ?? ''}`
  return keywordStageMatch(text) ?? 'TRAINING'
}

function summarizeTutorSupport(task: TaskBlueprint): string {
  const prompt =
    applyPromptTemplate(task.recommendedPromptTemplate, '当前知识点') ||
    applyPromptTemplate(task.promptScaffold, '当前知识点')
  if (prompt) return truncate(prompt.replace(/当前知识点/g, '这个知识点'), 56)

  switch (task.taskType) {
    case 'CONCEPT_EXPLAIN':
      return '先讲定义和边界，再给最小例子帮助你建立直觉。'
    case 'COMPARE_AND_CONNECT':
      return '把易混概念并排拆开，强调关键差异和连接方式。'
    case 'GUIDED_EXAMPLE':
      return '带你走一遍最小示例，把每一步为什么这么做讲清。'
    case 'SELF_EXPLANATION':
      return '先让你自己复述，再指出遗漏和不稳的地方。'
    case 'MICRO_PRACTICE':
      return '给一题小练习，盯住动作过程，不只看最终答案。'
    case 'CHECKPOINT_REVIEW':
      return '围绕完成信号做一次快速检查，确认能否进入下一段。'
    default:
      return '把这一步拆成更容易执行的小动作，降低起步难度。'
  }
}

function buildCompletionChecks(
  task: TaskBlueprint,
  stageCode: PlanStageCode
): string[] {
  const merged = [
    ...(task.completionCriteria ?? []),
    ...(task.selfEvaluationQuestions ?? []),
  ]
    .map((item) => item.trim())
    .filter(Boolean)

  if (merged.length) return merged.slice(0, 2)
  return [STAGE_META[stageCode].checkpoint]
}

function buildTaskGroups(
  plan: PlanPreviewData
): Record<PlanStageCode, PlanTaskCardView[]> {
  const groups = {
    STRUCTURE: [],
    UNDERSTANDING: [],
    TRAINING: [],
    REFLECTION: [],
  } as Record<PlanStageCode, PlanTaskCardView[]>

  const total = plan.tasks?.length ?? 0
  for (const [index, task] of (plan.tasks ?? []).entries()) {
    const stageCode = mapTaskToStageCode(task, index, total)
    groups[stageCode].push({
      taskId: task.taskId,
      title: task.title?.trim() || `任务 ${index + 1}`,
      stageCode,
      stageLabel: STAGE_META[stageCode].label,
      actionGoal: task.goal?.trim() || task.title?.trim() || '推进当前阶段目标',
      tutorSupport: summarizeTutorSupport(task),
      completionChecks: buildCompletionChecks(task, stageCode),
      estimatedTime: formatStageMinutes(task.estimatedMinutes),
      estimatedMinutes: task.estimatedMinutes,
    })
  }

  return groups
}

function currentStageCodeFromContext(
  plan: PlanPreviewData,
  ctx: PlanViewModelContext
): PlanStageCode | null {
  if (!ctx.sessionId || !ctx.progress || !plan.tasks?.length) return null
  const taskIndex = Math.max(0, (ctx.progress.currentIndex ?? 1) - 1)
  const task = plan.tasks[taskIndex]
  if (!task) return null
  return mapTaskToStageCode(task, taskIndex, plan.tasks.length)
}

export function buildPlanBattleMapView(
  plan: PlanPreviewData | null,
  ctx: PlanViewModelContext = {}
): PlanBattleMapView | null {
  if (!plan) return null

  const recommendedStageCode = inferRecommendedStageCode(plan, ctx)
  const strategyOverview = buildStrategyOverview(plan, ctx, recommendedStageCode)
  const stageByCode = mapStagesByCode(plan)
  const taskGroups = buildTaskGroups(plan)
  const currentStageCode = currentStageCodeFromContext(plan, ctx)

  const stageCards: PlanStageCardView[] = PLAN_STAGE_ORDER.map((code, index) => {
    const stage = stageByCode[code]
    const tasks = taskGroups[code]
    const taskMinutes = tasks.reduce(
      (sum, item) => sum + (item.estimatedMinutes ?? 0),
      0
    )
    const minutes =
      taskMinutes > 0 ? taskMinutes : (stage?.estimatedMinutes ?? undefined)
    const meta = STAGE_META[code]
    return {
      code,
      stageIndex: index + 1,
      label: meta.label,
      title: meta.title,
      objective: stage?.objective?.trim() || meta.defaultObjective,
      deliverable: meta.deliverable,
      tutorRole: meta.tutorRole,
      checkpoint: meta.checkpoint,
      estimatedTime: formatStageMinutes(minutes),
      estimatedMinutesTotal:
        minutes != null && minutes > 0 ? minutes : undefined,
      taskCount: tasks.length,
      isRecommended: code === recommendedStageCode,
      isCurrent: code === currentStageCode,
    }
  })

  const pathStrip: PlanPathStripItem[] = PLAN_STAGE_ORDER.map((code) => {
    const card = stageCards.find((c) => c.code === code)!
    return {
      code,
      title: card.title,
      scanLine: STAGE_PATH_SCAN_LINE[code],
      estimatedLabel: card.estimatedTime,
      isRecommended: code === recommendedStageCode,
      isCurrent: code === currentStageCode,
    }
  })

  const totalEstimatedMinutes = stageCards.reduce(
    (sum, c) => sum + (c.estimatedMinutesTotal ?? 0),
    0
  )
  const totalEstimatedLabel =
    totalEstimatedMinutes > 0
      ? `约 ${totalEstimatedMinutes} 分钟`
      : '约 45–60 分钟'

  const expandedStageCode = currentStageCode ?? recommendedStageCode

  const taskGroupsByStage = PLAN_STAGE_ORDER.map((code) => ({
    stageCode: code,
    stageLabel: STAGE_META[code].label,
    items: taskGroups[code],
  }))

  return {
    strategyOverview,
    stageCards,
    taskGroupsByStage,
    recommendedStageCode,
    expandedStageCode,
    pathStrip,
    totalEstimatedMinutes,
    totalEstimatedLabel,
    recommendedStartPhrase: RECOMMENDED_START_PHRASE[recommendedStageCode],
    whyShortLine: summarizeWhyShort(plan),
  }
}
