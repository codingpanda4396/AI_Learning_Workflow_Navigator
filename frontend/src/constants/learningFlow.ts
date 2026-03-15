/**
 * 学习执行流程：统一术语与主按钮文案，禁止在 UI 暴露 session/quiz/report 等技术词。
 */
import type { LearningStage } from '@/domain/learning-flow/types';

/** 阶段 -> 用户可见标签 */
export const LEARNING_STAGE_LABELS: Record<LearningStage, string> = {
  DIAGNOSIS: '诊断',
  PLAN_PREVIEW: '学习计划确认',
  LEARNING_TASK: '当前学习任务',
  TRAINING: '练习',
  EVALUATION: '学习结果评估',
  NEXT_ACTION: '下一步建议',
};

/** 页面标题（与路由/职责对应） */
export const LEARNING_PAGE_TITLES: Record<LearningStage, string> = {
  DIAGNOSIS: '诊断',
  PLAN_PREVIEW: '学习计划确认',
  LEARNING_TASK: '当前学习任务',
  TRAINING: '练习',
  EVALUATION: '学习结果评估',
  NEXT_ACTION: '下一步建议',
};

/** 主按钮文案：每页唯一主 CTA，直接表达动作 */
export const PRIMARY_CTA = {
  CONTINUE_TASK: '继续当前任务',
  ENTER_TRAINING: '进入练习',
  ENTER_TRAINING_AFTER_DONE: '我已完成，进入练习',
  START_TRAINING: '开始练习',
  SUBMIT_TRAINING: '提交练习',
  VIEW_NEXT_SUGGESTION: '查看下一步建议',
  START_NEXT: '开始下一步',
} as const;

/** 次按钮 / 通用 */
export const SECONDARY_LABELS = {
  BACK_TO_PROGRESS: '返回当前进度',
  VIEW_RECORDS: '查看学习记录',
  RETRY: '重新加载',
} as const;

/** 状态面板文案：简洁、可执行 */
export const STATE_COPY = {
  LOADING: '加载中',
  LOADING_HINT: '稍候',
  EMPTY: '暂无内容',
  EMPTY_HINT: '请按提示操作',
  ERROR: '加载失败',
  ERROR_HINT: '请点击下方按钮重试',
  BLOCKED: '暂时无法继续',
  BLOCKED_HINT: '请返回当前进度或稍后重试',
} as const;
