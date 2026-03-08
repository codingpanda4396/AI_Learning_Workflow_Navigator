import type { RunTaskResponse, SubmitTaskResponse, TaskDetailResponse } from '@/types'

interface RunTaskResponseDto {
  task_id: number
  stage: string
  node_id: number
  status: string
  output?: unknown
}

interface SubmitTaskResponseDto {
  task_id: number
  stage: string
  node_id: number
  score: number
  normalized_score?: number
  error_tags?: string[]
  feedback?: {
    diagnosis?: string
    fixes?: Array<string | { fix?: string }>
  }
  strengths?: string[]
  weaknesses?: string[]
  mastery_before: number
  mastery_delta: number
  mastery_after: number
  next_action: string
  next_task?: {
    task_id: number
    stage: string
    node_id: number
  } | null
}

interface TaskDetailResponseDto {
  task_id: number
  session_id: number
  node_id: number
  node_name: string
  stage: string
  objective: string
  status: string
  has_output: boolean
  output?: unknown
}

function prettifyKey(input: string): string {
  return input
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function toPrimitiveText(value: unknown): string | null {
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return null
}

function arrayToLines(items: unknown[]): string[] {
  const lines: string[] = []
  for (const item of items) {
    const primitive = toPrimitiveText(item)
    if (primitive !== null) {
      lines.push(primitive)
      continue
    }
    if (!item || typeof item !== 'object') {
      continue
    }
    const obj = item as Record<string, unknown>
    if (typeof obj.prompt === 'string') {
      lines.push(obj.prompt)
      continue
    }
    const summary = Object.entries(obj)
      .map(([key, value]) => {
        const text = toPrimitiveText(value)
        return text === null ? null : `${prettifyKey(key)}: ${text}`
      })
      .filter((entry): entry is string => Boolean(entry))
      .join(' | ')
    if (summary) {
      lines.push(summary)
    }
  }
  return lines
}

function parseSections(output: unknown) {
  if (!output || typeof output !== 'object') {
    return []
  }

  const outputObject = output as Record<string, unknown>
  const rawSections = outputObject.sections
  if (Array.isArray(rawSections)) {
    return rawSections
      .filter((section): section is Record<string, unknown> => Boolean(section) && typeof section === 'object')
      .map((section, index) => ({
        type: typeof section.type === 'string' ? section.type : 'summary',
        title:
          typeof section.title === 'string' && section.title.trim().length > 0
            ? section.title
            : `Section ${index + 1}`,
        bullets: Array.isArray(section.bullets) ? section.bullets.filter((item): item is string => typeof item === 'string') : undefined,
        steps: Array.isArray(section.steps) ? section.steps.filter((item): item is string => typeof item === 'string') : undefined,
        items: Array.isArray(section.items) ? section.items.filter((item): item is string => typeof item === 'string') : undefined,
        text: typeof section.text === 'string' ? section.text : undefined,
      }))
  }

  return Object.entries(outputObject).reduce<Array<{
    type: string
    title: string
    bullets?: string[]
    steps?: string[]
    items?: string[]
    text?: string
  }>>((sections, [key, value]) => {
    const title = prettifyKey(key)
    const primitive = toPrimitiveText(value)
    if (primitive !== null) {
      sections.push({ type: key, title, text: primitive })
      return sections
    }

    if (Array.isArray(value)) {
      const lines = arrayToLines(value)
      if (lines.length > 0) {
        const asSteps = key.includes('question') || key.includes('step')
        sections.push({
          type: key,
          title,
          ...(asSteps ? { steps: lines } : { bullets: lines }),
        })
      }
      return sections
    }

    if (value && typeof value === 'object') {
      const nested = arrayToLines([value])
      if (nested.length > 0) {
        sections.push({ type: key, title, items: nested })
      }
    }
    return sections
  }, [])
}

function parseFixes(fixes: Array<string | { fix?: string }> | undefined): string[] {
  if (!Array.isArray(fixes)) {
    return []
  }

  return fixes
    .map((item) => {
      if (typeof item === 'string') {
        return item
      }
      if (item && typeof item.fix === 'string') {
        return item.fix
      }
      return ''
    })
    .filter((item) => item.trim().length > 0)
}

export function mapRunTaskDto(dto: RunTaskResponseDto): RunTaskResponse {
  return {
    taskId: dto.task_id,
    stage: dto.stage,
    nodeId: dto.node_id,
    status: dto.status,
    output: {
      sections: parseSections(dto.output),
    },
  }
}

export function mapSubmitTaskDto(dto: SubmitTaskResponseDto): SubmitTaskResponse {
  return {
    taskId: dto.task_id,
    stage: dto.stage,
    nodeId: dto.node_id,
    score: dto.score,
    normalizedScore:
      typeof dto.normalized_score === 'number' && Number.isFinite(dto.normalized_score)
        ? dto.normalized_score
        : dto.score / 100,
    errorTags: dto.error_tags ?? [],
    feedback: {
      diagnosis: dto.feedback?.diagnosis ?? '',
      fixes: parseFixes(dto.feedback?.fixes),
    },
    strengths: Array.isArray(dto.strengths) ? dto.strengths.filter((item): item is string => typeof item === 'string') : [],
    weaknesses: Array.isArray(dto.weaknesses) ? dto.weaknesses.filter((item): item is string => typeof item === 'string') : [],
    masteryBefore: dto.mastery_before,
    masteryDelta: dto.mastery_delta,
    masteryAfter: dto.mastery_after,
    nextAction: dto.next_action,
    nextTask: dto.next_task
      ? {
          taskId: dto.next_task.task_id,
          stage: dto.next_task.stage,
          nodeId: dto.next_task.node_id,
        }
      : null,
  }
}

export function mapTaskDetailDto(dto: TaskDetailResponseDto): TaskDetailResponse {
  return {
    taskId: dto.task_id,
    sessionId: dto.session_id,
    nodeId: dto.node_id,
    nodeName: dto.node_name,
    stage: dto.stage,
    objective: dto.objective,
    status: dto.status,
    hasOutput: dto.has_output,
    output: {
      sections: parseSections(dto.output),
    },
  }
}
