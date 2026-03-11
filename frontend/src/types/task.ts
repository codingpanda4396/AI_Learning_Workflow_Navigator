export interface TaskDetail {
  taskId: number;
  sessionId?: number;
  nodeId?: number;
  nodeName?: string;
  stage: string;
  objective?: string;
  status?: string;
  hasOutput?: boolean;
  output?: unknown;
}

export interface TaskRunResult {
  taskId: number;
  stage: string;
  nodeId?: number;
  status?: string;
  generationMode?: string;
  generationReason?: string;
  output?: unknown;
}
