/**
 * 将 overview / task / quiz / report 数据映射为统一 flow snapshot。
 * UI 层不得再直接拿多接口字段拼流程判断。
 */

import type { LearningReport } from '@/types/feedback';
import type { QuizSnapshot } from '@/types/quiz';
import type { SessionOverview } from '@/types/session';
import type { TaskDetail, TaskRunResult } from '@/types/task';
import type {
  LearningFlowSnapshot,
  LearningStage,
  FlowPrimaryCTA,
  FlowTaskSummary,
  FlowTrainingState,
  FlowEvaluationState,
  FlowBlockedState,
} from './types';

function mapOverviewToStage(overview: SessionOverview | null): LearningStage {
  if (!overview) return 'NEXT_ACTION';
  const status = overview.sessionStatus ?? '';
  const stage = (overview.currentStage ?? '').toUpperCase();
  if (status === 'PRACTICING' || status === 'REPORT_READY') {
    return 'EVALUATION';
  }
  if (stage === 'TRAINING') return 'TRAINING';
  if (stage === 'STRUCTURE' || stage === 'UNDERSTANDING' || stage === 'REFLECTION') return 'LEARNING_TASK';
  return 'NEXT_ACTION';
}

function mapTaskSummary(overview: SessionOverview | null, detail: TaskDetail | null, result: TaskRunResult | null): FlowTaskSummary | null {
  const next = overview?.nextTask;
  if (detail) {
    return {
      taskId: detail.taskId,
      stage: detail.stage ?? result?.stage ?? '',
      nodeId: detail.nodeId ?? result?.nodeId,
      title: detail.objective,
    };
  }
  if (next) {
    return {
      taskId: next.taskId,
      stage: next.stage ?? '',
      nodeId: next.nodeId,
    };
  }
  return null;
}

function mapTrainingState(quiz: QuizSnapshot | null, quizViewStatus: string): FlowTrainingState {
  const status = quizViewStatus || 'idle';
  const canEnter =
    status === 'ready' ||
    status === 'answering' ||
    status === 'submitting' ||
    status === 'report-ready' ||
    status === 'next-round' ||
    status === 'generating';
  return {
    canEnter,
    status:
      status === 'generating'
        ? 'generating'
        : status === 'ready' || status === 'answering'
          ? status
          : status === 'submitting'
            ? 'submitting'
            : status === 'report-ready'
              ? 'report-ready'
              : status === 'next-round'
                ? 'next-round'
                : status === 'failed'
                  ? 'failed'
                  : 'idle',
    questionCount: quiz?.questions?.length ?? 0,
  };
}

function mapEvaluationState(report: LearningReport | null): FlowEvaluationState {
  return {
    canEnter: true,
    hasReport: Boolean(report && (report.overallSummary ?? report.diagnosisSummary ?? report.questionResults?.length)),
    recommendedAction: report?.recommendedAction ?? report?.nextStep?.recommendedAction,
  };
}

function mapPrimaryCTA(
  overview: SessionOverview | null,
  currentStage: LearningStage,
  canEnterTask: boolean,
  canEnterTraining: boolean,
  canEnterEvaluation: boolean
): FlowPrimaryCTA | null {
  const summary = overview?.summary;
  if (summary?.primaryActionLabel) {
    let stage: LearningStage = 'NEXT_ACTION';
    if (canEnterTask && (currentStage === 'NEXT_ACTION' || currentStage === 'LEARNING_TASK')) stage = 'LEARNING_TASK';
    else if (canEnterTraining) stage = 'TRAINING';
    else if (canEnterEvaluation) stage = 'EVALUATION';
    return {
      label: summary.primaryActionLabel,
      stage,
      hint: summary.nextStepHint,
    };
  }
  if (canEnterTask) return { label: '继续学习任务', stage: 'LEARNING_TASK', hint: summary?.nextStepHint };
  if (canEnterTraining) return { label: '进入学习检测', stage: 'TRAINING' };
  if (canEnterEvaluation) return { label: '查看反馈', stage: 'EVALUATION' };
  return { label: '继续下一步', stage: 'NEXT_ACTION', hint: summary?.nextStepHint };
}

export interface FlowMapperInput {
  sessionId: number;
  overview: SessionOverview | null;
  taskDetail: TaskDetail | null;
  taskResult: TaskRunResult | null;
  quizSnapshot: QuizSnapshot | null;
  quizViewStatus: string;
  report: LearningReport | null;
}

/**
 * 从各 store/API 数据构建统一 flow snapshot。
 */
export function toLearningFlowSnapshot(input: FlowMapperInput): LearningFlowSnapshot {
  const { sessionId, overview, taskDetail, taskResult, quizSnapshot, quizViewStatus, report } = input;
  const currentStage = mapOverviewToStage(overview);
  const taskSummary = mapTaskSummary(overview, taskDetail, taskResult);
  const currentTaskId = taskSummary?.taskId ?? overview?.nextTask?.taskId ?? null;
  const training = mapTrainingState(quizSnapshot, quizViewStatus);
  const evaluation = mapEvaluationState(report);

  const canEnterTask = Boolean(currentTaskId && overview?.sessionId === sessionId);
  const canEnterTraining = training.canEnter || currentStage === 'TRAINING';
  const canEnterEvaluation = evaluation.canEnter || evaluation.hasReport || currentStage === 'EVALUATION';

  const blocked: FlowBlockedState = {
    blocked: false,
  };
  if (overview?.sessionStatus === 'FAILED') {
    blocked.blocked = true;
    blocked.reason = '当前会话异常';
    blocked.recoverable = true;
  }

  const primaryCTA = mapPrimaryCTA(overview, currentStage, canEnterTask, canEnterTraining, canEnterEvaluation);

  return {
    sessionId,
    currentStage,
    currentTaskId,
    taskSummary,
    canEnterTask,
    canEnterTraining,
    canEnterEvaluation,
    primaryCTA,
    training,
    evaluation,
    blocked,
    legacyPrimaryActionPath: overview?.summary?.primaryActionPath,
  };
}
