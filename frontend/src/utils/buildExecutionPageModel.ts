import type {
  CurrentGuidanceBlock,
  CurrentTaskItem,
  ProgressItem,
  RecommendedUserActionItem,
  StructuredLearningGoal,
  TaskBlueprint,
  TaskScaffoldResponse,
} from '@/types/dto'
import type {
  ExecutionGuideActionModel,
  ExecutionGuideFeedbackModel,
  ExecutionGuideHelpSection,
  ExecutionKnowledgePointModel,
  ExecutionPageViewModel,
  ExecutionScaffoldCardModel,
} from '@/types/executionGuide'
import type { KnowledgePack } from '@/types/knowledgePack'
import { useKnowledgePack } from '@/composables/useKnowledgePack'
import { enrichExecutionSpec } from '@/utils/enrichExecutionSpec'
import type { KnowledgeDemoStageCode } from '@/constants/KnowledgeConfig'
import {
  mergePhaseCopyWithFallback,
  STAGE_RULES_BY_PHASE,
  TOPIC_DISPLAY_NAME,
  TOPIC_OBSERVATION_BULLETS,
  TOPIC_VISUAL_VARIANT,
  WORKBENCH_PHASE_SEQUENCE,
  type PhaseWorkbenchCopy,
} from '@/constants/executionWorkbenchContent'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import type { KnowledgePackId } from '@/types/knowledgePack'
import type {
  TaskExecutionWorkbenchModel,
  WorkbenchPhaseCode,
  WorkbenchTaskStatus,
} from '@/types/taskExecutionWorkbench'

interface BuildExecutionPageModelInput {
  task: CurrentTaskItem
  progress: ProgressItem | null
  planTasks: TaskBlueprint[]
  scaffold: TaskScaffoldResponse | null
  taskState: string
  guidedStepCurrent: number
  guidedStepTotal: number
  currentGuidance: CurrentGuidanceBlock | null
  recommendedUserActions: RecommendedUserActionItem[]
  latestFeedback: ExecutionGuideFeedbackModel | null
  checkpointQuestion: string
  selfExplainMissingPoints: string[]
  legacyComplete: boolean
  structuredGoal?: StructuredLearningGoal | null
  plan?: { knowledgeKey?: string; packId?: string } | null
  /** 驾驶席：微检查通过且可推进 */
  canAdvanceDriving?: boolean
}

const STEP_TIMES: Record<string, string> = {
  ORIENT: '2-3 分钟',
  EXPLORE: '3-4 分钟',
  SELF_EXPLAIN: '2-3 分钟',
  CHECK: '1-2 分钟',
  REMEDIAL: '2-3 分钟',
  PASS: '1 分钟',
}

const STEP_COPY: Record<
  string,
  {
    stageLabel: string
    eyebrow: string
    title: string
    description: string
    inputLabel?: string
    inputPlaceholder?: string
    primaryActionLabel: string
    helperText?: string
    defaultChips: { id: string; label: string; fill: string }[]
  }
