import type { PlanPreviewData } from '@/types/dto'

export interface PlanPresentationModel {
  firstActionTitle: string
  estimatedMinutes?: number
  whyOneLiner: string
  nextThreeLines: string[]
  commitCtaLabel: string
}

const DEFAULT_FLOW_LINES = [
  '先理解基本结构',
  '再用一个最小例子跑通',
  '最后进入简单做题或自我解释',
]

const DEFAULT_WHY =
  '结合你的目标与诊断，系统先安排更稳妥的起点，避免一上来就卡住。'

function truncate(s: string, max: number): string {
  if (s.length <= max) return s
  return s.slice(0, max) + '…'
}

/**
 * 将后端 PlanPreview 降噪为规划页「行动面板」所需展示模型（纯前端，不改 API）。
 */
export function buildPlanPresentation(
  plan: PlanPreviewData | null
): PlanPresentationModel | null {
  if (!plan) return null

  const entry = plan.recommendedEntry
  const firstTask = plan.tasks?.[0]
  const title =
    entry?.title?.trim() ||
    firstTask?.title?.trim() ||
    '从第一个学习任务开始'

  const minutes = entry?.estimatedMinutes ?? firstTask?.estimatedMinutes

  let why = entry?.reason?.trim()
  if (!why && plan.keyEvidence?.[0]) why = plan.keyEvidence[0].trim()
  if (!why && plan.recommendedStrategy?.reason)
    why = plan.recommendedStrategy.reason.trim()
  if (!why) why = DEFAULT_WHY
  why = truncate(why, 80)

  const nextThree: string[] = []
  if (plan.stages?.length) {
    for (const s of plan.stages.slice(0, 3)) {
      const line = s.objective
        ? `${s.title}：${s.objective}`
        : s.title
      nextThree.push(truncate(line, 56))
    }
  }
  while (nextThree.length < 3) {
    nextThree.push(DEFAULT_FLOW_LINES[nextThree.length])
  }

  return {
    firstActionTitle: title,
    estimatedMinutes: minutes,
    whyOneLiner: why,
    nextThreeLines: nextThree.slice(0, 3),
    commitCtaLabel: '进入学习任务',
  }
}
