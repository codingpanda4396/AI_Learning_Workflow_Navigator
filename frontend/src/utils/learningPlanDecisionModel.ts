import { STAGE_GUIDE_META } from '@/constants/guidanceConfig'
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
    label: '先理清',
    description: '把关键关系讲清',
  },
  UNDERSTANDING: {
    label: '再验证',
    description: '确认不是似懂非懂',
  },
  TRAINING: {
    label: '再训练',
    description: '把理解变成动作',
  },
  REFLECTION: {
    label: '最后确认',
    description: '知道哪里已经站稳',
  },
}

/** 与规范及后端 tag 对齐的用户可读断层句 */
const BLOCKER_TAG_TO_USER: Record<string, string> = {
  STRUCTURE_GAP: '还不会把知识点组织成自己的结构',
  KNOWLEDGE_NOT_ORGANIZED: '还不会把知识点组织成自己的结构',
  MECHANISM_CONFUSION: '关键机制还没有真正讲清',
  BOUNDARY_CONFUSION: '概念边界还不稳定',
  PRACTICE_BREAK: '一到自己做就容易断',
  FOUNDATION_GAP: '基础关系还没站稳',
  PREREQUISITE_GAP: '基础关系还没站稳',
  RELATIONSHIP_GAP: '还不会把知识点组织成自己的结构',
  CONCEPT_GAP: '概念边界还不稳定',
  SHALLOW_UNDERSTANDING_RISK: '理解还停留在表面，容易混',
  EXPRESSION_GAP: '一开口就容易断，表达还没压实',
  PROCEDURE_GAP: '一到自己做就容易断',
  QUESTION_TYPE_RECOGNITION_GAP: '题型判断还不稳，一动笔就偏',
}

/** 对比区「状态」短标签 */
const BLOCKER_TAG_TO_STATE_LABEL: Record<string, string> = {
  STRUCTURE_GAP: '结构还没搭稳',
  KNOWLEDGE_NOT_ORGANIZED: '结构还没搭稳',
  MECHANISM_CONFUSION: '机制因果不稳',
  BOUNDARY_CONFUSION: '概念边界不稳',
  PRACTICE_BREAK: '会看不太会做',
  FOUNDATION_GAP: '基础关系不稳',
  PREREQUISITE_GAP: '基础关系不稳',
  RELATIONSHIP_GAP: '关系线还没理清',
  CONCEPT_GAP: '概念边界不稳',
  SHALLOW_UNDERSTANDING_RISK: '似懂非懂',
  EXPRESSION_GAP: '表达没收住',
  PROCEDURE_GAP: '步骤容易断',
  QUESTION_TYPE_RECOGNITION_GAP: '题型判断不稳',
}

/** 与「所以先 …」拼接，不再带「先」前缀，避免「所以先先…」 */
const STRATEGY_CODE_TO_ACTION: Record<string, string> = {
  CONCEPT_CLARIFICATION: '讲清关键概念和关系',
  FRAMEWORK_BUILD: '把整体结构搭起来',
  FOUNDATION_PATCH: '补最影响后续推进的基础点',
  SPRINT_CORRECTION: '纠正当前最容易反复出错的点',
  DRILL_STRENGTHEN: '用一小轮训练把理解压实',
  LOCAL_REPAIR: '修掉最卡推进的局部问题',
}

function truncate(s: string, max: number): string {
  const t = s.trim()
  if (t.length <= max) return t
  return t.slice(0, max) + '…'
}

function gapKindFromTag(tag: string): GapKind {
  const u = tag.toUpperCase().replace(/\s+/g, '_')
  if (/PREREQUISITE|RELATIONSHIP|STRUCTURE_GAP|ORGANIZED|KNOWLEDGE_NOT/.test(u)) {
    return 'structure'
  }
  if (/BOUNDARY_CONFUSION|BOUNDARY/.test(u)) return 'boundary'
  if (/MECHANISM|CONCEPT_GAP|SHALLOW|UNDERSTANDING_RISK/.test(u)) {
    return 'mechanism'
  }
  if (/PROCEDURE|QUESTION_TYPE|PRACTICE_BREAK|EXPRESSION_GAP/.test(u)) {
    return 'practice'
  }
  if (/FOUNDATION_GAP|FOUNDATION/.test(u)) return 'foundation'
  if (/STRUCTURE|FRAMEWORK/.test(u)) return 'structure'
  return 'generic'
}

