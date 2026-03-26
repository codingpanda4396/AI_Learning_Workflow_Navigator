import { createEmptyWorkbenchModel } from '@/utils/buildExecutionPageModel'
import type { TaskExecutionWorkbenchModel } from '@/types/taskExecutionWorkbench'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'
import { WORKBENCH_PHASE_SEQUENCE } from '@/constants/executionWorkbenchContent'

/** 仅用于 Story / 本地预览，不参与真实路由数据流 */
export function createMockWorkbenchState(
  overrides: Partial<TaskExecutionWorkbenchModel> = {}
): TaskExecutionWorkbenchModel {
  const base = createEmptyWorkbenchModel()
  return {
    ...base,
    packId: 'ds_dfs_bfs',
    phaseProgress: {
      phases: WORKBENCH_PHASE_SEQUENCE,
      currentPhase: 'UNDERSTANDING',
      overallRatio: 0.42,
      stepLabel: '2/5',
      taskIndexLabel: '任务 1/4',
    },
    taskStatus: 'running',
    taskStatusLabel: '进行中',
    scaffoldProduct: {
      whatToOutput: ['用 3～4 句说明 BFS/DFS 在图上的推进差异'],
      recommendedSteps: ['先看扩展顺序', '再说栈/队列与递归'],
      avoid: ['只背定义不画图'],
      startActionLabel: '用推荐句式开始',
    },
    whyThisStep: {
      whyNow: '机制不清，刷题易变背模板。',
      skipRisk: '边界一换就选错策略。',
      expectedGain: '能解释何时用 BFS 或 DFS。',
    },
    stageRules: { rules: ['追因果', '用最小例子'] },
    topicHints: {
      topicDisplayName: 'DFS / BFS',
      bullets: ['搜索顺序', '栈 vs 队列', '适用题型'],
      visualVariant: 'graph',
    },
    stageMini: {
      roundLabel: '第 2 / 5 轮',
      actionsDone: 2,
      actionsTarget: 5,
      untilNextPhase: '再完成一轮对话后进入检查。',
      passedGate: false,
    },
    currentTask: {
      phaseDisplayZh: '机制理解',
      phaseCode: 'UNDERSTANDING',
      taskTitle: '把机制讲清楚',
      coreActionLine: '解释清楚搜索如何推进、为何用不同结构',
      completionLines: ['补上关键因果', '一个最小例子', '一个还不稳的点'],
    },
    emphasisPhase: 'UNDERSTANDING',
    ...overrides,
  }
}

/** 便于按阶段 / 知识点切换预览 */
export const MOCK_WORKBENCH_BY_PHASE: Record<WorkbenchPhaseCode, Partial<TaskExecutionWorkbenchModel>> = {
  STRUCTURE: {
    currentTask: {
      phaseDisplayZh: '结构建立',
      phaseCode: 'STRUCTURE',
      taskTitle: '先搭知识骨架',
      coreActionLine: '说清它是什么、占什么位置',
      completionLines: ['一句话定位', '解决什么问题', '和相邻概念边界'],
    },
    emphasisPhase: 'STRUCTURE',
    scaffoldProduct: {
      whatToOutput: ['各一条：DFS/BFS 先扩展谁'],
      recommendedSteps: ['先画搜索方向', '再说栈/队列'],
      avoid: ['先贴代码'],
      startActionLabel: '从骨架开始',
    },
  },
  UNDERSTANDING: {
    emphasisPhase: 'UNDERSTANDING',
  },
  TRAINING: {
    currentTask: {
      phaseDisplayZh: '应用训练',
      phaseCode: 'TRAINING',
      taskTitle: '写完整、再被纠错',
      coreActionLine: '用表达暴露真问题',
      completionLines: ['自述一段', '对照反馈', '改一轮表述'],
    },
    emphasisPhase: 'TRAINING',
    taskStatus: 'needs_fix',
    taskStatusLabel: '待修正',
  },
  REFLECTION: {
    currentTask: {
      phaseDisplayZh: '反思校准',
      phaseCode: 'REFLECTION',
      taskTitle: '从错误抽规律',
      coreActionLine: '写一条下次可执行策略',
      completionLines: ['错因', '规律', '下次检查动作'],
    },
    emphasisPhase: 'REFLECTION',
  },
}
