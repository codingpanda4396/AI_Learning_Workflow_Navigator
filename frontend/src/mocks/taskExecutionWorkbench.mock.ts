import { createEmptyWorkbenchModel } from '@/utils/buildExecutionPageModel'
import type { TaskExecutionWorkbenchModel } from '@/types/taskExecutionWorkbench'
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
      whatToOutput: ['用 3～4 句描述 BFS/DFS 在图上的推进差异'],
      recommendedSteps: ['先想队列弹出顺序', '再想 DFS 栈上状态'],
      avoid: ['不要只背定义'],
      startActionLabel: '我试着解释，你帮我检查',
    },
    whyThisStep: {
      whyNow: '机制不清，刷题会变成背模板。',
      skipRisk: '边界与最短路条件一换就错。',
      expectedGain: '你能解释为什么某题必须用 BFS/DFS。',
    },
    stageRules: { rules: ['追机制与因果，不背表面定义', '用最小例子串起来'] },
    topicHints: {
      topicDisplayName: 'DFS / BFS',
      bullets: ['搜索顺序', '栈 vs 队列', '适用题型'],
      visualVariant: 'graph',
    },
    stageMini: {
      roundLabel: '第 2 / 5 轮',
      actionsDone: 2,
      actionsTarget: 5,
      untilNextPhase: '完成后进入轻量检查。',
      passedGate: false,
    },
    currentTask: {
      phaseDisplayZh: '机制理解',
      phaseCode: 'UNDERSTANDING',
      taskTitle: '再补一句关键理解',
      coreActionLine: '搞清楚它是怎么工作的',
      completionLines: ['能补上关键因果', '能举例'],
    },
    emphasisPhase: 'UNDERSTANDING',
    ...overrides,
  }
}