> = {
  ORIENT: {
    stageLabel: 'STRUCTURE',
    eyebrow: '结构建立',
    title: '先搭知识骨架',
    description: '先别急着记细节。',
    inputLabel: '我的表达',
    inputPlaceholder: '先写一句：它是什么、在帮什么忙……',
    primaryActionLabel: '提交本轮表达',
    helperText: '',
    defaultChips: [
      {
        id: 'orient-problem',
        label: '它在解决什么问题？',
        fill: '我理解这个知识点，主要是在解决这样一个问题：',
      },
      {
        id: 'orient-compare',
        label: '它和相邻概念差在哪？',
        fill: '我现在最容易混淆的是它和相邻概念的区别：',
      },
      {
        id: 'orient-misread',
        label: '最容易误解哪一点？',
        fill: '我觉得这里最容易让人误解的地方是：',
      },
    ],
  },
  EXPLORE: {
    stageLabel: 'UNDERSTANDING',
    eyebrow: '机制理解',
    title: '把机制讲清楚',
    description: '你现在最需要讲清的是因果与过程。',
    inputLabel: '我的表达',
    inputPlaceholder: '例如：它为什么这样工作？少了哪一步会出问题？',
    primaryActionLabel: '提交本轮表达',
    helperText: '',
    defaultChips: [
      {
        id: 'explore-why',
        label: '为什么需要它？',
        fill: '我觉得它之所以必要，是因为：',
      },
      {
        id: 'explore-edge',
        label: '少了它会怎样？',
        fill: '如果没有这一点，最可能出现的问题是：',
      },
      {
        id: 'explore-example',
        label: '给一个最小例子',
        fill: '我想先用一个最小例子把它讲清楚：',
      },
    ],
  },
  SELF_EXPLAIN: {
    stageLabel: 'TRAINING',
    eyebrow: '表达训练',
    title: '用自己的话写完整',
    description: '表达 → 纠错 → 重构。',
    inputLabel: '我的表达',
    inputPlaceholder: '它是什么、为什么需要、哪里最容易错……',
    primaryActionLabel: '提交本轮表达',
    helperText: '',
    defaultChips: [
      {
        id: 'self-what',
        label: '先说它是什么',
        fill: '先用我的话说，它本质上是：',
      },
      {
        id: 'self-why',
        label: '再说为什么需要它',
        fill: '之所以需要它，是因为：',
      },
      {
        id: 'self-gap',
        label: '说说我还不稳的点',
        fill: '我现在还没完全讲顺的地方是：',
      },
    ],
  },
  REMEDIAL: {
    stageLabel: 'TRAINING',
    eyebrow: '表达训练',
    title: '只补这一处',
    description: '对照反馈，补上缺口。',
    inputLabel: '我的表达',
    inputPlaceholder: '补上刚才缺的那一句判断或因果……',
    primaryActionLabel: '提交本轮表达',
    helperText: '',
    defaultChips: [
      {
        id: 'remedial-gap',
        label: '我刚才漏了什么？',
        fill: '我刚才漏掉的关键点是：',
      },
      {
        id: 'remedial-why',
        label: '把因果补完整',
        fill: '更完整地说，之所以这样，是因为：',
      },
      {
        id: 'remedial-example',
        label: '用一句例子补上',
        fill: '如果用一个最小例子补上，它会是：',
      },
    ],
  },
  CHECK: {
    stageLabel: 'REFLECTION',
    eyebrow: '反思收敛',
    title: '独立作答',
    description: '写清依据，不抄书。',
    inputLabel: '我的表达',
    inputPlaceholder: '一两句话 + 你的判断依据。',
    primaryActionLabel: '提交本轮表达',
    helperText: '',
    defaultChips: [],
  },
  PASS: {
    stageLabel: 'REFLECTION',
    eyebrow: '反思收敛',
    title: '收束带走',
    description: '一句总结，两个要点。',
    primaryActionLabel: '完成本任务',
    helperText: '',
    defaultChips: [],
  },
}

function normalizeState(taskState: string, legacyComplete: boolean) {
  if (legacyComplete) return 'PASS'
  return STEP_COPY[taskState] ? taskState : 'ORIENT'
}

function trimText(text?: string | null) {
  return text?.trim() || ''
}

function truncate(text: string, max = 48) {
  if (text.length <= max) return text
  return `${text.slice(0, max - 1)}…`
}

function clipFirstLine(text: string, max = 80) {
  const line = text
    .split(/\n/)
    .map((s) => s.trim())
    .find(Boolean)
  if (!line) return ''
  return truncate(line, max)
}

function summarizeLine(...candidates: Array<string | undefined>) {
  const picked = candidates.map(trimText).find(Boolean) || ''
  return picked ? truncate(picked.replace(/\s+/g, ' '), 56) : ''
}

function statusForKnowledgePoint(index: number, currentIndex: number, totalTasks: number) {
  if (index + 1 < currentIndex) return 'done' as const
  if (index + 1 === Math.min(currentIndex, totalTasks)) return 'active' as const
  return 'upcoming' as const
}

