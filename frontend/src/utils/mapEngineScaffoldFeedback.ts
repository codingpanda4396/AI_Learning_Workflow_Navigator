import type { ExecutionGuideFeedbackModel } from '@/types/executionGuide'
import type { StructuredScaffoldFeedbackPayload } from '@/types/scaffoldEngine'

export function mapEngineFeedbackPayloadToGuide(
  fp: StructuredScaffoldFeedbackPayload | null | undefined
): ExecutionGuideFeedbackModel | null {
  if (!fp) return null
  const issues = fp.issuePoints?.filter(Boolean) ?? []
  const visible = Boolean(
    fp.completeness?.trim() ||
      issues.length ||
      fp.minimalRevision?.trim() ||
      fp.nextAction?.trim()
  )
  if (!visible) return null
  return {
    visible: true,
    title: '本轮反馈',
    mastered: fp.completeness?.trim() || '',
    strengths: fp.completeness?.trim() || '',
    gap: issues[0]?.trim() || fp.minimalRevision?.trim() || '',
    keyIssues: issues.slice(0, 3),
    errorTags: [],
    nextRestateAsk: fp.minimalRevision?.trim() || '',
    nextStep: fp.nextAction?.trim() || fp.minimalRevision?.trim() || '',
    actions: [
      { id: 'apply_suggestion', label: '按最小修改调整' },
      { id: 'restate', label: '重写一版' },
    ],
  }
}
