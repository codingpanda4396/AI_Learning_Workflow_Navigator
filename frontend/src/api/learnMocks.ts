import type { SessionOverview } from '@/types/session';
import type { TaskDetail, TaskRunResult } from '@/types/task';
import type { QuizSnapshot } from '@/types/quiz';
import type { LearningReport, GrowthDashboard } from '@/types/feedback';

const MOCK_SESSION_ID = 88;
const MOCK_TASK_ID = 201;

export const sessionOverviewMock: SessionOverview = {
  sessionId: MOCK_SESSION_ID,
  planInstanceId: 1,
  courseId: 'course-1',
  chapterId: 'chapter-1',
  goalText: '掌握本章核心概念',
  currentNodeId: 101,
  currentStage: 'TRAINING',
  sessionStatus: 'LEARNING',
  timeline: [
    { taskId: 200, stage: 'UNDERSTANDING', nodeId: 100, status: 'SUCCEEDED' },
    { taskId: MOCK_TASK_ID, stage: 'TRAINING', nodeId: 101, status: 'RUNNING' },
  ],
  nextTask: { taskId: MOCK_TASK_ID, stage: 'TRAINING', nodeId: 101 },
  masterySummary: [
    { nodeId: 100, nodeName: '基础概念', masteryValue: 0.8 },
  ],
  progress: { completedTaskCount: 1, totalTaskCount: 3, completionRate: 33 },
  summary: {
    currentTaskTitle: '阶段训练',
    currentTaskDescription: '完成当前节点练习',
    nextStepHint: '进入训练或查看报告',
    primaryActionLabel: '进入训练',
    primaryActionPath: `/learn/${MOCK_SESSION_ID}/training`,
    recentReportSummary: '',
  },
};

export const taskDetailMock: TaskDetail = {
  taskId: MOCK_TASK_ID,
  sessionId: MOCK_SESSION_ID,
  nodeId: 101,
  nodeName: '核心概念',
  stage: 'TRAINING',
  objective: '巩固理解',
  status: 'RUNNING',
  hasOutput: false,
};

export const taskRunResultMock: TaskRunResult = {
  taskId: MOCK_TASK_ID,
  stage: 'TRAINING',
  nodeId: 101,
  status: 'RUNNING',
  generationMode: 'MOCK',
  output: { title: ' mock 任务', steps: [] },
};

export const quizSnapshotMock: QuizSnapshot = {
  sessionId: MOCK_SESSION_ID,
  taskId: MOCK_TASK_ID,
  quizId: 1,
  generationStatus: 'SUCCEEDED',
  quizStatus: 'READY',
  questionCount: 2,
  answeredCount: 0,
  questions: [
    { questionId: 1, stem: '示例题目 A', options: ['A', 'B', 'C'] },
    { questionId: 2, stem: '示例题目 B', options: ['X', 'Y'] },
  ],
};

export const learningReportMock: LearningReport = {
  sessionId: MOCK_SESSION_ID,
  taskId: MOCK_TASK_ID,
  nodeId: 101,
  nodeName: '核心概念',
  stageCode: 'TRAINING',
  overallScore: 0.75,
  overallAccuracy: 0.75,
  correctCount: 1,
  questionCount: 2,
  strengths: ['概念理解尚可'],
  weaknesses: ['需多练巩固'],
  reviewFocus: ['重点回顾'],
  weakPoints: [],
  stepEvidence: [],
  questionResults: [],
};

export const growthDashboardMock: GrowthDashboard = {
  sessionId: MOCK_SESSION_ID,
  courseId: 'course-1',
  chapterId: 'chapter-1',
  learnedNodeCount: 1,
  masteredNodeCount: 0,
  averageMasteryScore: 0.5,
  topWeakPoints: [],
  masteryNodes: [
    { nodeId: 100, nodeName: '基础概念', masteryScore: 0.8, masteryStatus: 'LEARNING' },
  ],
};

export function getTaskDetailMock(taskId: number): TaskDetail {
  return { ...taskDetailMock, taskId, sessionId: MOCK_SESSION_ID };
}

export function getTaskRunResultMock(taskId: number): TaskRunResult {
  return { ...taskRunResultMock, taskId };
}

export function getSessionOverviewMock(sessionId: number): SessionOverview {
  return { ...sessionOverviewMock, sessionId };
}

export function getQuizSnapshotMock(sessionId: number): QuizSnapshot {
  return { ...quizSnapshotMock, sessionId };
}

export function getLearningReportMock(sessionId: number): LearningReport {
  return { ...learningReportMock, sessionId };
}

export function getGrowthDashboardMock(sessionId: number): GrowthDashboard {
  return { ...growthDashboardMock, sessionId };
}
