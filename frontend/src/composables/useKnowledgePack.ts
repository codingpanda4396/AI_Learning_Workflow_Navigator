import { KNOWLEDGE_PACKS } from '@/constants/knowledge-packs'
import type { PlanPreviewData, StructuredLearningGoal, TaskScaffoldResponse } from '@/types/dto'
import type { KnowledgePack, KnowledgePackId } from '@/types/knowledgePack'

function normalizeKey(raw?: string | null): string {
  return (raw || '').trim().toLowerCase()
}

function textCorpus(plan?: PlanPreviewData | null, goal?: StructuredLearningGoal | null): string {
  const parts: string[] = []
  const push = (s?: string | null) => {
    if (s?.trim()) parts.push(s.trim().toLowerCase())
  }
  push(plan?.knowledgeKey)
  push(plan?.packId)
  push(goal?.rawGoalText)
  push(goal?.normalizedGoalText)
  push(goal?.intentDescription)
  push(goal?.subject)
  for (const t of goal?.topics ?? []) push(t)
  push(plan?.goal)
  return parts.join('\n')
}

function inferByCorpus(corpus: string): KnowledgePackId | null {
  if (corpus.includes('os_process_thread') || (corpus.includes('进程') && corpus.includes('线程'))) return 'os_process_thread'
  if (corpus.includes('net_tcp_handshake') || (corpus.includes('tcp') && corpus.includes('握手'))) return 'net_tcp_handshake'
  if (corpus.includes('ds_dfs_bfs') || ((corpus.includes('dfs') || corpus.includes('深度优先')) && (corpus.includes('bfs') || corpus.includes('广度优先')))) return 'ds_dfs_bfs'
  if (corpus.includes('arch_cache_locality') || (corpus.includes('缓存') && corpus.includes('局部性'))) return 'arch_cache_locality'
  return null
}

export function resolveKnowledgePackId(input: {
  knowledgeKey?: string | null
  packId?: string | null
  plan?: PlanPreviewData | null
  scaffold?: TaskScaffoldResponse | null
  structuredGoal?: StructuredLearningGoal | null
}): KnowledgePackId | null {
  const explicit = [
    input.packId,
    input.knowledgeKey,
    input.plan?.packId,
    input.plan?.knowledgeKey,
    input.scaffold?.packId,
    input.scaffold?.knowledgeKey,
  ]
    .map(normalizeKey)
    .find((k) => k in KNOWLEDGE_PACKS)
  if (explicit && explicit in KNOWLEDGE_PACKS) return explicit as KnowledgePackId
  return inferByCorpus(textCorpus(input.plan, input.structuredGoal))
}

export function useKnowledgePack(input: {
  knowledgeKey?: string | null
  packId?: string | null
  plan?: PlanPreviewData | null
  scaffold?: TaskScaffoldResponse | null
  structuredGoal?: StructuredLearningGoal | null
}): KnowledgePack | null {
  const id = resolveKnowledgePackId(input)
  return id ? KNOWLEDGE_PACKS[id] : null
}