function wrongActionForGap(kind: GapKind): string {
  switch (kind) {
    case 'structure':
      return '直接去刷题'
    case 'mechanism':
      return '直接背结论'
    case 'boundary':
      return '继续混着学'
    case 'practice':
      return '只看讲解不自己说'
    case 'foundation':
      return '跳过基础直接做综合题'
    default:
      return '急着往下刷题'
  }
}

function pickPrimaryRiskTag(
  plan: PlanPreviewData,
  profile: LearnerProfileSnapshot | null | undefined,
  goalCtx: GoalContextSnapshot | null | undefined,
  diagnosis: DiagnosisEvidenceSummary | null | undefined
): string {
  const b = profile?.blockerTags?.find((x) => x?.trim())
  if (b) return b.trim()
  const r0 = plan.risks?.find((x) => x?.trim())
  if (r0) return r0.trim()
  const rt = profile?.riskTags?.find((x) => x?.trim())
  if (rt) return rt.trim()
  const gr = goalCtx?.riskTags?.find((x) => x?.trim())
  if (gr) return gr.trim()
  const pt = diagnosis?.primaryRiskTags?.find((x) => x?.trim())
  if (pt) return pt.trim()
  const pg = diagnosis?.primaryGapType?.trim()
  if (pg) return pg
  return ''
}

function blockerUserText(tag: string, packId: KnowledgePackId | null): string {
  const key = tag.toUpperCase().replace(/\s+/g, '_')
  if (BLOCKER_TAG_TO_USER[key]) return BLOCKER_TAG_TO_USER[key]!
  if (BLOCKER_TAG_TO_USER[tag]) return BLOCKER_TAG_TO_USER[tag]!
  for (const [k, v] of Object.entries(BLOCKER_TAG_TO_USER)) {
    if (tag.toUpperCase().includes(k) || k.includes(tag.toUpperCase())) return v
  }
  if (packId) {
    return pickTopicLine(
      LEARNING_PLAN_TOPIC_DECISION[packId].blockerCandidates,
      tag || packId
    )
  }
  return '当前最卡的那一步还没压实'
}

function stateLabelShort(tag: string, blockerText: string): string {
  const key = tag.toUpperCase().replace(/\s+/g, '_')
  if (BLOCKER_TAG_TO_STATE_LABEL[key]) return BLOCKER_TAG_TO_STATE_LABEL[key]!
  const t = tag.trim()
  if (t.length <= 12 && /[\u4e00-\u9fff]/.test(t)) return t
  return truncate(blockerText.replace(/。$/, ''), 14)
}

function strategyActionText(plan: PlanPreviewData): string {
  const raw = plan.recommendedStrategy?.code?.trim()
  if (raw) {
    const u = raw.toUpperCase().replace(/-/g, '_')
    if (STRATEGY_CODE_TO_ACTION[u]) return STRATEGY_CODE_TO_ACTION[u]!
    for (const [k, v] of Object.entries(STRATEGY_CODE_TO_ACTION)) {
      if (u.includes(k) || k.includes(u)) return v
    }
  }
  const label = plan.recommendedStrategy?.label?.trim()
  if (label && !/[A-Z_]{4,}/.test(label)) {
    return truncate(label.replace(/^先/, ''), 40)
  }
  const reason = plan.recommendedStrategy?.reason?.trim()
  if (reason) return truncate(reason.replace(/^先/, ''), 48)
  return '把眼前这一小步走稳'
}

