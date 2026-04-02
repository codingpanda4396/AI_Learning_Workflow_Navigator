import type {
  McqQuestion,
  PhaseKey,
  ReflectionStrategy,
  ScaffoldButton,
} from '@/types/executionWorkbench'

export const DFS_BFS_PHASE_GOALS: Record<PhaseKey, string> = {
  structure: '先分清 DFS 和 BFS 分别在做什么。',
  understanding: '理解它们为什么会呈现不同的搜索方式。',
  training: '试着用你自己的话，把 DFS / BFS 讲清楚。',
  reflection: '回看这次学习：你弄清了什么，还容易混淆什么。',
}

export const DFS_BFS_PHASE_STRIP: Record<PhaseKey, string> = {
  structure: '结构建立',
  understanding: '机制理解',
  training: '表达训练',
  reflection: '反思收束',
}

export const DFS_BFS_NEXT_LABELS: Record<PhaseKey, string> = {
  structure: '进入机制理解',
  understanding: '进入表达训练',
  training: '进入反思收束',
  reflection: '查看学习报告',
}

// ─── STRUCTURE 三道题 ───

export const DFS_BFS_STRUCTURE_QUESTIONS: McqQuestion[] = [
  {
    id: 'sq1',
    prompt: '下面哪种描述更接近 BFS？',
    correctId: 'B',
    options: [
      { id: 'A', label: '沿一条路径尽量深入，再回头继续探索' },
      { id: 'B', label: '从起点开始，按层逐步向外扩展' },
      { id: 'C', label: '随机挑一个相邻节点继续走' },
    ],
    feedbackByOption: {
      A: { tone: 'redirect', body: 'A 更接近 DFS。DFS 的感觉是先往深处走到底，再回退。' },
      B: { tone: 'positive', body: '对。BFS 的关键感受是"分层推进"，它会先看离起点近的，再看更远的。' },
      C: { tone: 'redirect', body: '不是随机。DFS 和 BFS 都有明确的遍历顺序，只是推进方式不同。' },
    },
  },
  {
    id: 'sq2',
    prompt: '下面哪种描述更接近 DFS？',
    correctId: 'B',
    options: [
      { id: 'A', label: '优先访问所有距离起点为 1 的点，再看距离为 2 的点' },
      { id: 'B', label: '优先沿当前路径继续深入，走不通时再回退' },
      { id: 'C', label: '每次都选择编号最小的节点' },
    ],
    feedbackByOption: {
      A: { tone: 'redirect', body: '这更像 BFS 的行为——按距离逐层展开。' },
      B: { tone: 'positive', body: '对。DFS 的核心就是"先深入一条路径到底，再回退寻找新分支"。' },
      C: { tone: 'redirect', body: '编号顺序只是实现细节，不是 DFS 的核心特征。' },
    },
  },
  {
    id: 'sq3',
    prompt: '如果你更关心"离起点最近的目标"，你会更倾向先想到哪种搜索方式？',
    correctId: 'A',
    options: [
      { id: 'A', label: 'BFS' },
      { id: 'B', label: 'DFS' },
      { id: 'C', label: '两者完全一样' },
    ],
    feedbackByOption: {
      A: { tone: 'positive', body: '对。BFS 天然更像"最近一圈一圈找"，所以适合寻找最短路径。' },
      B: { tone: 'redirect', body: 'DFS 更适合"先把一条路探到底"，不保证先找到最近的。' },
      C: { tone: 'redirect', body: '它们的推进方式不同，找到最近目标的效率也不一样。' },
    },
  },
]

// ─── UNDERSTANDING 脚手架（右侧面板）───

/** 学习脚手架顶栏副文案 */
export const UNDERSTANDING_SCAFFOLD_PANEL_HINT =
  '点击脚手架，快速把问题发到左侧。这一阶段重点不是记定义，而是看懂 DFS / BFS 分别是怎么走的。'

