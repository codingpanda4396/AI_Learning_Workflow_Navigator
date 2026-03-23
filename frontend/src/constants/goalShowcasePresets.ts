/**
 * 首页「快速开始」预设：填入的文案需与后端 PlanningContextAssembler
 * 及 showcaseKnowledgeConfig 的匹配规则一致，规划页才能进入对应演示视图。
 */
export type GoalShowcasePresetId = 'array_linked_list' | 'binary_tree' | 'dfs_bfs'

export type GoalShowcasePresetPatch = {
  rawGoalText: string
  topicHints: string[]
  subjectHint: string
}

export type GoalShowcasePreset = {
  id: GoalShowcasePresetId
  title: string
  subtitle: string
  apply: () => GoalShowcasePresetPatch
}

export const GOAL_SHOWCASE_PRESETS: GoalShowcasePreset[] = [
  {
    id: 'array_linked_list',
    title: '顺序表与链表',
    subtitle: '对比差异与选用场景',
    apply: () => ({
      rawGoalText:
        '我想搞清楚顺序表和链表到底有什么区别，以及做题、写代码时什么时候更该想到用哪个。',
      topicHints: ['链表', '顺序表'],
      subjectHint: '数据结构',
    }),
  },
  {
    id: 'binary_tree',
    title: '二叉树基础',
    subtitle: '先在脑子里画出样子',
    apply: () => ({
      rawGoalText:
        '我想先把二叉树的基本结构搞明白：根、孩子、叶子分别是什么，能在脑子里有画面。',
      topicHints: ['二叉树'],
      subjectHint: '数据结构',
    }),
  },
  {
    id: 'dfs_bfs',
    title: 'DFS 与 BFS',
    subtitle: '分清搜索方式与适用题',
    apply: () => ({
      rawGoalText:
        '我想分清深度优先（DFS）和广度优先（BFS）分别适合什么样的题，看见题干能更快判断方向。',
      topicHints: ['DFS', 'BFS'],
      subjectHint: '数据结构',
    }),
  },
]

/** 选中预设后在目标页展示的口语反馈（不暴露开发字段） */
export const GOAL_PRESET_FEEDBACK: Record<GoalShowcasePresetId, string> = {
  array_linked_list:
    '你选了「顺序表与链表」，我们会先帮你把差异和「什么时候用哪个」说清楚。',
  binary_tree:
    '你选了「二叉树基础」，我们会先帮你在脑子里把样子画清楚，再往下走。',
  dfs_bfs:
    '你选了「DFS 与 BFS」，我们会先帮你分清两种走法各适合什么样的题。',
}

export function findGoalShowcasePreset(
  id: string | null | undefined
): GoalShowcasePreset | undefined {
  if (!id) return undefined
  return GOAL_SHOWCASE_PRESETS.find((p) => p.id === id)
}