function titleToActionSentence(
  title: string,
  packId: KnowledgePackId | null,
  seed: string
): string {
  const t = title?.trim() || ''
  if (!t) {
    return packId
      ? pickTopicLine(
          LEARNING_PLAN_TOPIC_DECISION[packId].heroCorrectActionCandidates,
          seed
        )
      : '先用一句话讲清这一步到底要交付什么'
  }
  if (packId) {
    const pack = LEARNING_PLAN_TOPIC_DECISION[packId]
    const lower = t.toLowerCase()
    for (const c of pack.heroCorrectActionCandidates) {
      if (lower.includes('dfs') && lower.includes('bfs') && c.includes('DFS')) return c
      if (lower.includes('tcp') && c.includes('TCP')) return c
      if (
        (lower.includes('进程') || lower.includes('线程')) &&
        c.includes('进程')
      )
        return c
      if (lower.includes('缓存') && c.includes('缓存')) return c
    }
    const picked = pickTopicLine(pack.heroCorrectActionCandidates, t + seed)
    if (picked) return picked
  }
  let s = t
    .replace(/^结构建立[：:]\s*/i, '')
    .replace(/^概念澄清[：:]\s*/i, '')
    .replace(/^基础修补[：:]\s*/i, '')
    .replace(/澄清/g, '讲清')
  if (!/^先/.test(s)) s = `先${s.replace(/^要/, '')}`
  return truncate(s, 56)
}

function goalTextOneLine(
  structuredGoal: StructuredLearningGoal | null | undefined,
  plan: PlanPreviewData,
  diagnosis: DiagnosisEvidenceSummary | null | undefined
): string {
  const g = workflowTopicLabelFromStructuredGoal(structuredGoal ?? null)
  if (g) return g
  const pg = plan.goal?.trim()
  if (pg) return truncate(pg, 48)
  const sum = diagnosis?.summary?.trim()
  if (sum) return truncate(sum.split(/[。\n]/)[0] || sum, 48)
  return '把这一轮学习目标拆成能执行的一小步'
}

function ensureYouNowPrefix(s: string): string {
  const t = s.trim()
  if (/^你现在|^你主要|^你当前/.test(t)) return t
  return `你现在${t.replace(/^你/, '')}`
}

