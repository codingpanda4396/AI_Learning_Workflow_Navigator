import type { StructuredLearningGoal } from '@/types/dto'

const MAX_LEN = 32

function truncate(s: string, max: number): string {
  if (s.length <= max) return s
  return s.slice(0, max) + '…'
}

/**
 * 与规划页 resolveTopicLabel 中「仅从 structuredGoal 取值」的优先级一致。
 * 无可用字段时返回空字符串。
 */
export function workflowTopicLabelFromStructuredGoal(
  goal: StructuredLearningGoal | null | undefined
): string {
  if (!goal) return ''
  const fromTopic = goal.topics?.find((t) => t?.trim())?.trim()
  if (fromTopic) return truncate(fromTopic, MAX_LEN)
  const intent = goal.intentDescription?.trim()
  if (intent) return truncate(intent, MAX_LEN)
  const norm = goal.normalizedGoalText?.trim()
  if (norm) return truncate(norm, MAX_LEN)
  const raw = goal.rawGoalText?.trim()
  if (raw) return truncate(raw, MAX_LEN)
  return ''
}