function buildRoundCompletionCriteria(
  state: string,
  task: CurrentTaskItem,
  scaffold: TaskScaffoldResponse | null
): string[] {
  const fromScaffold = (scaffold?.completionSignals ?? [])
    .map((item) => trimText(item))
    .filter(Boolean)
    .slice(0, 3)
  if (fromScaffold.length >= 2) return fromScaffold

  const defaults: Record<string, string[]> = {
    ORIENT: [
      '一句话：它是什么',
      '它在解决什么问题',
      '和相邻概念的一点关系',
    ],
    EXPLORE: [
      '补上最关键的一处因果或机制',
      '一个最小例子',
      '一个你还不稳的点',
    ],
    SELF_EXPLAIN: ['用自己的话串起来', '为什么成立', '容易漏的判断'],
    REMEDIAL: ['缺口已补上', '能对照反馈自检', '可以继续推进'],
    CHECK: ['判断独立', '依据对准本点', '不照抄原文'],
    PASS: ['一句总结', '两个可复用要点', '下一步练什么'],
  }
  const fb = defaults[state] ?? defaults.ORIENT
  const fromTask = (task.completionCriteria ?? []).map((item) => trimText(item)).filter(Boolean).slice(0, 2)
  return [...fromTask, ...fb].filter(Boolean).slice(0, 3)
}

function buildKnowledgePoints(
  task: CurrentTaskItem,
  progress: ProgressItem | null,
  planTasks: TaskBlueprint[]
): ExecutionKnowledgePointModel[] {
  const totalTasks = Math.max(progress?.totalTasks ?? planTasks.length ?? 0, 4)
  const currentIndex = Math.min(Math.max(progress?.currentIndex ?? 1, 1), totalTasks)
  const planItems = planTasks.slice(0, 4)

  if (planItems.length === 4) {
    return planItems.map((item, index) => ({
      id: item.taskId || `knowledge-point-${index + 1}`,
      index: index + 1,
      title: summarizeLine(item.title, `知识点 ${index + 1}`) || `知识点 ${index + 1}`,
      subtitle:
        summarizeLine(item.goal, item.completionCriteria?.[0], item.taskMethod) ||
        `围绕第 ${index + 1} 个知识点推进`,
      status: statusForKnowledgePoint(index, currentIndex, totalTasks),
    }))
  }

  return Array.from({ length: 4 }, (_, index) => {
    const isCurrent = index + 1 === currentIndex
    return {
      id: `knowledge-point-fallback-${index + 1}`,
      index: index + 1,
      title: isCurrent ? summarizeLine(task.title, '当前知识点') || '当前知识点' : `后续知识点 ${index + 1}`,
      subtitle: isCurrent
        ? summarizeLine(task.goal, task.completionCriteria?.[0], task.taskMethod) || '这一轮先把当前知识点讲透'
        : '将在后续任务中继续展开',
      status: statusForKnowledgePoint(index, currentIndex, totalTasks),
    }
  })
}

function buildActionModel(
  state: string,
  task: CurrentTaskItem,
  scaffold: TaskScaffoldResponse | null,
  recommendedUserActions: RecommendedUserActionItem[],
  activePoint: ExecutionKnowledgePointModel | undefined,
  directive: string,
  inputPlaceholderSoft: string,
  stageCode: string,
  stageDisplayZh: string
): ExecutionGuideActionModel {
  const copy = STEP_COPY[state]
  const scaffoldPrompts =
    scaffold?.recommendedAskTemplates
      ?.filter(Boolean)
      .slice(0, 3)
      .map((prompt, index) => ({
        id: `${state}-scaffold-${index}`,
        label: prompt,
        fill: prompt,
      })) ?? []

  const actionChips =
    recommendedUserActions.slice(0, 2).map((item, index) => ({
      id: `${state}-action-${index}`,
      label: item.label,
      fill: item.label,
    })) ?? []

  const focusTips = [
    ...((task.completionCriteria ?? []).slice(0, 2)),
    ...((scaffold?.completionSignals ?? []).slice(0, 2)),
  ]
    .map((item) => trimText(item))
    .filter(Boolean)
    .slice(0, 3)

  return {
    mode: state === 'PASS' ? 'closure' : 'guided-input',
    phaseCode: stageCode,
    phaseDisplayZh: stageDisplayZh,
    eyebrow: copy.eyebrow,
    title: copy.title,
    description: copy.description,
    directive,
    inputLabel: copy.inputLabel,
    inputPlaceholder: inputPlaceholderSoft || copy.inputPlaceholder,
    primaryActionLabel: copy.primaryActionLabel,
    helperText: copy.helperText,
    passHint: scaffold?.completionSignals?.[0],
    focusLabel: activePoint ? `当前攻克知识点 ${activePoint.index}/4` : '当前攻克知识点',
    focusTitle: activePoint?.title || summarizeLine(task.title, '当前知识点') || '当前知识点',
    focusObjective:
      activePoint?.subtitle ||
      summarizeLine(task.goal, scaffold?.learningObjective, task.completionCriteria?.[0]),
    focusReason:
      summarizeLine(task.whyThisTask, scaffold?.whyThisTask) ||
      '这一轮先只围绕当前知识点推进，不把其它点混进来。',
    focusTips,
    chips:
      state === 'CHECK'
        ? []
        : [...copy.defaultChips, ...scaffoldPrompts, ...actionChips].slice(0, 3),
  }
}

