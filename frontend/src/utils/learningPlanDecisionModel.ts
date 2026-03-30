import {
  LEARNING_PLAN_TOPIC_DECISION,
  pickTopicLine,
} from '@/constants/learningPlanTopicDecision'
import { getPhaseWorkbenchCopy } from '@/constants/executionWorkbenchContent'
import { STAGE_GUIDE_META } from '@/constants/guidanceConfig'
import { resolveKnowledgePackId } from '@/composables/useKnowledgePack'
import type {
  DiagnosisEvidenceSummary,
  GoalContextSnapshot,
  LearnerProfileSnapshot,
  PlanPreviewData,
  StructuredLearningGoal,
  TaskBlueprint,
} from '@/types/dto'
import type { KnowledgePackId } from '@/types/knowledgePack'
import type {
  LearningPlanDecisionViewModel,
  LearningPlanPathStageKey,
} from '@/types/learningPlanDecision'
import {
  inferRecommendedStageCode,
  mapTaskToStageCode,
} from '@/utils/planPresentationModel'
import type { PlanStageCode, PlanViewModelContext } from '@/utils/planPresentationModel'
import { workflowTopicLabelFromStructuredGoal } from '@/utils/workflowTopicLabel'

type GapKind =
  | 'structure'
  | 'mechanism'
  | 'boundary'
  | 'practice'
  | 'foundation'
  | 'generic'

/** 阶段代码（与 StepFlow 一致） */
const PATH_STAGE_COPY: Record<LearningPlanPathStageKey, { label: string }> = {
  STRUCTURE: { label: 'STRUCTURE' },
  UNDERSTANDING: { label: 'UNDERSTANDING' },
  TRAINING: { label: 'TRAINING' },
  REFLECTION: { label: 'REFLECTION' },
}

/** Stage rail 折叠一句（与四阶段认知动作对齐） */
const STAGE_RAIL_LINE: Record<LearningPlanPathStageKey, string> = {
  STRUCTURE: '先站稳骨架',
  UNDERSTANDING: '再看机制如何运作',
  TRAINING: '再用自己的话讲清',
  REFLECTION: '最后收束错误规则',
}

const BLOCKER_TAG_TO_USER: Record<string, string> = {
  STRUCTURE_GAP: '结构还没搭稳',
  KNOWLEDGE_NOT_ORGANIZED: '结构还没搭稳',
  MECHANISM_CONFUSION: '关键机制还没讲清',
  BOUNDARY_CONFUSION: '概念边界还不稳定',
  PRACTICE_BREAK: '一到自己做就容易断',
  FOUNDATION_GAP: '基础关系还没站稳',
  PREREQUISITE_GAP: '基础关系还没站稳',
  RELATIONSHIP_GAP: '知识关系线还没理清',
  CONCEPT_GAP: '概念边界还不稳定',
  SHALLOW_UNDERSTANDING_RISK: '理解还停在表面',
  EXPRESSION_GAP: '一开口就容易卡住',
  PROCEDURE_GAP: '步骤一上手就容易乱',
  QUESTION_TYPE_RECOGNITION_GAP: '题型判断还不稳',
}

const STRATEGY_CODE_TO_ACTION: Record<string, string> = {
  CONCEPT_CLARIFICATION: '先讲清关键概念和关系',
  FRAMEWORK_BUILD: '先把整体结构搭起来',
  FOUNDATION_PATCH: '先补最影响推进的基础点',
  SPRINT_CORRECTION: '先纠正最容易反复出错的点',
  DRILL_STRENGTHEN: '先用一小轮训练把理解压实',
  LOCAL_REPAIR: '先修掉最卡推进的局部问题',
}

const TITLE_MAX = 18
const BLOCK_MAX = 28
const SUBTITLE_MAX = 40

function truncate(s: string, max: number): string {
  const t = s.trim()
  if (t.length <= max) return t
  return `${t.slice(0, max)}…`
}

