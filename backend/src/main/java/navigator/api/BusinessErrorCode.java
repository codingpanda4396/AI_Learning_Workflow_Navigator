package navigator.api;

/**
 * Sprint 1 业务错误码，用于状态机与非法路径校验。
 */
public enum BusinessErrorCode {
    RESOURCE_NOT_FOUND,
    DIAGNOSIS_NOT_COMPLETED,
    PLAN_NOT_COMMITTED,
    TASK_NOT_CURRENT,
    TASK_ALREADY_COMPLETED,
    SESSION_NOT_COMPLETED,
    SESSION_ALREADY_COMPLETED,
    DIAGNOSIS_ALREADY_COMPLETED,
    PLAN_ALREADY_COMMITTED,
    INVALID_ARGUMENT,
    INVALID_REQUEST,
    INTERNAL_ERROR,
    /** 已启用任务脚手架流程但未到达 PASS，不允许 complete */
    TASK_EXECUTION_NOT_READY_FOR_COMPLETE,
    /** 当前执行态不允许该操作 */
    INVALID_TASK_EXECUTION_STATE,
    /** 任务收束信息不满足要求（summary / 框架点等） */
    TASK_CLOSURE_INCOMPLETE
}