function buildHelpSections(guidance: CurrentGuidanceBlock | null): ExecutionGuideHelpSection[] {
  if (guidance?.bullets?.length) {
    return [
      {
        id: 'guidance',
        title: guidance.title || '当前提示',
        bullets: guidance.bullets.slice(0, 3),
      },
    ]
  }
  return []
}

function buildFeedback(
  latestFeedback: ExecutionGuideFeedbackModel | null,
  taskState: string,
  checkpointQuestion: string,
  selfExplainMissingPoints: string[]
): ExecutionGuideFeedbackModel {
  if (latestFeedback) return latestFeedback

  if (taskState === 'REMEDIAL' && selfExplainMissingPoints.length) {
    return {
      visible: true,
      title: '当前反馈',
      mastered: '主线已经出来了，现在不是重写整段，而是把缺掉的判断点补上。',
      gap: selfExplainMissingPoints[0],
      nextStep: '先把这一句补清，再继续往下推进当前知识点。',
      actions: [
        { id: 'apply_suggestion', label: '按建议补一句' },
        { id: 'show_example', label: '看一个最小示例' },
      ],
    }
  }

  if (taskState === 'CHECK' && checkpointQuestion) {
    return {
      visible: true,
      title: '当前反馈',
      mastered: '前面的理解和表述已经够了。',
      gap: '还没独立答完这道检查题。',
      nextStep: '直接用一两句话作答，重点写清你的判断依据。',
      actions: [],
    }
  }

  return {
    visible: false,
    title: '当前反馈',
    mastered: '',
    gap: '',
    nextStep: '',
    actions: [],
  }
}

function buildNextStepPreview(
  knowledgePoints: ExecutionKnowledgePointModel[],
  state: string,
  pack: KnowledgePack | null
): string {
  const nextPoint = knowledgePoints.find((item) => item.status === 'upcoming')
  let nextPreview = '完成当前动作后，进入下一小步或收束。'
  if (state === 'ORIENT' || state === 'EXPLORE') {
    nextPreview = nextPoint
      ? `下一步会推进「${nextPoint.title}」；先把眼前这一步做完。`
      : '先把当前这一步做完，再进入更深一点的理解。'
  } else if (state === 'SELF_EXPLAIN' || state === 'REMEDIAL') {
    nextPreview = '接下来是一道轻量检查，确认你能独立判断。'
  } else if (state === 'CHECK') {
    nextPreview = pack?.checkpoint.checkpointPrompt
      ? `完成后进入收束：${clipFirstLine(pack.checkpoint.checkpointPrompt, 80)}`
      : '答对后收束本点，带走一句总结与要点。'
  } else if (state === 'PASS') {
    nextPreview = nextPoint ? `提交后进入「${nextPoint.title}」。` : '提交后进入报告或下一任务。'
  }
  return nextPreview
}

