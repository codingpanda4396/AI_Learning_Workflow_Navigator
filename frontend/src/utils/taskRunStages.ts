import { STAGE_GUIDE_META } from '@/constants/guidanceConfig'
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
  currentDecisionReason: string
  currentLockReason: string
  currentUnlockCondition: string
  /** A：现在先做这一件事（用户口吻一句） */
  scaffoldDoNow: string
  /** B：这样开始最容易（1～2 条起手句式） */
  scaffoldStarterPhrases: string[]
  /** C：写到这一步就可以继续（2～3 条） */
  scaffoldPassBullets: string[]
  /** 进度区一行提示（替代长段系统摘要） */
  currentProgressHintLine: string
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
    title: STAGE_GUIDE_META.STRUCTURE.title,
    label: STAGE_GUIDE_META.STRUCTURE.label,
    scanLine: '先把整体框架搭起来',
    objective: STAGE_GUIDE_META.STRUCTURE.stageGoal,
    deliverable: '一段能说明整体框架与关键关系的最小结构描述。',
    tutorRole: STAGE_GUIDE_META.STRUCTURE.learnerFriendlyCopy.execution,
  },
  UNDERSTANDING: {
    title: STAGE_GUIDE_META.UNDERSTANDING.title,
    label: STAGE_GUIDE_META.UNDERSTANDING.label,
    scanLine: '再理解关键机制',
    objective: STAGE_GUIDE_META.UNDERSTANDING.stageGoal,
    deliverable: '一段能解释“为什么这样工作”的最小机制说明。',
    tutorRole: STAGE_GUIDE_META.UNDERSTANDING.learnerFriendlyCopy.execution,
  },
  TRAINING: {
    title: STAGE_GUIDE_META.TRAINING.title,
    label: STAGE_GUIDE_META.TRAINING.label,
    scanLine: '再做最小练习验证',
    objective: STAGE_GUIDE_META.TRAINING.stageGoal,
    deliverable: '一次结构化作答、最小练习或自我解释。',
    tutorRole: STAGE_GUIDE_META.TRAINING.learnerFriendlyCopy.execution,
  },
  REFLECTION: {
    title: STAGE_GUIDE_META.REFLECTION.title,
    label: STAGE_GUIDE_META.REFLECTION.label,
    scanLine: '最后收束薄弱点',
    objective: STAGE_GUIDE_META.REFLECTION.stageGoal,
    deliverable: '一次独立检查或一份可复用的简短复盘。',
    tutorRole: STAGE_GUIDE_META.REFLECTION.learnerFriendlyCopy.execution,
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

const STAGE_STARTERS: Record<PlanStageCode, [string, string]> = {
  STRUCTURE: ['我理解它是在……', '它主要解决的问题是……'],
  UNDERSTANDING: ['它之所以这样，是因为……', '我现在还不确定的是……'],
  TRAINING: ['我会先做这一步，因为……', '如果少了某一步，会怎样？'],
  REFLECTION: ['我的回答是……', '我还需要确认的是……'],
}

function buildScaffoldDoNow(
  stageCode: PlanStageCode,
  guidanceBullets: string[],
  primaryActionLine: string
): string {
  const b0 = guidanceBullets[0]?.trim()
  if (b0 && b0.length <= 56) return b0
  const launch = STAGE_GUIDE_META[stageCode].learnerFriendlyCopy.launch.trim()
  if (launch) return launch
  const first = primaryActionLine
    .replace(/^先做一件小事：[：]?\s*/m, '')
    .split(/\n/)
    .map((l) => l.trim())
    .find((l) => l.length > 0)
  return first || '先用自己的话写一小段，不用追求完美。'
}

function buildScaffoldStarterPhrases(stageCode: PlanStageCode): string[] {
  return [...STAGE_STARTERS[stageCode]]
}

function buildProgressHintLine(guidanceBullets: string[], passCondition: string): string {
  const g = guidanceBullets[0]?.trim()
  const raw = g || passCondition.trim()
  if (raw.length <= 52) return raw
  return `${raw.slice(0, 49)}…`
}

function buildScaffoldPassBullets(
  stageCode: PlanStageCode,
  scaffold: TaskScaffoldResponse | null,
  passCondition: string
): string[] {
  const signals = scaffold?.completionSignals?.map((s) => s.trim()).filter(Boolean) ?? []
  if (signals.length >= 2) return signals.slice(0, 3)
  const evidence = STAGE_GUIDE_META[stageCode].passEvidence.trim()
  const out: string[] = []
  if (evidence) out.push(evidence)
  if (passCondition && !out.includes(passCondition)) out.push(passCondition)
  return out.slice(0, 3)
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

function buildDecisionReason(stageCode: PlanStageCode): string {
  if (stageCode === 'REFLECTION') return '系统已经拿到足够多的过程证据，正在确认你能否独立收束。'
  if (stageCode === 'TRAINING') return '系统判断现在最重要的不是继续听解释，而是看你能不能自己做出动作。'
  if (stageCode === 'UNDERSTANDING') return '系统判断你还需要先讲清机制和因果，再进入训练会更稳。'
  return '系统判断你需要先把结构搭起来，避免一上来就掉进零散细节。'
}

function buildLockReason(
  taskState: string,
  stageCode: PlanStageCode,
  canSubmitSelfExplanation: boolean
): string {
  if (taskState === 'CHECK') return '你正在独立检查，系统先看证据是否足够，再决定是否放行。'
  if (taskState === 'PASS') return '当前阶段已经通过，现在可以把这轮学习收束成可复用的结果。'
  if (stageCode === 'TRAINING' && !canSubmitSelfExplanation) {
    return '你还没有完成足够的探索，当前证据不足以进入自我解释。'
  }
  if (stageCode === 'TRAINING' && canSubmitSelfExplanation) {
    return '你已完成两轮探索，现在必须先自己讲清楚，系统才会开放检查点。'
  }
  if (stageCode === 'UNDERSTANDING') {
    return '系统先要求你说清为什么成立，而不是直接跳去做题。'
  }
  if (stageCode === 'STRUCTURE') {
    return '系统先要求你把主题放进整体结构里，再继续追细节。'
  }
  return '系统还在等待关键证据，因此暂不开放下一步。'
}

function buildUnlockCondition(taskState: string, passCondition: string): string {
  if (taskState === 'PASS') return '下一步会进入总结区，留下这轮收获和下一步动作。'
  if (taskState === 'CHECK') return '通过这一轮检查后，系统才会开放最终收束。'
  return `解锁条件：${passCondition}`
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
    currentDecisionReason: buildDecisionReason(currentStageCode),
    currentLockReason: buildLockReason(
      input.taskState,
      currentStageCode,
      input.canSubmitSelfExplanation
    ),
    currentUnlockCondition: buildUnlockCondition(
      input.taskState,
      input.passCondition
    ),
    currentPrimaryActionTitle: buildPrimaryActionTitle(
      currentStageCode,
      input.taskState
    ),
    currentPrimaryActionDescription: input.currentActionLine,
    scaffoldDoNow: buildScaffoldDoNow(
      currentStageCode,
      guidanceBullets,
      input.currentActionLine
    ),
    scaffoldStarterPhrases: buildScaffoldStarterPhrases(currentStageCode),
    scaffoldPassBullets: buildScaffoldPassBullets(
      currentStageCode,
      input.scaffold,
      input.passCondition
    ),
    currentProgressHintLine: buildProgressHintLine(guidanceBullets, input.passCondition),
    path: buildPath(currentStageCode),
  }
}
