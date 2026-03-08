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
  error_tags?: string[]
  feedback?: {
    diagnosis?: string
    fixes?: Array<string | { fix?: string }>
  }
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

function parseSections(output: unknown) {
  if (!output || typeof output !== 'object') {
    return []
  }

  const rawSections = (output as { sections?: unknown }).sections
  if (!Array.isArray(rawSections)) {
    return []
  }

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
    errorTags: dto.error_tags ?? [],
    feedback: {
      diagnosis: dto.feedback?.diagnosis ?? '',
      fixes: parseFixes(dto.feedback?.fixes),
    },
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
