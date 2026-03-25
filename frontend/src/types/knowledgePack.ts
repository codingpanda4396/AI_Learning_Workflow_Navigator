import type { PlanStepIconKey } from '@/constants/planStepShell'

export type KnowledgePackId =
  | 'os_process_thread'
  | 'net_tcp_handshake'
  | 'ds_dfs_bfs'
  | 'arch_cache_locality'

export type KnowledgeType = 'COMPARE' | 'SEQUENCE' | 'CHOICE' | 'MECHANISM'

export type PlanningPackStep = {
  flowTitle: string
  flowSubtitle: string
  icon: PlanStepIconKey
  headline: string
  whyFirst: string
  objectiveIntro: string
  objectiveBullets: string[]
  timeLabel: string
  suggestedPrompt: string
  reflectionQuestions: string[]
  closingLine: string
}

export type PlanningPack = {
  hero: {
    title: string
    subtitle: string
    auxiliaryLine: string
  }
  optionalTips: string[]
  commonMisconceptions: string[]
  phaseHighlights?: Partial<Record<'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION', string>>
  steps: PlanningPackStep[]
}

export type ExecutionPack = {
  starterPrompts: string[]
  scaffoldCards: Array<{
    id: string
    title: string
    hint: string
    prompt: string
  }>
  phaseHero: Partial<Record<'ORIENT' | 'EXPLORE' | 'SELF_EXPLAIN' | 'CHECK' | 'REMEDIAL' | 'PASS', string>>
  phaseObjective: Partial<Record<'ORIENT' | 'EXPLORE' | 'SELF_EXPLAIN' | 'CHECK' | 'REMEDIAL' | 'PASS', string>>
  microCheckLabels: string[]
  phaseUi?: Partial<Record<'ORIENT' | 'EXPLORE' | 'SELF_EXPLAIN' | 'CHECK' | 'REMEDIAL' | 'PASS', string>>
}

export type TutorPack = {
  focusLabel: string
  constrainedHints: string[]
  suggestedQuestions: string[]
}

export type CheckpointPack = {
  checkpointPrompt: string
  checkpointRubric: string[]
}

export type KnowledgePack = {
  id: KnowledgePackId
  knowledgeType: KnowledgeType
  planning: PlanningPack
  execution: ExecutionPack
  tutor: TutorPack
  checkpoint: CheckpointPack
}
