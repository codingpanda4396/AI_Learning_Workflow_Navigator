export interface WeakPointItem {
  nodeId: number;
  nodeName: string;
  masteryScore?: number;
  trainingAccuracy?: number;
  latestEvaluationScore?: number;
  attemptCount?: number;
  recentErrorTags: string[];
  reasons: string[];
}

export interface ReportQuestionResult {
  questionId: number;
  type?: string;
  stem: string;
  userAnswer?: string;
  score?: number;
  correct?: boolean;
  feedback?: string;
  errorTags: string[];
}

export interface NextStepRecommendation {
  recommendedAction?: string;
  reason?: string;
  targetNodeId?: number;
  targetNodeName?: string;
  targetTaskType?: string;
  confidence?: number;
}

export interface LearningReport {
  sessionId: number;
  taskId?: number;
  nodeId?: number;
  nodeName?: string;
  stageCode?: string;
  stageLabel?: string;
  overallScore?: number;
  overallAccuracy?: number;
  correctCount?: number;
  questionCount?: number;
  diagnosisSummary?: string;
  overallSummary?: string;
  strengths: string[];
  weaknesses: string[];
  reviewFocus: string[];
  weakPoints: WeakPointItem[];
  questionResults: ReportQuestionResult[];
  recommendedAction?: string;
  suggestedNextAction?: string;
  selectedAction?: string;
  nextRoundAdvice?: string;
  nextStep?: NextStepRecommendation | null;
  growthRecorded?: boolean;
}

export interface GrowthDashboardNode {
  nodeId: number;
  nodeName: string;
  masteryScore?: number;
  masteryStatus?: string;
  current?: boolean;
  recommended?: boolean;
}

export interface GrowthDashboard {
  sessionId: number;
  courseId?: string;
  chapterId?: string;
  learnedNodeCount?: number;
  masteredNodeCount?: number;
  averageMasteryScore?: number;
  currentNodeId?: number;
  currentNodeName?: string;
  currentStageCode?: string;
  currentStageLabel?: string;
  topWeakPoints: string[];
  recentPerformance?: {
    attemptCount?: number;
    averageScore?: number;
    latestScore?: number;
    topErrorTags: string[];
  };
  recommendedNextStep?: NextStepRecommendation | null;
  masteryNodes: GrowthDashboardNode[];
}
