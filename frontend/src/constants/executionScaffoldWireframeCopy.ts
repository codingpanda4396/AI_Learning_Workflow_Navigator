import { DFS_BFS_ACTION } from '@/constants/dfsBfsStructureWorkbenchCopy'
import type { LearningActionCard } from '@/types/scaffoldEngine'

/** 线框稿：页面标题区（ExecutionHeader 下方） */
export const SCAFFOLD_WIREFRAME_PAGE = {
  /** 第二行主文案（结构阶段默认） */
  structureHeroLine: '先确认它属于哪一类',
  /** 第三行：行动导向一句（DFS/BFS 包 · 结构阶段） */
  structureActionLine: '先别讲原理，只把 DFS / BFS 放回正确的位置。',
  mainCardEyebrowFallback: '当前阶段',
  mainCardTitle: '当前任务',
  passLabel: '通过标准：',
  inputPlaceholder:
    '先写完整一句，不要只写关键词\n例如：DFS 和 BFS 都属于图或树的搜索 / 遍历方法',
  submitPrimary: '提交我的表达',
  submitSecondary: '不会写，给我一个开头',
  collapseTitle: '查看这一步说明',
  whySectionTitle: '为什么先做这一步',
  avoidSectionTitle: '本阶段不要做什么',
  progressSectionTitle: '当前进度',
  phaseInfoSectionTitle: '阶段说明',
  feedbackPass: '通过',
  feedbackFail: '还差一点',
  nextStepCta: '进入下一步',
  fallbackWhyStructure:
    '没有位置感，后面的机制理解和表达训练会飘。先知道 DFS / BFS 属于什么，再谈它们怎么工作。',
} as const

export type ScaffoldHintId = 'template' | 'offtrack' | 'example' | 'alt'

export interface ScaffoldHintItem {
  id: ScaffoldHintId
  label: string
  body: string
}

/** DFS/BFS · 位置卡：与线框稿一致的脚手架短内容 */
const DFS_BFS_POSITION_HINTS: ScaffoldHintItem[] = [
  {
    id: 'template',
    label: '给我一个开头模板',
    body: `你可以这样开头：

DFS 和 BFS 本质上都属于 ________ ，它们解决的是 ________ 。`,
  },
  {
    id: 'offtrack',
    label: '提醒我别跑偏',
    body: `这一轮不要写：
- 代码
- 复杂度
- 为什么 BFS 最短路
- 实现细节

这一轮只说「它属于哪一类」`,
  },
  {
    id: 'example',
    label: '看一个合格示例',
    body: `合格示例：

DFS 和 BFS 都属于图或树中的搜索 / 遍历方法，用来按一定规则访问节点。`,
  },
  {
    id: 'alt',
    label: '换一种提示方式',
    body: `换个想法：

如果把数据结构知识画成地图，
DFS / BFS 更接近「排序」还是「搜索」？
更接近「结构定义」还是「访问节点的方法」？`,
  },
]

const DFS_BFS_POSITION_STARTER =
  'DFS 和 BFS 本质上都属于图或树的搜索 / 遍历方法，它们解决的是如何按规则访问节点。'

export function getScaffoldWireframeHints(
  packId: string | null | undefined,
  actionId: string | undefined,
  card: LearningActionCard | null
): ScaffoldHintItem[] {
  if (packId === 'ds_dfs_bfs' && actionId === DFS_BFS_ACTION.POSITION) {
    return DFS_BFS_POSITION_HINTS
  }
  const templateBody = card?.exampleOutput?.trim()
    ? `你可以从这里改写：\n\n${card.exampleOutput.trim()}`
    : '先写一句「它是什么、解决什么问题」，再提交。'
  const offtrackBody =
    card?.forbiddenActions?.length || card?.forbiddenPrompts?.length
      ? `这一轮不要写：\n${[...(card.forbiddenActions ?? []), ...(card.forbiddenPrompts ?? [])]
          .filter(Boolean)
          .slice(0, 6)
          .map((l) => `- ${l}`)
          .join('\n')}`
      : '先完成当前卡片的唯一动作，不要展开无关细节。'
  const exampleBody = card?.exampleOutput?.trim()
    ? `合格示例：\n\n${card.exampleOutput.trim()}`
    : '用一句话点出知识点位置即可，不必写长。'
  const altBody =
    card?.nextActionHint?.trim() ||
    '只回答：它在知识地图里站在哪一类（例如搜索、遍历、图论基础）。'
  return [
    { id: 'template', label: '给我一个开头模板', body: templateBody },
    { id: 'offtrack', label: '提醒我别跑偏', body: offtrackBody },
    { id: 'example', label: '看一个合格示例', body: exampleBody },
    { id: 'alt', label: '换一种提示方式', body: altBody },
  ]
}

export function getStarterInsertText(
  packId: string | null | undefined,
  actionId: string | undefined,
  card: LearningActionCard | null
): string {
  if (packId === 'ds_dfs_bfs' && actionId === DFS_BFS_ACTION.POSITION) {
    return DFS_BFS_POSITION_STARTER
  }
  return card?.exampleOutput?.trim() || '它属于 ________ ，用来处理 ________ 。'
}

export function wireframeHeroSecondLine(stageKey: string | undefined, stageTitle: string): string {
  if (stageKey === 'STRUCTURE') return SCAFFOLD_WIREFRAME_PAGE.structureHeroLine
  return stageTitle?.trim() || SCAFFOLD_WIREFRAME_PAGE.structureHeroLine
}

export function wireframeActionThirdLine(
  stageKey: string | undefined,
  packId: string | null | undefined,
  stageDescription: string
): string {
  if (stageKey === 'STRUCTURE' && packId === 'ds_dfs_bfs') {
    return SCAFFOLD_WIREFRAME_PAGE.structureActionLine
  }
  const one = stageDescription
    .split(/\n/)
    .map((s) => s.trim())
    .find(Boolean)
  return one || '按主卡要求写一句即可。'
}

export function mergePassCriteriaLines(card: LearningActionCard | null): string {
  if (!card) return ''
  const raw = card.completionCriteria?.length
    ? card.completionCriteria
    : card.passCriteria ?? []
  const lines = raw.map((s) => s.trim()).filter(Boolean)
  if (!lines.length) return ''
  return lines.join('；')
}

/** 反馈正文取短：首句或前 120 字 */
export function shortFeedbackSentence(text: string, maxLen = 120): string {
  const t = text.trim()
  if (!t) return ''
  const first = t.split(/(?<=[。！？\n])/)[0]?.trim() || t
  if (first.length <= maxLen) return first
  return `${first.slice(0, maxLen)}…`
}
