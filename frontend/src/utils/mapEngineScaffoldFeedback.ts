import type { ExecutionGuideFeedbackModel } from '@/types/executionGuide'
import type { StructuredScaffoldFeedbackPayload } from '@/types/scaffoldEngine'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

function phaseFeedbackTitle(phase?: WorkbenchPhaseCode): string {
  if (phase === 'STRUCTURE') return '你可能混淆了'
  if (phase === 'UNDERSTANDING') return '机制理解提示'
  if (phase === 'TRAINING') return '表达改进建议'
  if (phase === 'REFLECTION') return '学习总结'
  return '教学反馈'
}

export function mapEngineFeedbackPayloadToGuide(
  fp: StructuredScaffoldFeedbackPayload | null | undefined,
  phase?: WorkbenchPhaseCode
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
    title: phaseFeedbackTitle(phase),
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
