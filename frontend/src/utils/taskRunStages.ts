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
    scanLine: '先搭起来',
    objective: STAGE_GUIDE_META.STRUCTURE.stageGoal,
    deliverable: '一段最小框架描述。',
    tutorRole: STAGE_GUIDE_META.STRUCTURE.learnerFriendlyCopy.execution,
  },
  UNDERSTANDING: {
    title: STAGE_GUIDE_META.UNDERSTANDING.title,
    label: STAGE_GUIDE_META.UNDERSTANDING.label,
    scanLine: '再讲明白',
    objective: STAGE_GUIDE_META.UNDERSTANDING.stageGoal,
    deliverable: '一段能讲清为什么的说明。',
    tutorRole: STAGE_GUIDE_META.UNDERSTANDING.learnerFriendlyCopy.execution,
  },
  TRAINING: {
    title: STAGE_GUIDE_META.TRAINING.title,
    label: STAGE_GUIDE_META.TRAINING.label,
    scanLine: '再动手练',
    objective: STAGE_GUIDE_META.TRAINING.stageGoal,
    deliverable: '一次最小练习或自我解释。',
    tutorRole: STAGE_GUIDE_META.TRAINING.learnerFriendlyCopy.execution,
  },
  REFLECTION: {
    title: STAGE_GUIDE_META.REFLECTION.title,
    label: STAGE_GUIDE_META.REFLECTION.label,
    scanLine: '最后检查',
    objective: STAGE_GUIDE_META.REFLECTION.stageGoal,
    deliverable: '一次独立检查或简短复盘。',
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
  if (stageCode === 'REFLECTION') return '先做个检查，把这一轮收住。'
  if (stageCode === 'TRAINING') return '先把理解变成动作，再继续往下走。'
  if (stageCode === 'UNDERSTANDING') return '先把关键机制讲清楚。'
  return '先把框架搭起来，再进入细节。'
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

function buildSystemSummary(guidanceBullets: string[], passCondition: string): string {
  const gap = guidanceBullets[0]?.trim()
  if (gap) {
    return `你现在先补：${gap}`
  }
  return `做到这里就可以继续：${passCondition}`
}

function buildDecisionReason(stageCode: PlanStageCode): string {
  if (stageCode === 'REFLECTION') return '现在先确认这一步是否已经站稳。'
  if (stageCode === 'TRAINING') return '现在先看你能不能自己做出来。'
  if (stageCode === 'UNDERSTANDING') return '现在先把机制和因果讲顺。'
  return '现在先把结构搭起来。'
}

function buildLockReason(
  taskState: string,
  stageCode: PlanStageCode,
  canSubmitSelfExplanation: boolean
): string {
  if (taskState === 'CHECK') return '你正在做独立检查，先把这一题答完。'
  if (taskState === 'PASS') return '这一步已经通过，现在把这一轮收住。'
  if (stageCode === 'TRAINING' && !canSubmitSelfExplanation) {
    return '先把前面的探索补够，再进入自我解释。'
  }
  if (stageCode === 'TRAINING' && canSubmitSelfExplanation) {
    return '先自己讲清楚，再进入检查。'
  }
  if (stageCode === 'UNDERSTANDING') {
    return '先说清为什么成立，不急着直接做题。'
  }
  if (stageCode === 'STRUCTURE') {
    return '先把主题放进整体结构里，再追细节。'
  }
  return '先把当前这一步做完，再打开下一步。'
}

function buildUnlockCondition(taskState: string, passCondition: string): string {
  if (taskState === 'PASS') return '下一步会进入总结区，留下一句收获和下一步动作。'
  if (taskState === 'CHECK') return '答完这一轮检查后，就能进入最后收束。'
  return `做到这里就继续：${passCondition}`
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
    currentSystemSummary: buildSystemSummary(guidanceBullets, input.passCondition),
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
