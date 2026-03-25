import type {
  CurrentGuidanceBlock,
  CurrentTaskItem,
  ProgressItem,
  RecommendedUserActionItem,
  TaskBlueprint,
  TaskScaffoldResponse,
} from '@/types/dto'
import type {
  ExecutionGuideActionModel,
  ExecutionGuideFeedbackModel,
  ExecutionGuideHelpSection,
  ExecutionGuideProgressRailModel,
  ExecutionKnowledgePointModel,
  ExecutionPageViewModel,
  ExecutionScaffoldCardModel,
} from '@/types/executionGuide'

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
    eyebrow: '先搭骨架，别急着啃细节',
    title: '先别急着抠细节，用一句话说说你觉得它是干什么的',
    description: '不需要一次说对，先把直觉写出来，导师会帮你收紧。',
    inputLabel: '先写一句',
    inputPlaceholder: '例如：我觉得它主要是在解决……，如果没有它，通常会……',
    primaryActionLabel: '发给导师',
    helperText: '写一句就够，后面可以再用上面的卡片追问。',
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
    eyebrow: '只吃透眼前这一点',
    title: '顺着刚才的对话，再补上一句关键理解',
    description: '今天不用学完全部，先把当前这一点讲清楚。',
    inputLabel: '补上一句',
    inputPlaceholder: '例如：它之所以要这样做，是因为……',
    primaryActionLabel: '发给导师',
    helperText: '每次只补一处，别贪多。',
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
    eyebrow: '现在轮到你开口',
    title: '用自己的话串一遍',
    description: '三四句就够，把关系讲顺即可。',
    inputLabel: '写下你的表述',
    inputPlaceholder: '例如：它是什么、为什么需要它、少了它会怎样……',
    primaryActionLabel: '提交复述',
    helperText: '能说顺比说全更重要。',
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
    eyebrow: '只补这一处缺口',
    title: '把刚才漏掉的那一句补上',
    description: '不用重写整段，只补关键点。',
    inputLabel: '把这句话补上',
    inputPlaceholder: '例如：我刚才没说清的是……，补上后应该是……',
    primaryActionLabel: '补好并提交',
    helperText: '只补这一处。',
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
    eyebrow: '最后一道小题',
    title: '自己答一下这道题',
    description: '写清你的判断依据即可。',
    inputLabel: '你的答案',
    inputPlaceholder: '用一两句话直接回答即可。',
    primaryActionLabel: '提交检查',
    helperText: '能独立答出来就够了。',
    defaultChips: [],
  },
  PASS: {
    stageLabel: 'REFLECTION',
    eyebrow: '把这一点收成能带走的话',
    title: '写一句总结，再带走两个关键判断',
    description: '下次打开还能接着用，而不是留在这一次输入里。',
    primaryActionLabel: '我已经搭好这个点的框架，进入下一个',
    helperText: '过关了再往下走。',
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

function summarizeLine(...candidates: Array<string | undefined>) {
  const picked = candidates.map(trimText).find(Boolean) || ''
  return picked ? truncate(picked.replace(/\s+/g, ' '), 56) : ''
}

function statusForKnowledgePoint(index: number, currentIndex: number, totalTasks: number) {
  if (index + 1 < currentIndex) return 'done' as const
  if (index + 1 === Math.min(currentIndex, totalTasks)) return 'active' as const
  return 'upcoming' as const
}

const STAGE_RAIL_LABEL: Record<string, string> = {
  STRUCTURE: '搭框架',
  UNDERSTANDING: '讲过程',
  TRAINING: '练表达',
  REFLECTION: '微检查与复盘',
}

function buildScaffoldCards(
  focus: string,
  scaffold: TaskScaffoldResponse | null
): ExecutionScaffoldCardModel[] {
  const f = trimText(focus) || '这个知识点'
  const templates = scaffold?.recommendedAskTemplates?.filter(Boolean).slice(0, 3) ?? []
  const base: ExecutionScaffoldCardModel[] = [
    {
      id: 'scaffold-frame',
      title: '先搭框架',
      hint: '三句话搭骨架：是什么、解决什么、和相邻概念的关系。先不展开细节。',
      prompt: `关于「${f}」，请先用 3 句话帮我搭建这个知识点的框架：它是什么、它解决什么问题、它和相邻概念有什么关系。先不要展开太多细节。`,
    },
    {
      id: 'scaffold-example',
      title: '先看最小例子',
      hint: '用足够简单的例子，先把直觉建立起来。',
      prompt: `关于「${f}」，请用一个最小例子说明这个知识点到底在干什么，例子要足够简单，让我能先建立直觉。`,
    },
    {
      id: 'scaffold-expose',
      title: '先暴露我的理解',
      hint: '你先说一句话，导师只点最关键的问题，不直接给标准答案。',
      prompt: `关于「${f}」，我先说一句我的理解，你只指出最关键的问题，不要直接给标准答案。我目前的理解是：`,
    },
  ]
  return base.map((card, index) => ({
    ...card,
    prompt: templates[index] || card.prompt,
  }))
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
      '能一句话说清它是什么',
      '能说出它在解决什么问题',
      '能说出它和某个相关概念的一点关系',
    ],
    EXPLORE: [
      '能补上当前最关键的一处因果或机制',
      '能举一个最小例子',
      '能说清还没想透的一个点',
    ],
    SELF_EXPLAIN: ['能用自己的话串起来', '能说出为什么成立', '能说出容易漏掉的判断'],
    REMEDIAL: ['把刚才缺的那一句补上', '能对照导师反馈自检', '能继续往下推进'],
    CHECK: ['能独立写出判断', '依据能对准当前知识点', '不依赖照抄原文'],
    PASS: ['有一句带得走的总结', '有两个下次还能用的要点', '知道下一步练什么'],
  }
  const fb = defaults[state] ?? defaults.ORIENT
  const fromTask = (task.completionCriteria ?? []).map((item) => trimText(item)).filter(Boolean).slice(0, 2)
  return [...fromTask, ...fb].filter(Boolean).slice(0, 3)
}

