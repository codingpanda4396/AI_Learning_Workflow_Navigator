export const LEARNING_SCAFFOLD_ENGINE_PACK_IDS = ['ds_dfs_bfs'] as const

export type LearningScaffoldEnginePackId = (typeof LEARNING_SCAFFOLD_ENGINE_PACK_IDS)[number]

export function supportsLearningScaffoldEngine(packId: string | null | undefined): boolean {
  return (
    packId != null && (LEARNING_SCAFFOLD_ENGINE_PACK_IDS as readonly string[]).includes(packId)
  )
}
