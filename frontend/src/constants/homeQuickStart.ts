import type { CreateGoalRequest } from '@/types/dto'
import { PreferenceTag } from '@/types/enums'
import type { PreferenceTagType } from '@/types/enums'
import type { KnowledgePackId } from '@/types/knowledgePack'

/**
 * 与 `KNOWLEDGE_PACKS` / 诊断演示包一致：仅这些主题具备完整学习编排，其余首页入口为占位展示。
 */
export const HOME_CONFIGURED_TOPIC_KEYS = new Set<KnowledgePackId>([
  'os_process_thread',
  'net_tcp_handshake',
  'ds_dfs_bfs',
  'arch_cache_locality',
])

export function isHomeTopicConfigured(topicKey: string): boolean {
  return HOME_CONFIGURED_TOPIC_KEYS.has(topicKey as KnowledgePackId)
}

export type QuickStartIntentKey = 'structure' | 'mechanism' | 'practice' | 'reflection'

export type HomeTopic = {
  key: string
  label: string
  description: string
  recommendedIntent?: QuickStartIntentKey
  rawGoalText: string
}

export type HomeSubject = {
  key: string
  label: string
  description: string
  cardHint: string
  topicKeys: string[]
}

export type QuickStartIntent = {
  key: QuickStartIntentKey
  label: string
  subtitle: string
  recommendation: string
  ctaLabel: string
  preferenceTag: PreferenceTagType
}

