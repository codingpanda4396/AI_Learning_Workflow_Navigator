import type { CompleteTaskRequest } from '@/types/dto'
import type { TaskCompletionStatusType } from '@/types/enums'

const DEFAULT_NEXT_PRACTICE = '先巩固本任务要点，有需要再继续下一练。'

/**
 * 组装任务完成请求：直接完成模式不传收束字段；脚手架模式传 summaryText 等以满足 TaskClosureValidator。
 */
export function buildCompleteTaskPayload(input: {
  sessionId: string
  completionStatus: TaskCompletionStatusType
  legacyComplete: boolean
  summaryText: string
  learnedPoint1: string
  learnedPoint2: string
  nextPracticeIntent: string
  learnerReflection?: string
  taskStartedAt: number
  userMessageCount: number
}): CompleteTaskRequest {
  const elapsedMin = Math.max(
    1,
    Math.round((Date.now() - input.taskStartedAt) / 60000)
  )

  const base: CompleteTaskRequest = {
    sessionId: input.sessionId,
    completionStatus: input.completionStatus,
    durationMinutes: elapsedMin,
    interactionCount: input.userMessageCount,
    learnerReflection: input.learnerReflection?.trim() || undefined,
  }

  if (input.legacyComplete) {
    return base
  }

  const next =
    input.nextPracticeIntent.trim() || DEFAULT_NEXT_PRACTICE

  return {
    ...base,
    summaryText: input.summaryText.trim(),
    learnedFrameworkPoints: [
      input.learnedPoint1.trim(),
      input.learnedPoint2.trim(),
    ],
    nextPracticeIntent: next,
    closurePayloadVersion: 'sprint4-v1',
    selfRatedConfidence: 'MEDIUM',
    userSummarySubmitted: true,
  }
}
