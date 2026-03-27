import type { KnowledgePackId } from '@/types/knowledgePack'

/** 四知识点特化：优先于通用 title→动作句映射 */
export type LearningPlanTopicDecisionPack = {
  /** 与 recommendedEntry.title 关键词匹配时优先采用 */
  heroCorrectActionCandidates: string[]
  blockerCandidates: string[]
  /** 错误路径区「低效结果」口语备选（与 skipRisk 改写互斥优先） */
  wrongPathResultLines: string[]
}

export const LEARNING_PLAN_TOPIC_DECISION: Record<
  KnowledgePackId,
  LearningPlanTopicDecisionPack
> = {
  ds_dfs_bfs: {
    heroCorrectActionCandidates: [
      '先讲清 DFS 和 BFS 的核心区别',
      '先把遍历顺序、数据结构和适用场景分开',
      '先理清「搜索过程」而不是直接背模板',
    ],
    blockerCandidates: [
      '还不会把 DFS 和 BFS 组织成自己的结构',
      '知道名字，但边界和用法容易混',
      '一看题就想套模板，机制还没站稳',
    ],
    wrongPathResultLines: [
      '直接刷题会把「会看」和「会做」混在一起',
      '继续背模板会让你更难判断什么时候该用 DFS、什么时候该用 BFS',
    ],
  },
  net_tcp_handshake: {
    heroCorrectActionCandidates: [
      '先讲清 TCP 为什么需要可靠传输机制',
      '先把三次握手、确认机制和重传逻辑分开',
      '先理清 TCP 解决的核心问题，再进入细节',
    ],
    blockerCandidates: [
      '关键机制会看，但因果链不稳',
      '能背过程，但说不清为什么这样设计',
      '不同机制容易混成一团',
    ],
    wrongPathResultLines: [
      '直接背流程图会让你记住步骤，却解释不了原因',
      '直接做题会不断卡在机制因果上',
    ],
  },
  os_process_thread: {
    heroCorrectActionCandidates: [
      '先讲清进程和线程到底分别是什么',
      '先分开「资源拥有者」和「调度单位」',
      '先理清共享与隔离，再进入调度和切换',
    ],
    blockerCandidates: [
      '概念边界还不稳定',
      '知道区别，但放到操作系统里就容易混',
      '能背定义，但不能解释为什么要这样设计',
    ],
    wrongPathResultLines: [
      '直接刷题会把概念混淆放大',
      '继续死记定义，后面一碰调度和切换就会断',
    ],
  },
  arch_cache_locality: {
    heroCorrectActionCandidates: [
      '先讲清为什么有缓存一致性问题',
      '先分开「缓存带来的性能收益」和「一致性带来的复杂度」',
      '先理清多核下为什么会出现不一致',
    ],
    blockerCandidates: [
      '问题背景还没真正站稳',
      '知道有协议，但不知道为什么必须有协议',
      '概念会看，但系统层因果还没有连起来',
    ],
    wrongPathResultLines: [
      '直接记协议状态会让知识碎掉',
      '跳过问题背景直接学协议，会越学越抽象',
    ],
  },
}

export function pickTopicLine(
  lines: string[],
  seed: string
): string {
  if (!lines.length) return ''
  let h = 0
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0
  return lines[h % lines.length]!
}