const TOPICS: Record<string, HomeTopic> = {
  os_process_thread: {
    key: 'os_process_thread',
    label: '进程与线程',
    description: '分清资源归属、调度对象和切换成本。',
    recommendedIntent: 'structure',
    rawGoalText:
      '我想开启一轮关于操作系统中进程与线程的学习流程，先把两者的角色、关系和常见混淆点建立成清晰结构。',
  },
  os_page_replacement: {
    key: 'os_page_replacement',
    label: '页面置换',
    description: '搞懂缺页时为什么换它，不换别的。',
    recommendedIntent: 'mechanism',
    rawGoalText:
      '我想开启一轮关于操作系统里页面置换的学习流程，重点理解缺页发生时为什么会选择特定页面被替换，以及常见算法的判断依据。',
  },
  os_virtual_memory: {
    key: 'os_virtual_memory',
    label: '虚拟内存',
    description: '把地址空间、页表和缺页中断串起来。',
    recommendedIntent: 'structure',
    rawGoalText:
      '我想开启一轮关于操作系统里虚拟内存的学习流程，先建立地址空间、页表、缺页中断和磁盘换入换出的整体关系。',
  },
  os_scheduling: {
    key: 'os_scheduling',
    label: '调度算法',
    description: '比较响应时间、吞吐量和公平性的取舍。',
    recommendedIntent: 'practice',
    rawGoalText:
      '我想开启一轮关于操作系统里调度算法的学习流程，重点理解不同调度策略的目标差异，并能判断题目中该如何选择。',
  },
  os_deadlock: {
    key: 'os_deadlock',
    label: '死锁',
    description: '记住条件不难，难的是会分析和规避。',
    recommendedIntent: 'reflection',
    rawGoalText:
      '我想开启一轮关于操作系统里死锁的学习流程，先梳理死锁产生的条件、识别方式以及预防和避免思路。',
  },
  net_tcp_handshake: {
    key: 'net_tcp_handshake',
    label: 'TCP 三次握手',
    description: '每一步到底在确认什么，要讲顺。',
    recommendedIntent: 'mechanism',
    rawGoalText:
      '我想开启一轮关于计算机网络里 TCP 三次握手的学习流程，先搞懂连接建立为什么要分三步，以及每一步在确认什么。',
  },
  net_flow_control: {
    key: 'net_flow_control',
    label: '流量控制',
    description: '分清窗口、吞吐和接收能力的关系。',
    recommendedIntent: 'mechanism',
    rawGoalText:
      '我想开启一轮关于计算机网络里流量控制的学习流程，重点理解滑动窗口如何限制发送速度，以及它与接收端处理能力之间的关系。',
  },
  net_congestion_control: {
    key: 'net_congestion_control',
    label: '拥塞控制',
    description: '别再把慢开始和流量控制混在一起。',
    recommendedIntent: 'structure',
    rawGoalText:
      '我想开启一轮关于计算机网络里拥塞控制的学习流程，先建立慢开始、拥塞避免、快重传和快恢复之间的整体脉络。',
  },
  net_http_caching: {
    key: 'net_http_caching',
    label: 'HTTP 缓存',
    description: '强缓存和协商缓存总在题里绕人。',
    recommendedIntent: 'practice',
    rawGoalText:
      '我想开启一轮关于计算机网络里 HTTP 缓存的学习流程，目标是分清强缓存与协商缓存的判断过程，并能在场景题里用出来。',
  },
  net_routing: {
    key: 'net_routing',
    label: '路由转发',
    description: '弄懂数据包为什么会走这条路。',
    recommendedIntent: 'reflection',
    rawGoalText:
      '我想开启一轮关于计算机网络里路由转发的学习流程，先梳理路由表、下一跳和转发决策之间的关系。',
  },
  ds_dfs_bfs: {
    key: 'ds_dfs_bfs',
    label: 'DFS 与 BFS',
    description: '名字会背，但选法总不稳。',
    recommendedIntent: 'practice',
    rawGoalText:
      '我想开启一轮关于数据结构里 DFS 和 BFS 的学习流程，先分清两种搜索方式分别适合什么样的题，并建立判断线索。',
  },
  ds_heap: {
    key: 'ds_heap',
    label: '堆与优先队列',
    description: '看到 Top K 时，第一反应要更快。',
    recommendedIntent: 'practice',
    rawGoalText:
      '我想开启一轮关于数据结构里堆与优先队列的学习流程，重点理解它的使用场景、维护方式以及典型题目的判断信号。',
  },
  ds_union_find: {
    key: 'ds_union_find',
    label: '并查集',
    description: '连通性问题要一眼想到它。',
    recommendedIntent: 'mechanism',
    rawGoalText:
      '我想开启一轮关于数据结构里并查集的学习流程，先理解路径压缩、按秩合并和连通性判断之间的关系。',
  },
  ds_topological_sort: {
    key: 'ds_topological_sort',
    label: '拓扑排序',
    description: '有依赖关系的题，先认图结构。',
    recommendedIntent: 'structure',
    rawGoalText:
      '我想开启一轮关于数据结构里拓扑排序的学习流程，先建立有向无环图、入度变化和排序结果之间的整体结构。',
  },
  ds_binary_tree: {
    key: 'ds_binary_tree',
    label: '二叉树遍历',
    description: '前中后序不是背下来，而是会用。',
    recommendedIntent: 'reflection',
    rawGoalText:
      '我想开启一轮关于数据结构里二叉树遍历的学习流程，先整理不同遍历方式的访问顺序、适用场景和常见题型。',
  },
  arch_cache_locality: {
    key: 'arch_cache_locality',
    label: '缓存与局部性',
    description: '顺序访问为什么更快，要说到点上。',
    recommendedIntent: 'reflection',
    rawGoalText:
      '我想开启一轮关于组成原理里缓存与局部性的学习流程，先搞懂为什么访问顺序会影响性能，以及缓存命中的核心直觉。',
  },
  arch_pipeline: {
    key: 'arch_pipeline',
    label: '流水线冒险',
    description: '结构、数据、控制三类总是混。',
    recommendedIntent: 'structure',
    rawGoalText:
      '我想开启一轮关于组成原理里流水线冒险的学习流程，重点建立结构冒险、数据冒险和控制冒险的区别与应对方式。',
  },
  arch_interrupt: {
    key: 'arch_interrupt',
    label: '中断处理',
    description: '请求怎么被响应，关键链路要清楚。',
    recommendedIntent: 'mechanism',
    rawGoalText:
      '我想开启一轮关于组成原理里中断处理的学习流程，先理解中断请求、响应过程和现场保护恢复的关键机制。',
  },
  arch_instruction_cycle: {
    key: 'arch_instruction_cycle',
    label: '指令周期',
    description: '取指、译码、执行总被背成碎片。',
    recommendedIntent: 'structure',
    rawGoalText:
      '我想开启一轮关于组成原理里指令周期的学习流程，先把取指、译码、执行、中断等阶段串成完整路径。',
  },
  arch_bus_dma: {
    key: 'arch_bus_dma',
    label: '总线与 DMA',
    description: '弄懂谁在搬数据，CPU 何时让位。',
    recommendedIntent: 'practice',
    rawGoalText:
      '我想开启一轮关于组成原理里总线与 DMA 的学习流程，重点理解总线仲裁、DMA 传输和 CPU 参与方式之间的关系。',
  },
}

