import type { PlanStepIconKey } from '@/constants/planStepShell'
import type { PlanPreviewData, StructuredLearningGoal } from '@/types/dto'

/** 仅取匹配所需字段，避免与 planPresentationModel 循环引用 */
export type ShowcaseResolveContext = {
  structuredGoal?: StructuredLearningGoal | null
}

/** 单步：StepFlow + 当前步引导卡（演示知识点专用，与 API 字段解耦） */
export type ShowcaseStepConfig = {
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

export type ShowcaseHeroConfig = {
  title: string
  subtitle: string
  auxiliaryLine: string
}

export type ShowcaseKnowledgeConfig = {
  knowledgeKey: string
  hero: ShowcaseHeroConfig
  optionalTips?: string[]
  steps: ShowcaseStepConfig[]
}

function goalTextCorpus(
  plan: PlanPreviewData | null,
  ctx: ShowcaseResolveContext
): string {
  const g = ctx.structuredGoal
  const parts: string[] = []
  if (g?.rawGoalText) parts.push(g.rawGoalText)
  if (g?.normalizedGoalText) parts.push(g.normalizedGoalText)
  if (g?.intentDescription) parts.push(g.intentDescription)
  if (g?.subject) parts.push(g.subject)
  if (g?.sourceContext) parts.push(g.sourceContext)
  if (g?.priorityModule) parts.push(g.priorityModule)
  for (const t of g?.topics ?? []) {
    if (t?.trim()) parts.push(t)
  }
  if (plan?.goal?.trim()) parts.push(plan.goal)
  for (const task of plan?.tasks ?? []) {
    if (task.title?.trim()) parts.push(task.title)
    if (task.goal?.trim()) parts.push(task.goal)
  }
  return parts.join('\n')
}

/** 与后端 PlanningContextAssembler.matchesArrayVsLinkedListShowcaseGoal 对齐 */
export function matchesArrayVsLinkedListShowcase(
  plan: PlanPreviewData | null,
  ctx: ShowcaseResolveContext
): boolean {
  const corpus = goalTextCorpus(plan, ctx)
  const hasList = corpus.includes('链表')
  const hasSeq = corpus.includes('顺序表')
  const hasArray = corpus.includes('数组')
  return hasList && (hasSeq || hasArray)
}

const ARRAY_VS_LINKED_LIST: ShowcaseKnowledgeConfig = {
  knowledgeKey: 'array_vs_linked_list',
  hero: {
    title: '现在，我们先把这两个容易混的知识点分开',
    subtitle: '别急着背定义，先搞清楚区别，后面才不会一直混',
    auxiliaryLine:
      '这一步我们只解决一个问题：顺序表和链表到底差在哪，以及什么时候该选哪个',
  },
  optionalTips: [
    '拿张纸随手画一下「连续放」和「用指针串起来」，画丑一点没关系',
    '遇到题先别算复杂度，先问一句：这题更像在折腾「中间插来插去」，还是「随便按编号取一下」？',
  ],
  steps: [
    {
      flowTitle: '先把这两个东西分清楚',
      flowSubtitle: '先搞懂它们最核心的区别',
      icon: 'compare',
      headline: '先把顺序表和链表分清楚',
      whyFirst:
        '很多人学到这里会混，不是因为听不懂，而是因为总觉得它们「差不多」。\n如果一开始没分清，后面一做题就容易乱。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '说出它们最核心的两个区别',
        '碰到常见场景时，知道该优先考虑哪一个',
      ],
      timeLabel: '大概 7 分钟就够了',
      suggestedPrompt:
        '帮我从存储方式、插入删除、访问特点这几个角度，直观地对比顺序表和链表的区别，不要太抽象。',
      reflectionQuestions: [
        '我能说出至少两个区别吗？',
        '如果题目要求频繁插入删除，我会选哪个？',
        '如果题目更强调随机访问，我会想到哪个？',
      ],
      closingLine:
        '如果让我给别人解释，我会怎么区分顺序表和链表？用一两句大白话就行。',
    },
    {
      flowTitle: '看场景时怎么选',
      flowSubtitle:
        '知道什么时候更适合用顺序表，什么时候更适合用链表',
      icon: 'puzzle',
      headline: '看到题目条件，先猜更适合用哪一个',
      whyFirst:
        '很多人会背区别，但一做题就懵，多半是还没把「题目在考什么」和「该用哪种」连起来。\n我们先练几次「看到线索就站队」，后面会快很多。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '从题干里抓出一两个关键线索（比如插删多不多、要不要按下标秒取）',
        '先给一个「更可能用顺序表 / 更可能用链表」的判断，再说理由',
      ],
      timeLabel: '大概 8 分钟就够了',
      suggestedPrompt:
        '请给我 4 个很常见的学习/做题场景（比如频繁头插、按编号访问、中间频繁删改、内存碎片敏感等），每个场景只说：更像顺序表还是更像链表，用一句大白话讲清为什么。',
      reflectionQuestions: [
        '哪一类场景我最容易误判？是「看起来在改中间」其实还是「按下标取」？',
        '如果我把「能随机访问」理解错了，会把我带到哪个错误选项？',
        '我能不能用一句口诀提醒自己：什么时候先想顺序表，什么时候先想链表？',
      ],
      closingLine:
        '挑一个你刚才最犹豫的场景，用你自己的话讲：我会先看哪一条线索，然后怎么选。',
    },
    {
      flowTitle: '自己试着讲一遍',
      flowSubtitle: '不用看答案，用自己的话说清楚',
      icon: 'message',
      headline: '合上材料，像跟同学讲一样说一遍',
      whyFirst:
        '看得懂不等于讲得顺。你只要能讲出来，哪里卡壳会立刻露出来。\n我们先不追求完美，先把话说完整。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '不偷看笔记，把「区别 + 一个场景例子」串成一小段话',
        '发现自己哪一句会卡住或绕圈',
      ],
      timeLabel: '大概 6 分钟就够了',
      suggestedPrompt:
        '先别急着教我，我想先用大白话讲一遍「顺序表和链表差在哪、各适合啥场景」。讲完后你帮我标出我讲得不准或漏掉的关键点，用很短的纠正就行。',
      reflectionQuestions: [
        '我有没有不小心把两个东西讲成「其实都一样」？',
        '我能不能用「一个生活类比」把差别说清楚？',
        '哪一句我一讲就心虚？那就圈出来，下一步专门补它。',
      ],
      closingLine:
        '用录音或打字都行：30 秒内讲完整版，不讲漂亮，讲真实。',
    },
    {
      flowTitle: '最后检查一下',
      flowSubtitle: '确认自己不是看懂了，而是真的会分辨',
      icon: 'check',
      headline: '快速自检：你是真会分，还是只是看过',
      whyFirst:
        '很多人到这里会松一口气，但其实最容易翻车的是「以为自己会了」。\n我们用几个很快的问题把底试出来，不会就当场标出来，别带到下一章。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '用三个问题把自己问住：能答上就说明这一关过了',
        '诚实标出还剩哪一条会犹豫',
      ],
      timeLabel: '大概 6 分钟就够了',
      suggestedPrompt:
        '请出 3 个「只考判断、不考背定义」的小问：每个问题给一个简短场景，让我选「更偏顺序表 / 更偏链表」，并只允许我用一句话说理由。做完后告诉我哪一题我最该复盘。',
      reflectionQuestions: [
        '哪一题我选的时候还在猜？把题干关键词写下来。',
        '如果再来一道变形的题，我会从哪一步开始想？',
        '我今天最想记住的一句「防混提醒」是什么？',
      ],
      closingLine:
        '用一句话收尾：顺序表和链表，我现在最怕混的是哪一点？下一步我只补这一点。',
    },
  ],
}

/**
 * 后续可接入：binary_tree_highlight、dfs_vs_bfs 等，结构与 ARRAY_VS_LINKED_LIST 相同。
 */
export const SHOWCASE_KNOWLEDGE_CONFIGS: ShowcaseKnowledgeConfig[] = [
  ARRAY_VS_LINKED_LIST,
]

type ShowcaseMatcher = (
  plan: PlanPreviewData,
  ctx: ShowcaseResolveContext
) => boolean

const SHOWCASE_MATCHERS: Record<string, ShowcaseMatcher> = {
  array_vs_linked_list: (plan, ctx) => matchesArrayVsLinkedListShowcase(plan, ctx),
  // dfs_vs_bfs: (plan, ctx) => ...
}

export function resolveShowcaseKnowledge(
  plan: PlanPreviewData | null,
  ctx: ShowcaseResolveContext
): ShowcaseKnowledgeConfig | null {
  if (!plan?.tasks?.length || plan.tasks.length !== 4) return null
  for (const config of SHOWCASE_KNOWLEDGE_CONFIGS) {
    const match = SHOWCASE_MATCHERS[config.knowledgeKey]
    if (match?.(plan, ctx)) return config
  }
  return null
}
