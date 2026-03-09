# 学习反馈层 MVP：知识点掌握度与薄弱点诊断

## 1. 数据模型
新增 `node_mastery`（V14 migration）：
- `user_id` + `node_id` 唯一键
- `session_id` 记录最近一次重算来源
- `mastery_score`：0-100
- `training_accuracy`：0-100
- `recent_error_tags_json`
- `latest_evaluation_score`
- `attempt_count`
- `updated_at`

设计原因：
- 旧 `mastery` 仅有 `mastery_value`，无法承载诊断所需维度
- 新表用于反馈汇总，仍同步回写旧 `mastery`，保证现有路径规划兼容

## 2. 服务能力

### MasteryService
- `recalculateNodeMastery(sessionId, taskId, userId)`
  - 校验 session/task/user 归属
  - 仅允许 `TRAINING` task
  - 从 `practice_submission` 汇总正确率、错误标签、连续错误
  - 读取 task 最新评估分（`task_attempt.score`）
  - 重算并 upsert `node_mastery`
  - 同步更新旧 `mastery`（`mastery_score/100`）
  - 写学习事件 `NODE_MASTERY_RECALCULATED`
- `getNodeMastery(sessionId, userId, nodeId)`

### WeakPointDiagnosisService
- `diagnoseWeakPoints(sessionId, userId)`
  - 按 session 所属 chapter 读取该用户 `node_mastery`
  - 产出弱项节点与诊断摘要

## 3. 规则化 mastery 算法（可解释）
输入：
- `training_accuracy`（practice submission 正确率）
- `latest_evaluation_score`（task 最新评估分）
- `repeatedTagKinds`（重复出现 >=2 次的错误标签种类数）
- `consecutiveWrong`（最近连续错误次数）
- `attempt_count`

公式：
- `evaluation = latest_evaluation_score`，为空则按 50
- `score = training_accuracy*0.6 + evaluation*0.3 + 10`
- 处罚：
  - `- min(3, consecutiveWrong) * 8`
  - `- min(3, repeatedTagKinds) * 4`
- 奖励：
  - 若 `attempt_count >= 3 且 training_accuracy >= 80`，`+5`
- 最终 clamp 到 `[0, 100]`

## 4. 薄弱点诊断规则
节点命中任一条件即归为 weak node：
- `mastery_score < 60` -> `LOW_MASTERY_SCORE`
- `attempt_count >= 2 且 training_accuracy < 70` -> `LOW_TRAINING_ACCURACY`
- `recent_error_tags` 数量 >= 2 -> `REPEATED_ERROR_TAGS`

API：
- `GET /api/session/{sessionId}/learning-feedback/weak-points`
- 返回：`weak_nodes + diagnosis_summary`

## 5. 对路径规划可消费的数据结构建议
建议 planner 直接消费以下结构（来自 weak-points API 或 service）：

```json
{
  "session_id": 200,
  "diagnosis_summary": "Detected 2 weak nodes...",
  "weak_nodes": [
    {
      "node_id": 301,
      "node_name": "Binary Search",
      "mastery_score": 52.0,
      "training_accuracy": 60.0,
      "latest_evaluation_score": 68,
      "attempt_count": 4,
      "recent_error_tags": ["MISSING_STEPS", "CONCEPT_CONFUSION"],
      "reasons": ["LOW_MASTERY_SCORE", "LOW_TRAINING_ACCURACY"]
    }
  ]
}
```

推荐 planner 使用方式：
- 按 `mastery_score asc` 选前 N 个 node 优先补强
- 将 `recent_error_tags` 映射到插入任务模板（例如 `MISSING_STEPS -> UNDERSTANDING`）
- 用 `latest_evaluation_score` 作为是否升级到 TRAINING 变式题的阈值依据
