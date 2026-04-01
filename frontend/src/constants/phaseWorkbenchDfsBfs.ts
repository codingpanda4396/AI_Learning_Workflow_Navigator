import type {
  ReflectionQuestion,
  ReflectionStrategy,
  UnderstandingQuestion,
} from '@/types/phaseWorkbench'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

export const DFS_BFS_PHASE_INTRO: Record<WorkbenchPhaseCode, string> = {
  STRUCTURE: '先分清 DFS / BFS 的搜索轮廓。',
  UNDERSTANDING: '现在回答为什么会这样推进。',
  TRAINING: '用你自己的话讲清楚，不背模板。',
  REFLECTION: '收束误区，提炼可迁移策略。',
}

export const DFS_BFS_UNDERSTANDING_QUESTIONS: UnderstandingQuestion[] = [
  {
    id: 'u1',
    prompt: 'DFS 为什么会回溯？',
    expectedId: 'B',
    options: [
      { id: 'A', label: '因为队列为空' },
      { id: 'B', label: '因为当前路径走不下去' },
      { id: 'C', label: '因为优先访问浅层节点' },
    ],
    feedbackByOption: {
      A: { tag: 'BFS_STRUCTURE_CONFUSION', body: 'DFS 回溯的原因是当前路径无法继续，不是队列。' },
      B: { tag: 'OK', body: '对，路径走不通就返回上一个分叉点继续。' },
      C: { tag: 'SHALLOW_FIRST_CONFUSION', body: 'DFS 是先深入，不是先浅层。' },
    },
  },
  {
    id: 'u2',
    prompt: '为什么递归常用于 DFS？',
    expectedId: 'B',
    options: [
      { id: 'A', label: '因为递归一定更快' },
      { id: 'B', label: '调用栈匹配进入-返回过程' },
      { id: 'C', label: '因为代码更短' },
    ],
    feedbackByOption: {
      A: { tag: 'PERFORMANCE_MISUNDERSTANDING', body: '关键不是速度，而是结构匹配。' },
      B: { tag: 'OK', body: '对，调用栈天然保存“从哪里返回”。' },
      C: { tag: 'SURFACE_ADVANTAGE_CONFUSION', body: '短只是表象，本质是机制匹配。' },
    },
  },
]

export const DFS_BFS_REFLECTION_QUESTION: ReflectionQuestion = {
  id: 'r1',
  prompt: '你最容易混淆的点是？',
  options: [
    { id: 'A', label: 'DFS / BFS 策略区别' },
    { id: 'B', label: 'DFS 回溯原因' },
    { id: 'C', label: '递归和 DFS 关系' },
  ],
}

export const DFS_BFS_REFLECTION_STRATEGIES: ReflectionStrategy[] = [
  { id: 's1', label: '先问它解决什么问题' },
  { id: 's2', label: '再问它如何推进搜索' },
  { id: 's3', label: '最后问它为什么这么推进' },
]
