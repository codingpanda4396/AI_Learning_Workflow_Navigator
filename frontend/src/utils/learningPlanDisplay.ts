import { LEARNING_STAGE_DISPLAY, LEARNING_STAGE_SEQUENCE, STATUS_DISPLAY, type StatusDisplay } from '@/constants/learning'
import type { AsyncStatus, DisplayStatus, LearningStage, PlannedNode, StepStatus, TaskStatus, TimelineItem } from '@/types'

export type LearningStepKey = LearningStage | 'UNKNOWN'
export type LearningStepState = 'completed' | 'current' | 'upcoming'

export interface LearningStepDefinition {
  key: LearningStepKey
  order: 1 | 2 | 3 | 4
  title: string
  objective: string
  description: string
  shortDescription: string
  actionText: string
}

export interface DisplayTaskMeta {
  phaseKey: LearningStepKey
  phaseLabel: string
  displayTitle: string
  displayDescription: string
  actionText: string
  statusLabel: string
}

export interface SessionStageViewModel {
  key: LearningStepKey
  order: 1 | 2 | 3 | 4
  title: string
  objective: string
  description: string
  state: LearningStepState
  stepStatus: StepStatus
  statusLabel: string
  actionHint: string
  actionText: string
  completionStandard: string
  isActionable: boolean
  isBusy: boolean
  busyLabel: string | null
  reason: string
}

type StepSource = Pick<TimelineItem, 'stage' | 'status'> & {
  objective?: string
  nodeName?: string
}

const UNKNOWN_STEP: LearningStepDefinition = {
  key: 'UNKNOWN',
  order: 1,
  title: '准备中',
  objective: '系统正在整理当前学习阶段。',
  description: '当前还没有足够信息判断所处阶段，页面会在数据返回后自动更新。',
  shortDescription: '等待阶段信息',
  actionText: '查看详情',
}

export const LEARNING_STEPS = LEARNING_STAGE_SEQUENCE.map((stage) => {
  const display = LEARNING_STAGE_DISPLAY[stage]
  return {
    ...display,
    objective: display.shortDescription,
    description: display.shortDescription,
  }
}) as readonly LearningStepDefinition[]

function normalizeCode(value?: string | null) {
  return (value ?? '').trim().toUpperCase()
}

function getDefaultCompletionStandard(step: LearningStepDefinition) {
  if (step.key === 'STRUCTURE') return '能说清这一轮要学的内容、顺序和知识关系。'
  if (step.key === 'UNDERSTANDING') return '能用自己的话解释关键概念和原理。'
  if (step.key === 'TRAINING') return '完成练习并拿到可用反馈。'
  if (step.key === 'EVALUATION') return '看懂结果，知道自己下一步该补什么。'
  return '等待系统同步当前阶段。'
}

function getStepStatusLabel(status: StepStatus) {
  return STATUS_DISPLAY[status].label
}

function getLockedReason(step: LearningStepDefinition) {
  return `先完成前一步，才能进入“${step.title}”。`
}

function getStageActionText(stepStatus: StepStatus) {
  if (stepStatus === 'AVAILABLE') return '开始'
  if (stepStatus === 'ACTIVE') return '继续'
  if (stepStatus === 'DONE') return '查看结果'
  if (stepStatus === 'ERROR') return '重试'
  return '等待解锁'
}

function getStageActionHint(step: LearningStepDefinition, stepStatus: StepStatus) {
  if (stepStatus === 'AVAILABLE') return `现在可以开始“${step.title}”。`
  if (stepStatus === 'ACTIVE') return `现在先把“${step.title}”完成。`
  if (stepStatus === 'DONE') return `“${step.title}”已经完成，可以继续往下。`
  if (stepStatus === 'ERROR') return `“${step.title}”出现异常，建议重新进入。`
  return getLockedReason(step)
}

export function normalizeLearningStage(input?: string | null): LearningStepKey {
  const normalized = normalizeCode(input)
  if (!normalized) {
    return 'UNKNOWN'
  }
  if (normalized === 'REFLECTION') {
    return 'EVALUATION'
  }
  return LEARNING_STAGE_SEQUENCE.includes(normalized as LearningStage) ? (normalized as LearningStage) : 'UNKNOWN'
}

