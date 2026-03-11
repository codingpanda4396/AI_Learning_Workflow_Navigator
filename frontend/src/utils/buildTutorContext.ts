import type { RunTaskResponse, SessionOverviewResponse, TaskOutputSection } from '@/types'
import { getLearningStageDisplay, normalizeLearningStage } from '@/utils/learningPlanDisplay'

const CONTEXT_START = '<<AI_TUTOR_CONTEXT>>'
const CONTEXT_END = '<</AI_TUTOR_CONTEXT>>'

export interface TutorContext {
  sessionId: number | null
  stage: string | null
  stepLabel: string
  taskId: number | null
  topic: string
  course: string
  chapter: string
  goal: string
  taskTitle: string
  taskGoal: string
  taskSummary: string
  contextTitle: string
}

interface BuildTutorContextInput {
  sessionId?: number | null
  stage?: string | null
  stepLabel?: string | null
  taskId?: number | null
  topic?: string | null
  course?: string | null
  chapter?: string | null
  goal?: string | null
  taskTitle?: string | null
  taskGoal?: string | null
  taskSummary?: string | null
  session?: SessionOverviewResponse | null
  task?: RunTaskResponse | null
}

function cleanText(value?: string | null) {
  return value?.trim() ?? ''
}

function compactText(value: string, maxLength = 220) {
  if (value.length <= maxLength) {
    return value
  }
  return `${value.slice(0, maxLength - 1).trimEnd()}…`
}

function extractSectionSummary(sections: TaskOutputSection[] = []) {
  const parts = sections
    .flatMap((section) => [section.text, ...(section.bullets ?? []), ...(section.steps ?? []), ...(section.items ?? [])])
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .slice(0, 4)
    .map((item) => item.trim())

  return compactText(parts.join('；'))
}

export function getTutorStepLabel(stage?: string | null, fallback?: string | null) {
  const fallbackText = cleanText(fallback)
  if (fallbackText) {
    return fallbackText
  }

  const normalized = normalizeLearningStage(stage)
  if (normalized === 'STRUCTURE') return '搭框架'
  if (normalized === 'UNDERSTANDING') return '学明白'
  if (normalized === 'TRAINING') return '做检测'
  if (normalized === 'EVALUATION') return '看结果'
  return getLearningStageDisplay(stage).title
}

export function buildTutorContext(input: BuildTutorContextInput): TutorContext {
  const session = input.session
  const task = input.task
  const stepLabel = getTutorStepLabel(input.stage ?? task?.stage ?? session?.currentStage, input.stepLabel)
  const topic = cleanText(input.topic) || cleanText(input.taskTitle) || cleanText(input.goal) || '当前任务'
  const taskTitle = cleanText(input.taskTitle) || topic
  const taskSummary = cleanText(input.taskSummary) || extractSectionSummary(task?.output.sections) || cleanText(input.taskGoal)
  const taskGoal = cleanText(input.taskGoal) || taskSummary || cleanText(input.goal)

  return {
    sessionId: input.sessionId ?? session?.sessionId ?? null,
    stage: cleanText(input.stage) || task?.stage || session?.currentStage || null,
    stepLabel,
    taskId: input.taskId ?? null,
    topic,
    course: cleanText(input.course) || cleanText(session?.courseId),
    chapter: cleanText(input.chapter) || cleanText(session?.chapterId),
    goal: cleanText(input.goal) || cleanText(session?.goalText),
    taskTitle,
    taskGoal,
    taskSummary,
    contextTitle: `${stepLabel} · ${topic}`,
  }
}

export function buildTutorRequestContent(message: string, context: TutorContext) {
  const lines = [
    `${CONTEXT_START}`,
    `sessionId: ${context.sessionId ?? ''}`,
    `stage: ${context.stage ?? ''}`,
    `stepLabel: ${context.stepLabel}`,
    `taskId: ${context.taskId ?? ''}`,
    `topic: ${context.topic}`,
    `course: ${context.course}`,
    `chapter: ${context.chapter}`,
    `goal: ${context.goal}`,
    `taskTitle: ${context.taskTitle}`,
    `taskGoal: ${context.taskGoal}`,
    `taskSummary: ${context.taskSummary}`,
    `${CONTEXT_END}`,
    '',
    message.trim(),
  ]

  return lines.join('\n')
}

export function stripTutorContext(content: string) {
  const pattern = new RegExp(`${CONTEXT_START}[\\s\\S]*?${CONTEXT_END}\\s*`, 'g')
  return content.replace(pattern, '').trim()
}