const FALLBACK_PHASE_COPY: PhaseWorkbenchCopy = {
  whatToOutput: ['一小段能自检的表述'],
  recommendedSteps: ['先结论，再一句依据或例子'],
  avoid: ['空答', '整段粘贴'],
  whyNow: '规划里这一步正在补你的能力栈。',
  skipRisk: '后面会用更高成本暴露缺口。',
  expectedGain: '你能说清这一轮交付了什么。',
}

function isKnowledgePackId(id: string | null | undefined): id is KnowledgePackId {
  return (
    id === 'os_process_thread' ||
    id === 'net_tcp_handshake' ||
    id === 'ds_dfs_bfs' ||
    id === 'arch_cache_locality'
  )
}

function computeWorkbenchTaskStatus(
  taskState: string,
  latestFeedback: ExecutionGuideFeedbackModel | null,
  canAdvanceDriving: boolean
): { status: WorkbenchTaskStatus; label: string } {
  const labels: Record<WorkbenchTaskStatus, string> = {
    running: '进行中',
    submitted: '已提交',
    needs_fix: '待修正',
    can_advance: '可进入下一阶段',
  }
  if (canAdvanceDriving) return { status: 'can_advance', label: labels.can_advance }
  if (
    taskState === 'REMEDIAL' ||
    (latestFeedback?.visible && (latestFeedback.keyIssues?.length ?? 0) > 0)
  ) {
    return { status: 'needs_fix', label: labels.needs_fix }
  }
  if (latestFeedback?.visible) return { status: 'submitted', label: labels.submitted }
  return { status: 'running', label: labels.running }
}

function buildWorkbenchModel(
  input: BuildExecutionPageModelInput,
  opts: {
    pack: KnowledgePack | null
    phaseCode: WorkbenchPhaseCode
    stageDisplayZh: string
    copyTitle: string
    currentDirective: string
    completionCriteria: string[]
    knowledgePointName: string
    roundGoal: string
    scaffoldCards: ExecutionScaffoldCardModel[]
    nextPreview: string
  }
): TaskExecutionWorkbenchModel {
  const packId = opts.pack?.id && isKnowledgePackId(opts.pack.id) ? opts.pack.id : null
  const phaseKey = opts.phaseCode as KnowledgeDemoStageCode
  const merged = mergePhaseCopyWithFallback(packId, phaseKey, FALLBACK_PHASE_COPY)
  const firstCard = opts.scaffoldCards[0]
  const startLabel = firstCard?.actionLabel?.trim() || '从推荐动作开始'

  const totalTasks = Math.max(input.progress?.totalTasks ?? 4, 1)
  const idx = Math.min(Math.max(input.progress?.currentIndex ?? 1, 1), totalTasks)
  const stepFrac = input.guidedStepCurrent / Math.max(input.guidedStepTotal, 1)
  const overallRatio = Math.min(1, (idx - 1) / totalTasks + stepFrac / totalTasks)

  const ts = computeWorkbenchTaskStatus(
    input.taskState,
    input.latestFeedback,
    input.canAdvanceDriving ?? false
  )

  const topicName = packId ? TOPIC_DISPLAY_NAME[packId] : opts.knowledgePointName
  const bullets = packId
    ? TOPIC_OBSERVATION_BULLETS[packId]
    : ['先写清判断依据', '再用最小例子检验', '最后收束成一句话']
  const visual = packId ? TOPIC_VISUAL_VARIANT[packId] : 'hierarchy'

  const core =
    clipFirstLine(opts.currentDirective, 120) || clipFirstLine(opts.roundGoal, 120) || opts.copyTitle

  return {
    packId,
    phaseProgress: {
      phases: WORKBENCH_PHASE_SEQUENCE,
      currentPhase: opts.phaseCode,
      overallRatio,
      stepLabel: `${input.guidedStepCurrent}/${input.guidedStepTotal}`,
      taskIndexLabel: input.progress
        ? `任务 ${input.progress.currentIndex} / ${input.progress.totalTasks}`
        : '任务 1 / 1',
    },
    taskStatus: ts.status,
    taskStatusLabel: ts.label,
    scaffoldProduct: {
      whatToOutput: merged.whatToOutput,
      recommendedSteps: merged.recommendedSteps,
      avoid: merged.avoid,
      startActionLabel: startLabel,
      startBehavior: firstCard?.behavior,
    },
    whyThisStep: {
      whyNow: merged.whyNow,
      skipRisk: merged.skipRisk,
      expectedGain: merged.expectedGain,
    },
    stageRules: {
      rules: STAGE_RULES_BY_PHASE[phaseKey] ?? STAGE_RULES_BY_PHASE.STRUCTURE,
    },
    topicHints: {
      topicDisplayName: topicName,
      bullets,
      visualVariant: visual,
    },
    stageMini: {
      roundLabel: `第 ${input.guidedStepCurrent} / ${input.guidedStepTotal} 轮`,
      actionsDone: input.guidedStepCurrent,
      actionsTarget: input.guidedStepTotal,
      untilNextPhase: opts.nextPreview,
      passedGate: input.canAdvanceDriving ?? false,
    },
    currentTask: {
      phaseDisplayZh: opts.stageDisplayZh,
      phaseCode: opts.phaseCode,
      taskTitle: opts.copyTitle,
      coreActionLine: core,
      completionLines: opts.completionCriteria.slice(0, 3),
    },
    emphasisPhase: opts.phaseCode,
  }
}

