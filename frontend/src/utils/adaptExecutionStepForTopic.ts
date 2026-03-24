import type { ExecutionStep } from '@/types/execution'

const PLACEHOLDER = '__TOPIC__'

/** 用于 aiTutor / 埋点等稳定 key，仅字母数字与下划线 */
export function slugifyTopicKey(label: string): string {
  const s = label
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
  return s || 'topic'
}

function fill(s: string, topicLabel: string): string {
  return s.split(PLACEHOLDER).join(topicLabel)
}

/**
 * 将步配置中的 __TOPIC__ 占位符替换为用户选题；并设置 knowledgeKey。
 */
export function adaptExecutionStepForTopic(
  base: ExecutionStep,
  topicLabel: string
): ExecutionStep {
  const t = topicLabel.trim() || '当前主题'
  const slug = slugifyTopicKey(t)

  const mapStrings = (arr: string[] | undefined) =>
    arr?.map((x) => fill(x, t)) ?? []

  return {
    ...base,
    knowledgeKey: `demo_${slug}`,
    knowledgePoint: fill(base.knowledgePoint ?? '', t),
    title: fill(base.title, t),
    goal: fill(base.goal, t),
    prompt: fill(base.prompt, t),
    promptWhyTitle: base.promptWhyTitle
      ? fill(base.promptWhyTitle, t)
      : base.promptWhyTitle,
    promptWhyBullets: mapStrings(base.promptWhyBullets),
    reflectionQuestions: mapStrings(base.reflectionQuestions),
    inputHint: base.inputHint ? fill(base.inputHint, t) : base.inputHint,
    inputPlaceholder: base.inputPlaceholder
      ? fill(base.inputPlaceholder, t)
      : base.inputPlaceholder,
    completionHeadline: base.completionHeadline
      ? fill(base.completionHeadline, t)
      : base.completionHeadline,
    completionAchievements: mapStrings(base.completionAchievements),
    completionNextHint: base.completionNextHint
      ? fill(base.completionNextHint, t)
      : base.completionNextHint,
  }
}
