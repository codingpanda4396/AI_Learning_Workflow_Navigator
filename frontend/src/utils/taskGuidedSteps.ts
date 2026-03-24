import type { CognitiveUnitItem, TaskScaffoldResponse } from '@/types/dto'

export interface TaskGuidedStep {
  id: string
  index: number
  label: string
  hint?: string
}

const STEP_TO_UNIT: Record<string, string> = {
  orient: 'understand',
  explore: 'explore',
  explain: 'self_explain',
  check: 'verify',
}

function unitForStep(
  stepId: string,
  units?: CognitiveUnitItem[] | null
): CognitiveUnitItem | null {
  const uid = STEP_TO_UNIT[stepId]
  if (!uid || !units?.length) return null
  return units.find((u) => u.unitId === uid) ?? null
}

/**
 * 将后端 execution state 映射为「带练」步骤高亮（纯 UX，不伪造状态）。
 */
export function getCurrentGuidedStepId(
  taskState: string,
  exploreRoundCount: number,
  legacyComplete: boolean
): string {
  if (legacyComplete) return 'wrap'
  const s = taskState
  if (s === 'PASS') return 'wrap'
  if (s === 'CHECK') return 'check'
  if (s === 'EXPLORE' && exploreRoundCount >= 2) return 'explain'
  if (s === 'EXPLORE' || s === 'REMEDIAL' || s === 'ASK') return 'explore'
  if (s === 'INIT') return 'orient'
  return 'orient'
}

export function buildTaskGuidedSteps(
  legacyComplete: boolean,
  scaffold?: TaskScaffoldResponse | null
): TaskGuidedStep[] {
  if (legacyComplete) {
    return [
      {
        id: 'wrap',
        index: 1,
        label: '进入下一步',
        hint: '选好完成状态后继续',
      },
    ]
  }
  const units = scaffold?.cognitiveUnits
  const base: TaskGuidedStep[] = [
    {
      id: 'orient',
      index: 1,
      label: '搞清它怎么工作',
      hint: '知道这步在解决什么问题',
    },
    {
      id: 'explore',
      index: 2,
      label: '用例子跑一遍',
      hint: '和导师聊清楚一个小点',
    },
    {
      id: 'explain',
      index: 3,
      label: '你自己讲清楚',
      hint: '用自己的话说一遍就好',
    },
    {
      id: 'check',
      index: 4,
      label: '快速练一下',
      hint: '两个小问题巩固一下',
    },
    {
      id: 'wrap',
      index: 5,
      label: '进入下一步',
      hint: '收个尾就能继续',
    },
  ]
  if (!units?.length) return base
  return base.map((s) => {
    if (s.id === 'wrap') return s
    const u = unitForStep(s.id, units)
    if (!u) return s
    return {
      ...s,
      label: u.label?.trim() || s.label,
      hint: u.targetOutcome?.trim() || u.failureSignal?.trim() || s.hint,
    }
  })
}

/** 去掉任务级引导前缀，供标题区第二行展示（与「这一步我们一起搞懂」配对） */
export function getTaskCognitiveHeadlineBody(
  scaffold: TaskScaffoldResponse | null
): string {
  if (!scaffold) return ''
  const line =
    scaffold.taskLevelLearningIntent?.trim() ||
    scaffold.learningObjective?.trim() ||
    ''
  if (!line) return ''
  const body = line
    .replace(/^你现在要学会：[：]?\s*/, '')
    .replace(/^这一步我们一起搞懂：[：]?\s*/, '')
    .trim()
  return body || line
}

/** @deprecated 使用 getTaskCognitiveHeadlineBody + 模板固定引导语 */
export function getTaskCognitiveHeadline(scaffold: TaskScaffoldResponse | null): string {
  const body = getTaskCognitiveHeadlineBody(scaffold)
  if (!body) return ''
  return '这一步我们一起搞懂：' + body
}

function pickUnit(
  units: CognitiveUnitItem[] | undefined | null,
  unitId: string
): CognitiveUnitItem | null {
  if (!units?.length) return null
  return units.find((u) => u.unitId === unitId) ?? null
}

const SMALL_STEP_LEAD = '先做一件小事：'

