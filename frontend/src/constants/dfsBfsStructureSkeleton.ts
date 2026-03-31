/**
 * DFS/BFS · STRUCTURE 阶段：判断骨架维度与规则校验（前端确定性逻辑，不依赖 LLM）
 */

export type D1Id = 'graph' | 'state_space'
export type D2Id = 'path_one' | 'all_exhaust' | 'shortest'
export type D3Id = 'visit_once' | 'depth_cap' | 'multi_source' | 'directed' | 'none'
export type D4Id = 'path_out' | 'steps_layers' | 'reachability' | 'all_solutions'
export type D5Id = 'DFS' | 'BFS'

export interface DfsBfsStructureSelection {
  d1: D1Id | null
  d2: D2Id | null
  d3: D3Id[]
  d4: D4Id | null
  d5: D5Id | null
}

/** 例题展示（短句） */
export const DFS_BFS_EXAMPLE_TITLE = '从起点到终点的最少步数'

/** 兼容旧锚点文案（提交/日志等） */
export const DFS_BFS_STRUCTURE_TOPIC_ANCHOR =
  '从网格「1」走到「0」的最少步数：把可走格子看成邻接，问最少边数。'

/** 「为什么是 BFS？」一句提示（不展开长文） */
export const DFS_BFS_WHY_BFS_ONE_LINER = '最少步数题优先用 BFS：按层扩展、用队列推进。'

export const D1_OPTIONS: { id: D1Id; label: string }[] = [
  { id: 'graph', label: '图上的路径 / 连通' },
  { id: 'state_space', label: '状态空间里的转移' },
]

export const D2_OPTIONS: { id: D2Id; label: string }[] = [
  { id: 'path_one', label: '找一条路径' },
  { id: 'all_exhaust', label: '试所有可能 / 穷尽分支' },
  { id: 'shortest', label: '最短步数 / 最少边' },
]

export const D3_OPTIONS: { id: D3Id; label: string }[] = [
  { id: 'visit_once', label: '避免重复访问' },
  { id: 'depth_cap', label: '深度 / 层数上限' },
  { id: 'multi_source', label: '多起点' },
  { id: 'directed', label: '有向 / 方向敏感' },
  { id: 'none', label: '无额外约束' },
]

export const D4_OPTIONS: { id: D4Id; label: string }[] = [
  { id: 'path_out', label: '具体路径' },
  { id: 'steps_layers', label: '步数或层数' },
  { id: 'reachability', label: '是否可达' },
  { id: 'all_solutions', label: '所有解' },
]

export const D5_OPTIONS: { id: D5Id; label: string }[] = [
  { id: 'DFS', label: 'DFS' },
  { id: 'BFS', label: 'BFS' },
]

// --- 单任务骨架工作台 UI（三槽 + 方向 + 分步）---

export type DfsBfsDirection = 'DFS' | 'BFS'

/** 流程：先选方向 → 错选 DFS 时纠偏 → BFS 就绪可提交 */
export type DfsBfsStructureFlowStep = 'direction' | 'dfs_correction' | 'bfs_ready'

/**
 * 六个词块：题目信号 ×2、推进 ×2、遍历方式 ×2（互不重复）
 */
export type DfsBfsBlockId =
  | 'shortest_steps'
  | 'depth_try'
  | 'queue'
  | 'rec_stack'
  | 'one_deep'
  | 'layer_wide'

export type DfsBfsSkeletonSlotKey = 'signal' | 'advance' | 'traverse'

export interface DfsBfsCorrectionPicks {
  signal: DfsBfsBlockId | null
  advance: DfsBfsBlockId | null
  traverse: DfsBfsBlockId | null
}

export interface DfsBfsStructureWorkbenchUi {
  direction: DfsBfsDirection | null
  flowStep: DfsBfsStructureFlowStep
  /** DFS 纠偏步：与 BFS 对齐的三组二选一 */
  correctionPicks: DfsBfsCorrectionPicks
  slots: Record<DfsBfsSkeletonSlotKey, DfsBfsBlockId | null>
}

export const DFS_BFS_BLOCK_LABELS: Record<DfsBfsBlockId, string> = {
  shortest_steps: '最短步数',
  depth_try: '深度尝试',
  queue: '队列',
  rec_stack: '递归/栈',
  one_deep: '一条路先走深',
  layer_wide: '一层一层扩展',
}

