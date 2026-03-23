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

/** 规划页常驻「画面感」提示（不放入折叠 OptionalTips） */
export type ShowcaseMindImageHint = {
  title: string
  body: string
  /** 为 true 时展示极简二叉树 ASCII（仅结构示意）；默认不展示 */
  showSimpleBinaryTreeAscii?: boolean
}

/** 演示页气质：方法选择型（如 DFS/BFS）与默认对比/结构型区分样式 */
export type ShowcaseFocusType = 'default' | 'method-selection'

/**
 * 演示知识点配置（对外文档字段对照）：
 * - hero.title → heroTitle
 * - hero.subtitle → heroSubtitle
 * - hero.auxiliaryLine → learningFocus
 * - steps[].whyFirst → whyThisFirst
 * - steps[].suggestedPrompt → aiPrompt
 * - steps[].objectiveBullets → expectedGain[]
 * - steps[].reflectionQuestions → reflectionQuestions[]
 * - optionalTips[] / mindImageHint → optionalTip / 画面提示
 * - judgmentTips[] → 看题判断提醒（method-selection）
 * - knowledgeLabel → 知识点正式名称（可选展示）
 */
export type ShowcaseKnowledgeConfig = {
  knowledgeKey: string
  hero: ShowcaseHeroConfig
  optionalTips?: string[]
  /** 脑海画面提示：强化「抽象概念可视化」体感 */
  mindImageHint?: ShowcaseMindImageHint
  focusType?: ShowcaseFocusType
  /** 方法选择型：当前任务卡下方的「看题提醒自己」条目 */
  judgmentTips?: string[]
  /** 知识点标题（如「DFS 和 BFS 的区别与使用场景」），可选用于页眉补充 */
  knowledgeLabel?: { title: string; subtitle?: string }
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

/** 与后端 PlanningContextAssembler.matchesBinaryTreeBasicShowcaseGoal 对齐 */
export function matchesBinaryTreeBasicShowcase(
  plan: PlanPreviewData | null,
  ctx: ShowcaseResolveContext
): boolean {
  const corpus = goalTextCorpus(plan, ctx)
  return corpus.includes('二叉树')
}

/** 与后端 PlanningContextAssembler.matchesDfsVsBfsShowcaseGoal 对齐 */
export function matchesDfsVsBfsShowcase(
  plan: PlanPreviewData | null,
  ctx: ShowcaseResolveContext
): boolean {
  const raw = goalTextCorpus(plan, ctx)
  const lower = raw.toLowerCase()
  const hasDfs = lower.includes('dfs') || raw.includes('深度优先')
  const hasBfs = lower.includes('bfs') || raw.includes('广度优先')
  return hasDfs && hasBfs
}

/**
 * 二叉树 · 基本结构理解（抽象概念建模演示）
 * knowledgeKey: binary_tree_basic
 */
const BINARY_TREE_BASIC: ShowcaseKnowledgeConfig = {
  knowledgeKey: 'binary_tree_basic',
  hero: {
    title: '现在，我们先把二叉树真正看明白',
    subtitle: '先有画面，再抠细节，别一上来就背定义',
    auxiliaryLine:
      '这一步先不写代码，先把「长什么样、谁在上谁在下」在你脑子里想明白',
  },
  mindImageHint: {
    title: '你可以把它想成……',
    body:
      '一棵向下展开的小树：最上面一个点，下面分出左右两个方向。先抓住这个形状，术语会好记很多。',
    showSimpleBinaryTreeAscii: true,
  },
  optionalTips: [
    '拿张纸随手画一棵只有三层的二叉树，节点画成圆也行、方也行，丑一点没关系',
    '想到「家谱」「公司汇报线」也可以，关键是：每个位置最多往下分出两条支路',
  ],
  steps: [
    {
      flowTitle: '先在脑子里有个画面',
      flowSubtitle: '别急着记定义，先把二叉树想象出来',
      icon: 'brain',
      headline: '先把二叉树在脑子里「画出来」',
      whyFirst:
        '很多人不是不会背定义，而是脑子里没有画面。没画面的话，后面遍历、递归都会越学越乱。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '用自己的话解释什么是二叉树',
        '指出根节点、子节点、叶子节点分别是什么',
        '看着一张简单图，能说清节点之间的关系',
      ],
      timeLabel: '大概 8 分钟就够了',
      suggestedPrompt:
        '请你用最直观的方式解释什么是二叉树，可以用生活中的类比来讲，比如家谱、组织结构或者树枝，不要一上来就讲很抽象的定义。',
      reflectionQuestions: [
        '二叉树和「一排元素连起来」那种有什么不一样？',
        '什么叫根节点？',
        '什么样的节点可以叫叶子节点？',
      ],
      closingLine:
        '如果我要给一个没学过的人解释二叉树，我会怎么说？',
    },
    {
      flowTitle: '再把几个关键位置认清',
      flowSubtitle: '弄明白根节点、子节点、叶子节点分别是什么',
      icon: 'puzzle',
      headline: '让「根、子节点、叶子」和你脑中的形状对上号',
      whyFirst:
        '背下定义却一指图就问「这是啥」的情况很常见。我们先把几个位置词和画面绑在一起，后面才不会指东说西。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '随便指一个节点，能判断它是不是根、有没有左右孩子、是不是叶子',
        '能说清「左子树」「右子树」在图里大概是哪一块',
      ],
      timeLabel: '大概 7 分钟就够了',
      suggestedPrompt:
        '请用最直白的方式说明：在一棵二叉树里，根节点、子节点（左/右孩子）、叶子节点各是什么；最好结合一张只有 5～7 个节点的简单示意图来讲，少用公式化定义。',
      reflectionQuestions: [
        '根节点和其他节点最本质的差别是什么？',
        '叶子节点还能不能再往下分？',
        '我说「某节点的左子树」时，脑子里对应的是图上的哪一片？',
      ],
      closingLine:
        '闭上眼睛想一下：我会先用哪三个词，向同桌描述「这棵树里谁在上、谁在下、谁在末端」？',
    },
    {
      flowTitle: '自己试着讲给别人听',
      flowSubtitle: '能讲出来，才说明你真的开始懂了',
      icon: 'message',
      headline: '不照抄定义，把二叉树讲成一段完整的话',
      whyFirst:
        '看得懂不等于讲得顺。你只要能讲出来，哪里卡壳会立刻露出来。\n我们先不追求完美，先把话说完整。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '不偷看笔记，用一分钟把「形状 + 根/叶子/左右」串成一小段话',
        '发现自己哪一句会绕圈、跳过或用词发虚',
      ],
      timeLabel: '大概 8 分钟就够了',
      suggestedPrompt:
        '我想先用大白话讲一遍「二叉树在我脑子里长什么样，根、叶子、左右子树各是啥关系」。请你在我讲完后，只指出我讲得含糊或跳步的地方，用很短的话纠正。',
      reflectionQuestions: [
        '我有没有把二叉树讲成「和普通列表差不多」？',
        '别人听了会不会误以为还有「第三个分叉」？',
        '哪一句我一讲就不确定？把它圈出来，下一步专门补它。',
      ],
      closingLine:
        '用一句话收尾：如果只能带别人记住一个画面，我会选哪一句？',
    },
    {
      flowTitle: '检查还有没有糊涂的地方',
      flowSubtitle: '看看哪些词你还是说不清',
      icon: 'check',
      headline: '诚实点名：还有哪些词你一说就心虚',
      whyFirst:
        '很多人到这里会松一口气，但最容易翻车的是「以为自己有画面了」。用几个快问把底试出来，说不清就标出来，别带到下一关。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '用两三个快问快答检查：根、孩子、叶子、左/右子树是否还说利落',
        '诚实写下：还剩哪一两个词一讲就含糊',
      ],
      timeLabel: '大概 6 分钟就够了',
      suggestedPrompt:
        '请出 3 个「只考画面感、不考代码」的小问：比如指认根/叶子、说清左子树包含哪些节点、判断某节点有没有右孩子等。我答完后，告诉我哪一题最值得复盘。',
      reflectionQuestions: [
        '哪一题我答的时候还在猜？把题干关键词写下来。',
        '如果明天再看，我最怕忘的是哪一点？',
        '我今天最想记住的一句「防糊涂提醒」是什么？',
      ],
      closingLine:
        '用一句话收尾：二叉树这一小块里，我现在最想说清但还没说清的是哪一点？下一步我只补这一点。',
    },
  ],
}