function formatSmallStepAction(
  u: CognitiveUnitItem | null,
  fallback: string
): string {
  if (u?.actionBullets?.length) {
    const b = u.actionBullets
    if (b.length >= 2) {
      const head = b.slice(0, 2).map((x, i) => `${i + 1}. ${x}`).join('\n')
      const tail =
        b.length > 2
          ? '\n' +
            b
              .slice(2)
              .map((x, i) => `${i + 3}. ${x}`)
              .join('\n')
          : ''
      return `${SMALL_STEP_LEAD}\n\n想一想：\n${head}${tail}`
    }
    if (b.length === 1) {
      return `${SMALL_STEP_LEAD}\n\n${b[0]}`
    }
  }
  if (u?.targetOutcome?.trim()) {
    return `${SMALL_STEP_LEAD}\n\n${u.targetOutcome.trim()}`
  }
  return fallback
}

/**
 * 将后端 execution state 映射为当前行动说明（优先使用认知单元）。
 */
export function getCurrentActionInstruction(
  taskState: string,
  exploreRoundCount: number,
  scaffold: TaskScaffoldResponse | null,
  taskGoal: string,
  legacyComplete: boolean
): string {
  const units = scaffold?.cognitiveUnits
  if (legacyComplete) {
    return '本任务可直接完成：在下面选好状态，点击按钮继续即可。'
  }
  if (taskState === 'PASS') {
    return '你已经走完这一环了。在下面用一句话收个尾，就能进入下一步。'
  }
  if (taskState === 'CHECK') {
    const u = pickUnit(units, 'verify')
    return formatSmallStepAction(
      u,
      `${SMALL_STEP_LEAD}\n\n读题，用一两句自己的话回答下面这个问题。`
    )
  }
  if (taskState === 'EXPLORE' && exploreRoundCount >= 2) {
    const u = pickUnit(units, 'self_explain')
    return formatSmallStepAction(
      u,
      `${SMALL_STEP_LEAD}\n\n用你自己的话写一段理解（大约三五句话即可），然后点「我讲完了，继续」。`
    )
  }
  if (taskState === 'EXPLORE' || taskState === 'REMEDIAL' || taskState === 'ASK') {
    const u = pickUnit(units, 'explore')
    if (u?.prompts?.some((p) => p.required)) {
      return `${SMALL_STEP_LEAD}\n\n在下面选一句最贴近的问题（点一下会填进对话区），或直接在对话里说说你卡在哪里。`
    }
    const hint = scaffold?.recommendedAskTemplates?.[0]?.trim()
    if (hint && hint.length <= 72) {
      return `${SMALL_STEP_LEAD}\n\n可以先围绕这句话和导师聊聊：「${hint}」`
    }
    if (hint) {
      return `${SMALL_STEP_LEAD}\n\n推荐问法在下面「如果你有点不确定」里，点一句填进对话区。`
    }
    return `${SMALL_STEP_LEAD}\n\n在对话里说说你不会的点，或贴上你目前的思路。`
  }
  const u = pickUnit(units, 'understand')
  if (u?.learningObjective?.trim()) {
    return `${SMALL_STEP_LEAD}\n\n${u.learningObjective.trim()}`
  }
  const obj = scaffold?.learningObjective?.trim()
  if (obj) {
    return `${SMALL_STEP_LEAD}\n\n先看清楚本步目标，再往下和导师聊：${obj}`
  }
  const g = taskGoal?.trim()
  if (g) {
    return `${SMALL_STEP_LEAD}\n\n先看清任务目标，再和导师聊：${g}`
  }
  return `${SMALL_STEP_LEAD}\n\n先看清上面的任务目标，再进入对话。`
}

const HEADER_ONELINER_MAX = 80

/**
 * 顶栏「一句最短目标」：从完整行动说明里取首行，避免多段小字。
 */
export function getHeaderGoalOneLiner(
  taskState: string,
  exploreRoundCount: number,
  scaffold: TaskScaffoldResponse | null,
  taskGoal: string,
  legacyComplete: boolean
): string {
  const full = getCurrentActionInstruction(
    taskState,
    exploreRoundCount,
    scaffold,
    taskGoal,
    legacyComplete
  )
  const stripped = full.replace(/^先做一件小事：[：]?\s*/m, '').trim()
  const firstLine =
    stripped
      .split(/\n/)
      .map((l) => l.trim())
      .find((l) => l.length > 0) ?? stripped
  if (firstLine.length <= HEADER_ONELINER_MAX) return firstLine
  return `${firstLine.slice(0, HEADER_ONELINER_MAX - 1)}…`
}
