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

// ─── UNDERSTANDING 脚手架按钮 ───

export const DFS_BFS_SCAFFOLD_BUTTONS: ScaffoldButton[] = [
  {
    id: 'u-why-backtrack',
    group: '解释机制',
    title: '为什么 DFS 会回退？',
    injectPrompt:
      '请不要直接给结论，而是一步一步解释：为什么 DFS 会沿一条路径不断深入，并且在走不下去时发生回退？请结合遍历过程来讲。',
  },
  {
    id: 'u-why-layer',
    group: '解释机制',
    title: '为什么 BFS 会分层？',
    injectPrompt:
      '请一步一步解释：为什么 BFS 会呈现出"从起点开始一层一层向外扩展"的效果？请重点讲"为什么会这样"，不要只给定义。',
  },
  {
    id: 'u-root-diff',
    group: '对比理解',
    title: 'DFS 和 BFS 的根本差别是什么？',
    injectPrompt:
      '请不要只说"一个深度一个广度"。请从搜索推进方式、访问顺序感受、适合解决的问题三个角度，对比解释 DFS 和 BFS 的根本差别。',
  },
  {
    id: 'u-when-matters',
    group: '对比理解',
    title: '什么时候这种差异会很重要？',
    injectPrompt:
      '请结合具体场景说明：DFS 和 BFS 的差异在什么情况下会真正影响结果或效率？请举一个能体现差异的例子。',
  },
  {
    id: 'u-small-diagram',
    group: '辅助理解',
    title: '用一个小图例解释',
    injectPrompt:
      '请用一个非常小的图或树的例子，分别演示 DFS 和 BFS 的访问顺序，并解释为什么会出现这种顺序。',
  },
  {
    id: 'u-check-gap',
    group: '辅助理解',
    title: '检查我哪里没懂',
    injectPrompt:
      '如果一个人知道"DFS 往深处走，BFS 按层扩展"，但还是说不清"为什么"，这通常说明他缺了哪一层理解？请帮我定位。',
  },
]

// ─── UNDERSTANDING 系统开场消息 ───

export const UNDERSTANDING_SYSTEM_OPENER =
  '你已经能区分 DFS 和 BFS 的表面差异了。现在我们来弄清楚：为什么 DFS 会先深入，为什么 BFS 会分层推进？你可以直接提问，也可以点右侧脚手架。'

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
