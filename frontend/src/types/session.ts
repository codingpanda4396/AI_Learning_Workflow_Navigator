export interface SessionCreatePayload {
  courseId: string;
  chapterId: string;
  goalText: string;
}

export interface SessionProgress {
  completedTaskCount: number;
  totalTaskCount: number;
  completionRate: number;
}

export interface SessionTimelineItem {
  taskId: number;
  stage: string;
  nodeId?: number;
  status: string;
}

export interface SessionNextTask {
  taskId: number;
  stage: string;
  nodeId?: number;
}

export interface MasterySummaryItem {
  nodeId: number;
  nodeName: string;
  masteryValue?: number;
}

export interface SessionOverview {
  sessionId: number;
  courseId: string;
  chapterId: string;
  goalText?: string;
  currentNodeId?: number;
  currentStage?: string;
  timeline: SessionTimelineItem[];
  nextTask?: SessionNextTask | null;
  masterySummary: MasterySummaryItem[];
  progress: SessionProgress;
}

export interface CurrentSessionInfo {
  sessionId: number;
  courseId: string;
  chapterId: string;
  goalText?: string;
  currentNodeId?: number;
  currentStage?: string;
}