/** 供 TaskRunView 空态占位 */
export function createEmptyWorkbenchModel(): TaskExecutionWorkbenchModel {
  return {
    packId: null,
    phaseProgress: {
      phases: WORKBENCH_PHASE_SEQUENCE,
      currentPhase: 'STRUCTURE',
      overallRatio: 0,
      stepLabel: '0/0',
      taskIndexLabel: '任务 0/0',
    },
    taskStatus: 'running',
    taskStatusLabel: '进行中',
    scaffoldProduct: {
      whatToOutput: FALLBACK_PHASE_COPY.whatToOutput,
      recommendedSteps: FALLBACK_PHASE_COPY.recommendedSteps,
      avoid: FALLBACK_PHASE_COPY.avoid,
      startActionLabel: '开始',
    },
    whyThisStep: {
      whyNow: '',
      skipRisk: '',
      expectedGain: '',
    },
    stageRules: { rules: STAGE_RULES_BY_PHASE.STRUCTURE },
    topicHints: {
      topicDisplayName: '',
      bullets: [],
      visualVariant: 'hierarchy',
    },
    stageMini: {
      roundLabel: '',
      actionsDone: 0,
      actionsTarget: 0,
      untilNextPhase: '',
      passedGate: false,
    },
    currentTask: {
      phaseDisplayZh: '',
      phaseCode: 'STRUCTURE',
      taskTitle: '',
      coreActionLine: '',
      completionLines: [],
    },
    emphasisPhase: 'STRUCTURE',
  }
}

