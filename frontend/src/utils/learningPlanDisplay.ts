import { LEARNING_STAGE_DISPLAY, LEARNING_STAGE_SEQUENCE, STATUS_DISPLAY, type StatusDisplay } from '@/constants/learning'
import type {
  DisplayStatus,
  LearningStage,
  PlannedNode,
  TaskStatus,
  TimelineItem,
} from '@/types'

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

type StepSource = Pick<TimelineItem, 'stage' | 'status'> & {
  objective?: string
  nodeName?: string
}

const UNKNOWN_STEP: LearningStepDefinition = {
  key: 'UNKNOWN',
  order: 1,
  title: '阶段待识别',
  objective: '等待后端返回明确阶段后继续。',
  description: '当前记录里没有可识别的学习阶段，页面不会再把它默认当成结构阶段。',
  shortDescription: '等待明确阶段',
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
    return `${step.title}：${topic}。${source.objective.trim()}`
  }
  return `${step.title}：围绕 ${topic}，${step.description}`
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
    displayTitle: source.nodeName ? `${step.title}：${source.nodeName}` : step.title,
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

  const resolved = findFirstKnownStage([
    nextTask?.stage,
    runningTask?.stage,
    pendingTask?.stage,
    currentStage,
  ])

  if (resolved !== 'UNKNOWN') {
    return resolved
  }

  return timeline.length > 0 || nextTask?.stage || currentStage ? 'UNKNOWN' : 'STRUCTURE'
}

export function buildLearningStepStates(
  timeline: TimelineItem[],
  nextTask: { stage: string } | null,
  currentStage?: string | null,
) {
  const currentKey = getCurrentLearningStep(timeline, nextTask, currentStage)
  const currentOrder = currentKey === 'UNKNOWN' ? null : getLearningStepDefinition(currentKey).order

  return LEARNING_STEPS.map((step) => {
    const sameStageItems = timeline.filter((item) => normalizeLearningStage(item.stage) === step.key)
    const allDone = sameStageItems.length > 0 && sameStageItems.every((item) => item.status === 'SUCCEEDED')
    const isCurrent = currentKey !== 'UNKNOWN' && step.key === currentKey
    let state: LearningStepState = 'upcoming'

    if (allDone || (currentOrder !== null && step.order < currentOrder)) {
      state = 'completed'
    } else if (isCurrent) {
      state = 'current'
    }

    return {
      ...step,
      state,
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
    return getSuggestedActionText('RUNNING')
  }
  if (normalizedStatus === 'SUCCEEDED') {
    return getSuggestedActionText('SUCCEEDED')
  }

  const step = getLearningStepDefinition(stage)
  return step.actionText
}

export function getTaskExecutionStatus(status?: string | null): TaskStatus | 'UNKNOWN' {
  const normalized = normalizeCode(status)
  if (normalized === 'PENDING' || normalized === 'RUNNING' || normalized === 'SUCCEEDED' || normalized === 'FAILED') {
    return normalized
  }
  return 'UNKNOWN'
}
