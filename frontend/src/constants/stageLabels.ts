import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

/** 四阶段对用户可见全称（与 .cursor/rules/copywriting.mdc 一致） */
export const PHASE_DISPLAY_FULL_ZH: Record<WorkbenchPhaseCode, string> = {
  STRUCTURE: '结构建立',
  UNDERSTANDING: '机制理解',
  TRAINING: '表达训练',
  REFLECTION: '反思收敛',
}

/** 顶栏、进度条等窄位短标签 */
export const PHASE_DISPLAY_SHORT_ZH: Record<WorkbenchPhaseCode, string> = {
  STRUCTURE: '结构',
  UNDERSTANDING: '机制',
  TRAINING: '表达',
  REFLECTION: '反思',
}

export function phaseCodeToFullZh(code: string | undefined | null): string {
  if (!code) return ''
  const k = code as WorkbenchPhaseCode
  return PHASE_DISPLAY_FULL_ZH[k] ?? code
}

export function phaseCodeToShortZh(code: string | undefined | null): string {
  if (!code) return ''
  const k = code as WorkbenchPhaseCode
  return PHASE_DISPLAY_SHORT_ZH[k] ?? code
}

/** 顶栏阶段条展示（与四阶段顺序一致，UNDERSTANDING 用「理解机制」） */
export const PHASE_STRIP_LABELS_ZH: Record<WorkbenchPhaseCode, string> = {
  STRUCTURE: '结构建立',
  UNDERSTANDING: '理解机制',
  TRAINING: '表达训练',
  REFLECTION: '反思收敛',
}
