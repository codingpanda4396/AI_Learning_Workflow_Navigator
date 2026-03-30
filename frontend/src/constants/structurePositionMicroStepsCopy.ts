/**
 * DFS/BFS · STRUCTURE · 位置卡（dfs_bfs_structure_position）
 * 渐进式轻判断：归类 → 作用 → 一句话确认
 */
export type StructureMicroStep = 'classify' | 'function' | 'confirm'

export const STRUCTURE_MICRO_HEADER = {
  phaseLine: '结构建立',
  heroLine: '先确认它属于哪一类',
  actionLine: '先别讲原理，只把 DFS / BFS 放回正确的位置。',
} as const

export const STRUCTURE_MAIN_TASK_CARD = {
  eyebrow: '结构建立',
  title: '当前任务',
  body: '先判断 DFS / BFS 属于哪一类方法',
  helper: '这一步只做分类，不展开原理，不讨论实现。',
  passLine: '做到这里就算通过：能明确说出它属于「图 / 树的搜索或遍历方法」即可。',
} as const

export const STRUCTURE_CLASSIFY = {
  title: 'DFS / BFS 更接近下面哪一类？',
  options: [
    { id: 'graph_search', label: '图 / 树中的搜索与遍历方法' },
    { id: 'sort', label: '排序算法' },
    { id: 'shortest', label: '最短路算法' },
    { id: 'struct_def', label: '数据结构定义' },
  ],
} as const

export const STRUCTURE_FUNCTION = {
  title: '它主要在解决什么问题？',
  options: [
    { id: 'visit_rule', label: '按某种规则访问节点' },
    { id: 'order', label: '维护有序性' },
    { id: 'shortest_dist', label: '查找最短距离' },
    { id: 'node_store', label: '定义节点存储结构' },
  ],
} as const

export const STRUCTURE_CONFIRM = {
  title: '最后，用一句话说出来',
  placeholder:
    '例如：DFS 和 BFS 都属于图或树中的搜索 / 遍历方法，用来按规则访问节点。',
  submit: '提交这一轮判断',
} as const

export const STRUCTURE_MICRO_CTA = {
  continue: '继续',
} as const

/** 折叠说明：结构建立 · 位置卡（口语化） */
export const STRUCTURE_POSITION_COLLAPSE = {
  title: '查看这一步说明',
  whyTitle: '为什么先做这一步',
  whyBody: '先知道它是什么，再去理解它怎么工作。',
  avoidTitle: '这一轮先不做什么',
  avoidBody: '先不写代码，不比复杂度，也不展开最短路。',
  outcomeTitle: '这一轮结束后你会得到什么',
  outcomeBody: '你会知道 DFS / BFS 在整个数据结构知识里的位置，不再把它当成零散技巧。',
} as const