export function getLearningStepDefinition(stepKey?: string | null): LearningStepDefinition {
  const normalized = normalizeLearningStage(stepKey)
  if (normalized === 'UNKNOWN') {
    return UNKNOWN_STEP
  }
  return LEARNING_STEPS.find((step) => step.key === normalized) ?? UNKNOWN_STEP
}

export function getLearningStageDisplay(stepKey?: string | null) {
  return getLearningStepDefinition(stepKey)
}

function createTopicAwareDescription(step: LearningStepDefinition, source: StepSource, topic: string) {
  if (source.objective?.trim()) {
    return `${step.title}：围绕“${topic}”，${source.objective.trim()}`
  }
  return `${step.title}：围绕“${topic}”，${step.description}`
}

export function getStatusDisplay(status?: string | null): StatusDisplay {
  const normalized = normalizeCode(status) as DisplayStatus
  return STATUS_DISPLAY[normalized] ?? { label: '待处理', actionText: '查看详情' }
}

export function getStatusLabel(status?: string | null) {
  return getStatusDisplay(status).label
}

export function getSuggestedActionText(status?: string | null) {
  return getStatusDisplay(status).actionText
}

export function mapTaskToDisplayMeta(source: StepSource, topic: string): DisplayTaskMeta {
  const step = getLearningStepDefinition(source.stage)
  const statusDisplay = getStatusDisplay(source.status)

  return {
    phaseKey: step.key,
    phaseLabel: step.title,
    displayTitle: source.nodeName ? `${step.title} · ${source.nodeName}` : step.title,
    displayDescription: createTopicAwareDescription(step, source, topic),
    actionText: getPrimaryActionText(source.stage, source.status),
    statusLabel: statusDisplay.label,
  }
}

function findFirstKnownStage(stages: Array<string | null | undefined>) {
  for (const stage of stages) {
    const normalized = normalizeLearningStage(stage)
    if (normalized !== 'UNKNOWN') {
      return normalized
    }
  }
  return 'UNKNOWN'
}

export function getCurrentLearningStep(
  timeline: TimelineItem[],
  nextTask: { stage: string } | null,
  currentStage?: string | null,
): LearningStepKey {
  const runningTask = timeline.find((item) => item.status === 'RUNNING')
  const pendingTask = timeline.find((item) => item.status !== 'SUCCEEDED')

  const resolved = findFirstKnownStage([nextTask?.stage, runningTask?.stage, pendingTask?.stage, currentStage])

  if (resolved !== 'UNKNOWN') {
    return resolved
  }

  return timeline.length > 0 || nextTask?.stage || currentStage ? 'UNKNOWN' : 'STRUCTURE'
}

function getStageTaskStatus(items: TimelineItem[]) {
  if (items.some((item) => item.status === 'FAILED')) return 'FAILED'
  if (items.some((item) => item.status === 'RUNNING')) return 'RUNNING'
  if (items.length > 0 && items.every((item) => item.status === 'SUCCEEDED')) return 'SUCCEEDED'
  if (items.some((item) => item.status === 'PENDING')) return 'PENDING'
  return 'EMPTY'
}

function resolveStepStatus(
  step: LearningStepDefinition,
  currentStep: LearningStepKey,
  items: TimelineItem[],
): StepStatus {
  const taskStatus = getStageTaskStatus(items)
  if (taskStatus === 'FAILED') return 'ERROR'
  if (taskStatus === 'SUCCEEDED') return 'DONE'

  if (currentStep === 'UNKNOWN') {
    return items.length > 0 ? 'AVAILABLE' : 'LOCKED'
  }

  const currentOrder = getLearningStepDefinition(currentStep).order
  if (step.order < currentOrder) return 'DONE'
  if (step.order > currentOrder) return 'LOCKED'
  if (taskStatus === 'RUNNING') return 'ACTIVE'
  if (taskStatus === 'PENDING' || taskStatus === 'EMPTY') return 'AVAILABLE'
  return 'ACTIVE'
}

