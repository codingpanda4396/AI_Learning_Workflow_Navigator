import type { ExecutionStep } from '@/types/execution'

/** 文案中的知识点名用 __TOPIC__，由 adaptExecutionStepForTopic 替换 */
export const EXECUTION_STEP1: ExecutionStep = {
  stepId: 'step1',
  knowledgeKey: 'demo_topic',
  knowledgePoint: '__TOPIC__',
  title: '建立结构画面',
  goal: '理解__TOPIC__的基本形态',
  prompt:
    '请用最直观的方式解释什么是__TOPIC__，可以用生活类比说明',
  promptWhyTitle: '为什么这样问？',
  promptWhyBullets: [
    '先建立画面，比死记定义更容易往下学',
    '逼自己用「自己的话」复述，能暴露理解缺口',
    '向 AI 提问时，越具体越容易得到能跟读的讲解',
  ],
  reflectionQuestions: [
    '什么是__TOPIC__？',
    '和相邻知识点相比，__TOPIC__ 的关键区别是什么？',
  ],
  inputHint:
    '顺着上面那几个问题，写几句就行：问完之后，你脑子里对__TOPIC__是否有一幅清晰画面？说得出来吗？不用完美，真实就好。',
  inputPlaceholder: '例如：我理解__TOPIC__是……',
  completionHeadline: '这一关：结构认知，你拿下了',
  completionAchievements: [
    '你真的按我的提示去问过一遍了',
    '能用自己的话碰到__TOPIC__的核心要点',
    '把「问—想—写」这一小段走通了',
  ],
  completionNextHint:
    '接下来我带你看下一步：先有了整体画面，再把细节扣回去，就对上了。',
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
