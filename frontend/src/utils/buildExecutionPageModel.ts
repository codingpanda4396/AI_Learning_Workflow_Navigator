import type {
  CurrentGuidanceBlock,
  CurrentTaskItem,
  ProgressItem,
  RecommendedUserActionItem,
  TaskScaffoldResponse,
} from '@/types/dto'
import type {
  ExecutionGuideActionModel,
  ExecutionGuideFeedbackModel,
  ExecutionGuideHelpSection,
  ExecutionGuideProgressRailModel,
  ExecutionPageViewModel,
} from '@/types/executionGuide'

interface BuildExecutionPageModelInput {
  task: CurrentTaskItem
  progress: ProgressItem | null
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
    progress: ExecutionGuideProgressRailModel
  }
> = {
  ORIENT: {
    stageLabel: '先确认你在解决什么问题',
    eyebrow: '第 1 步',
    title: '先用一句话说清，这个知识点在解决什么问题',
    description: '不求完整，也不求标准答案。先把你现在的理解写出来。',
    inputLabel: '先写下你的最小理解',
    inputPlaceholder: '例如：它是为了解决……，如果没有它，通常会……',
    primaryActionLabel: '写好了，帮我判断下一步',
    helperText: '先写一句就够了，系统会根据这句继续追问。',
    defaultChips: [
      {
        id: 'orient-problem',
        label: '它是什么问题的答案？',
        fill: '我理解这个知识点，主要是在解决这样一个问题：',
      },
      {
        id: 'orient-compare',
        label: '它和相近概念差在哪？',
        fill: '我现在最容易混淆的是它和相近概念的区别：',
      },
      {
        id: 'orient-misread',
        label: '最容易混淆的点是什么？',
        fill: '我觉得这里最容易让人混淆的点是：',
      },
    ],
    progress: {
      done: '任务目标已经明确，现在开始进入第一步。',
      current: '用一句话写出这个知识点在解决什么问题。',
      next: '系统会根据你的表述判断该补机制、边界还是例子。',
      later: '讲清之后进入独立表达，再做快速检查。',
    },
  },
  EXPLORE: {
    stageLabel: '先把关键机制补清楚',
    eyebrow: '继续推进',
    title: '补一句最关键的机制、边界或最小例子',
    description: '不用全写完，只补最关键的一点。系统会根据这一句继续带你走。',
    inputLabel: '补上当前最关键的一句',
    inputPlaceholder: '例如：它之所以需要这样做，是因为……',
    primaryActionLabel: '继续这一步',
    helperText: '每次只补一个点，比一次讲很多更容易推进。',
    defaultChips: [
      {
        id: 'explore-why',
        label: '为什么需要它？',
        fill: '我觉得它之所以需要存在，是因为：',
      },
      {
        id: 'explore-edge',
        label: '少了它会怎样？',
        fill: '如果没有这一步，最可能出现的问题是：',
      },
      {
        id: 'explore-example',
        label: '给一个最小例子',
        fill: '我想先用一个最小例子理解它：',
      },
    ],
    progress: {
      done: '你已经开始动手表达，不是停留在看说明。',
      current: '补清当前最关键的一点，让理解更稳。',
      next: '补到位后会进入“用自己的话讲一遍”。',
      later: '讲清后再做独立检查，最后收束这一轮。',
    },
  },
  SELF_EXPLAIN: {
    stageLabel: '先用自己的话讲清楚',
    eyebrow: '自我解释',
    title: '用自己的话串起“是什么 + 为什么”',
    description: '不用像背答案，三四句话讲清核心关系就可以继续。',
    inputLabel: '写下你的完整表述',
    inputPlaceholder: '例如：它是什么、为什么需要、如果少了会怎样……',
    primaryActionLabel: '我讲完了，继续下一步',
    helperText: '重点不是术语多准确，而是你能不能把因果讲清楚。',
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
        fill: '我现在还没有完全讲顺的地方是：',
      },
    ],
    progress: {
      done: '前面的探索已经足够支撑独立表达。',
      current: '把零散理解串成一段自己的解释。',
      next: '通过后会进入一题快速检查。',
      later: '检查通过后，再把收获收束成下一步动作。',
    },
  },
  REMEDIAL: {
    stageLabel: '先补当前暴露出的关键缺口',
    eyebrow: '补关键缺口',
    title: '只补最关键的一句，不用整段重写',
    description: '你已经接近了，现在只要把当前缺的点补清楚，再交给系统判断。',
    inputLabel: '把缺的那一句补上',
    inputPlaceholder: '例如：我刚才没说清的是……，补上后应该是……',
    primaryActionLabel: '补好了，再帮我看一次',
    helperText: '补当前缺口就行，不用回到第一步重来。',
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
    progress: {
      done: '你已经把主体轮廓说出来了。',
      current: '只补当前暴露出的最关键缺口。',
      next: '补到位后会回到检查或收束，不会无限来回。',
      later: '通过后把本轮收获整理成可继续的下一步。',
    },
  },
  CHECK: {
    stageLabel: '先独立回答这一题',
    eyebrow: '快速检查',
    title: '不看提示，先自己答完这一题',
    description: '这一步只看你能不能独立说清关键判断，不追求长答案。',
    inputLabel: '写下你的检查题答案',
    inputPlaceholder: '用一两句话直接回答即可。',
    primaryActionLabel: '答好了，检查这一题',
    helperText: '独立答出来，说明这一步已经不是只靠提示在走。',
    defaultChips: [],
    progress: {
      done: '你已经完成了理解和自我解释。',
      current: '独立答一题，确认这一步真的站稳了。',
      next: '通过后会进入最后的简短收束。',
      later: '收束结束后直接进入下一任务或下一阶段。',
    },
  },
  PASS: {
    stageLabel: '收束这一轮学习',
    eyebrow: '最后一步',
    title: '用一句总结收住这一步，再带走两个要点',
    description: '把这轮真正学到的东西收成可复用的表达，方便直接进入下一步。',
    primaryActionLabel: '进入下一步',
    helperText: '这一步不需要再解释系统逻辑，只要留下一句你能带走的话。',
    defaultChips: [],
    progress: {
      done: '这一关已经通过，主目标已经完成。',
      current: '收成一句总结和两个带走的要点。',
      next: '提交后会自动进入下一任务或报告页。',
      later: '后续阶段会继续围绕新的当前任务推进。',
    },
  },
}