export function buildLearningStepStates(
  timeline: TimelineItem[],
  nextTask: { stage: string } | null,
  currentStage?: string | null,
) {
  const currentKey = getCurrentLearningStep(timeline, nextTask, currentStage)

  return LEARNING_STEPS.map((step) => {
    const sameStageItems = timeline.filter((item) => normalizeLearningStage(item.stage) === step.key)
    const stepStatus = resolveStepStatus(step, currentKey, sameStageItems)
    const state: LearningStepState =
      stepStatus === 'DONE' ? 'completed' : stepStatus === 'ACTIVE' || stepStatus === 'AVAILABLE' ? 'current' : 'upcoming'

    return {
      ...step,
      state,
    }
  })
}

export function buildSessionStageViewModels(
  timeline: TimelineItem[],
  nextTask: { stage: string } | null,
  currentStage?: string | null,
  asyncStatus: AsyncStatus = 'IDLE',
): SessionStageViewModel[] {
  const currentKey = getCurrentLearningStep(timeline, nextTask, currentStage)

  return LEARNING_STEPS.map((step) => {
    const stageItems = timeline.filter((item) => normalizeLearningStage(item.stage) === step.key)
    const stepStatus = resolveStepStatus(step, currentKey, stageItems)
    const state: LearningStepState =
      stepStatus === 'DONE' ? 'completed' : stepStatus === 'ACTIVE' || stepStatus === 'AVAILABLE' ? 'current' : 'upcoming'
    const currentTask =
      stageItems.find((item) => item.status === 'RUNNING') ??
      stageItems.find((item) => item.status !== 'SUCCEEDED') ??
      null
    const isBusy = asyncStatus === 'RUNNING' && (stepStatus === 'ACTIVE' || stepStatus === 'AVAILABLE')
    const busyLabel = isBusy ? '处理中' : null
    const actionText = getStageActionText(stepStatus)
    const actionHint = getStageActionHint(step, stepStatus)
    const reason =
      stepStatus === 'LOCKED'
        ? getLockedReason(step)
        : currentTask
          ? `当前任务 #${currentTask.taskId} 正在推动这一阶段继续完成。`
          : stepStatus === 'DONE'
            ? `这一阶段已经完成，可以继续下一步。`
            : `现在优先完成“${step.title}”。`

    return {
      key: step.key,
      order: step.order,
      title: step.title,
      objective: step.objective,
      description: step.description,
      state,
      stepStatus,
      statusLabel: isBusy ? '处理中' : getStepStatusLabel(stepStatus),
      actionHint,
      actionText: isBusy ? '处理中...' : actionText,
      completionStandard: getDefaultCompletionStandard(step),
      isActionable: stepStatus === 'AVAILABLE' || stepStatus === 'ACTIVE' || stepStatus === 'ERROR' || stepStatus === 'DONE',
      isBusy,
      busyLabel,
      reason,
    }
  })
}

export function findNodeNameByTask(taskId: number, plannedNodes: PlannedNode[], fallbackNodeId?: number | null) {
  for (const plan of plannedNodes) {
    if (plan.stages.some((stage) => stage.taskId === taskId)) {
      return plan.nodeName
    }
  }

  if (fallbackNodeId) {
    return plannedNodes.find((plan) => plan.nodeId === fallbackNodeId)?.nodeName ?? `主题 ${fallbackNodeId}`
  }

  return '当前知识点'
}

export function getPrimaryActionText(stage?: string | null, status?: string | null) {
  const normalizedStatus = normalizeCode(status)
  if (normalizedStatus === 'RUNNING') {
    return '继续'
  }
  if (normalizedStatus === 'SUCCEEDED') {
    return '查看结果'
  }

  const step = getLearningStepDefinition(stage)
  return step.key === 'UNKNOWN' ? '查看详情' : step.actionText
}

export function getTaskExecutionStatus(status?: string | null): TaskStatus | 'UNKNOWN' {
  const normalized = normalizeCode(status)
  if (normalized === 'PENDING' || normalized === 'RUNNING' || normalized === 'SUCCEEDED' || normalized === 'FAILED') {
    return normalized
  }
  return 'UNKNOWN'
}
