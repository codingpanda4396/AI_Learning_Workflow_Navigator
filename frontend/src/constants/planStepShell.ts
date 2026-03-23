/**
 * 规划页「引导壳层」：口语标题 / 副标题 / 图标 / 为什么先做这一步 / 一句话目标。
 * 与后端 TaskType、常见 4 任务序列对齐；不替代 API 合同字段。
 */

export type PlanStepIconKey =
  | 'brain'
  | 'puzzle'
  | 'message'
  | 'check'
  | 'compare'
  | 'practice'

export type StepShell = {
  /** StepFlow 里的短标题（口语） */
  title: string
  subtitle: string
  icon: PlanStepIconKey
  /** 当前步卡片主标题；缺省时用 title */
  headline?: string
  /** 卡片顶部「为什么」长文案 */
  whyThisStep: string
  /** 一句话能力目标（无 objectiveBullets 时用） */
  oneLineObjective: string
  /** 能力目标引导语 + 列表（优先于 oneLineObjective） */
  objectiveIntro?: string
  objectiveBullets?: string[]
  /** 覆盖默认反思提问（避免与 AI 提示重复） */
  reflectionLines?: string[]
}

/** 基础修补：CONCEPT_EXPLAIN → GUIDED_EXAMPLE → SELF_EXPLANATION → CHECKPOINT_REVIEW */
export const FOUNDATION_FOUR_SHELL: StepShell[] = [
  {
    title: '先把它到底是个啥说清楚',
    subtitle: '先搞懂「它在讲什么」，后面才不会越看越懵',
    icon: 'brain',
    whyThisStep:
      '很多人一上来就抠细节，结果连「这玩意儿是啥」都没对齐。我们先花一点点时间把它说清楚，你会轻松很多。',
    oneLineObjective: '你能用一句大白话讲清楚它是什么，并举一个小例子。',
  },
  {
    title: '看一个最小例子跟着走一遍',
    subtitle: '先跟着走一遍，你会突然明白很多',
    icon: 'puzzle',
    whyThisStep:
      '光看定义容易飘，例子能把事情落到地上。我们先走一遍最小的，你会更有感觉。',
    oneLineObjective: '你能顺着例子说出：每一步在干嘛，为什么要这样走。',
  },
  {
    title: '合上资料，自己讲一遍',
    subtitle: '讲不出来，往往就是还没真懂',
    icon: 'message',
    whyThisStep:
      '试着讲一遍，你会立刻知道哪里是「以为自己懂了」。别怕讲得不漂亮，先讲出来。',
    oneLineObjective: '你能不偷看原文，用自己的话把要点串起来。',
  },
  {
    title: '快速检查一下：哪里还虚？',
    subtitle: '先把薄的地方找出来，再往下走',
    icon: 'check',
    whyThisStep:
      '与其带着糊涂往前冲，不如先花两分钟把漏洞标出来。你会发现后面省很多时间。',
    oneLineObjective: '你能诚实说出：哪一块还讲不顺、哪一题还会犹豫。',
  },
]

/** 概念澄清：第二步为对比 */
export const CONCEPT_CLARIFICATION_FOUR_SHELL: StepShell[] = [
  {
    title: '先把最容易搞混的地方搞清楚',
    subtitle: '搞清区别，后面才不会越学越乱',
    icon: 'compare',
    headline: '搞清楚链表和顺序表到底有什么区别',
    whyThisStep:
      '很多人一开始就学细节，但其实最容易卡住的地方，是分不清这些概念。\n我们先把容易混的地方理清楚，后面会顺很多。',
    oneLineObjective: '',
    objectiveIntro: '学完这一小步，你应该能：',
    objectiveBullets: [
      '说清楚它们最核心的区别',
      '遇到题目时不再分不清',
    ],
    reflectionLines: [
      '试着想一想：如果只用一个很俗的大白话讲差别，你会怎么说？',
      '你发现最容易把哪两点搞在一起？先标出来也行。',
    ],
  },
  {
    title: '把它和容易混的那一块摆在一起比一比',
    subtitle: '先比清楚边界，做题就不容易选错',
    icon: 'compare',
    whyThisStep:
      '只背名字不对比，一做题就容易懵。我们先把容易混的点摊开，你会稳很多。',
    oneLineObjective: '你能随口说出至少两处不一样，或者一处很关键的联系。',
  },
  FOUNDATION_FOUR_SHELL[1],
  FOUNDATION_FOUR_SHELL[2],
]

const SHELL_BY_TASK_TYPE: Record<string, StepShell> = {
  CONCEPT_EXPLAIN: FOUNDATION_FOUR_SHELL[0],
  GUIDED_EXAMPLE: FOUNDATION_FOUR_SHELL[1],
  COMPARE_AND_CONNECT: CONCEPT_CLARIFICATION_FOUR_SHELL[1],
  SELF_EXPLANATION: FOUNDATION_FOUR_SHELL[2],
  CHECKPOINT_REVIEW: FOUNDATION_FOUR_SHELL[3],
  MICRO_PRACTICE: {
    title: '动手做一道很小的题',
    subtitle: '做一小下，比光看更能试出真懂没懂',
    icon: 'practice',
    whyThisStep:
      '你会发现：一做就露馅的地方，往往就是下一步最该补的。',
    oneLineObjective: '你能把题做完，并用一句话说清「为什么这么做」。',
  },
}

const FALLBACK_SHELL: StepShell = {
  title: '先把这一小步走完',
  subtitle: '步子小一点，更容易真的推进',
  icon: 'brain',
  whyThisStep: '我们不贪多，先把眼前这一小步走稳，你会更有底气往下走。',
  oneLineObjective: '你能照着引导做完，并且自己心里觉得说得通。',
}

export function shellForTaskType(taskType: string | undefined): StepShell {
  if (!taskType) return FALLBACK_SHELL
  return SHELL_BY_TASK_TYPE[taskType] ?? FALLBACK_SHELL
}

/** 识别常见 4 任务序列，返回整段壳层；否则 null */
export function shellRowForFourTaskSequence(
  taskTypes: string[]
): StepShell[] | null {
  if (taskTypes.length !== 4) return null
  const key = taskTypes.join(',')
  if (
    key ===
    'CONCEPT_EXPLAIN,GUIDED_EXAMPLE,SELF_EXPLANATION,CHECKPOINT_REVIEW'
  ) {
    return FOUNDATION_FOUR_SHELL
  }
  if (
    key ===
    'CONCEPT_EXPLAIN,COMPARE_AND_CONNECT,GUIDED_EXAMPLE,SELF_EXPLANATION'
  ) {
    return CONCEPT_CLARIFICATION_FOUR_SHELL
  }
  return null
}

function looksLikeRawCode(s: string): boolean {
  return /^[A-Z0-9_]+$/.test(s.trim())
}

/**
 * 合并 API 任务标题与壳层：路径上始终用壳层短标题；卡片主标题优先用人能读的 API 标题。
 */
export function mergeShellWithTaskTitle(
  taskTitle: string,
  taskType: string | undefined,
  rowShellStep?: StepShell | null
): StepShell {
  const base = rowShellStep ?? shellForTaskType(taskType)
  const trimmed = taskTitle?.trim()
  const apiOk = !!(trimmed && !looksLikeRawCode(trimmed))
  const headline = apiOk
    ? trimmed!
    : (base.headline?.trim() || base.title)
  return { ...base, title: base.title, headline }
}
