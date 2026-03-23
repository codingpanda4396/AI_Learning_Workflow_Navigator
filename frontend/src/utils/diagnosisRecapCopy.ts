import type { LearnerProfileSnapshot } from '@/types/dto'

/** 诊断提交成功后写入；规划页读后清除，用于一次性「诊断总结」卡片 */
export const SESSION_KEY_PLAN_DIAGNOSIS_RECAP = 'plan_show_diagnosis_recap'

function foundationLine(level: string | undefined): string {
  switch (level) {
    case 'BEGINNER':
      return '你对这块还比较陌生，我们会从最容易跟上的一步带你入门。'
    case 'BASIC':
      return '你已经接触过这个知识点，但理解还不够扎实。'
    case 'INTERMEDIATE':
      return '你已经有一定基础，接下来更适合把零散知识串起来。'
    case 'SOLID':
      return '你整体掌握得不错，这一轮更偏向查漏补缺和提速。'
    default:
      return '我已经根据你的选择，大致摸清了你的起点。'
  }
}

const BLOCKER_LINE_PREFIX = '当前最卡的是：'

function blockerLine(blockingPoint: string | undefined): string {
  switch (blockingPoint) {
    case 'CONCEPT_GAP':
      return '当前最卡的是：概念还没在脑子里站稳。'
    case 'PROCEDURE_GAP':
      return '当前最卡的是：看懂讲解和自己动手之间还差一截。'
    case 'QUESTION_TYPE_RECOGNITION_GAP':
      return '当前最卡的是：题型一变就容易懵，缺少稳定的判断习惯。'
    case 'RELATIONSHIP_GAP':
      return '当前最卡的是：知识点之间怎么连起来，还不太有感觉。'
    case 'EXPRESSION_GAP':
      return '当前最卡的是：心里大概明白，但说不清楚、写不顺。'
    default:
      return '当前最卡的是：需要先把「从哪下手」说清楚，再往下练。'
  }
}

/** 规划决策页「你的问题」一行（去掉「当前最卡的是：」前缀，避免与标题重复） */
export function buildPlanProblemOneLiner(
  snapshot: LearnerProfileSnapshot | null | undefined
): string {
  if (!snapshot) return '先对齐学习起点'
  const full = blockerLine(snapshot.blockingPoint)
  return full.startsWith(BLOCKER_LINE_PREFIX)
    ? full.slice(BLOCKER_LINE_PREFIX.length)
    : full
}

function riskOfRushingLine(blockingPoint: string | undefined): string {
  switch (blockingPoint) {
    case 'CONCEPT_GAP':
    case 'RELATIONSHIP_GAP':
      return '如果直接刷题，很容易越做越乱，先稳住主线更划算。'
    case 'PROCEDURE_GAP':
    case 'QUESTION_TYPE_RECOGNITION_GAP':
      return '如果跳过动手示范，很容易卡在「看懂但不会做」的循环里。'
    case 'EXPRESSION_GAP':
      return '如果只靠刷题不复盘表达，很容易停留在似懂非懂。'
    default:
      return '如果步子跨太大，容易累还容易泄气，我们先把这一小步走稳。'
  }
}

/** 规划页「诊断总结」卡片内的短句列表（不含标题与收尾） */
export function buildDiagnosisRecapBullets(
  snapshot: LearnerProfileSnapshot | null | undefined
): string[] {
  if (!snapshot) {
    return [
      '我已经记住了你刚才的选择。',
      '接下来会按更适合你的节奏，把学习拆成一小步一小步。',
      '你不用自己想「下一步该干嘛」，跟着走就行。',
    ]
  }
  return [
    foundationLine(snapshot.foundationLevel),
    blockerLine(snapshot.blockingPoint),
    riskOfRushingLine(snapshot.blockingPoint),
  ]
}

const CLOSING_RECAP =
  '所以我帮你安排了一条更稳的学习路径'

export function diagnosisRecapClosingLine(): string {
  return CLOSING_RECAP
}

/** 规划页标题下「你现在是：…」单行摘要 */
export function buildUserStateSummaryLine(
  snapshot: LearnerProfileSnapshot | null | undefined
): string {
  if (!snapshot?.foundationLevel && !snapshot?.blockingPoint) {
    return '先跟我把眼前这一小步走稳，你会越来越有把握。'
  }
  const parts: string[] = []
  switch (snapshot.foundationLevel) {
    case 'BEGINNER':
      parts.push('刚开始接触这一块')
      break
    case 'BASIC':
      parts.push('学过一点，但还不够清晰')
      break
    case 'INTERMEDIATE':
      parts.push('有基础，想把它串得更牢')
      break
    case 'SOLID':
      parts.push('整体不错，想再精进一点')
      break
    default:
      parts.push('正在找准你的节奏')
  }
  switch (snapshot.blockingPoint) {
    case 'CONCEPT_GAP':
      parts.push('需要先把手上的概念理顺')
      break
    case 'PROCEDURE_GAP':
      parts.push('更需要有人带一遍「怎么做」')
      break
    case 'QUESTION_TYPE_RECOGNITION_GAP':
      parts.push('需要先练「看见题先判断方向」')
      break
    case 'RELATIONSHIP_GAP':
      parts.push('更需要把几块知识连起来看')
      break
    case 'EXPRESSION_GAP':
      parts.push('更需要把想法说清楚、写清楚')
      break
    default:
      parts.push('我们会先补短板再往前推')
  }
  return parts.join('；')
}

export function shouldShowDiagnosisRecapFromSession(): boolean {
  try {
    return sessionStorage.getItem(SESSION_KEY_PLAN_DIAGNOSIS_RECAP) === '1'
  } catch {
    return false
  }
}

export function clearDiagnosisRecapSessionFlag(): void {
  try {
    sessionStorage.removeItem(SESSION_KEY_PLAN_DIAGNOSIS_RECAP)
  } catch {
    /* ignore */
  }
}
