import {
  LEARNING_PLAN_TOPIC_DECISION,
  pickTopicLine,
} from '@/constants/learningPlanTopicDecision'
import { getPhaseWorkbenchCopy } from '@/constants/executionWorkbenchContent'
import { resolveKnowledgePackId } from '@/composables/useKnowledgePack'
import type {
  DiagnosisEvidenceSummary,
  GoalContextSnapshot,
  LearnerProfileSnapshot,
  PlanPreviewData,
  StructuredLearningGoal,
} from '@/types/dto'
import type { KnowledgePackId } from '@/types/knowledgePack'
import type {
  LearningPlanDecisionViewModel,
  LearningPlanPathStageKey,
} from '@/types/learningPlanDecision'
import { inferRecommendedStageCode } from '@/utils/planPresentationModel'
import type { PlanStageCode, PlanViewModelContext } from '@/utils/planPresentationModel'
import { workflowTopicLabelFromStructuredGoal } from '@/utils/workflowTopicLabel'

type GapKind =
  | 'structure'
  | 'mechanism'
  | 'boundary'
  | 'practice'
  | 'foundation'
  | 'generic'

const PATH_STAGE_COPY: Record<
  LearningPlanPathStageKey,
  { label: string; description: string }
> = {
  STRUCTURE: {
    label: 'STRUCTURE',
    description: '先搭骨架，知道知识点怎么站位。',
  },
  UNDERSTANDING: {
    label: 'UNDERSTANDING',
    description: '再讲机制，讲清为什么会这样。',
  },
  TRAINING: {
    label: 'TRAINING',
    description: '再做训练，把理解变成动作。',
  },
  REFLECTION: {
    label: 'REFLECTION',
    description: '最后回看，沉淀判断规则。',
  },
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
      return STRATEGY_CODE_TO_ACTION[normalized]
    }
  }
  const label = plan.recommendedStrategy?.label?.trim()
  if (label && !/[A-Z_]{4,}/.test(label)) return truncate(label, 28)
  const reason = plan.recommendedStrategy?.reason?.trim()
  if (reason) return truncate(reason.replace(/^先/, ''), 28)
  return '先把眼前这一小步走稳'
}

function titleToActionSentence(
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
    return '先用一句话讲清这一步到底要交付什么'
  }
  if (packId) {
    return pickTopicLine(
      LEARNING_PLAN_TOPIC_DECISION[packId].heroCorrectActionCandidates,
      `${raw}${seed}`
    )
  }
  return truncate(raw.replace(/^先/, ''), 32)
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

function buildCurrentStateSentence(
  plan: PlanPreviewData,
  profile: LearnerProfileSnapshot | null | undefined,
  diagnosis: DiagnosisEvidenceSummary | null | undefined,
  blockerText: string
): string {
  const diagnosisSummary = diagnosis?.summary?.trim()
  if (diagnosisSummary) {
    return truncate(diagnosisSummary.split(/[。!\n]/)[0] || diagnosisSummary, 34)
  }
  const reason = plan.recommendedStrategy?.reason?.trim()
  if (reason) return truncate(reason, 34)
  const foundationLevel = profile?.foundationLevel?.trim()
  if (foundationLevel) return `当前基础状态：${truncate(foundationLevel, 24)}`
  return `当前先要处理的是：${truncate(blockerText, 18)}`
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

function buildOutcomeText(
  plan: PlanPreviewData,
  stage: PlanStageCode,
  packId: KnowledgePackId | null,
  blockerText: string
): string {
  if (packId) {
    const expectedGain = getPhaseWorkbenchCopy(packId, stage)?.expectedGain
    if (expectedGain) return truncate(expectedGain, 40)
  }
  if (plan.recommendedEntry?.reason?.trim()) {
    return truncate(plan.recommendedEntry.reason, 40)
  }
  const evidence = plan.keyEvidence?.find(Boolean)?.trim()
  if (evidence) return `先把“${truncate(evidence, 18)}”讲清。`
  return `先把“${truncate(blockerText, 14)}”压实，再往后推。`
}

function buildFirstTaskReason(
  blockerText: string,
  strategyAction: string
): string {
  return `${strategyAction}，把“${truncate(blockerText, 12)}”先处理掉。`
}

function buildContrastText(
  wrongActionText: string,
  stage: PlanStageCode,
  blockerText: string,
  packId: KnowledgePackId | null
): string {
  if (packId) {
    const skipRisk = getPhaseWorkbenchCopy(packId, stage)?.skipRisk
    if (skipRisk) return `现在不建议${wrongActionText}，否则容易${truncate(skipRisk, 26)}。`
  }
  return `现在不建议${wrongActionText}，否则“${truncate(blockerText, 12)}”会继续拖住后面四步。`
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
  const taskName = titleToActionSentence(
    plan.recommendedEntry?.title?.trim() || '',
    packId,
    `${primaryTag}${plan.recommendedEntry?.title ?? ''}`
  )
  const goalText = goalTextOneLine(
    ctx.structuredGoal,
    plan,
    ctx.diagnosisEvidenceSummary
  )
  const outcomeText = buildOutcomeText(plan, stage, packId, blockerText)
  const estimatedMinutes =
    plan.recommendedEntry?.estimatedMinutes && plan.recommendedEntry.estimatedMinutes > 0
      ? plan.recommendedEntry.estimatedMinutes
      : fallbackMinutes(stage)

  const pathStages: LearningPlanDecisionViewModel['pathPreview']['stages'] = (
    ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'] as const
  ).map((key) => ({
    key,
    label: PATH_STAGE_COPY[key].label,
    description: PATH_STAGE_COPY[key].description,
    stateLabel: key === stage ? '当前起点' : '后续推进',
    active: key === stage,
  }))

  return {
    hero: {
      eyebrow: '学习规划',
      title: `这轮先从 ${PATH_STAGE_COPY[stage].label} 开始`,
      decisionText: `${goalText} 这一轮先不求铺开，先把“${blockerText}”处理掉。`,
      reasonText: `${buildCurrentStateSentence(plan, ctx.learnerProfileSnapshot, ctx.diagnosisEvidenceSummary, blockerText)}。`,
      outcomeText,
      ctaLabel: '开始学习',
      ctaSubtext: `先完成 1 个小任务，预计 ${estimatedMinutes} 分钟。`,
    },
    reasoning: {
      title: '为什么这样安排',
      summary: '先收住起点，再顺着四步推进。',
      bullets: [
        {
          label: '当前卡点',
          text: blockerText,
        },
        {
          label: '先做什么',
          text: strategyAction,
        },
        {
          label: '避免什么',
          text: buildContrastText(wrongActionText, stage, blockerText, packId),
        },
      ],
    },
    firstTask: {
      title: '开始前预告',
      taskName,
      reasonText: buildFirstTaskReason(blockerText, strategyAction),
      estimatedTimeText: `预计 ${estimatedMinutes} 分钟`,
      benefitText: outcomeText,
    },
    pathPreview: {
      title: '接下来按四步推进',
      summary: '路线先给你看清，但现在只做第一步。',
      stages: pathStages,
    },
    contrast: buildContrastText(wrongActionText, stage, blockerText, packId),
  }
}
