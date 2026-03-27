export type LearningPlanPathStageKey =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export type LearningPlanDecisionViewModel = {
  hero: {
    title: string
    goalHint?: string
    blockerText: string
    wrongActionText: string
    correctActionText: string
    ctaLabel: string
    ctaSubtext?: string
  }
  reason: {
    title: string
    goalText: string
    blockerText: string
    consequenceText: string
  }
  causal: {
    title: string
    currentState: string
    strategyAction: string
    expectedResult: string
  }
  firstTask: {
    title: string
    intro: string
    actionText: string
    estimatedTimeText: string
    benefitText: string
  }
  pathPreview: {
    title: string
    stages: Array<{
      key: LearningPlanPathStageKey
      label: string
      description: string
      active: boolean
    }>
  }
  contrast: {
    title: string
    riskText: string
    betterPathText: string
  }
}
