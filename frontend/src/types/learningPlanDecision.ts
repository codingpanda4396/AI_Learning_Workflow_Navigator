export type LearningPlanPathStageKey =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export type LearningPlanDecisionViewModel = {
  hero: {
    /** 例：当前起点 · STRUCTURE */
    eyebrow: string
    /** 主标题，≤18 字 */
    title: string
    /** 仅一句副标题 */
    subtitle: string
    chips: [string, string, string]
    ctaLabel: string
    ctaSubtext?: string
    secondaryCtaLabel: string
  }
  reasoning: {
    accordionTitle: string
    bullets: Array<{
      label: string
      text: string
    }>
  }
  firstTask: {
    headline: string
    goalLine: string
    deliverableLine: string
    errorReminder: string
    enterTaskLabel: string
  }
  pathPreview: {
    title: string
    stages: Array<{
      key: LearningPlanPathStageKey
      label: string
      /** 折叠时在轨道中展示的一句 */
      railLine: string
      stateLabel: string
      active: boolean
    }>
  }
}
