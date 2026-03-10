import client from '@/api/client'
import type { LearningFeedbackResponse } from '@/types'

interface WeakPointNodeDto {
  node_id: number
  node_name: string
  mastery_score?: number | null
  training_accuracy?: number | null
  latest_evaluation_score?: number | null
  attempt_count?: number | null
  recent_error_tags?: string[]
  reasons?: string[]
}

interface LearningFeedbackResponseDto {
  session_id: number
  diagnosis_summary?: string
  weak_nodes?: WeakPointNodeDto[]
}

export async function getLearningFeedback(sessionId: number): Promise<LearningFeedbackResponse> {
  const { data } = await client.get<LearningFeedbackResponseDto>(`/session/${sessionId}/learning-feedback/weak-points`)
  return {
    sessionId: data.session_id,
    diagnosisSummary: data.diagnosis_summary ?? '',
    weakNodes: (data.weak_nodes ?? []).map((item) => ({
      nodeId: item.node_id,
      nodeName: item.node_name,
      masteryScore: typeof item.mastery_score === 'number' ? item.mastery_score : 0,
      trainingAccuracy: typeof item.training_accuracy === 'number' ? item.training_accuracy : 0,
      latestEvaluationScore:
        typeof item.latest_evaluation_score === 'number' ? item.latest_evaluation_score : null,
      attemptCount: typeof item.attempt_count === 'number' ? item.attempt_count : 0,
      recentErrorTags: Array.isArray(item.recent_error_tags)
        ? item.recent_error_tags.filter((tag): tag is string => typeof tag === 'string')
        : [],
      reasons: Array.isArray(item.reasons)
        ? item.reasons.filter((reason): reason is string => typeof reason === 'string')
        : [],
    })),
  }
}
