import type { KnowledgePackId } from '@/types/knowledgePack'
import type { DiagnosisOptionValue } from '@/constants/KnowledgeConfig'
import type { FoundationUiId } from '@/utils/diagnosisSubmitMapper'

/**
 * 知识点诊断四选项 → 通用「基础熟悉度」映射，保证 submit 仍走原 3 题契约。
 * A 最弱 / D 最强 的单调语义在各题中保持一致。
 */
const TOPIC_FOUNDATION: Record<
  KnowledgePackId,
  Record<DiagnosisOptionValue, FoundationUiId>
> = {
  os_process_thread: {
    A: 'fu_none',
    B: 'fu_fuzzy',
    C: 'fu_shaky',
    D: 'fu_solid_practice',
  },
  net_tcp_handshake: {
    A: 'fu_fuzzy',
    B: 'fu_shaky',
    C: 'fu_shaky',
    D: 'fu_solid_practice',
  },
  ds_dfs_bfs: {
    A: 'fu_none',
    B: 'fu_fuzzy',
    C: 'fu_shaky',
    D: 'fu_solid_practice',
  },
  arch_cache_locality: {
    A: 'fu_fuzzy',
    B: 'fu_shaky',
    C: 'fu_shaky',
    D: 'fu_solid_practice',
  },
}

export function mapTopicDiagnosisToFoundation(
  packId: KnowledgePackId,
  option: DiagnosisOptionValue
): FoundationUiId {
  return TOPIC_FOUNDATION[packId][option] ?? 'fu_fuzzy'
}
