import type { CreateGoalRequest } from '@/types/dto'
import { PreferenceTag } from '@/types/enums'
import type { PreferenceTagType } from '@/types/enums'

export type TopicAvailability = 'live' | 'coming-soon'

export type HomeTopic = {
  key: string
  label: string
  availability: TopicAvailability
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
  key: 'structure' | 'mechanism' | 'practice' | 'reflection'
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
    previewTitle: '先把系统里的“谁在运行”分清',
    previewBody: '这一轮会先拆清进程、线程、调度单位和常见误区，不从术语堆开始。',
    launchSummary: '适合用来建立操作系统运行结构的第一层认知。',
    rawGoalText:
      '我想开启一轮关于操作系统中进程与线程的学习流程，先把两者的角色、关系和常见混淆点建立成清晰结构。',
  },
  os_interrupts: {
    key: 'os_interrupts',
    label: '中断与系统调用',
    availability: 'coming-soon',
    previewTitle: '即将开放：从用户态到内核态的入口',
    previewBody: '后续会补上中断、异常、系统调用之间的边界与流转，现在先保留为扩展方向。',
    launchSummary: '属于即将扩展的操作系统章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  os_memory: {
    key: 'os_memory',
    label: '虚拟内存',
    availability: 'coming-soon',
    previewTitle: '即将开放：地址空间如何被组织',
    previewBody: '未来会覆盖页表、地址翻译、缺页与置换，帮助用户把内存管理串成完整图景。',
    launchSummary: '属于即将扩展的操作系统章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  net_tcp_handshake: {
    key: 'net_tcp_handshake',
    label: 'TCP 三次握手',
    availability: 'live',
    previewTitle: '先看清连接为什么要分三步建立',
    previewBody: '这一轮会聚焦 SYN、ACK、状态变化与“为什么不是两次”的机制逻辑。',
    launchSummary: '适合快速切入计算机网络里的连接建立机制。',
    rawGoalText:
      '我想开启一轮关于计算机网络里 TCP 三次握手的学习流程，先搞懂连接建立为什么要分三步，以及每一步在确认什么。',
  },
  net_routing: {
    key: 'net_routing',
    label: '路由转发',
    availability: 'coming-soon',
    previewTitle: '即将开放：数据包如何一步步找到路',
    previewBody: '后续会把路由选择、下一跳和转发表放进同一条学习链路里，现在先作为扩展能力展示。',
    launchSummary: '属于即将扩展的计算机网络章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  net_http: {
    key: 'net_http',
    label: 'HTTP 请求链路',
    availability: 'coming-soon',
    previewTitle: '即将开放：从浏览器到服务端的一次完整往返',
    previewBody: '未来会把 DNS、TCP、HTTP 和响应路径串起来做成可演示流程。',
    launchSummary: '属于即将扩展的计算机网络章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  ds_dfs_bfs: {
    key: 'ds_dfs_bfs',
    label: 'DFS 与 BFS',
    availability: 'live',
    previewTitle: '先分清两种搜索方式，再谈题型判断',
    previewBody: '这一轮会延续现有演示能力，让用户先看清搜索顺序，再学会在题目里站队。',
    launchSummary: '这是当前最成熟的一条演示链路，能直接带到后续规划页。',
    rawGoalText:
      '我想开启一轮关于数据结构里 DFS 和 BFS 的学习流程，先分清两种搜索方式分别适合什么样的题，并建立判断线索。',
  },
  ds_heap: {
    key: 'ds_heap',
    label: '堆与优先队列',
    availability: 'coming-soon',
    previewTitle: '即将开放：什么时候该把“最值维护”交给堆',
    previewBody: '后续会补上堆的结构、操作代价和典型题感，现在先作为能力边界展示。',
    launchSummary: '属于即将扩展的数据结构章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  ds_union_find: {
    key: 'ds_union_find',
    label: '并查集',
    availability: 'coming-soon',
    previewTitle: '即将开放：集合合并与连通性判断',
    previewBody: '未来会覆盖路径压缩、按秩合并和连通类问题识别。',
    launchSummary: '属于即将扩展的数据结构章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  arch_cache_locality: {
    key: 'arch_cache_locality',
    label: '缓存与局部性',
    availability: 'live',
    previewTitle: '先理解“为什么访问顺序会影响快慢”',
    previewBody: '这一轮会把 Cache 命中、空间局部性、时间局部性放到同一条解释链里。',
    launchSummary: '适合从组成原理里先抓住性能直觉和硬件行为。',
    rawGoalText:
      '我想开启一轮关于组成原理里缓存与局部性的学习流程，先搞懂为什么访问顺序会影响性能，以及缓存命中的核心直觉。',
  },
  arch_pipeline: {
    key: 'arch_pipeline',
    label: '流水线冒险',
    availability: 'coming-soon',
    previewTitle: '即将开放：指令为什么会互相卡住',
    previewBody: '后续会覆盖结构冒险、数据冒险和控制冒险，让流水线不再只是一张图。',
    launchSummary: '属于即将扩展的组成原理章节，还不能直接开启流程。',
    rawGoalText: '',
  },
  arch_bus: {
    key: 'arch_bus',
    label: '总线与带宽',
    availability: 'coming-soon',
    previewTitle: '即将开放：部件之间如何交换数据',
    previewBody: '未来会把带宽、仲裁和系统瓶颈一起组织成更产品化的学习路径。',
    launchSummary: '属于即将扩展的组成原理章节，还不能直接开启流程。',
    rawGoalText: '',
  },
}

