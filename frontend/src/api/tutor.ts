import { request } from './request'

/** GET /api/ai-tutor/prompt、GET /api/ai-tutor/explain */
export interface AiTutorEnvelope {
  source: string
  content: string
}

export interface AiTutorExplainPostData {
  source: string
  explanation: string
}

/** 不阻塞：预取 explain 入缓存 */
export function postTutorPrefetch(step: string, knowledgePoint: string): void {
  void request
    .post<void>('/api/ai-tutor/prefetch', { step, knowledgePoint })
    .catch(() => {
      /* 忽略：主路径不依赖 prefetch 成功 */
    })
}

export async function getTutorPrompt(
  step: string,
  knowledgePoint: string
): Promise<AiTutorEnvelope> {
  const { data } = await request.get<AiTutorEnvelope>(
    '/api/ai-tutor/prompt',
    { params: { step, knowledgePoint } }
  )
  return data
}

export async function getTutorExplain(
  step: string,
  knowledgePoint: string
): Promise<AiTutorEnvelope> {
  const { data } = await request.get<AiTutorEnvelope>(
    '/api/ai-tutor/explain',
    { params: { step, knowledgePoint } }
  )
  return data
}

/** 兼容旧调用：POST explain */
export async function postTutorExplain(body: {
  step: string
  knowledgePoint: string
  userPrompt?: string
}): Promise<AiTutorExplainPostData> {
  const { data } = await request.post<AiTutorExplainPostData>(
    '/api/ai-tutor/explain',
    body
  )
  return data
}
