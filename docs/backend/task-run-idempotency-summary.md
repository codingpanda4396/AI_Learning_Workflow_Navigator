# Task Run Idempotency Summary

## Scope

本次实现了 `POST /api/task/{taskId}/run` 的真实执行流程，重点满足幂等与结构化输出持久化。

## Core Requirement (MUST)

已实现以下强约束：

- 当 `task.status == SUCCEEDED` 且 `output_json` 非空时，直接返回已存储输出。
- 不重复调用生成逻辑。

状态与输出基于 `task_attempt` 最新记录判定。

## Key Classes

- API
  - `TaskController#runTask(Long taskId)`
- Application
  - `RunTaskUseCase`
  - `TaskRunnerService`
- Domain
  - `Task`（新增行为方法：`markRunning` / `markSucceeded`）
  - `Stage`
  - `TaskStatus`
- Infrastructure
  - `TaskRepository`（新增 run 相关方法）
  - `JdbcTaskRepository`

## Run Flow

1. 读取任务（含最新 attempt 的 `status` 和 `output_json`）。
2. 若 `SUCCEEDED + output_json 非空`：直接返回缓存输出（幂等命中）。
3. 若状态为 `RUNNING`：抛 `CONFLICT`。
4. 若状态为 `PENDING/FAILED/CANCELLED`：
   - 新建一条 `task_attempt`，状态 `RUNNING`
   - 根据 `stage` 进入规则生成器
   - 生成稳定 JSON 结构
   - 更新该 attempt 为 `SUCCEEDED` 并保存 `output_json`
   - 返回执行结果

## Rule-based Output Shapes

- `STRUCTURE`
  - `sections`: `concepts/structure/relations/summary`
- `UNDERSTANDING`
  - `sections`: `concepts/mechanism/misconceptions/summary`
- `TRAINING`
  - `questions/variants/rubric`
- `REFLECTION`
  - `diagnosis/reflection_points/next_steps`

## UNDERSTANDING Example Output

```json
{
  "sections": [
    {
      "type": "concepts",
      "title": "核心概念",
      "bullets": ["关键定义", "必要条件", "触发条件"]
    },
    {
      "type": "mechanism",
      "title": "机制链路",
      "steps": ["步骤1：输入与前提", "步骤2：状态变化", "步骤3：输出与结果"]
    },
    {
      "type": "misconceptions",
      "title": "常见误区",
      "items": ["误区1：只记结论", "误区2：忽略边界情况"]
    },
    {
      "type": "summary",
      "title": "总结",
      "text": "理解因果链路比背诵步骤更重要。"
    }
  ]
}
```

## Validation Result

联调验证通过：

- 同一 `taskId` 连续调用两次 `/run`，第二次返回已缓存结果。
- 数据库 `task_attempt` 计数保持为 1（未重复创建 attempt）。

## Curl Example

```bash
curl -X POST http://127.0.0.1:8080/api/task/1/run
```