export const HOME_SUBJECTS: HomeSubject[] = [
  {
    key: 'operating-system',
    label: '操作系统',
    caption: '把运行时结构先搭起来',
    topicKeys: ['os_process_thread', 'os_interrupts', 'os_memory'],
  },
  {
    key: 'computer-network',
    label: '计算机网络',
    caption: '看清连接与传输机制',
    topicKeys: ['net_tcp_handshake', 'net_routing', 'net_http'],
  },
  {
    key: 'data-structure',
    label: '数据结构',
    caption: '从题感判断走向可执行方法',
    topicKeys: ['ds_dfs_bfs', 'ds_heap', 'ds_union_find'],
  },
  {
    key: 'computer-architecture',
    label: '组成原理',
    caption: '把底层性能直觉建立起来',
    topicKeys: ['arch_cache_locality', 'arch_pipeline', 'arch_bus'],
  },
]

export const HOME_QUICK_STARTS: QuickStartIntent[] = [
  {
    key: 'structure',
    label: '我想先建立结构',
    subtitle: '先把知识地图、边界和概念位置搭起来。',
    ctaLabel: '开启结构搭建',
    previewPrefix: '这轮会先搭结构',
    preferenceTag: PreferenceTag.FRAMEWORK_FIRST,
  },
  {
    key: 'mechanism',
    label: '我想搞懂机制',
    subtitle: '先解释为什么这样运作，再进入应用。',
    ctaLabel: '开启机制理解',
    previewPrefix: '这轮会先讲机制',
    preferenceTag: PreferenceTag.CONCEPT_FIRST,
  },
  {
    key: 'practice',
    label: '我想开始练习',
    subtitle: '先抓题目判断和操作方法，再补原理。',
    ctaLabel: '开启练习模式',
    previewPrefix: '这轮会先进入练习',
    preferenceTag: PreferenceTag.PRACTICE_FIRST,
  },
  {
    key: 'reflection',
    label: '我想复盘这轮学习',
    subtitle: '先整理已学内容，再识别遗漏和下一步。',
    ctaLabel: '开启复盘流程',
    previewPrefix: '这轮会先做复盘',
    preferenceTag: PreferenceTag.STEP_BY_STEP,
  },
]

export const HOME_DEFAULT_TOPIC_KEY = HOME_SUBJECTS.flatMap((subject) =>
  subject.topicKeys.map((topicKey) => TOPICS[topicKey])
).find((topic) => topic.availability === 'live')?.key as string

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

  const rawGoalText = [
    topic.rawGoalText,
    buildIntentSuffix(topic.label, intent.key),
  ].join('')

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
    structure: ` 这次我希望先建立 ${topicLabel} 的整体结构，再进入细节。`,
    mechanism: ` 这次我希望先搞懂 ${topicLabel} 背后的关键机制和因果关系。`,
    practice: ` 这次我希望先进入和 ${topicLabel} 相关的判断与练习，再倒推原理。`,
    reflection: ` 这次我希望先围绕 ${topicLabel} 做一轮复盘，整理我已懂与未懂的部分。`,
  }
  return suffixMap[intentKey]
}