export const DFS_BFS_SLOT_BLOCKS: Record<DfsBfsSkeletonSlotKey, DfsBfsBlockId[]> = {
  signal: ['depth_try', 'shortest_steps'],
  advance: ['rec_stack', 'queue'],
  traverse: ['one_deep', 'layer_wide'],
}

/** BFS 与「最少步数」例题对齐的槽位（隐式 visited 在 deriveLegacy 中补） */
export function bfsAlignedSlots(): Record<DfsBfsSkeletonSlotKey, DfsBfsBlockId> {
  return {
    signal: 'shortest_steps',
    advance: 'queue',
    traverse: 'layer_wide',
  }
}

export function createEmptyDfsBfsWorkbenchUi(): DfsBfsStructureWorkbenchUi {
  return {
    direction: null,
    flowStep: 'direction',
    correctionPicks: { signal: null, advance: null, traverse: null },
    slots: { signal: null, advance: null, traverse: null },
  }
}

/** 纠偏步：三组均为 BFS 侧答案时可改选 BFS */
export function isDfsCorrectionReadyToSwitch(ui: DfsBfsStructureWorkbenchUi): boolean {
  const { signal, advance, traverse } = ui.correctionPicks
  return signal === 'shortest_steps' && advance === 'queue' && traverse === 'layer_wide'
}

/** 从 DFS 纠偏步切到 BFS 就绪（预填槽位） */
export function applyBfsSwitchFromCorrection(ui: DfsBfsStructureWorkbenchUi): DfsBfsStructureWorkbenchUi {
  const slots = bfsAlignedSlots()
  return {
    ...ui,
    direction: 'BFS',
    flowStep: 'bfs_ready',
    correctionPicks: { signal: null, advance: null, traverse: null },
    slots: { ...slots },
  }
}

/** 回到仅选方向 */
export function resetToDirectionOnly(): DfsBfsStructureWorkbenchUi {
  return createEmptyDfsBfsWorkbenchUi()
}

/** 用户直接选 BFS：进入就绪态并预填 */
export function enterBfsReadyFromDirectionPick(): DfsBfsStructureWorkbenchUi {
  return {
    direction: 'BFS',
    flowStep: 'bfs_ready',
    correctionPicks: { signal: null, advance: null, traverse: null },
    slots: { ...bfsAlignedSlots() },
  }
}

/** 用户选 DFS：进入纠偏步 */
export function enterDfsCorrectionFromDirectionPick(): DfsBfsStructureWorkbenchUi {
  return {
    direction: 'DFS',
    flowStep: 'dfs_correction',
    correctionPicks: { signal: null, advance: null, traverse: null },
    slots: { signal: null, advance: null, traverse: null },
  }
}

export function isDfsBfsWorkbenchUiFilled(ui: DfsBfsStructureWorkbenchUi): boolean {
  if (ui.direction !== 'BFS' || ui.flowStep !== 'bfs_ready') return false
  return ui.slots.signal !== null && ui.slots.advance !== null && ui.slots.traverse !== null
}

/** 由工作台 UI 推导旧版五维选择（供提交与 inferExpectedD5） */
export function deriveLegacySelection(ui: DfsBfsStructureWorkbenchUi): DfsBfsStructureSelection {
  const { direction, slots } = ui
  const signal = slots.signal
  const traverse = slots.traverse

  if (!direction || !signal || !slots.advance || !traverse) {
    return { d1: null, d2: null, d3: [], d4: null, d5: null }
  }

  const d1: D1Id = 'graph'
  const d5: D5Id = direction

  let d2: D2Id
  let d4: D4Id

  if (signal === 'shortest_steps') {
    d2 = 'shortest'
    d4 = 'steps_layers'
  } else {
    d2 = 'all_exhaust'
    d4 = 'all_solutions'
  }

  const d3: D3Id[] = ['visit_once']

  return { d1, d2, d3, d4, d5 }
}

export type DfsBfsWorkbenchHighlight =
  | 'direction'
  | 'signal'
  | 'advance'
  | 'traverse'
  | 'd5'
  | null

export interface ValidateWorkbenchUiResult {
  ok: boolean
  conflictField: 'd5' | null
  message: string
  highlight: DfsBfsWorkbenchHighlight
}

