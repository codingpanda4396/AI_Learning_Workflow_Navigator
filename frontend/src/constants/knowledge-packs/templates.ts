import type { KnowledgePack } from '@/types/knowledgePack'

type KnowledgeTemplate = Pick<KnowledgePack, 'knowledgeType' | 'planning' | 'execution' | 'tutor' | 'checkpoint'>

export const COMPARE_TEMPLATE: KnowledgeTemplate = {
  knowledgeType: 'COMPARE',
  planning: {
    hero: {
      title: '先把差异框出来，再看细节',
      subtitle: '对比不是背结论，而是建立判断坐标',
      auxiliaryLine: '先看边界，再选策略。',
    },
    optionalTips: ['先列 3 个最容易混淆的维度，再逐个分清。'],
    commonMisconceptions: ['只背定义，不看使用边界。'],
    steps: [],
  },
  execution: {
    starterPrompts: ['先做一张对比表', '先说最容易混淆的一点', '用场景判断差异'],
    scaffoldCards: [],
    phaseHero: {},
    phaseObjective: {},
    microCheckLabels: ['我能说出关键差异'],
  },
  tutor: {
    focusLabel: '优先对比与纠偏',
    constrainedHints: ['先对比，再深入机制。'],
    suggestedQuestions: ['这两个概念最关键的差异是什么？'],
  },
  checkpoint: {
    checkpointPrompt: '给你一个场景，判断该用哪个并解释理由。',
    checkpointRubric: ['结论明确', '理由对应场景', '能指出另一方案为什么不优先'],
  },
}

export const SEQUENCE_TEMPLATE: KnowledgeTemplate = {
  ...COMPARE_TEMPLATE,
  knowledgeType: 'SEQUENCE',
}

export const CHOICE_TEMPLATE: KnowledgeTemplate = {
  ...COMPARE_TEMPLATE,
  knowledgeType: 'CHOICE',
}

export const MECHANISM_TEMPLATE: KnowledgeTemplate = {
  ...COMPARE_TEMPLATE,
  knowledgeType: 'MECHANISM',
}