/**
 * DFS 与 BFS · 方法选择型演示
 * knowledgeKey: dfs_vs_bfs
 */
const DFS_VS_BFS: ShowcaseKnowledgeConfig = {
  knowledgeKey: 'dfs_vs_bfs',
  focusType: 'method-selection',
  knowledgeLabel: {
    title: 'DFS 和 BFS 的区别与使用场景',
    subtitle: '方法选择型：先会分搜法，再会看题站队',
  },
  hero: {
    title: '现在，我们把 DFS 和 BFS 真正分开',
    subtitle: '不是背两个名字，而是搞清楚它们搜的方式和使用场景',
    auxiliaryLine: '这一步我们重点解决「看见题目时到底该想到谁」',
  },
  optionalTips: [
    '不用纠结实现细节，先把「一路往下试」和「一层一层扩」在脑子里跑通',
    '看见「最少步数 / 最短层数」先停一秒：这往往是 BFS 的信号',
  ],
  judgmentTips: [
    '想一路往下试，常会想到 DFS',
    '想一层一层扩展，常会想到 BFS',
    '一看到「最少步数 / 最短层数」这类描述，要优先警觉 BFS',
  ],
  steps: [
    {
      flowTitle: '先看清它们到底怎么搜',
      flowSubtitle: '先理解 DFS 和 BFS 的过程差别',
      icon: 'compare',
      headline: '先看懂 DFS 和 BFS 到底有什么不一样',
      whyFirst:
        '很多人背过名字，但看见题还是空白。多半是还没把「它们各自怎么搜」想成自己能复述的过程。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '说清 DFS 和 BFS 的搜索过程差别',
        '面对简单场景时，能初步判断更适合哪一种',
        '不再只会机械地背「深度优先、广度优先」',
      ],
      timeLabel: '大概 8 分钟就够了',
      suggestedPrompt:
        '请你用一个简单例子说明 DFS 和 BFS 分别是怎么搜索的，重点讲清搜索顺序的区别，尽量讲直观一点。',
      reflectionQuestions: [
        'DFS 是怎么一路走下去的？',
        'BFS 为什么会一层一层找？',
        '如果题目更关心「最短层数/最少步数」，我应该更容易想到谁？',
      ],
      closingLine: '如果有人问我 DFS 和 BFS 的区别，我会怎么解释？',
    },
    {
      flowTitle: '再看题目里怎么选',
      flowSubtitle: '知道什么场景下更容易想到哪一种',
      icon: 'puzzle',
      headline: '从题干里抓线索：这题更像「往下试」还是「一层层扩」',
      whyFirst:
        '会背名字不等于会做题。我们先练一件事：从题干里读出更偏「一路往下试」还是「一层层扩」。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '说出两三类更常联想到 DFS 的题干信号',
        '说出两三类更常联想到 BFS 的题干信号（含「最少步数/最短层数」警觉）',
      ],
      timeLabel: '大概 8 分钟就够了',
      suggestedPrompt:
        '请给我 5 个极简题目描述片段（每段一两句话），分别随机偏 DFS 或偏 BFS，但不要直接写算法名；我每段只回答「更可能 DFS / 更可能 BFS」并用一句话说线索。最后指出我最容易看走眼的 1～2 种表述。',
      reflectionQuestions: [
        '哪一类词最容易让我误判成「只能 DFS」？',
        '「最少步数」出现时，我为什么应该先想到 BFS？',
        '我会给自己一句什么提醒，避免看见图就只想到一种搜法？',
      ],
      closingLine:
        '挑一段我最犹豫的题干，用自己的话写下：我先看哪两个词，再决定站哪一队。',
    },
    {
      flowTitle: '自己判断一次',
      flowSubtitle: '别只看懂，要试着自己做选择',
      icon: 'message',
      headline: '不翻笔记，先给出一道「假想题」你会怎么选',
      whyFirst:
        '看懂别人的例子和自己站队，中间还隔一层。我们用一道你自己编或改编的小场景，把选择说清楚。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '自编（或改编）一个极简场景，并明确说「我会用 DFS / BFS，因为…」',
        '发现自己哪句话是在套话、哪句话是真的在看线索',
      ],
      timeLabel: '大概 7 分钟就够了',
      suggestedPrompt:
        '我想口述一道很小的图、网格或状态转移场景（不超过三句话），并说明我会选 DFS 还是 BFS。请你只追问：我的理由是否对应了搜索顺序，而不是在背模板。',
      reflectionQuestions: [
        '我的理由里，有没有「其实两种都能做」却没说清为什么我更偏爱这一种？',
        '如果换一个问法（比如从「计数」改成「最少步数」），我的选择会不会变？',
      ],
      closingLine:
        '用 20 秒语音或一行字：「这题我会先想到 ___，最先看的线索是 ___。」',
    },
    {
      flowTitle: '检查你是不是真的会分',
      flowSubtitle: '确认不是记住了词，而是真的能判断',
      icon: 'check',
      headline: '快速自检：你是真会分，还是只是记得两个名字',
      whyFirst:
        '最容易翻车的是「以为自己会了」。用几道只考判断的小题把底试出来，犹豫就标记，别带到下一题。',
      objectiveIntro: '学完这一小步，你应该能：',
      objectiveBullets: [
        '完成 3 个「只考站队」的小判断并自评把握度',
        '诚实写下仍未条件反射想到 BFS 的那类关键词',
      ],
      timeLabel: '大概 6 分钟就够了',
      suggestedPrompt:
        '请出 3 个只考判断的小问：每个给一个很短的场景描述，让我只选 DFS 或 BFS，并允许我用一句话说理由。做完后告诉我哪一题最值得复盘。',
      reflectionQuestions: [
        '哪一题我还在猜？把题干里的触发词写下来。',
        '如果明天换一道变体，我会从哪一步开始想？',
        '我今天最想带走的一句「看题提醒自己」是什么？',
      ],
      closingLine:
        '一句话收尾：DFS 和 BFS，我现在最怕混的是哪一点？下一步我只补这一点。',
    },
  ],
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
        '很多人不是听不懂，而是总觉得它们「差不多」。一开始没分清，后面一做题就容易乱。',
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
        '很多人会背区别，但一做题就懵，多半是还没把题干线索和「该用哪种」连起来。先练几次「看见线索就站队」。',
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
 * 顺序：更具体的演示优先；dfs_vs_bfs 先于 array，避免双主题 corpus 歧义时倾向图遍历演示。
 */
export const SHOWCASE_KNOWLEDGE_CONFIGS: ShowcaseKnowledgeConfig[] = [
  BINARY_TREE_BASIC,
  DFS_VS_BFS,
  ARRAY_VS_LINKED_LIST,
]

type ShowcaseMatcher = (
  plan: PlanPreviewData,
  ctx: ShowcaseResolveContext
) => boolean

const SHOWCASE_MATCHERS: Record<string, ShowcaseMatcher> = {
  binary_tree_basic: (plan, ctx) => matchesBinaryTreeBasicShowcase(plan, ctx),
  dfs_vs_bfs: (plan, ctx) => matchesDfsVsBfsShowcase(plan, ctx),
  array_vs_linked_list: (plan, ctx) => matchesArrayVsLinkedListShowcase(plan, ctx),
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
