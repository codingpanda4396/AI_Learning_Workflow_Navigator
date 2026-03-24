import type { CurrentGuidanceBlock, RecommendedUserActionItem } from '@/types/dto'

export const TASK_STATE_META: Record<
  string,
  { label: string; variant: 'default' | 'success' | 'warning' | 'error' }
> = {
  ORIENT: { label: 'ORIENT / 定位任务', variant: 'default' },
  EXPLORE: { label: 'EXPLORE / 推进探索', variant: 'default' },
  SELF_EXPLAIN: { label: 'SELF_EXPLAIN / 自我解释', variant: 'default' },
  CHECK: { label: 'CHECK / 微检查', variant: 'warning' },
  REMEDIAL: { label: 'REMEDIAL / 修补薄弱点', variant: 'warning' },
  PASS: { label: 'PASS / 可以收束', variant: 'success' },
}

export const GUIDANCE_PHASE_LABELS: Record<string, string> = {
  CLARIFY_GOAL: '澄清目标',
  BUILD_FRAME: '搭建框架',
  TRY_EXPRESS: '尝试表达',
  PROBE_GAPS: '探测缺口',
  META_REFLECT: '反思抽象',
  TRANSITION_HINT: '过渡提示',
}

export const TUTOR_ACTIONS = [
  {
    id: 'explain_concept',
    label: '解释概念',
    description: '只讲这一步里的核心词，不展开成整题答案。',
  },
  {
    id: 'minimal_example',
    label: '给一个小例子',
    description: '用最小例子走一遍关键步骤即可。',
  },
  {
    id: 'concept_compare',
    label: '提醒我别混淆',
    description: '点出最容易混的两个点，帮我分清边界。',
  },
]

export function fallbackGuidanceForState(
  taskState: string,
  canSelfExplain: boolean
): {
  phase: string
  guidance: CurrentGuidanceBlock
  actions: RecommendedUserActionItem[]
} {
  if (taskState === 'CHECK') {
    return {
      phase: 'PROBE_GAPS',
      guidance: {
        title: '用一题快速确认你真的会了',
        bullets: ['保持简洁回答', '只覆盖当前任务的关键判断'],
      },
      actions: [],
    }
  }
  if (taskState === 'PASS') {
    return {
      phase: 'TRANSITION_HINT',
      guidance: {
        title: '任务已通过，收束并进入下一步',
        bullets: ['总结本次收获', '留下下一次练习动作'],
      },
      actions: [],
    }
  }
  if (taskState === 'REMEDIAL') {
    return {
      phase: 'PROBE_GAPS',
      guidance: {
        title: '先修补薄弱点，再回来继续推进',
        bullets: ['回到最卡的概念', '让 Tutor 只给提示，不直接给答案'],
      },
      actions: [],
    }
  }
  if (canSelfExplain) {
    return {
      phase: 'TRY_EXPRESS',
      guidance: {
        title: '把理解讲出来，暴露还不稳的地方',
        bullets: ['用自己的话解释', '重点讲为什么'],
      },
      actions: [],
    }
  }
  return {
    phase: 'BUILD_FRAME',
    guidance: {
      title: '先把框架搭出来，再继续问',
      bullets: ['明确当前任务目标', '从一个最小问题切入'],
    },
    actions: [],
  }
}

export function tutorPromptFor(actionId: string, focus: string) {
  switch (actionId) {
    case 'explain_concept':
      return `请只解释「${focus}」在当前任务里的核心概念，不要直接给完整答案。`
    case 'minimal_example':
      return `请给我一个和「${focus}」相关的最小例子，只展示关键步骤。`
    case 'concept_compare':
      return `请把「${focus}」里最容易混淆的两个概念做一个简短对比。`
    case 'check_statement':
      return `我准备这样理解「${focus}」：请检查我的表述哪里不完整，只指出缺口。`
    case 'hint_only':
      return `请只给我推进「${focus}」的下一步提示，不直接给答案。`
    default:
      return `请帮助我继续推进「${focus}」这个任务。`
  }
}
