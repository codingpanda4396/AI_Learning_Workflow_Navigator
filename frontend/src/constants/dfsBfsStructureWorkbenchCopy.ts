/** 旧版大文本工作台组件仍引用这些类型 */
export interface StructureWorkbenchMainCopy {
  phaseTag: string
  mainTitle: string
  roundTask: string
  currentQuestion: string
  requirementChips: string[]
  inputLabel: string
  placeholder: string
  exampleHint: string
  primaryAction: string
  secondaryAction: string
}

export interface StructureAnswerHintsCopy {
  title: string
  howToTitle: string
  howToItems: string[]
  avoidTitle: string
  avoidItems: string[]
  passTitle: string
  passItems: string[]
}

/** 与后端 DfsBfsStructureScaffoldDefinition actionId 一致 */
export const DFS_BFS_ACTION = {
  POSITION: 'dfs_bfs_structure_position',
  PREREQ: 'dfs_bfs_structure_prereq',
  NEXT: 'dfs_bfs_structure_next',
  DEFER: 'dfs_bfs_structure_defer',
} as const

export const STRUCTURE_PHASE_COPY = {
  mainTitle: '先搭知识骨架',
  subtitle:
    '这一阶段先看位置、关系和边界，不要求你先会解释',
  eyebrow: '结构建立',
  scaffoldTitle: '脚手架动作',
  scaffoldHint: '先选一张卡，系统会生成这一块的知识骨架。',
  emptySkeleton: '点击左侧任一卡片，系统会先为你搭出这一块的知识骨架。',
  loadingSkeleton: '正在生成骨架…',
  skeletonLabels: {
    module: '所属模块',
    prerequisites: '前置概念',
    connections: '后续连接',
    defer: '暂不展开',
  },
  learnCta: '点击学习',
  feedback: {
    gotNext: '我看懂了，下一张',
    clarify: '这里有点模糊，再解释一次',
    adjacent: '我想看它和相邻概念的关系',
  },
  optionalLineLabel: '用一句话写下你现在对它的位置感受（可选）',
  optionalPlaceholder: '可选，不写也能继续',
  primaryComplete: '进入机制理解',
} as const

export interface StructureScaffoldCardVm {
  promptKey: string
  title: string
  blurb: string
}

export const DFS_BFS_SCAFFOLD_CARDS: StructureScaffoldCardVm[] = [
  {
    promptKey: DFS_BFS_ACTION.POSITION,
    title: 'DFS / BFS 在知识体系中的位置',
    blurb: '看清它站在知识地图的哪一层、和谁在一条线上。',
  },
  {
    promptKey: DFS_BFS_ACTION.PREREQ,
    title: '学它之前要先知道什么',
    blurb: '只列最小前置，避免跳步。',
  },
  {
    promptKey: DFS_BFS_ACTION.NEXT,
    title: '学完它会连接到哪些主题',
    blurb: '知道学完后自然往哪走，先粗后细。',
  },
  {
    promptKey: DFS_BFS_ACTION.DEFER,
    title: '这一轮先不要进入哪些细节',
    blurb: '把边界划清，避免被实现细节淹没。',
  },
]