export const HOME_SUBJECTS: HomeSubject[] = [
  {
    key: 'operating-system',
    label: '操作系统',
    description: '围绕进程、内存和调度，把系统怎么跑起来这件事看清楚。',
    cardHint: '进程与线程 / 页面置换 / 虚拟内存',
    topicKeys: [
      'os_process_thread',
      'os_page_replacement',
      'os_virtual_memory',
      'os_scheduling',
      'os_deadlock',
    ],
  },
  {
    key: 'computer-network',
    label: '计算机网络',
    description: '从连接建立到数据转发，理顺每一步为什么这样设计。',
    cardHint: 'TCP 三次握手 / 流量控制 / HTTP 缓存',
    topicKeys: [
      'net_tcp_handshake',
      'net_flow_control',
      'net_congestion_control',
      'net_http_caching',
      'net_routing',
    ],
  },
  {
    key: 'data-structure',
    label: '数据结构',
    description: '面向做题场景，把常见结构和判断线索连起来。',
    cardHint: 'DFS 与 BFS / 堆与优先队列 / 并查集',
    topicKeys: [
      'ds_dfs_bfs',
      'ds_heap',
      'ds_union_find',
      'ds_topological_sort',
      'ds_binary_tree',
    ],
  },
  {
    key: 'computer-architecture',
    label: '组成原理',
    description: '把性能、执行路径和底层部件之间的关系建立起来。',
    cardHint: '缓存与局部性 / 流水线冒险 / 指令周期',
    topicKeys: [
      'arch_cache_locality',
      'arch_pipeline',
      'arch_interrupt',
      'arch_instruction_cycle',
      'arch_bus_dma',
    ],
  },
]

export const HOME_QUICK_STARTS: QuickStartIntent[] = [
  {
    key: 'structure',
    label: '先搭结构',
    subtitle: '先把知识点放回整体框架里。',
    recommendation: '适合刚切入一个新主题时用。',
    ctaLabel: '开始这一轮',
    preferenceTag: PreferenceTag.FRAMEWORK_FIRST,
  },
  {
    key: 'mechanism',
    label: '先懂机制',
    subtitle: '先把关键因果和原理讲清楚。',
    recommendation: '适合知道名词，但机理还不顺时用。',
    ctaLabel: '开始这一轮',
    preferenceTag: PreferenceTag.CONCEPT_FIRST,
  },
  {
    key: 'practice',
    label: '先做练习',
    subtitle: '先从判断和小题目切进去。',
    recommendation: '适合做题容易卡壳时用。',
    ctaLabel: '开始这一轮',
    preferenceTag: PreferenceTag.PRACTICE_FIRST,
  },
  {
    key: 'reflection',
    label: '先做复盘',
    subtitle: '先确认自己已经会了什么。',
    recommendation: '适合学过一轮，想快速收束时用。',
    ctaLabel: '开始这一轮',
    preferenceTag: PreferenceTag.STEP_BY_STEP,
  },
]

export const HOME_DEFAULT_SUBJECT_KEY = HOME_SUBJECTS[0].key
export const HOME_DEFAULT_TOPIC_KEY = HOME_SUBJECTS[0].topicKeys[0]

export function getHomeTopic(topicKey: string): HomeTopic {
  return TOPICS[topicKey]
}

export function getHomeSubject(subjectKey: string): HomeSubject {
  const subject = HOME_SUBJECTS.find((item) => item.key === subjectKey)
  if (!subject) {
    throw new Error(`Unknown subject key: ${subjectKey}`)
  }
  return subject
}

export function getHomeTopicsBySubject(subjectKey: string): HomeTopic[] {
  return getHomeSubject(subjectKey).topicKeys.map((topicKey) => getHomeTopic(topicKey))
}

export function getHomeSubjectByTopic(topicKey: string): HomeSubject | undefined {
  return HOME_SUBJECTS.find((subject) => subject.topicKeys.includes(topicKey))
}

export function buildHomeGoalRequest(
  topicKey: string,
  intentKey: QuickStartIntent['key']
): CreateGoalRequest {
  const topic = getHomeTopic(topicKey)
  const subject = getHomeSubjectByTopic(topicKey)
  const intent = HOME_QUICK_STARTS.find((item) => item.key === intentKey)

  if (!subject || !intent) {
    throw new Error('Unable to build goal request for current selection.')
  }

  const rawGoalText = [topic.rawGoalText, buildIntentSuffix(topic.label, intent.key)].join('')

  return {
    rawGoalText,
    subjectHint: subject.label,
    topicHints: [topic.label],
    preferenceTags: [intent.preferenceTag],
    sourceContext: 'homepage_quick_start',
  }
}

function buildIntentSuffix(topicLabel: string, intentKey: QuickStartIntent['key']): string {
  const suffixMap: Record<QuickStartIntent['key'], string> = {
    structure: ` 这次我想先建立 ${topicLabel} 的整体结构，再进入细节。`,
    mechanism: ` 这次我想先弄懂 ${topicLabel} 背后的关键机制和因果关系。`,
    practice: ` 这次我想先进入和 ${topicLabel} 相关的判断与练习，再倒推原理。`,
    reflection: ` 这次我想先围绕 ${topicLabel} 做一轮复盘，整理我已懂与未懂的部分。`,
  }
  return suffixMap[intentKey]
}