function gapKindFromTag(tag: string): GapKind {
  const upper = tag.toUpperCase().replace(/\s+/g, '_')
  if (/PREREQUISITE|RELATIONSHIP|STRUCTURE_GAP|ORGANIZED|KNOWLEDGE_NOT/.test(upper)) {
    return 'structure'
  }
  if (/BOUNDARY_CONFUSION|BOUNDARY/.test(upper)) return 'boundary'
  if (/MECHANISM|CONCEPT_GAP|SHALLOW|UNDERSTANDING_RISK/.test(upper)) return 'mechanism'
  if (/PROCEDURE|QUESTION_TYPE|PRACTICE_BREAK|EXPRESSION_GAP/.test(upper)) return 'practice'
  if (/FOUNDATION_GAP|FOUNDATION/.test(upper)) return 'foundation'
  return 'generic'
}

function wrongActionForGap(kind: GapKind): string {
  switch (kind) {
    case 'structure':
      return '直接刷题'
    case 'mechanism':
      return '直接背结论'
    case 'boundary':
      return '继续混着学'
    case 'practice':
      return '只看讲解不自己说'
    case 'foundation':
      return '跳过基础直接做综合题'
    default:
      return '急着往下推进'
  }
}

function pickPrimaryRiskTag(
  plan: PlanPreviewData,
  profile: LearnerProfileSnapshot | null | undefined,
  goalCtx: GoalContextSnapshot | null | undefined,
  diagnosis: DiagnosisEvidenceSummary | null | undefined
): string {
  return (
    profile?.blockerTags?.find(Boolean)?.trim() ||
    plan.risks?.find(Boolean)?.trim() ||
    profile?.riskTags?.find(Boolean)?.trim() ||
    goalCtx?.riskTags?.find(Boolean)?.trim() ||
    diagnosis?.primaryRiskTags?.find(Boolean)?.trim() ||
    diagnosis?.primaryGapType?.trim() ||
    ''
  )
}

function blockerUserText(tag: string, packId: KnowledgePackId | null): string {
  const normalized = tag.toUpperCase().replace(/\s+/g, '_')
  if (BLOCKER_TAG_TO_USER[normalized]) return BLOCKER_TAG_TO_USER[normalized]
  if (packId) {
    return pickTopicLine(
      LEARNING_PLAN_TOPIC_DECISION[packId].blockerCandidates,
      tag || packId
    )
  }
  return '当前最卡的那一步还没压实'
}

function strategyActionText(plan: PlanPreviewData): string {
  const strategyCode = plan.recommendedStrategy?.code?.trim()
  if (strategyCode) {
    const normalized = strategyCode.toUpperCase().replace(/-/g, '_')
    if (STRATEGY_CODE_TO_ACTION[normalized]) {
      return truncate(STRATEGY_CODE_TO_ACTION[normalized], BLOCK_MAX)
    }
  }
  const label = plan.recommendedStrategy?.label?.trim()
  if (label && !/[A-Z_]{4,}/.test(label)) return truncate(label, BLOCK_MAX)
  const reason = plan.recommendedStrategy?.reason?.trim()
  if (reason) return truncate(reason.replace(/^先/, ''), BLOCK_MAX)
  return '从推荐起点开始'
}

function titleToHeadline(
  title: string,
  packId: KnowledgePackId | null,
  seed: string
): string {
  const raw = title.trim()
  if (!raw) {
    if (packId) {
      return pickTopicLine(
        LEARNING_PLAN_TOPIC_DECISION[packId].heroCorrectActionCandidates,
        seed
      )
    }
    return '先完成这一步的最小交付'
  }
  if (packId) {
    return pickTopicLine(
      LEARNING_PLAN_TOPIC_DECISION[packId].heroCorrectActionCandidates,
      `${raw}${seed}`
    )
  }
  return truncate(raw.replace(/^先/, ''), 40)
}

function goalTextOneLine(
  structuredGoal: StructuredLearningGoal | null | undefined,
  plan: PlanPreviewData,
  diagnosis: DiagnosisEvidenceSummary | null | undefined
): string {
  const goalLabel = workflowTopicLabelFromStructuredGoal(structuredGoal ?? null)
  if (goalLabel) return goalLabel
  if (plan.goal?.trim()) return truncate(plan.goal, 36)
  if (diagnosis?.summary?.trim()) {
    return truncate(diagnosis.summary.split(/[。!\n]/)[0] || diagnosis.summary, 36)
  }
  return '这轮学习目标'
}

