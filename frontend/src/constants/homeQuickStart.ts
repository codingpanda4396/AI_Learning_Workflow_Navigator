import type { CreateGoalRequest } from '@/types/dto'
import { PreferenceTag } from '@/types/enums'
import type { PreferenceTagType } from '@/types/enums'

export type TopicAvailability = 'live' | 'coming-soon'

export type QuickStartIntentKey = 'structure' | 'mechanism' | 'practice' | 'reflection'

export type HomeTopic = {
  key: string
  label: string
  availability: TopicAvailability
  /** 可演示知识点对应的推荐起手方式 */
  recommendedIntent?: QuickStartIntentKey
  previewTitle: string
  previewBody: string
  launchSummary: string
  rawGoalText: string
}

export type HomeSubject = {
  key: string
  label: string
  caption: string
  topicKeys: string[]
}

export type QuickStartIntent = {
  key: QuickStartIntentKey
  label: string
  subtitle: string
  ctaLabel: string
  previewPrefix: string
  preferenceTag: PreferenceTagType
}

const TOPICS: Record<string, HomeTopic> = {
  os_process_thread: {
    key: 'os_process_thread',
    label: '进程与线程',
    availability: 'live',
    recommendedIntent: 'structure',
    previewTitle: '建立运行视角',
    previewBody: '进程、线程与调度的基本关系。',
    launchSummary: '适合先搭整体结构。',
    rawGoalText:
      '我想开启一轮关于操作系统中进程与线程的学习流程，先把两者的角色、关系和常见混淆点建立成清晰结构。',
  },
  os_interrupts: {
    key: 'os_interrupts',
    label: '中断与系统调用',
    availability: 'coming-soon',
    previewTitle: '用户态进内核',
    previewBody: '中断、异常与系统调用的边界。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  os_memory: {
    key: 'os_memory',
    label: '虚拟内存',
    availability: 'coming-soon',
    previewTitle: '地址空间',
    previewBody: '页表、缺页与置换。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  net_tcp_handshake: {
    key: 'net_tcp_handshake',
    label: 'TCP 三次握手',
    availability: 'live',
    recommendedIntent: 'mechanism',
    previewTitle: '连接为何三步',
    previewBody: 'SYN/ACK 与状态变化。',
    launchSummary: '适合先搞清机制。',
    rawGoalText:
      '我想开启一轮关于计算机网络里 TCP 三次握手的学习流程，先搞懂连接建立为什么要分三步，以及每一步在确认什么。',
  },
  net_routing: {
    key: 'net_routing',
    label: '路由转发',
    availability: 'coming-soon',
    previewTitle: '选路与下一跳',
    previewBody: '路由表与转发过程。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  net_http: {
    key: 'net_http',
    label: 'HTTP 请求链路',
    availability: 'coming-soon',
    previewTitle: '一次请求的路径',
    previewBody: 'DNS、TCP 与 HTTP。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  ds_dfs_bfs: {
    key: 'ds_dfs_bfs',
    label: 'DFS 与 BFS',
    availability: 'live',
    recommendedIntent: 'practice',
    previewTitle: '搜索方式与题型',
    previewBody: '遍历顺序与选用场景。',
    launchSummary: '演示链路成熟。',
    rawGoalText:
      '我想开启一轮关于数据结构里 DFS 和 BFS 的学习流程，先分清两种搜索方式分别适合什么样的题，并建立判断线索。',
  },
  ds_heap: {
    key: 'ds_heap',
    label: '堆与优先队列',
    availability: 'coming-soon',
    previewTitle: '何时用堆',
    previewBody: '结构、代价与典型题。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  ds_union_find: {
    key: 'ds_union_find',
    label: '并查集',
    availability: 'coming-soon',
    previewTitle: '连通与合并',
    previewBody: '路径压缩与按秩合并。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  arch_cache_locality: {
    key: 'arch_cache_locality',
    label: '缓存与局部性',
    availability: 'live',
    recommendedIntent: 'reflection',
    previewTitle: '顺序与命中',
    previewBody: '局部性与缓存命中直觉。',
    launchSummary: '适合从复盘已有认知切入。',
    rawGoalText:
      '我想开启一轮关于组成原理里缓存与局部性的学习流程，先搞懂为什么访问顺序会影响性能，以及缓存命中的核心直觉。',
  },
  arch_pipeline: {
    key: 'arch_pipeline',
    label: '流水线冒险',
    availability: 'coming-soon',
    previewTitle: '指令停顿',
    previewBody: '结构、数据与控制冒险。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
  arch_bus: {
    key: 'arch_bus',
    label: '总线与带宽',
    availability: 'coming-soon',
    previewTitle: '部件交换数据',
    previewBody: '带宽与瓶颈。',
    launchSummary: '扩展位',
    rawGoalText: '',
  },
}

export const HOME_SUBJECTS: HomeSubject[] = [
  {
    key: 'operating-system',
    label: '操作系统',
    caption: '运行与资源',
    topicKeys: ['os_process_thread', 'os_interrupts', 'os_memory'],
  },
  {
    key: 'computer-network',
    label: '计算机网络',
    caption: '连接与传输',
    topicKeys: ['net_tcp_handshake', 'net_routing', 'net_http'],
  },
  {
    key: 'data-structure',
    label: '数据结构',
    caption: '模型与算法',
    topicKeys: ['ds_dfs_bfs', 'ds_heap', 'ds_union_find'],
  },
  {
    key: 'computer-architecture',
    label: '组成原理',
    caption: '底层与性能',
    topicKeys: ['arch_cache_locality', 'arch_pipeline', 'arch_bus'],
  },
]

export const HOME_QUICK_STARTS: QuickStartIntent[] = [
  {
    key: 'structure',
    label: '先搭结构',
    subtitle: '先画边界与知识地图。',
    ctaLabel: '开始这轮学习',
    previewPrefix: '先搭结构',
    preferenceTag: PreferenceTag.FRAMEWORK_FIRST,
  },
  {
    key: 'mechanism',
    label: '先懂机制',
    subtitle: '先弄清因果与原理。',
    ctaLabel: '开始这轮学习',
    previewPrefix: '先懂机制',
    preferenceTag: PreferenceTag.CONCEPT_FIRST,
  },
  {
    key: 'practice',
    label: '先做练习',
    subtitle: '先练判断，再补原理。',
    ctaLabel: '开始这轮学习',
    previewPrefix: '先做练习',
    preferenceTag: PreferenceTag.PRACTICE_FIRST,
  },
  {
    key: 'reflection',
    label: '先做复盘',
    subtitle: '先清点已会与缺口。',
    ctaLabel: '开始这轮学习',
    previewPrefix: '先做复盘',
    preferenceTag: PreferenceTag.STEP_BY_STEP,
  },
]

export const HOME_DEFAULT_TOPIC_KEY = HOME_SUBJECTS.flatMap((subject) =>
  subject.topicKeys.map((topicKey) => TOPICS[topicKey])
).find((topic) => topic.availability === 'live')?.key as string

export const HOME_TOPIC_SLOT_COUNT = HOME_SUBJECTS.reduce((n, s) => n + s.topicKeys.length, 0)

export const HOME_LIVE_TOPIC_COUNT = HOME_SUBJECTS.flatMap((s) => s.topicKeys).filter(
  (k) => TOPICS[k].availability === 'live'
).length

export function getHomeTopic(topicKey: string): HomeTopic {
  return TOPICS[topicKey]
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

  if (!subject || !intent || topic.availability !== 'live') {
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
