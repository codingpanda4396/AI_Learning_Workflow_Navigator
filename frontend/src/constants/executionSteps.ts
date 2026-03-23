import type { ExecutionStep } from '@/types/execution'

export const EXECUTION_STEP1: ExecutionStep = {
  stepId: 'step1',
  title: '建立结构画面',
  goal: '理解二叉树的基本形态',
  prompt:
    '请用最直观的方式解释什么是二叉树，可以用生活类比说明',
  promptWhyTitle: '为什么这样问？',
  promptWhyBullets: [
    '先建立画面，比死记定义更容易往下学',
    '逼自己用「自己的话」复述，能暴露理解缺口',
    '向 AI 提问时，越具体越容易得到能跟读的讲解',
  ],
  reflectionQuestions: ['什么是二叉树？', '什么是叶子节点？'],
  inputHint:
    '结合上面的自检问题，用你自己的话写几句：你向 AI 问完之后，脑子里留下了什么画面？不必追求完美，真实即可。',
  inputPlaceholder: '例如：我理解二叉树是……',
  completionHeadline: '本步完成：结构认知',
  completionAchievements: [
    '已按脚手架向学习渠道（含 AI）发起过提问',
    '能用自己的话触及「树形结构」这一核心画面',
    '完成了一次「问—想—写」的闭环练习',
  ],
  completionNextHint:
    '下一步将进入「基本操作」：在有了画面之后，再把操作与结构对上号。',
  state: 'PROMPT_SHOWN',
}

const REGISTRY: Record<number, ExecutionStep> = {
  1: EXECUTION_STEP1,
}

export function getExecutionStepConfig(
  step: number | string
): ExecutionStep {
  const n = typeof step === 'string' ? parseInt(step, 10) : step
  const key = Number.isFinite(n) ? n : 1
  const config = REGISTRY[key]
  if (!config) {
    return { ...EXECUTION_STEP1 }
  }
  return { ...config }
}