function normalizeState(taskState: string, legacyComplete: boolean) {
  if (legacyComplete) return 'PASS'
  return STEP_COPY[taskState] ? taskState : 'ORIENT'
}

function buildActionModel(
  state: string,
  scaffold: TaskScaffoldResponse | null,
  recommendedUserActions: RecommendedUserActionItem[]
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

  if (scaffold?.whyThisTask) {
    sections.push({
      id: 'why-now',
      title: '为什么现在先做这一步',
      body: scaffold.whyThisTask,
    })
  }

  if (guidance?.bullets?.length) {
    sections.push({
      id: 'guidance',
      title: guidance.title || '系统此刻在关注什么',
      bullets: guidance.bullets,
    })
  }

  if (scaffold?.completionSignals?.length || task.completionCriteria?.length) {
    sections.push({
      id: 'signals',
      title: '做到什么样算通过',
      bullets: (scaffold?.completionSignals?.length
        ? scaffold.completionSignals
        : task.completionCriteria) ?? [],
    })
  }

  if (scaffold?.antiPatterns?.length) {
    sections.push({
      id: 'anti-patterns',
      title: '这一步先别这样做',
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
      title: '系统反馈',
      mastered: '主线已经有了，现在不是重写整段，而是补关键缺口。',
      gap: selfExplainMissingPoints[0],
      nextStep: '按建议补一句，再交给系统重新判断。',
      actions: [
        { id: 'apply_suggestion', label: '按建议补一句' },
        { id: 'show_example', label: '看一个最小示例' },
      ],
    }
  }

  if (taskState === 'CHECK' && checkpointQuestion) {
    return {
      visible: true,
      title: '系统反馈',
      mastered: '前面的理解和表达已经够了，现在只差独立确认。',
      gap: '还没有独立答完这道检查题。',
      nextStep: '直接用一两句话回答，不需要展开成长文。',
      actions: [],
    }
  }

  return {
    visible: false,
    title: '系统反馈',
    mastered: '',
    gap: '',
    nextStep: '',
    actions: [],
  }
}

export function buildExecutionPageModel(
  input: BuildExecutionPageModelInput
): ExecutionPageViewModel {
  const state = normalizeState(input.taskState, input.legacyComplete)
  const copy = STEP_COPY[state]

  return {
    currentStepIndex: input.guidedStepCurrent,
    currentStepTitle: copy.title,
    header: {
      title: input.task.title,
      stageLabel: copy.stageLabel,
      stepLabel: `${input.guidedStepCurrent}/${input.guidedStepTotal}`,
      estimatedTime: STEP_TIMES[state],
      subtitle: copy.description,
      taskLabel: input.progress
        ? `任务 ${input.progress.currentIndex}/${input.progress.totalTasks}`
        : undefined,
    },
    mainAction: buildActionModel(
      state,
      input.scaffold,
      input.recommendedUserActions
    ),
    feedback: buildFeedback(
      input.latestFeedback,
      state,
      input.checkpointQuestion,
      input.selfExplainMissingPoints
    ),
    progressRail: copy.progress,
    helpSections: buildHelpSections(input.task, input.scaffold, input.currentGuidance),
  }
}
