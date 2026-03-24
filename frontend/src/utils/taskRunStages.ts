import type { TaskScaffoldResponse } from '@/types/dto'
import type { PlanStageCode } from '@/utils/planPresentationModel'

export interface TaskRunStageStripItem {
  code: PlanStageCode
  title: string
  label: string
  scanLine: string
  status: 'done' | 'current' | 'upcoming'
}

export interface TaskRunStageViewModel {
  currentStageCode: PlanStageCode
  currentStageTitle: string
  currentStageLabel: string
  currentStageObjective: string
  currentStageDeliverable: string
  currentStageTutorRole: string
  currentPassCondition: string
  currentWhyNow: string
  currentSystemSummary: string
  currentPrimaryActionTitle: string
  currentPrimaryActionDescription: string
  path: TaskRunStageStripItem[]
}

type StageMeta = {
  title: string
  label: string
  scanLine: string
  objective: string
  deliverable: string
  tutorRole: string
}

const STAGE_ORDER: PlanStageCode[] = [
  'STRUCTURE',
  'UNDERSTANDING',
  'TRAINING',
  'REFLECTION',
]

const STAGE_META: Record<PlanStageCode, StageMeta> = {
  STRUCTURE: {
    title: 'STRUCTURE',
    label: '结构建立',
    scanLine: '先把整体框架搭起来',
    objective: '先知道这是什么、在整体里哪里、解决什么问题。',
    deliverable: '一段能说明整体框架与关键关系的最小结构描述。',
    tutorRole: '先搭结构，不展开过量细节。',
  },
  UNDERSTANDING: {
    title: 'UNDERSTANDING',
    label: '机制理解',
    scanLine: '再理解关键机制',
    objective: '把关键机制、因果链路和容易混淆的边界说清楚。',
    deliverable: '一段能解释“为什么这样工作”的最小机制说明。',
    tutorRole: '先追问机制和因果，再补关键概念。',
  },
  TRAINING: {
    title: 'TRAINING',
    label: '应用训练',
    scanLine: '再做最小练习验证',
    objective: '把理解变成动作，用最小练习证明你真的会用。',
    deliverable: '一次结构化作答、最小练习或自我解释。',
    tutorRole: '先让你动手，再按错误类型给最小提示。',
  },
  REFLECTION: {
    title: 'REFLECTION',
    label: '反思校准',
    scanLine: '最后收束薄弱点',
    objective: '确认你学会了什么、还缺什么、下一步该怎么练。',
    deliverable: '一次独立检查或一份可复用的简短复盘。',
    tutorRole: '先校准证据，再收束到下一步动作。',
  },
}

export interface ResolveTaskRunStageInput {
  taskState: string
  legacyComplete: boolean
  canSubmitSelfExplanation: boolean
  taskGoal: string
  scaffold: TaskScaffoldResponse | null
  guidanceTitle?: string
  guidanceBullets?: string[]
  currentActionLine: string
  passCondition: string
}

export function resolveCurrentTaskStage(
  taskState: string,
  legacyComplete: boolean,
  canSubmitSelfExplanation: boolean
): PlanStageCode {
  if (legacyComplete || taskState === 'CHECK' || taskState === 'PASS') {
    return 'REFLECTION'
  }
  if (
    taskState === 'SELF_EXPLAIN' ||
    taskState === 'REMEDIAL' ||
    canSubmitSelfExplanation
  ) {
    return 'TRAINING'
  }
  if (taskState === 'EXPLORE' || taskState === 'ASK') {
    return 'UNDERSTANDING'
  }
  return 'STRUCTURE'
}

function buildPrimaryActionTitle(stageCode: PlanStageCode, taskState: string): string {
  if (stageCode === 'REFLECTION') {
    return taskState === 'PASS' ? '收束这轮学习' : '完成独立检查'
  }
  if (stageCode === 'TRAINING') {
    return taskState === 'REMEDIAL' ? '先补最关键缺口' : '按脚手架完成最小训练'
  }
  if (stageCode === 'UNDERSTANDING') {
    return '解释关键机制'
  }
  return '搭出最小知识结构'
}

function buildWhyNow(
  stageCode: PlanStageCode,
  guidanceTitle: string,
  guidanceBullets: string[]
): string {
  if (guidanceTitle) {
    return guidanceBullets.length ? `${guidanceTitle}：${guidanceBullets[0]}` : guidanceTitle
  }
  if (stageCode === 'REFLECTION') return '系统正在确认你是否已经能独立说明并完成收束。'
  if (stageCode === 'TRAINING') return '系统判断现在最重要的是把理解变成动作，而不是继续听解释。'
  if (stageCode === 'UNDERSTANDING') return '系统判断你还需要先讲清机制，再进入训练。'
  return '系统判断你需要先搭起框架，避免直接掉进零散细节。'
}

function buildSystemSummary(
  stageCode: PlanStageCode,
  guidanceBullets: string[],
  passCondition: string
): string {
  const gap = guidanceBullets[0]?.trim()
  if (gap) {
    return `系统当前判断你在 ${stageCode}，还缺少：${gap}`
  }
  return `系统当前判断你在 ${stageCode}，通过标准是：${passCondition}`
}

function buildPath(currentStageCode: PlanStageCode): TaskRunStageStripItem[] {
  const currentIndex = STAGE_ORDER.indexOf(currentStageCode)
  return STAGE_ORDER.map((code, index) => ({
    code,
    title: STAGE_META[code].title,
    label: STAGE_META[code].label,
    scanLine: STAGE_META[code].scanLine,
    status: index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'upcoming',
  }))
}

export function buildTaskRunStageViewModel(
  input: ResolveTaskRunStageInput
): TaskRunStageViewModel {
  const currentStageCode = resolveCurrentTaskStage(
    input.taskState,
    input.legacyComplete,
    input.canSubmitSelfExplanation
  )
  const meta = STAGE_META[currentStageCode]
  const guidanceTitle = input.guidanceTitle?.trim() || ''
  const guidanceBullets = input.guidanceBullets ?? []
  const deliverable =
    input.scaffold?.learningObjective?.trim() ||
    input.scaffold?.taskLevelLearningIntent?.trim() ||
    input.taskGoal?.trim() ||
    meta.deliverable

  return {
    currentStageCode,
    currentStageTitle: meta.title,
    currentStageLabel: meta.label,
    currentStageObjective: meta.objective,
    currentStageDeliverable: deliverable,
    currentStageTutorRole: meta.tutorRole,
    currentPassCondition: input.passCondition,
    currentWhyNow: buildWhyNow(currentStageCode, guidanceTitle, guidanceBullets),
    currentSystemSummary: buildSystemSummary(
      currentStageCode,
      guidanceBullets,
      input.passCondition
    ),
    currentPrimaryActionTitle: buildPrimaryActionTitle(
      currentStageCode,
      input.taskState
    ),
    currentPrimaryActionDescription: input.currentActionLine,
    path: buildPath(currentStageCode),
  }
}
