export type StageGuideCode =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export interface StageDecisionEvidence {
  stageGoal: string
  passEvidence: string
  skipRisk: string
}

export interface TutorActionPolicy {
  id: string
  label: string
}

export interface StageVisualTone {
  accent: string
  soft: string
  border: string
}

export interface StageGuideMeta {
  code: StageGuideCode
  title: string
  label: string
  stageMeta: string
  stageGoal: string
  allowedTutorActions: TutorActionPolicy[]
  passEvidence: string
  skipRisk: string
  learnerFriendlyCopy: {
    launch: string
    summary: string
    execution: string
  }
  visualTone: StageVisualTone
}

export const STAGE_GUIDE_META: Record<StageGuideCode, StageGuideMeta> = {
  STRUCTURE: {
    code: 'STRUCTURE',
    title: 'STRUCTURE',
    label: '先搭框架',
    stageMeta: '先把这块内容放对位置。',
    stageGoal: '先搭出最小框架，知道它和前后内容怎么连起来。',
    allowedTutorActions: [
      { id: 'explain_concept', label: '解释核心概念' },
      { id: 'concept_compare', label: '对比相邻概念' },
      { id: 'hint_only', label: '只给最小提示' },
    ],
    passEvidence: '你能说清它是什么，放在整体里哪里。',
    skipRisk: '这一步不搭稳，后面很容易只记住零散结论。',
    learnerFriendlyCopy: {
      launch: '先把这块内容放进整体框架里。',
      summary: '先搭位置和边界，再往下走。',
      execution: '现在先把骨架写出来，不用急着补全细节。',
    },
    visualTone: {
      accent: 'text-sky-700',
      soft: 'from-sky-50 via-white to-cyan-50',
      border: 'border-sky-200/80',
    },
  },
  UNDERSTANDING: {
    code: 'UNDERSTANDING',
    title: 'UNDERSTANDING',
    label: '讲清关键点',
    stageMeta: '先讲清为什么，再谈会不会做。',
    stageGoal: '把关键机制、因果和易混点讲清楚。',
    allowedTutorActions: [
      { id: 'minimal_example', label: '给最小例子' },
      { id: 'concept_compare', label: '拆开混淆点' },
      { id: 'check_statement', label: '检查我的解释' },
    ],
    passEvidence: '你能用自己的话讲清为什么会这样。',
    skipRisk: '这一步没讲清，后面很容易变成会背不会用。',
    learnerFriendlyCopy: {
      launch: '先把关键机制讲清楚。',
      summary: '现在更需要先讲明白，不是立刻做题。',
      execution: '现在先把因果讲顺，再进入下一步。',
    },
    visualTone: {
      accent: 'text-indigo-700',
      soft: 'from-indigo-50 via-white to-violet-50',
      border: 'border-indigo-200/80',
    },
  },
  TRAINING: {
    code: 'TRAINING',
    title: 'TRAINING',
    label: '动手练一遍',
    stageMeta: '先做一遍，再看是不是真的会。',
    stageGoal: '把理解变成动作，用最小练习站稳这一步。',
    allowedTutorActions: [
      { id: 'hint_only', label: '只给下一步提示' },
      { id: 'minimal_example', label: '补一个最小例子' },
      { id: 'check_statement', label: '检查我的做法' },
    ],
    passEvidence: '你能独立做完一题或一步，并说出理由。',
    skipRisk: '不动手练，前面的理解很难真正站稳。',
    learnerFriendlyCopy: {
      launch: '先动手做一小步。',
      summary: '这一步先做出来，不继续停在听解释。',
      execution: '现在轮到你自己动手，先把这一步做完。',
    },
    visualTone: {
      accent: 'text-accent-hover',
      soft: 'from-accent-muted/75 via-white to-white',
      border: 'border-accent/25',
    },
  },
  REFLECTION: {
    code: 'REFLECTION',
    title: 'REFLECTION',
    label: '检查并收住',
    stageMeta: '先收一收，再决定下一步。',
    stageGoal: '回看这一步学会了什么、还差什么、接下来练什么。',
    allowedTutorActions: [
      { id: 'check_statement', label: '检查我的总结' },
      { id: 'hint_only', label: '只提醒下一步' },
    ],
    passEvidence: '你能说出已经会什么、还差什么、下一步练什么。',
    skipRisk: '这一步不收住，问题很容易在下一轮重复出现。',
    learnerFriendlyCopy: {
      launch: '最后先把收获和缺口收一下。',
      summary: '先收住这一轮，再决定后面怎么练。',
      execution: '现在先做个简短检查，把这一轮收住。',
    },
    visualTone: {
      accent: 'text-accent-hover',
      soft: 'from-accent-muted/80 via-white to-accent-muted/40',
      border: 'border-accent/25',
    },
  },
}

export function stageList(): StageGuideMeta[] {
  return [
    STAGE_GUIDE_META.STRUCTURE,
    STAGE_GUIDE_META.UNDERSTANDING,
    STAGE_GUIDE_META.TRAINING,
    STAGE_GUIDE_META.REFLECTION,
  ]
}