function buildHeroSubtitle(state: string, activePoint: ExecutionKnowledgePointModel | undefined): string {
  const name = activePoint?.title ? `「${activePoint.title}」` : '眼前这个点'
  if (state === 'ORIENT') {
    return `先别急着学细。选一个上面的问法，或自己写一句，把 ${name} 的骨架搭出来。`
  }
  if (state === 'EXPLORE') {
    return `顺着导师的反馈，再补一句：${name} 里最卡你的那一处。`
  }
  if (state === 'SELF_EXPLAIN') {
    return `现在轮到你：把 ${name} 用自己的话讲顺。`
  }
  if (state === 'REMEDIAL') {
    return `先补上刚才露出来的缺口，再往下走。`
  }
  if (state === 'CHECK') {
    return `不看提示，独立写答案。`
  }
  if (state === 'PASS') {
    return `把这一点收成下次还能用的话，然后进入下一个。`
  }
  return `先专注 ${name}。`
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
  activePoint: ExecutionKnowledgePointModel | undefined
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
    eyebrow: copy.eyebrow,
    title: copy.title,
    description: copy.description,
    inputLabel: copy.inputLabel,
    inputPlaceholder: copy.inputPlaceholder,
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

function buildHelpSections(
  task: CurrentTaskItem,
  scaffold: TaskScaffoldResponse | null,
  guidance: CurrentGuidanceBlock | null
): ExecutionGuideHelpSection[] {
  const sections: ExecutionGuideHelpSection[] = []

  if (scaffold?.whyThisTask || task.whyThisTask) {
    sections.push({
      id: 'why-now',
      title: '为什么现在先做这个知识点',
      body: scaffold?.whyThisTask || task.whyThisTask,
    })
  }

  if (guidance?.bullets?.length) {
    sections.push({
      id: 'guidance',
      title: guidance.title || '当前重点',
      bullets: guidance.bullets,
    })
  }

  if (scaffold?.completionSignals?.length || task.completionCriteria?.length) {
    sections.push({
      id: 'signals',
      title: '做到这里就可以继续',
      bullets: (scaffold?.completionSignals?.length
        ? scaffold.completionSignals
        : task.completionCriteria) ?? [],
    })
  }

  if (scaffold?.antiPatterns?.length) {
    sections.push({
      id: 'anti-patterns',
      title: '如果还卡住，先避开这些',
      bullets: scaffold.antiPatterns,
    })
  }

  return sections
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

function buildProgressRail(
  knowledgePoints: ExecutionKnowledgePointModel[],
  activePoint: ExecutionKnowledgePointModel | undefined,
  state: string,
  completionLines: string[]
): ExecutionGuideProgressRailModel {
  const stageKey = STEP_COPY[state].stageLabel
  const stageZh = STAGE_RAIL_LABEL[stageKey] || '推进中'
  const nextPoint = knowledgePoints.find((item) => item.status === 'upcoming')
  const remaining = knowledgePoints.filter((item) => item.status === 'upcoming').length

  const progressLines: string[] = [
    `当前阶段：${stageZh}`,
    activePoint ? `当前知识点：${activePoint.title}` : '当前知识点：进行中',
  ]
  if (remaining > 0) {
    progressLines.push(`这一轮还剩 ${remaining} 个点没吃透（先啃眼前这个）`)
  } else {
    progressLines.push('这一轮就剩眼前这个点')
  }

  let nextPreview = '完成当前点后，系统会带你收束或进入下一任务。'
  if (state === 'ORIENT' || state === 'EXPLORE') {
    nextPreview = nextPoint
      ? `下一步会进入「${nextPoint.title}」；先把当前点吃透。`
      : '先把骨架搭稳，再进入更深一点的理解。'
  } else if (state === 'SELF_EXPLAIN' || state === 'REMEDIAL') {
    nextPreview = '接下来是一道轻量检查题，确认你能独立判断。'
  } else if (state === 'CHECK') {
    nextPreview = '答对后收束本点，带走总结与要点。'
  } else if (state === 'PASS') {
    nextPreview = nextPoint
      ? `提交后进入「${nextPoint.title}」。`
      : '提交后进入报告或下一任务。'
  }

  return {
    progressSectionTitle: '本轮进度',
    progressLines,
    criteriaSectionTitle: '本轮完成标准',
    completionLines,
    nextSectionTitle: '下一步预告',
    nextPreview,
    knowledgeOutline: knowledgePoints,
  }
}

export function buildExecutionPageModel(
  input: BuildExecutionPageModelInput
): ExecutionPageViewModel {
  const state = normalizeState(input.taskState, input.legacyComplete)
  const copy = STEP_COPY[state]
  const knowledgePoints = buildKnowledgePoints(input.task, input.progress, input.planTasks)
  const activePoint =
    knowledgePoints.find((item) => item.status === 'active') || knowledgePoints[0]

  const focusTitle =
    activePoint?.title || summarizeLine(input.task.title, '当前知识点') || '当前知识点'
  const completionCriteria = buildRoundCompletionCriteria(state, input.task, input.scaffold)
  const metaParts = [
    copy.stageLabel,
    input.progress ? `任务 ${input.progress.currentIndex}/${input.progress.totalTasks}` : null,
    activePoint ? `知识点 ${activePoint.index}/4` : null,
  ].filter(Boolean) as string[]
  const metaLine = metaParts.join(' · ')

  const scaffoldCards = buildScaffoldCards(focusTitle, input.scaffold)

  return {
    currentStepIndex: input.guidedStepCurrent,
    currentStepTitle: copy.title,
    header: {
      heroTitle: `这轮只解决一个问题：${focusTitle}`,
      heroSubtitle: buildHeroSubtitle(state, activePoint),
      completionCriteria,
      metaLine,
      title: activePoint?.title || input.task.title,
      stageLabel: `${copy.stageLabel} · 执行中`,
      stepLabel: `${input.guidedStepCurrent}/${input.guidedStepTotal}`,
      estimatedTime: STEP_TIMES[state],
      subtitle: activePoint
        ? `今天不用学完全部，先把「${activePoint.title}」吃透。`
        : copy.description,
      taskLabel: input.progress
        ? `任务 ${input.progress.currentIndex}/${input.progress.totalTasks}`
        : undefined,
      trackTitle: '本轮知识点',
      trackSubtitle: activePoint
        ? `当前第 ${activePoint.index} 个点，完成后再往后走。`
        : '按知识点逐个推进。',
      knowledgePoints,
    },
    mainAction: buildActionModel(
      state,
      input.task,
      input.scaffold,
      input.recommendedUserActions,
      activePoint
    ),
    feedback: buildFeedback(
      input.latestFeedback,
      state,
      input.checkpointQuestion,
      input.selfExplainMissingPoints
    ),
    progressRail: buildProgressRail(knowledgePoints, activePoint, state, completionCriteria),
    helpSections: buildHelpSections(input.task, input.scaffold, input.currentGuidance),
    scaffoldCards,
  }
}
