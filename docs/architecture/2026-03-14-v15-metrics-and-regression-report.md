# V1.5 埋点与回归结果（比赛展示版）

## 本轮交付范围

- 已完成首屏关键链路埋点：`preview 展示`、`preview 确认`、`首任务完成`。
- 已完成后端 Micrometer 指标埋点：`fallback rate`、`evidence coverage`、`candidate divergence`、`preview accepted`。
- 已补充 10 个案例的回归测试套件，覆盖证据可追溯、风险文案、趋势判断、第一步解释稳定性。

## 埋点口径

### 后端（Micrometer）

- `learning.plan.preview.total`：preview 请求总量。
- `learning.plan.preview.fallback{applied,evidence_bucket,divergence_bucket}`：fallback 计数与分桶。
- `learning.plan.preview.evidence.count`：每次 preview 的证据条数分布。
- `learning.plan.preview.candidate.alternative.count`：候选分化（备选策略数）分布。
- `learning.plan.preview.accepted{has_first_task}`：用户确认预览并进入学习流程。

### 前端（本地统计，便于演示）

- 存储键：`plan_preview_metrics_v1_5`。
- `previewShown`：进入规划页并首次拿到某个 `previewId`。
- `previewAccepted`：点击“确认并进入”并成功返回。
- `firstTaskCompleted`：完成被记录的第一任务并进入下一步。
- 页面会输出快照日志，便于演示时直接展示实时转化率：
  - `previewAcceptedRate = previewAccepted / previewShown`
  - `firstTaskCompletionRate = firstTaskCompleted / previewAccepted`

## 回归案例（10 个）

测试入口：`LearnerEvidenceAggregatorRegressionTest`

| Case ID | 画像/输入特征 | 预期验证点 | 结果 |
| --- | --- | --- | --- |
| case-01-weak-concept | 概念与关系双弱，分数低位 | 跳过风险强调“断链返工” | PASS |
| case-02-weak-code-mapping | 代码映射弱 | 跳过风险强调“会概念落不到代码” | PASS |
| case-03-stable-high-confidence | 多信号稳定，分数上升 | 置信提示高、趋势 up | PASS |
| case-04-downward-trend | 分数明显下降 | 趋势 down | PASS |
| case-05-low-evidence | 几乎无历史证据 | 证据兜底文案稳定输出 | PASS |
| case-06-relationship-risk | 关系理解弱 | 风险文案命中依赖断链 | PASS |
| case-07-borderline-up | 临界上升（+8） | 趋势 up 阈值正确 | PASS |
| case-08-borderline-down | 临界下降（-9） | 趋势 down 阈值正确 | PASS |
| case-09-weak-confidence-hint | 置信信号弱 | 置信提示走保守文案 | PASS |
| case-10-mixed-medium | 混合中等信号 | 证据+风险+趋势均可生成 | PASS |

## 执行结果

已执行命令：

- `mvn -Dtest=LearnerEvidenceAggregatorRegressionTest test`

结果：

- Tests run: `1`
- Failures: `0`
- Errors: `0`
- 该测试内部覆盖 `10` 个回归案例断言，全部通过。

## 比赛展示建议（可直接用）

- 展示“同主题不同画像”时，先给两组对比：
  - 一组概念弱用户（风险文案偏“断链返工”）
  - 一组代码映射弱用户（风险文案偏“概念到代码卡顿”）
- 同时打开浏览器控制台展示 `previewAcceptedRate` 与 `firstTaskCompletionRate`，体现“首屏行动优先”改造效果。
- 后台指标页重点展示：
  - `fallback rate` 下降或可控
  - `evidence coverage` 提升（high bucket 占比）
  - `candidate divergence` 保持在 medium/wide 桶
