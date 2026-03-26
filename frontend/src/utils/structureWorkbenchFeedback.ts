import type { ValidationResult } from '@/types/scaffoldEngine'

export type StructureFeedbackTone =
  | 'empty'
  | 'too_short'
  | 'missing_dimension'
  | 'implementation_drift'
  | 'pass'
  | 'server_reject'

export interface StructureFeedbackBlock {
  tone: StructureFeedbackTone
  title: string
  body: string
}

const MIN_CHARS = 14

/** 与 DfsBfsStructureValidator.BOUNDARY_TERMS_ZH + CODEISH 大致对齐的前端提示 */
const IMPL_PATTERN =
  /复杂度|时间复杂度|空间复杂度|O\s*\(|o\s*\(|递归|递归栈|\b栈\b|\b队列\b|最短路|最短路径|dijkstra|刷题|leetcode|力扣|题解|模板|代码|实现|伪代码|入队|出队|回溯|public\s+static|void\s+main|import\s+java|\bdef\b|\bint\s+\w+\s*[=;]|for\s*\(|while\s*\(|#include/i

function countSentences(text: string): number {
  const t = text.trim()
  if (!t) return 0
  const rough = t.split(/[。！？.!?]+/).map((s) => s.trim()).filter(Boolean)
  return Math.max(rough.length, t.length > 0 ? 1 : 0)
}

function hasTopicAnchors(text: string): boolean {
  const t = text.toLowerCase()
  return (
    /dfs|bfs|深度|广度|图/.test(t) &&
    (/搜索|遍历|走路|可达|连通|层次|扩展/.test(t) || /dfs|bfs/.test(t))
  )
}

/** 首卡：与 spec 文案一致 */
export const PROBLEM_FEEDBACK_COPY: Record<
  StructureFeedbackTone,
  { title: string; body: string }
> = {
  empty: {
    title: '',
    body: '先写出你的理解，再提交本轮。',
  },
  too_short: {
    title: '还需要补充',
    body: '当前回答太短。请至少写 2 句话，并说明它们“解决什么问题”或“适合什么场景”。',
  },
  missing_dimension: {
    title: '还差一点',
    body: '请补上“位置 / 作用 / 场景 / 差异”中的至少一个角度，不要只写“我不知道”或一句结论。',
  },
  implementation_drift: {
    title: '方向偏了',
    body: '这一轮先不要讲递归、队列或代码流程，先回答它们在知识体系里“是做什么的”。',
  },
  pass: {
    title: '本轮达标',
    body: '你已经说清了 DFS / BFS 的用途和差异，可以进入下一步。',
  },
  server_reject: {
    title: '还需要补充',
    body: '',
  },
}

function localFeedbackForDraftProblem(draft: string): StructureFeedbackBlock | null {
  const raw = draft.trim()
  if (!raw) {
    return { tone: 'empty', title: '', body: PROBLEM_FEEDBACK_COPY.empty.body }
  }
  if (IMPL_PATTERN.test(raw)) {
    return {
      tone: 'implementation_drift',
      title: PROBLEM_FEEDBACK_COPY.implementation_drift.title,
      body: PROBLEM_FEEDBACK_COPY.implementation_drift.body,
    }
  }
  if (raw.length < MIN_CHARS || countSentences(raw) < 2) {
    return {
      tone: 'too_short',
      title: PROBLEM_FEEDBACK_COPY.too_short.title,
      body: PROBLEM_FEEDBACK_COPY.too_short.body,
    }
  }
  if (!hasTopicAnchors(raw) || raw.length < 28) {
    return {
      tone: 'missing_dimension',
      title: PROBLEM_FEEDBACK_COPY.missing_dimension.title,
      body: PROBLEM_FEEDBACK_COPY.missing_dimension.body,
    }
  }
  return null
}

function mapServerValidation(v: ValidationResult): StructureFeedbackBlock | null {
  if (v.passed) {
    return {
      tone: 'pass',
      title: PROBLEM_FEEDBACK_COPY.pass.title,
      body: PROBLEM_FEEDBACK_COPY.pass.body,
    }
  }
  const et = v.errorType
  if (et === 'INSUFFICIENT_CONTENT') {
    return {
      tone: 'too_short',
      title: PROBLEM_FEEDBACK_COPY.too_short.title,
      body: v.message?.trim() || PROBLEM_FEEDBACK_COPY.too_short.body,
    }
  }
  if (et === 'BOUNDARY_VIOLATION') {
    return {
      tone: 'implementation_drift',
      title: PROBLEM_FEEDBACK_COPY.implementation_drift.title,
      body: v.message?.trim() || PROBLEM_FEEDBACK_COPY.implementation_drift.body,
    }
  }
  return {
    tone: 'server_reject',
    title: '还需要调整',
    body: v.message?.trim() || '请按右侧提示改写后再提交。',
  }
}

export interface FeedbackOptions {
  /** dfs_bfs_structure_problem 使用 spec 本地启发；其余卡主要依赖服务端 */
  mode: 'problem' | 'generic'
  draft: string
  lastValidation: ValidationResult | null | undefined
  /** 上次提交时的正文，用于判断用户是否已修改草稿 */
  lastSubmittedDraft: string | null | undefined
}

export function computeStructureWorkbenchFeedback(opts: FeedbackOptions): StructureFeedbackBlock | null {
  const { mode, draft, lastValidation, lastSubmittedDraft } = opts
  const trimmed = draft.trim()

  if (lastValidation?.passed) {
    return {
      tone: 'pass',
      title: PROBLEM_FEEDBACK_COPY.pass.title,
      body:
        mode === 'problem'
          ? PROBLEM_FEEDBACK_COPY.pass.body
          : '本步已通过校验，可以继续。',
    }
  }

  const unchangedAfterFail =
    lastValidation &&
    !lastValidation.passed &&
    lastSubmittedDraft != null &&
    trimmed === lastSubmittedDraft.trim()

  if (unchangedAfterFail && lastValidation) {
    const mapped = mapServerValidation(lastValidation)
    if (mapped) return mapped
  }

  if (mode === 'problem') {
    const local = localFeedbackForDraftProblem(draft)
    if (local) return local
    return null
  }

  if (!trimmed) {
    return { tone: 'empty', title: '', body: PROBLEM_FEEDBACK_COPY.empty.body }
  }
  if (trimmed.length < MIN_CHARS || countSentences(trimmed) < 2) {
    return {
      tone: 'too_short',
      title: '还需要补充',
      body: '当前回答太短。请至少写 2 句话，把要求点覆盖到。',
    }
  }
  if (IMPL_PATTERN.test(trimmed)) {
    return {
      tone: 'implementation_drift',
      title: '方向偏了',
      body: '这一轮先不要展开实现与题型细节，先完成结构层面的表述。',
    }
  }
  return null
}