function buildHeroSubtitle(
  plan: PlanPreviewData,
  profile: LearnerProfileSnapshot | null | undefined,
  diagnosis: DiagnosisEvidenceSummary | null | undefined,
  stage: PlanStageCode,
  packId: KnowledgePackId | null,
  blockerText: string
): string {
  const entryReason = plan.recommendedEntry?.reason?.trim()
  if (entryReason) return truncate(entryReason, SUBTITLE_MAX)
  const diagnosisSummary = diagnosis?.summary?.trim()
  if (diagnosisSummary) {
    return truncate(diagnosisSummary.split(/[。!\n]/)[0] || diagnosisSummary, SUBTITLE_MAX)
  }
  const strategyReason = plan.recommendedStrategy?.reason?.trim()
  if (strategyReason) return truncate(strategyReason, SUBTITLE_MAX)
  if (packId) {
    const gain = getPhaseWorkbenchCopy(packId, stage)?.expectedGain
    if (gain) return truncate(gain, SUBTITLE_MAX)
  }
  const foundationLevel = profile?.foundationLevel?.trim()
  if (foundationLevel) return truncate(`当前基础：${foundationLevel}`, SUBTITLE_MAX)
  return truncate(`先处理「${truncate(blockerText, 12)}」，再往后推进。`, SUBTITLE_MAX)
}

function fallbackMinutes(stage: PlanStageCode): number {
  switch (stage) {
    case 'STRUCTURE':
      return 10
    case 'UNDERSTANDING':
      return 12
    case 'TRAINING':
      return 10
    case 'REFLECTION':
      return 8
    default:
      return 8
  }
}

function buildContrastText(
  wrongActionText: string,
  stage: PlanStageCode,
  blockerText: string,
  packId: KnowledgePackId | null
): string {
  if (packId) {
    const skipRisk = getPhaseWorkbenchCopy(packId, stage)?.skipRisk
    if (skipRisk) return `不建议${wrongActionText}，否则${truncate(skipRisk, 20)}。`
  }
  return `不建议${wrongActionText}，否则「${truncate(blockerText, 10)}」会拖住后面几步。`
}

function countTasksInStage(plan: PlanPreviewData, stage: PlanStageCode): number {
  const tasks = plan.tasks ?? []
  const n = tasks.length
  if (n === 0) return 0
  return tasks.filter((t, i) => mapTaskToStageCode(t, i, n) === stage).length
}

function buildFirstTaskContent(
  plan: PlanPreviewData,
  stage: PlanStageCode,
  packId: KnowledgePackId | null,
  primaryTag: string,
  recommendedEntryTitle: string
): {
  headline: string
  goalLine: string
  deliverableLine: string
  errorReminder: string
} {
  const task: TaskBlueprint | undefined = plan.tasks?.[0]
  const seed = `${primaryTag}${recommendedEntryTitle}`

  const headline = titleToHeadline(
    task?.title?.trim() || plan.recommendedEntry?.title?.trim() || '',
    packId,
    seed
  )

  const goalLine = truncate(
    task?.goal?.trim() ||
      plan.recommendedEntry?.reason?.trim() ||
      STAGE_GUIDE_META[stage as keyof typeof STAGE_GUIDE_META].stageGoal,
    BLOCK_MAX
  )

  const criteria = task?.completionCriteria?.filter(Boolean) ?? []
  const deliverableLine =
    criteria.length > 0
      ? truncate(criteria.slice(0, 2).join('；'), 56)
      : truncate(STAGE_GUIDE_META[stage as keyof typeof STAGE_GUIDE_META].passEvidence, BLOCK_MAX)

  const misconception = plan.commonMisconceptions?.find(Boolean)?.trim()
  const skip = packId ? getPhaseWorkbenchCopy(packId, stage)?.skipRisk : undefined
  const errorReminder = truncate(
    misconception || skip || `不要先背术语，先${STAGE_RAIL_LINE[stage]}`,
    BLOCK_MAX
  )

  return { headline, goalLine, deliverableLine, errorReminder }
}

