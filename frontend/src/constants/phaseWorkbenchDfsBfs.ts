import type {
  ReflectionQuestion,
  ReflectionStrategy,
  StageIntroCopy,
  StructureQuestion,
  UnderstandingQuestion,
  WorkbenchScaffoldAction,
} from '@/types/phaseWorkbench'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

export const DFS_BFS_PHASE_INTRO: Record<WorkbenchPhaseCode, StageIntroCopy> = {
  STRUCTURE: {
    title: '建立基本轮廓',
    subtitle: '先分清 DFS 和 BFS 分别是什么，不用担心答错。',
  },
  UNDERSTANDING: {
    title: '理解背后的机制',
    subtitle: '这一步要弄清楚，为什么 DFS 会回溯，为什么 BFS 会分层推进。',
  },
  TRAINING: {
    title: '用你自己的话讲清楚',
    subtitle: '试着自己解释一次，系统会帮你发现表达缺口。',
  },
  REFLECTION: {
    title: '把这次学习收住',
    subtitle: '最后看一眼，你刚才最容易混淆什么，下次怎么更快判断。',
  },
}

export const DFS_BFS_DEFAULT_EXPLANATION: Record<WorkbenchPhaseCode, { title: string; body: string }> = {
  STRUCTURE: {
    title: '认知起点',
    body: 'DFS 更像沿一条路先走到底再回退；BFS 更像一层一层向外扩展。本阶段只先分清轮廓差异。',
  },
  UNDERSTANDING: {
    title: '因果起点',
    body: 'DFS 回溯来自“当前路径走不通”；BFS 分层推进来自“队列按层展开”。先把现象和机制连起来。',
  },
  TRAINING: {
    title: '表达提示',
    body: '好的表达需要同时说清“是什么”和“为什么”，不求标准答案，但要让别人听懂。',
  },
  REFLECTION: {
    title: '收口提示',
    body: '反思阶段不引入新知识，只提炼最易混淆点和下一次可执行的判断策略。',
  },
}

export const DFS_BFS_STRUCTURE_QUESTION: StructureQuestion = {
  id: 's1',
  prompt: '下面哪种描述更接近 BFS？',
  options: [
    { id: 'A', label: '沿一条路径尽量深入，再回退继续探索。' },
    { id: 'B', label: '从起点开始，按层逐步向外扩展。' },
    { id: 'C', label: '随机选择邻居，直到遍历完成。' },
  ],
}

export const DFS_BFS_UNDERSTANDING_QUESTIONS: UnderstandingQuestion[] = [
  {
    id: 'u1',
    prompt: '为什么 BFS 更容易按层扩展？',
    expectedId: 'B',
    options: [
      { id: 'A', label: '因为 BFS 总是更快' },
      { id: 'B', label: '因为队列天然保持先入先出，能按层推进' },
      { id: 'C', label: '因为 BFS 会优先回溯' },
    ],
    feedbackByOption: {
      A: { tag: '速度误解', body: '重点不是更快，而是推进机制支持按层访问。' },
      B: { tag: '机制命中', body: '对，队列让节点按进入顺序扩展，天然形成层次。' },
      C: { tag: '术语混淆', body: '回溯更接近 DFS 的现象，不是 BFS 的核心机制。' },
    },
  },
  {
    id: 'u2',
    prompt: '为什么 DFS 会出现回溯？',
    expectedId: 'A',
    options: [
      { id: 'A', label: '当前路径无法继续时，需要返回上一个分叉点' },
      { id: 'B', label: '队列中下一层节点优先被处理' },
      { id: 'C', label: '因为 DFS 只能访问同一层节点' },
    ],
    feedbackByOption: {
      A: { tag: '机制命中', body: '对，回溯来自“深度优先路径走不通后返回”。' },
      B: { tag: '结构混淆', body: '这是 BFS 的层序特征，不是 DFS 回溯机制。' },
      C: { tag: '现象误判', body: 'DFS 恰恰不是同层优先，而是优先深入。' },
    },
  },
]

export const DFS_BFS_REFLECTION_QUESTION: ReflectionQuestion = {
  id: 'r1',
  prompt: '你刚才最容易混淆的是哪一点？',
  options: [
    { id: 'A', label: '概念轮廓没分清' },
    { id: 'B', label: '现象知道但原因不清楚' },
    { id: 'C', label: '自己会想但讲不出来' },
  ],
}

export const DFS_BFS_REFLECTION_STRATEGIES: ReflectionStrategy[] = [
  { id: 's1', label: '先看是按层还是向深处推进' },
  { id: 's2', label: '先看更适合队列还是递归/栈' },
  { id: 's3', label: '先画一个小图再决定' },
]

export const DFS_BFS_SCAFFOLD_ACTIONS: Record<WorkbenchPhaseCode, WorkbenchScaffoldAction[]> = {
  STRUCTURE: [
    { id: 'vs-compare', title: '看一个直观对比', prompt: '用一句话对比 DFS 和 BFS 的推进轮廓。' },
    { id: 'vs-diagram', title: '看一个图式解释', prompt: '给一个层级图示意 DFS 与 BFS 的访问顺序。' },
    { id: 'vs-misread', title: '看一个常见误解', prompt: '指出初学者最容易把 DFS/BFS 混淆在哪。' },
    { id: 'vs-simple', title: '换一个更简单的说法', prompt: '用不超过 20 字各解释一次 DFS 和 BFS。' },
  ],
  UNDERSTANDING: [
    { id: 'u-cause', title: '看因果链条', prompt: '把 DFS 回溯和 BFS 分层推进的因果链条写成三步。' },
    { id: 'u-stack-queue', title: '看栈/队列对比', prompt: '解释栈/递归和队列如何影响遍历顺序。' },
    { id: 'u-backtrack', title: '看回溯是怎么出现的', prompt: '举一个 DFS 回溯出现的最小场景。' },
    { id: 'u-layer', title: '看分层推进过程', prompt: '用层次编号说明 BFS 如何逐层访问。' },
  ],
  TRAINING: [
    { id: 't-frame', title: '给我一个表达框架', prompt: '给一个 3 句式表达框架：是什么、为什么、何时用。' },
    { id: 't-template', title: '给我一个对比模板', prompt: '给一个 DFS/BFS 对比模板，可直接套用。' },
    { id: 't-opening', title: '给我一个更清晰的开头', prompt: '给一个开头句，帮助我先讲清核心差异。' },
    { id: 't-exam', title: '给我一个更像考试答案的说法', prompt: '输出一个考试型精简表达。' },
  ],
  REFLECTION: [
    { id: 'r-mixup', title: '看常见混淆点', prompt: '总结这轮最容易混淆的三个点。' },
    { id: 'r-fast-check', title: '看快速判断法', prompt: '给一个 10 秒内判断 DFS/BFS 的检查清单。' },
    { id: 'r-transfer', title: '看迁移策略样例', prompt: '给一个可迁移到新题目的判断策略模板。' },
    { id: 'r-recap', title: '看最短复盘', prompt: '把本轮学习收束成三句复盘。' },
  ],
}

export const DFS_BFS_NEXT_ACTION_LABEL: Record<WorkbenchPhaseCode, string> = {
  STRUCTURE: '进入机制理解',
  UNDERSTANDING: '进入表达训练',
  TRAINING: '进入反思收口',
  REFLECTION: '查看学习报告',
}