export function buildExecutionPageModel(
  input: BuildExecutionPageModelInput
): ExecutionPageViewModel {
  const state = normalizeState(input.taskState, input.legacyComplete)
  const pack = useKnowledgePack({
    scaffold: input.scaffold,
    structuredGoal: input.structuredGoal,
    knowledgeKey: input.plan?.knowledgeKey,
    packId: input.plan?.packId,
  })
  const copy = STEP_COPY[state]
  const knowledgePoints = buildKnowledgePoints(input.task, input.progress, input.planTasks)
  const activePoint =
    knowledgePoints.find((item) => item.status === 'active') || knowledgePoints[0]

  let enriched = enrichExecutionSpec({
    task: input.task,
    scaffold: input.scaffold,
    pack,
    taskState: input.taskState,
    legacyComplete: input.legacyComplete,
    checkpointQuestion: input.checkpointQuestion,
    activeKnowledgePointTitle: activePoint?.title || input.task.title,
    recommendedUserActions: input.recommendedUserActions,
    stageMachineLabel: copy.stageLabel,
    stepTimes: STEP_TIMES,
  })

  if (state === 'CHECK' && pack) {
    enriched = {
      ...enriched,
      completionStandardLines: pack.checkpoint.checkpointRubric.slice(0, 2),
    }
  }

  const completionCriteria =
    enriched.completionStandardLines.length >= 2
      ? enriched.completionStandardLines
      : buildRoundCompletionCriteria(state, input.task, input.scaffold)

  const metaParts = [
    phaseCodeToFullZh(copy.stageLabel),
    input.progress ? `任务 ${input.progress.currentIndex}/${input.progress.totalTasks}` : null,
    activePoint ? `知识点 ${activePoint.index}/4` : null,
  ].filter(Boolean) as string[]
  const metaLine = metaParts.join(' · ')

  const scaffoldCards = enriched.scaffoldCards
  const nextPreview = buildNextStepPreview(knowledgePoints, state, pack)

  const progressRail = {
    stageSectionTitle: '当前阶段',
    stageLabel: enriched.stageDisplay,
    deliverableSectionTitle: '当前这一步要产出',
    deliverableLine: enriched.currentDeliverable,
    stuckSectionTitle: '卡住时怎么做',
    stuckActions: enriched.stuckActions,
    nextSectionTitle: '下一步预告',
    nextPreview,
    knowledgeOutline: knowledgePoints,
  }

  const workbench = buildWorkbenchModel(input, {
    pack,
    phaseCode: copy.stageLabel as WorkbenchPhaseCode,
    stageDisplayZh: enriched.stageDisplay,
    copyTitle: copy.title,
    currentDirective: enriched.currentDirective,
    completionCriteria,
    knowledgePointName: enriched.knowledgePointName,
    roundGoal: enriched.roundGoal,
    scaffoldCards,
    nextPreview,
  })

  return {
    currentStepIndex: input.guidedStepCurrent,
    currentStepTitle: copy.title,
    header: {
      phaseCode: copy.stageLabel,
      phaseDisplayZh: enriched.stageDisplay,
      strategyLine: undefined,
      anchorActionLine:
        clipFirstLine(enriched.currentDirective, 120) || clipFirstLine(enriched.roundGoal, 120),
      heroTitle: enriched.knowledgePointName,
      heroSubtitle: enriched.roundGoal,
      completionCriteria,
      metaLine,
      title: activePoint?.title || input.task.title,
      stageLabel: `当前阶段：${enriched.stageDisplay}`,
      stepLabel: `${input.guidedStepCurrent}/${input.guidedStepTotal}`,
      estimatedTime: enriched.estimatedTimeLabel,
      subtitle:
        pack?.execution.phaseObjective[
          state as keyof typeof pack.execution.phaseObjective
        ] ||
        (activePoint
          ? `今天不用学完全部，先把「${activePoint.title}」吃透。`
          : copy.description),
      taskLabel: input.progress
        ? `任务 ${input.progress.currentIndex}/${input.progress.totalTasks}`
        : undefined,
      trackTitle: '本轮知识点',
      trackSubtitle: activePoint
        ? `当前第 ${activePoint.index} 个点，完成后再往后走。`
        : '按知识点逐个推进。',
      knowledgePoints,
      operationConsole: {
        knowledgePointName: enriched.knowledgePointName,
        knowledgePointType: enriched.knowledgePointType,
        roundGoal: enriched.roundGoal,
        completionStandardLines: enriched.completionStandardLines,
        estimatedTimeLabel: enriched.estimatedTimeLabel,
      },
    },
    mainAction: buildActionModel(
      state,
      input.task,
      input.scaffold,
      input.recommendedUserActions,
      activePoint,
      enriched.currentDirective,
      enriched.inputPlaceholderSoft,
      copy.stageLabel,
      enriched.stageDisplay
    ),
    feedback: buildFeedback(
      input.latestFeedback,
      state,
      input.checkpointQuestion,
      input.selfExplainMissingPoints
    ),
    progressRail,
    helpSections: buildHelpSections(input.currentGuidance),
    scaffoldCards,
    tutorConsole: {
      currentDirective: enriched.currentDirective,
      inputPlaceholderSoft: enriched.inputPlaceholderSoft,
      stageDisplay: enriched.stageDisplay,
      currentDeliverable: enriched.currentDeliverable,
      stuckActions: enriched.stuckActions,
      phasePromptChips: enriched.phasePromptChips,
    },
    workbench,
  }
}
