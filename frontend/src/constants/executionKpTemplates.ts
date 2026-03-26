import type { KnowledgePointExecutionTemplate } from '@/types/knowledgePack'

export type ExecutionStartModeTemplate = {
  id: string
  title: string
  hint: string
  actionLabel: string
  prompt: (kpName: string) => string
  /** 默认：直接发送；prefill 只填入输入框由用户补全 */
  behavior?: 'send' | 'prefill'
}

const CONCEPT_MODES: ExecutionStartModeTemplate[] = [
  {
    id: 'concept-from-zero',
    title: '从头讲',
    hint: '',
    actionLabel: '我不会，帮我从头讲',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，我不会，请从 0 讲清楚（按顺序）：\n1）它是什么\n2）为什么不用更简单的替代\n3）我最该先记住的一条判断`,
  },
  {
    id: 'concept-minimal-example',
    title: '最小例子',
    hint: '',
    actionLabel: '给我一个最简单例子',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，请给一个最小例子，一步一步走一遍，并标出关键转折点。`,
  },
  {
    id: 'concept-check-my',
    title: '检查我的理解',
    hint: '',
    actionLabel: '我试着解释，你帮我检查',
    behavior: 'prefill',
    prompt: (kp) =>
      `关于「${kp}」，下面是我的理解。请你判断：\n- 哪对了\n- 哪错了\n- 哪里不完整\n\n我的理解是：\n`,
  },
]

const PROCESS_MODES: ExecutionStartModeTemplate[] = [
  {
    id: 'process-from-zero',
    title: '从头讲',
    hint: '',
    actionLabel: '我不会，帮我从头讲',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，我不会，请按时间顺序把过程讲清楚：先发生什么、中间怎么变、最后得到什么。`,
  },
  {
    id: 'process-minimal-example',
    title: '最小例子',
    hint: '',
    actionLabel: '给我一个最简单例子',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，请给一个最小例子，标出每一步的状态变化。`,
  },
  {
    id: 'process-check-my',
    title: '检查我的复述',
    hint: '',
    actionLabel: '我试着解释，你帮我检查',
    behavior: 'prefill',
    prompt: (kp) =>
      `关于「${kp}」，我先按自己的话复述流程。请你判断：\n- 哪一步对\n- 哪一步漏了\n- 哪一步顺序不对\n\n我的复述是：\n`,
  },
]

const STRUCTURE_MODES: ExecutionStartModeTemplate[] = [
  {
    id: 'struct-from-zero',
    title: '从头讲',
    hint: '',
    actionLabel: '我不会，帮我从头讲',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，我不会，请从 0 讲清楚结构：由哪些部分组成、各自负责什么、它们如何连接。`,
  },
  {
    id: 'struct-minimal-example',
    title: '最小例子',
    hint: '',
    actionLabel: '给我一个最简单例子',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，请给一个最小例子，演示结构如何变化（操作前后对比）。`,
  },
  {
    id: 'struct-check-my',
    title: '检查我的理解',
    hint: '',
    actionLabel: '我试着解释，你帮我检查',
    behavior: 'prefill',
    prompt: (kp) =>
      `关于「${kp}」，下面是我对结构的理解。请你判断：\n- 哪对了\n- 哪错了\n- 哪里不完整\n\n我的理解是：\n`,
  },
]

const PROBLEM_MODES: ExecutionStartModeTemplate[] = [
  {
    id: 'prob-from-zero',
    title: '从头讲',
    hint: '',
    actionLabel: '我不会，帮我从头讲',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，我不会，请带我识别题型：这题在考什么、题干里最关键的条件是哪一句、通常思路分几步。`,
  },
  {
    id: 'prob-skeleton',
    title: '解题骨架',
    hint: '',
    actionLabel: '给我看解题骨架',
    behavior: 'send',
    prompt: (kp) =>
      `关于「${kp}」，请只给解题骨架（步骤与依据），不要直接给出最终数值答案。`,
  },
  {
    id: 'prob-check-my',
    title: '检查我的一步',
    hint: '',
    actionLabel: '我先写一步，你帮我对不对',
    behavior: 'prefill',
    prompt: (kp) =>
      `关于「${kp}」，我先写出我的第一步。请你判断方向对不对，并指出我漏看了哪个条件。\n\n我的第一步是：\n`,
  },
]

const STUCK_BY_TYPE: Record<KnowledgePointExecutionTemplate, string[]> = {
  CONCEPT: ['看例子', '看对比', '请求简化'],
  PROCESS: ['看例子', '看对比', '请求简化'],
  STRUCTURE: ['看例子', '看对比', '请求简化'],
  PROBLEM: ['看例子', '看对比', '请求简化'],
}

export function getStartModeTemplates(
  t: KnowledgePointExecutionTemplate
): ExecutionStartModeTemplate[] {
  switch (t) {
    case 'PROCESS':
      return PROCESS_MODES
    case 'STRUCTURE':
      return STRUCTURE_MODES
    case 'PROBLEM':
      return PROBLEM_MODES
    case 'CONCEPT':
    default:
      return CONCEPT_MODES
  }
}

export function defaultStuckActions(t: KnowledgePointExecutionTemplate): string[] {
  return [...STUCK_BY_TYPE[t]]
}

export function resolveStartModes(
  t: KnowledgePointExecutionTemplate,
  kpName: string
): Array<{
  id: string
  title: string
  hint: string
  actionLabel: string
  prompt: string
  behavior?: 'send' | 'prefill'
}> {
  return getStartModeTemplates(t).map((m) => ({
    id: m.id,
    title: m.title,
    hint: m.hint,
    actionLabel: m.actionLabel,
    prompt: m.prompt(kpName),
    behavior: m.behavior ?? 'send',
  }))
}
