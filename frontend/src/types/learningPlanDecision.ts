export type LearningPlanPathStageKey =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export type LearningPlanDecisionViewModel = {
  hero: {
    eyebrow: string
    title: string
    decisionText: string
    reasonText: string
    outcomeText: string
    ctaLabel: string
    ctaSubtext?: string
  }
  reasoning: {
    title: string
    summary: string
    bullets: Array<{
      label: string
      text: string
    }>
  }
  firstTask: {
    title: string
    taskName: string
    reasonText: string
    estimatedTimeText: string
    benefitText: string
  }
  pathPreview: {
    title: string
    summary: string
    stages: Array<{
      key: LearningPlanPathStageKey
      label: string
      description: string
      stateLabel: string
      active: boolean
    }>
  }
  contrast: string
}