export function buildLearningPlanDecisionViewModel(
  plan: PlanPreviewData,
  ctx: {
    structuredGoal?: StructuredLearningGoal | null
    goalContextSnapshot?: GoalContextSnapshot | null
    learnerProfileSnapshot?: LearnerProfileSnapshot | null
    diagnosisEvidenceSummary?: DiagnosisEvidenceSummary | null
  }
): LearningPlanDecisionViewModel {
  const vmCtx: PlanViewModelContext = {
    structuredGoal: ctx.structuredGoal,
    learnerProfileSnapshot: ctx.learnerProfileSnapshot,
  }

  const packId = resolveKnowledgePackId({
    plan,
    structuredGoal: ctx.structuredGoal ?? undefined,
  })
  const stage = inferRecommendedStageCode(plan, vmCtx)
  const primaryTag = pickPrimaryRiskTag(
    plan,
    ctx.learnerProfileSnapshot,
    ctx.goalContextSnapshot,
    ctx.diagnosisEvidenceSummary
  )
  const blockerText = blockerUserText(primaryTag || 'GENERIC', packId)
  const gapKind = gapKindFromTag(primaryTag || plan.recommendedStrategy?.code || 'GENERIC')
  const wrongActionText = wrongActionForGap(gapKind)
  const strategyAction = strategyActionText(plan)

  const goalText = goalTextOneLine(
    ctx.structuredGoal,
    plan,
    ctx.diagnosisEvidenceSummary
  )

  const estimatedMinutes =
    plan.recommendedEntry?.estimatedMinutes && plan.recommendedEntry.estimatedMinutes > 0
      ? plan.recommendedEntry.estimatedMinutes
      : plan.tasks?.[0]?.estimatedMinutes && plan.tasks[0].estimatedMinutes! > 0
        ? plan.tasks[0].estimatedMinutes!
        : fallbackMinutes(stage)

  const inStageCount = countTasksInStage(plan, stage)
  const taskCountChip =
    inStageCount > 0 ? `完成 ${inStageCount} 个任务` : '先完成 1 个任务'

  const rawHeroTitle =
    plan.recommendedEntry?.title?.trim() ||
    plan.tasks?.[0]?.title?.trim() ||
    goalText ||
    `进入 ${PATH_STAGE_COPY[stage as LearningPlanPathStageKey].label}`

  const heroTitle = truncate(rawHeroTitle, TITLE_MAX)

  const subtitle = buildHeroSubtitle(
    plan,
    ctx.learnerProfileSnapshot,
    ctx.diagnosisEvidenceSummary,
    stage,
    packId,
    blockerText
  )

  const skipRiskLine = truncate(
    buildContrastText(wrongActionText, stage, blockerText, packId),
    BLOCK_MAX
  )

  const firstTask = buildFirstTaskContent(
    plan,
    stage,
    packId,
    primaryTag,
    plan.recommendedEntry?.title ?? ''
  )

  const pathStages: LearningPlanDecisionViewModel['pathPreview']['stages'] = (
    ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'] as const
  ).map((key) => ({
    key,
    label: PATH_STAGE_COPY[key].label,
    railLine: STAGE_RAIL_LINE[key],
    stateLabel: key === stage ? '当前起点' : '后续推进',
    active: key === stage,
  }))

  const chips: [string, string, string] = [
    `预计 ${estimatedMinutes} 分钟`,
    taskCountChip,
    `当前卡点：${truncate(blockerText, 10)}`,
  ]

  return {
    hero: {
      eyebrow: `当前起点 · ${PATH_STAGE_COPY[stage as LearningPlanPathStageKey].label}`,
      title: heroTitle,
      subtitle,
      chips,
      ctaLabel: `进入 ${PATH_STAGE_COPY[stage as LearningPlanPathStageKey].label}`,
      ctaSubtext: undefined,
      secondaryCtaLabel: '查看为什么从这里开始',
    },
    reasoning: {
      accordionTitle: '为什么从这里开始',
      bullets: [
        { label: '当前卡点', text: truncate(blockerText, BLOCK_MAX) },
        { label: '先处理', text: truncate(strategyAction, BLOCK_MAX) },
        { label: '跳过风险', text: skipRiskLine },
      ],
    },
    firstTask: {
      ...firstTask,
      enterTaskLabel: '进入任务',
    },
    pathPreview: {
      title: '四阶段推进',
      stages: pathStages,
    },
  }
}
