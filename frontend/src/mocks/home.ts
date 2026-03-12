export type WorkflowStatus = 'DONE' | 'RUNNING' | 'PENDING';

export interface ActiveSession {
  id: string;
  goal: string;
  course: string;
  chapter: string;
  phase: string;
  currentTask: string;
  progress: number;
}

export interface WorkflowNode {
  key: string;
  label: string;
  status: WorkflowStatus;
}

export interface ModuleEntry {
  key: string;
  title: string;
  description: string;
  route: string;
}

export interface LearningSummary {
  metrics: Array<{
    key: string;
    label: string;
    value: string;
  }>;
  recentSessions: Array<{
    id: string;
    title: string;
    progressText: string;
  }>;
  recentEvaluation: {
    title: string;
    result: string;
  } | null;
  newKnowledge: string[];
}

export interface StartLearningForm {
  goal: string;
  course: string;
  chapter: string;
}

export const homeActiveSession: ActiveSession = {
  id: 'mock-active-session',
  goal: '掌握二叉树基础',
  course: '数据结构',
  chapter: '树',
  phase: '任务执行',
  currentTask: '理解二叉树的存储结构与遍历逻辑',
  progress: 40,
};

export const homeWorkflowNodes: WorkflowNode[] = [
  { key: 'diagnosis', label: '能力诊断', status: 'DONE' },
  { key: 'plan', label: '学习规划', status: 'DONE' },
  { key: 'task', label: '任务执行', status: 'RUNNING' },
  { key: 'evaluation', label: '训练评估', status: 'PENDING' },
  { key: 'knowledge', label: '知识沉淀', status: 'PENDING' },
  { key: 'growth', label: '动态演化', status: 'PENDING' },
];

export const homeModuleEntries: ModuleEntry[] = [
  { key: 'diagnosis', title: '能力诊断', description: '查看当前能力水平并定位薄弱点', route: '/diagnosis' },
  { key: 'plan', title: '学习规划', description: '生成个性化学习路径', route: '/plan' },
  { key: 'task', title: '任务执行', description: '进入当前学习任务', route: '/task' },
  { key: 'evaluation', title: '训练评估', description: '查看最近训练结果', route: '/evaluation' },
  { key: 'knowledge', title: '知识沉淀', description: '浏览你的知识图谱', route: '/knowledge' },
  { key: 'growth', title: '成长轨迹', description: '查看长期学习成长趋势', route: '/growth' },
];

export const homeLearningSummary: LearningSummary = {
  metrics: [
    { key: 'goals', label: '累计学习目标', value: '18' },
    { key: 'knowledge', label: '累计知识点', value: '126' },
    { key: 'accuracy', label: '平均正确率', value: '84%' },
  ],
  recentSessions: [
    { id: 'session-101', title: '二叉树遍历专项训练', progressText: '已完成 40%，当前正在执行任务' },
    { id: 'session-087', title: '栈与队列基础巩固', progressText: '已完成，当前进入知识沉淀' },
    { id: 'session-074', title: '线性表核心概念回顾', progressText: '已完成，整体掌握较稳定' },
  ],
  recentEvaluation: {
    title: '二叉树章节阶段测评',
    result: '正确率 82%，你对递归遍历掌握较好，层序遍历还可以继续练习。',
  },
  newKnowledge: ['二叉树顺序存储与链式存储区别', '前序 / 中序 / 后序遍历逻辑', '满二叉树与完全二叉树判定'],
};
