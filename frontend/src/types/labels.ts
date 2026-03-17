/**
 * 枚举 -> 中文文案映射
 */
import {
  TimeBudget,
  SelfReportedLevel,
  PreferenceTag,
  GoalType,
  TaskCompletionStatus,
  ResultStatus,
  NextActionType,
} from './enums'

export const timeBudgetLabels: Record<string, string> = {
  [TimeBudget.WITHIN_15_MIN]: '15 分钟内',
  [TimeBudget.WITHIN_30_MIN]: '30 分钟内',
  [TimeBudget.WITHIN_60_MIN]: '1 小时内',
  [TimeBudget.MULTI_DAY]: '多天',
  [TimeBudget.LONG_TERM]: '长期',
}

export const selfReportedLevelLabels: Record<string, string> = {
  [SelfReportedLevel.BEGINNER]: '完全新手',
  [SelfReportedLevel.BASIC]: '略懂皮毛',
  [SelfReportedLevel.PARTIAL_UNDERSTANDING]: '部分理解',
  [SelfReportedLevel.CAN_EXPLAIN_BUT_NOT_APPLY]: '能讲但不会用',
  [SelfReportedLevel.SOLID_BUT_WANT_IMPROVE]: '较扎实想提升',
}

export const preferenceTagLabels: Record<string, string> = {
  [PreferenceTag.CONCEPT_FIRST]: '先讲概念',
  [PreferenceTag.EXAMPLE_FIRST]: '先看例子',
  [PreferenceTag.PRACTICE_FIRST]: '先练再学',
  [PreferenceTag.STEP_BY_STEP]: '循序渐进',
  [PreferenceTag.FAST_TRACK]: '快速过',
  [PreferenceTag.FRAMEWORK_FIRST]: '先搭框架',
}

export const goalTypeLabels: Record<string, string> = {
  [GoalType.LEARN_NEW_CONCEPT]: '学习新概念',
  [GoalType.REVIEW_FOR_EXAM]: '考前复习',
  [GoalType.FIX_SPECIFIC_BLOCKER]: '解决具体卡点',
  [GoalType.PRACTICE_ENHANCEMENT]: '做题强化',
  [GoalType.BUILD_SYSTEMATIC_UNDERSTANDING]: '系统建立理解',
}

export const taskCompletionStatusLabels: Record<string, string> = {
  [TaskCompletionStatus.NOT_STARTED]: '未开始',
  [TaskCompletionStatus.IN_PROGRESS]: '进行中',
  [TaskCompletionStatus.COMPLETED]: '已完成',
  [TaskCompletionStatus.SKIPPED]: '已跳过',
  [TaskCompletionStatus.BLOCKED]: '已阻塞',
}

export const resultStatusLabels: Record<string, string> = {
  [ResultStatus.ACHIEVED]: '已达成',
  [ResultStatus.PARTIALLY_ACHIEVED]: '部分达成',
  [ResultStatus.NOT_ACHIEVED]: '未达成',
}

export const nextActionTypeLabels: Record<string, string> = {
  [NextActionType.CONTINUE]: '继续推进',
  [NextActionType.REINFORCE]: '巩固练习',
  [NextActionType.REMEDIATE_PREREQUISITE]: '补前置知识',
  [NextActionType.REDUCE_GRANULARITY]: '拆小任务',
  [NextActionType.CHANGE_STRATEGY]: '换学习策略',
}

export const errorCodeLabels: Record<string, string> = {
  RESOURCE_NOT_FOUND: '资源不存在，请从目标输入重新开始',
  INVALID_ARGUMENT: '参数无效',
  DIAGNOSIS_NOT_COMPLETED: '请先完成诊断',
  PLAN_NOT_COMMITTED: '请先确认学习计划',
  TASK_NOT_CURRENT: '任务状态异常',
  TASK_ALREADY_COMPLETED: '该任务已完成',
  SESSION_NOT_COMPLETED: '会话未完成',
  SESSION_ALREADY_COMPLETED: '会话已完成',
  DIAGNOSIS_ALREADY_COMPLETED: '诊断已完成',
  PLAN_ALREADY_COMMITTED: '计划已确认',
  INVALID_REQUEST: '请求无效',
  INTERNAL_ERROR: '服务器错误，请稍后重试',
  BAD_REQUEST: '请求参数错误',
  NOT_FOUND: '资源不存在',
  CONFLICT: '状态冲突',
}
