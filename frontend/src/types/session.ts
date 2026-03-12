export type SessionBusinessStatus =
  | 'ANALYZING'
  | 'PLANNING'
  | 'LEARNING'
  | 'PRACTICING'
  | 'REPORT_READY'
  | 'COMPLETED'
  | 'FAILED';

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
  sessionStatus?: SessionBusinessStatus;
  timeline: SessionTimelineItem[];
  nextTask?: SessionNextTask | null;
  masterySummary: MasterySummaryItem[];
  progress: SessionProgress;
  summary?: {
    currentTaskTitle: string;
    currentTaskDescription: string;
    nextStepHint: string;
    primaryActionLabel: string;
    primaryActionPath: string;
    recentReportSummary: string;
  };
}

export interface CurrentSessionInfo {
  sessionId: number;
  courseId: string;
  chapterId: string;
  goalText?: string;
  currentNodeId?: number;
  currentStage?: string;
  sessionStatus?: SessionBusinessStatus;
}