export const DFS_BFS_SCAFFOLD_BUTTONS: ScaffoldButton[] = [
  // ── 直观理解 ──
  {
    id: 'u-intuition-dfs-like',
    group: '直观理解',
    title: 'DFS 像什么？',
    injectPrompt:
      '用生活里好懂的类比帮我形容 DFS（比如走迷宫、沿一条路探到底之类），越口语越好。不要先甩教科书定义。',
  },
  {
    id: 'u-intuition-bfs-like',
    group: '直观理解',
    title: 'BFS 像什么？',
    injectPrompt:
      '用生活里好懂的类比帮我形容 BFS（比如水波一圈圈漾开、按距离一圈圈找），越口语越好。不要先甩教科书定义。',
  },
  {
    id: 'u-intuition-walk-dfs',
    group: '直观理解',
    title: '画个小图带我走一遍 DFS',
    injectPrompt:
      '画一个特别小的图或树（大概 4～6 个点就行），从起点开始标出 DFS 的访问顺序（1、2、3…）。每一步说清楚「为什么下一步轮到它」，我跟着你走一遍。',
  },
  {
    id: 'u-intuition-walk-bfs',
    group: '直观理解',
    title: '画个小图带我走一遍 BFS',
    injectPrompt:
      '画一个特别小的图或树（大概 4～6 个点就行），从起点开始标出 BFS 的访问顺序（1、2、3…）。每一步说清楚「为什么下一步轮到它」，我跟着你走一遍。',
  },
  // ── 顺序机制 ──
  {
    id: 'u-order-dfs-down',
    group: '顺序机制',
    title: 'DFS 为什么会一路往下走？',
    injectPrompt:
      '用栈或递归的思路讲清楚：DFS 为啥总是先往深处钻？请点到「后进先出」或「先处理刚到的分支」这种直觉，别写成定理证明。',
  },
  {
    id: 'u-order-bfs-layers',
    group: '顺序机制',
    title: 'BFS 为什么是一层一层？',
    injectPrompt:
      '用队列讲清楚：BFS 为啥看起来像「先近后远」、一层一层往外铺？请点到「先进先出」和队列里点的处理顺序，口语讲就行。',
  },
  {
    id: 'u-order-dfs-back',
    group: '顺序机制',
    title: 'DFS 为什么会回头？',
    injectPrompt:
      '结合栈/递归说明：什么时候这条路走不下去了、系统是怎么「退回去」试别的岔路的？用一两句人话，别用「回溯」空喊。',
  },
  {
    id: 'u-order-bfs-no-deep-first',
    group: '顺序机制',
    title: 'BFS 为什么不会先跑深处？',
    injectPrompt:
      '对比队列的弹出顺序：为啥 BFS 不会先冲到很远很深的点？用「队列里谁先被处理」说清楚，别和 DFS 混着讲结论。',
  },
  // ── 做题判断 ──
  {
    id: 'u-prob-when-dfs',
    group: '做题判断',
    title: '什么题先想 DFS？',
    injectPrompt:
      '做题时哪些关键词或问法会让我先想到 DFS？举几类常见题型（比如枚举路径、回溯、走到底再换支路），用口语列出来，别写成考点清单腔。',
  },
  {
    id: 'u-prob-when-bfs',
    group: '做题判断',
    title: '什么题先想 BFS？',
    injectPrompt:
      '做题时哪些关键词或问法会让我先想到 BFS？举几类常见题型（比如最少步数、按层扩散），用口语列出来。',
  },
  {
    id: 'u-prob-shortest-bfs',
    group: '做题判断',
    title: '最短路为什么先想 BFS？',
    injectPrompt:
      '在边权都相等（或步数一层算 1）的图里，为啥找「最少步数/最短层数」常常先想到 BFS？用人话讲直觉，可以带一句和 DFS 的对比。',
  },
  {
    id: 'u-prob-quick-tell',
    group: '做题判断',
    title: '我做题时怎么快速区分？',
    injectPrompt:
      '给我一套很短、能照着用的「快速区分」小抄：比如先看题目问的是啥、再想用队列还是栈/递归，控制在几条 bullet，像给自己作弊条一样。',
  },
]

// ─── UNDERSTANDING 系统开场消息 ───

export const UNDERSTANDING_SYSTEM_OPENER =
  '你已经能分清 DFS 和 BFS 的大致感觉了。接下来我们一起把它「怎么走」看明白：可以随便问，也可以点右侧学习脚手架，让问题先出现在左边输入框里。'

export const UNDERSTANDING_CLOSURE_HINT =
  '你已经把关键机制问到了。下面试着进入表达训练，用你自己的话讲一次。'

export const UNDERSTANDING_MAX_TURNS = 6

// ─── TRAINING 文案 ───

export const TRAINING_TASK_TITLE = '用你自己的话讲清楚 DFS / BFS'

export const TRAINING_TASK_SUBTITLE =
  '不用追求术语标准，先把你真正理解到的内容说出来。系统会帮你补齐漏洞。'

export const TRAINING_REQUIREMENTS = [
  '先说 DFS 是怎么搜索的',
  '再说 BFS 是怎么搜索的',
  '最后说二者最重要的区别',
]

export const TRAINING_SYSTEM_OPENER =
  '现在轮到你来讲。试着不用背定义，而是像在给同学解释一样，说清楚 DFS 和 BFS。'

export const TRAINING_MAX_ROUNDS = 3

// ─── REFLECTION 配置 ───

export const DFS_BFS_CONFUSION_POINTS = [
  '只会背现象，不会讲原因',
  '容易把"回退"当成额外动作，而不是搜索自然结果',
  '容易把 BFS 理解成"同时往外扩散"，但说不清为什么会分层',
]

export const DFS_BFS_REFLECTION_STRATEGIES: ReflectionStrategy[] = [
  { id: 'rs1', label: '看到"最近/最短层数"先想到 BFS' },
  { id: 'rs2', label: '看到"先把一条路探到底"先想到 DFS' },
  { id: 'rs3', label: '先分清"现象"和"机制"再解释' },
]

export const REFLECTION_INPUT_PLACEHOLDER =
  '用一句话写下：下次你怎么更快区分 DFS 和 BFS？'

export const REFLECTION_CLOSURE_TEXT =
  '很好，这次你不仅分清了 DFS / BFS，还提炼出了一个可复用的判断方式。可以进入学习报告。'