/** 工作台专用校验：短句 + 高亮槽位（仅 BFS 就绪可提交） */
export function validateDfsBfsWorkbenchUi(ui: DfsBfsStructureWorkbenchUi): ValidateWorkbenchUiResult {
  if (ui.flowStep !== 'bfs_ready' || ui.direction !== 'BFS') {
    return {
      ok: false,
      conflictField: null,
      message: '先完成方向与纠偏，再提交。',
      highlight: 'direction',
    }
  }
  if (!ui.slots.signal) {
    return { ok: false, conflictField: null, message: '题目信号还差一项。', highlight: 'signal' }
  }
  if (!ui.slots.advance) {
    return { ok: false, conflictField: null, message: '推进方式还差一项。', highlight: 'advance' }
  }
  if (!ui.slots.traverse) {
    return { ok: false, conflictField: null, message: '遍历方式还差一项。', highlight: 'traverse' }
  }

  const { direction, slots } = ui
  const { signal, advance } = slots

  if (direction === 'BFS' && signal === 'depth_try') {
    return {
      ok: false,
      conflictField: null,
      message: '题目信号用「最短步数」对齐这题。',
      highlight: 'signal',
    }
  }
  if (direction === 'BFS' && advance === 'rec_stack') {
    return {
      ok: false,
      conflictField: null,
      message: '推进方式选「队列」。',
      highlight: 'advance',
    }
  }
  if (direction === 'BFS' && slots.traverse === 'one_deep') {
    return {
      ok: false,
      conflictField: null,
      message: '遍历方式选「一层一层扩展」。',
      highlight: 'traverse',
    }
  }

  const sel = deriveLegacySelection(ui)
  const legacy = validateDfsBfsStructure(sel)
  if (!legacy.ok) {
    return {
      ok: false,
      conflictField: legacy.conflictField,
      message:
        legacy.conflictField === 'd5'
          ? '方向与判断不一致，请对齐三项。'
          : legacy.message,
      highlight: legacy.conflictField === 'd5' ? 'd5' : 'direction',
    }
  }

  return { ok: true, conflictField: null, message: '', highlight: null }
}

/** 由 D1–D4 推导期望遍历倾向（比赛演示用确定性规则） */
export function inferExpectedD5(sel: Pick<DfsBfsStructureSelection, 'd2' | 'd4'>): D5Id {
  const { d2, d4 } = sel
  if (d4 === 'all_solutions') return 'DFS'
  if (d2 === 'shortest') return 'BFS'
  if (d2 === 'all_exhaust') return 'DFS'
  if (d2 === 'path_one') return 'DFS'
  return 'DFS'
}

export function isSkeletonFilled(sel: DfsBfsStructureSelection): boolean {
  if (!sel.d1 || !sel.d2 || !sel.d4 || !sel.d5) return false
  if (!sel.d3.length) return false
  return true
}

export interface ValidateSkeletonResult {
  ok: boolean
  conflictField: 'd5' | null
  message: string
}

export function validateDfsBfsStructure(sel: DfsBfsStructureSelection): ValidateSkeletonResult {
  if (!isSkeletonFilled(sel)) {
    return { ok: false, conflictField: null, message: '先补全所有维度。' }
  }
  const expected = inferExpectedD5(sel)
  if (sel.d5 !== expected) {
    return {
      ok: false,
      conflictField: 'd5',
      message: '第五维与前几维不一致，改「遍历倾向」或回看「遍历目标」。',
    }
  }
  return { ok: true, conflictField: null, message: '' }
}

export function formatSkeletonForSubmit(sel: DfsBfsStructureSelection): string {
  const d3Labels = sel.d3
    .map((id) => D3_OPTIONS.find((o) => o.id === id)?.label || id)
    .join('、')
  const lines = [
    '【DFS/BFS 判断骨架】',
    `模型：${D1_OPTIONS.find((o) => o.id === sel.d1)?.label}`,
    `遍历目标：${D2_OPTIONS.find((o) => o.id === sel.d2)?.label}`,
    `约束：${d3Labels}`,
    `输出：${D4_OPTIONS.find((o) => o.id === sel.d4)?.label}`,
    `倾向：${sel.d5}`,
  ]
  return lines.join('\n')
}