function buildCurrentStateSentence(
  plan: PlanPreviewData,
  profile: LearnerProfileSnapshot | null | undefined,
  diagnosis: DiagnosisEvidenceSummary | null | undefined
): string {
  const d = diagnosis?.summary?.trim()
  if (d) {
    const line = truncate(d.split(/[。\n]/)[0] || d, 72)
    return ensureYouNowPrefix(line.endsWith('。') ? line : `${line}。`)
  }

  const parts: string[] = []
  const fl = profile?.foundationLevel?.trim()
  if (fl) parts.push(`基础自评偏${fl}`)
  const lp = profile?.learningPreference?.trim()
  if (lp) parts.push(`学习偏好：${lp}`)
  const es = profile?.executionStability?.trim()
  if (es) parts.push(`执行稳定性：${es}`)

  const rs = plan.recommendedStrategy?.reason?.trim()
  if (rs) return ensureYouNowPrefix(truncate(rs, 72))

  const ev = plan.keyEvidence?.filter((x) => x?.trim()) ?? []
  if (ev.length) {
    return ensureYouNowPrefix(truncate(ev.slice(0, 2).join('；'), 72))
  }

  if (parts.length) return `你现在${parts.join('，')}。`
  return '你现在不是完全不会，而是关键一步还没对齐到行动上。'
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

/** 策略因果区第三句：只填「结果收益」短片段，模板里会加「这样后面才」 */
function buildCausalOutcomeFragment(
  plan: PlanPreviewData,
  stage: PlanStageCode,
  packId: KnowledgePackId | null
): string {
  if (packId) {
    const eg = getPhaseWorkbenchCopy(packId, stage)?.expectedGain
    if (eg) return truncate(eg.replace(/。$/, ''), 44)
  }
  const entryReason = plan.recommendedEntry?.reason?.trim()
  if (entryReason) {
    const one = entryReason.split(/[。\n]/)[0]?.trim()
    if (one && one.length <= 56) return truncate(one.replace(/。$/, ''), 56)
  }
  const risks = plan.risks?.filter((x) => x?.trim()) ?? []
  if (risks.length) {
    return `不容易在「${truncate(risks[0]!, 18)}」上反复栽跟头`
  }
  return truncate(
    STAGE_GUIDE_META[stage].skipRisk.replace(/^这一步/, '少').replace(/。$/, ''),
    48
  )
}

function buildFirstTaskBenefit(
  plan: PlanPreviewData,
  stage: PlanStageCode,
  packId: KnowledgePackId | null
): string {
  if (packId) {
    const eg = getPhaseWorkbenchCopy(packId, stage)?.expectedGain
    if (eg) return truncate(eg, 72)
  }
  const why = plan.recommendedEntry?.reason?.trim()
  if (why) return truncate(why, 72)
  return buildCausalOutcomeFragment(plan, stage, packId)
}

function buildInefficiencyResult(
  plan: PlanPreviewData,
  stage: PlanStageCode,
  packId: KnowledgePackId | null,
  seed: string
): string {
  if (packId) {
    const line = pickTopicLine(
      LEARNING_PLAN_TOPIC_DECISION[packId].wrongPathResultLines,
      seed
    )
    if (line) return line
  }
  const wb = packId ? getPhaseWorkbenchCopy(packId, stage) : null
  if (wb?.skipRisk) return wb.skipRisk
  const r = plan.risks?.find((x) => x?.trim())
  if (r) return truncate(r, 48)
  return STAGE_GUIDE_META[stage].skipRisk
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
  const gapKind = primaryTag
    ? gapKindFromTag(primaryTag)
    : gapKindFromTag(
        `${plan.recommendedStrategy?.code ?? ''} ${(plan.risks ?? []).join(' ')}`
      )

  const blockerText = blockerUserText(primaryTag || 'GENERIC', packId)
  const wrongActionText = wrongActionForGap(gapKind)
  const entryTitle = plan.recommendedEntry?.title?.trim() || ''
  const correctActionText = titleToActionSentence(
    entryTitle,
    packId,
    primaryTag + entryTitle
  )
  const goalText = goalTextOneLine(
    ctx.structuredGoal,
    plan,
    ctx.diagnosisEvidenceSummary
  )
  const goalHint =
    goalText && goalText !== '把这一轮学习目标拆成能执行的一小步'
      ? `目标：${goalText}`
      : undefined

  const est =
    plan.recommendedEntry?.estimatedMinutes && plan.recommendedEntry.estimatedMinutes > 0
      ? plan.recommendedEntry.estimatedMinutes
      : fallbackMinutes(stage)
  const estimatedTimeText = `预计 ${est} 分钟`

  const benefitText = buildFirstTaskBenefit(plan, stage, packId)

  const stateShort = stateLabelShort(primaryTag || 'GENERIC', blockerText)
  const ineffResult = buildInefficiencyResult(plan, stage, packId, primaryTag + entryTitle)
  const riskText = `以你现在「${stateShort}」的情况，直接 ${wrongActionText} 会 ${ineffResult}`

  const pathStages: LearningPlanDecisionViewModel['pathPreview']['stages'] = (
    ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'] as const
  ).map((key) => ({
    key,
    label: PATH_STAGE_COPY[key].label,
    description: PATH_STAGE_COPY[key].description,
    active: key === stage,
  }))

  return {
    hero: {
      title: '这是给你的学习起点',
      goalHint,
      blockerText,
      wrongActionText,
      correctActionText,
      ctaLabel: '开始第一步',
      ctaSubtext: '先完成 1 个小任务，系统再带你往后走',
    },
    reason: {
      title: '为什么先处理这个',
      goalText,
      blockerText,
      consequenceText: '这个点不先处理，后面会越学越散',
    },
    causal: {
      title: '为什么这样安排',
      currentState: buildCurrentStateSentence(
        plan,
        ctx.learnerProfileSnapshot,
        ctx.diagnosisEvidenceSummary
      ),
      strategyAction: strategyActionText(plan),
      expectedResult: buildCausalOutcomeFragment(plan, stage, packId),
    },
    firstTask: {
      title: '第一步要做什么',
      intro: '先完成这一件事。',
      actionText: titleToActionSentence(entryTitle, packId, entryTitle + 'task'),
      estimatedTimeText,
      benefitText,
    },
    pathPreview: {
      title: '接下来怎么推进',
      stages: pathStages,
    },
    contrast: {
      title: '为什么不先那样学',
      riskText,
      betterPathText: `先按现在这条路径走，能先把 ${blockerText.replace(/。$/, '')} 处理掉。`,
    },
  }
}
