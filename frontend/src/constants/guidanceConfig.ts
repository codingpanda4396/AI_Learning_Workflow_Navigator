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
    label: '结构建立',
    stageMeta: '先知道自己在学什么，再进入细节。',
    stageGoal: '先搭出知识框架，知道这个点和前后内容分别是什么关系。',
    allowedTutorActions: [
      { id: 'explain_concept', label: '解释核心概念' },
      { id: 'concept_compare', label: '对比相邻概念' },
      { id: 'hint_only', label: '只给最小提示' },
    ],
    passEvidence: '你能说清“它是什么、在整体里哪里、和什么最相关”。',
    skipRisk: '直接跳过这一步，后面很容易只记住零散结论，遇到变形题就断线。',
    learnerFriendlyCopy: {
      launch: '先把主题放进脑子里的地图，再继续问 AI。',
      summary: '先从结构切入，把主题的位置和边界搭起来。',
      execution: '现在先把骨架讲出来，不急着把所有细节都展开。',
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
    label: '机制理解',
    stageMeta: '先讲清为什么成立，再谈会不会做。',
    stageGoal: '把关键机制、因果链路和容易混淆的边界说清楚。',
    allowedTutorActions: [
      { id: 'minimal_example', label: '给最小例子' },
      { id: 'concept_compare', label: '拆开混淆点' },
      { id: 'check_statement', label: '检查我的解释' },
    ],
    passEvidence: '你能用自己的话讲清“为什么这样工作”，并区分最容易混淆的点。',
    skipRisk: '直接跳去训练，很容易出现“会背不会用”的假熟练。',
    learnerFriendlyCopy: {
      launch: '先把机制讲清楚，再开始练，后面会稳很多。',
      summary: '你当前更适合先搞懂机制，而不是立刻做题。',
      execution: '现在先把因果链路讲清楚，系统再决定要不要放你进入训练。',
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
    label: '应用训练',
    stageMeta: '先把理解变成动作，再判断是否真的会用。',
    stageGoal: '把理解落到动作里，通过最小练习建立可重复的方法感。',
    allowedTutorActions: [
      { id: 'hint_only', label: '只给下一步提示' },
      { id: 'minimal_example', label: '补一个最小例子' },
      { id: 'check_statement', label: '检查我的做法' },
    ],
    passEvidence: '你能独立完成一题或一步，并说出自己为什么这样做。',
    skipRisk: '如果没有动作验证，前面的理解很难变成稳定表现。',
    learnerFriendlyCopy: {
      launch: '先动手做一小步，再让系统根据你的表现纠偏。',
      summary: '这轮会让你先做出动作，而不是继续停留在听解释。',
      execution: '现在轮到你自己动手，系统会根据你的表现决定是提示还是放行。',
    },
    visualTone: {
      accent: 'text-emerald-700',
      soft: 'from-emerald-50 via-white to-teal-50',
      border: 'border-emerald-200/80',
    },
  },
  REFLECTION: {
    code: 'REFLECTION',
    title: 'REFLECTION',
    label: '反思校准',
    stageMeta: '先确认学会了什么，再决定下一步怎么走。',
    stageGoal: '回看证据，确认学会了什么、还缺什么，以及下一步怎么补。',
    allowedTutorActions: [
      { id: 'check_statement', label: '检查我的总结' },
      { id: 'hint_only', label: '只提醒下一步' },
    ],
    passEvidence: '你能明确说出“我已经会什么、还差什么、下一步练什么”。',
    skipRisk: '如果不做这一步，问题会反复出现，下一轮也很难排得更准。',
    learnerFriendlyCopy: {
      launch: '最后别急着结束，把收获和缺口收束下来才算一轮完整学习。',
      summary: '最后要做一次校准，让系统知道你下一轮该怎么推进。',
      execution: '现在不是继续聊，而是先确认你是否已经能独立说明并收束这一轮。',
    },
    visualTone: {
      accent: 'text-amber-700',
      soft: 'from-amber-50 via-white to-orange-50',
      border: 'border-amber-200/80',
    },
  },
}

export const HOME_MISUSE_COMPARISONS = [
  {
    wrong: '直接问答案',
    better: '先说自己理解到哪，再请求最小帮助',
  },
  {
    wrong: '一次把问题问太大',
    better: '先拆小，再沿着阶段一步步推进',
  },
  {
    wrong: '聊完就走',
    better: '最后留下一句总结和下一步动作',
  },
]

export function stageList(): StageGuideMeta[] {
  return [
    STAGE_GUIDE_META.STRUCTURE,
    STAGE_GUIDE_META.UNDERSTANDING,
    STAGE_GUIDE_META.TRAINING,
    STAGE_GUIDE_META.REFLECTION,
  ]
}
