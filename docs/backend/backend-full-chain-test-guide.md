# 后端业务全链路测试手册

## 1. 目标

本文给出一套可以真实跑通后端主业务闭环的测试方法，覆盖以下链路：

`注册/登录 -> 创建学习会话 -> 规划任务 -> 执行阶段任务 -> 生成训练题 -> 提交训练答案 -> 生成反馈 -> 查看成长/诊断结果`

默认采用“规则兜底模式”测试：

- 不依赖外部 LLM 可用性
- 仍然会经过真实 Controller / UseCase / Service / Repository / PostgreSQL / Flyway
- 能覆盖主要业务表写入与聚合查询

## 2. 代码结论

结合当前后端代码和文档，最稳妥的全链路入口如下：

- 鉴权：`/api/auth/register`、`/api/auth/login`
- 会话：`/api/session/create`、`/api/session/{sessionId}/plan`、`/api/session/{sessionId}/overview`
- 任务执行：`/api/task/{taskId}/run`
- Session 级训练闭环：
  - `POST /api/sessions/{sessionId}/quiz/generate`
  - `GET /api/sessions/{sessionId}/quiz/status`
  - `GET /api/sessions/{sessionId}/quiz`
  - `POST /api/sessions/{sessionId}/quiz/submit`
  - `GET /api/sessions/{sessionId}/feedback`
  - `POST /api/sessions/{sessionId}/next-action`
- 聚合结果：
  - `GET /api/session/{sessionId}/learning-feedback/weak-points`
  - `GET /api/session/{sessionId}/learning-feedback/report`
  - `GET /api/session/{sessionId}/growth-dashboard`

测试时推荐使用种子章节 `chapter_id=tcp`，因为 `V9__seed_concept_nodes.sql` 已内置 `tcp/udp/http/process-thread/memory/io` 等章节知识点。

## 3. 前置条件

### 3.1 环境

- Java 17+
- Maven 3.6+
- PostgreSQL

### 3.2 数据库

后端会自动执行 Flyway，至少需要能连上一个 PostgreSQL 库。

默认配置来源：

- `backend/src/main/resources/application.yml`
- 可选覆盖：`backend/application-local.yml`

### 3.3 推荐测试配置

为了让链路更稳定，建议在启动时关闭 LLM，使用规则兜底：

```powershell
$env:LLM_ENABLED="false"
$env:TUTOR_LLM_ENABLED="false"
```

这样以下能力仍然可测通：

- `TaskRunnerService` 会回退到模板生成
- `PracticeServiceImpl` 会回退到 `RulePracticeGenerator`
- `PracticeFeedbackReportGenerator` 会回退到规则反馈

## 4. 启动方式

先做一次编译校验：

```powershell
cd D:\Panda_Code\AI_Learning_Workflow_Navigator\backend
mvn -q -DskipTests compile
```

再启动服务：

```powershell
cd D:\Panda_Code\AI_Learning_Workflow_Navigator\backend
$env:LLM_ENABLED="false"
$env:TUTOR_LLM_ENABLED="false"
mvn spring-boot:run
```

启动后先检查：

```powershell
Invoke-RestMethod http://localhost:8080/health
```

预期返回成功响应。

也可以打开 Swagger：

- [swagger-ui](http://localhost:8080/swagger-ui.html)

## 5. 全链路执行步骤

下面脚本按顺序执行整条主链路，建议直接在 PowerShell 里逐段运行。

### 5.1 初始化变量并注册/登录

```powershell
$base = "http://localhost:8080"
$username = "e2e_user_" + (Get-Date -Format "MMddHHmmss")
$password = "Passw0rd!123"

$registerBody = @{
  username = $username
  password = $password
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/auth/register" `
  -ContentType "application/json" `
  -Body $registerBody

$loginBody = @{
  username = $username
  password = $password
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$token = $login.token
$headers = @{ Authorization = "Bearer $token" }
```

预期结果：

- 注册成功返回 `user_id`
- 登录成功返回 `token`
- 后续所有 `/api/**` 请求都带 `Authorization: Bearer <token>`

### 5.2 创建学习会话

```powershell
$createSessionBody = @{
  course_id = "computer_network"
  chapter_id = "tcp"
  goal_text = "理解 TCP 可靠传输机制，并完成一次训练闭环"
} | ConvertTo-Json

$session = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/session/create" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $createSessionBody

$sessionId = $session.session_id
$sessionId
```

预期结果：

- 返回 `session_id`
- `learning_session` 新增 1 条数据
- `current_stage` 初始应为 `STRUCTURE`
- `current_node_id` 指向 `tcp` 章节第一个知识点

### 5.3 规划任务

```powershell
$plan = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/session/$sessionId/plan?mode=adaptive" `
  -Headers $headers

$plan
```

预期结果：

- `plans` 非空
- 每个知识点应生成四类任务：`STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION`
- `task` 表新增多条记录

### 5.4 读取总览并提取任务 ID

```powershell
$overview = Invoke-RestMethod `
  -Method Get `
  -Uri "$base/api/session/$sessionId/overview" `
  -Headers $headers

$overview.timeline

$structureTaskId = ($overview.timeline | Where-Object { $_.stage -eq "STRUCTURE" } | Select-Object -First 1).task_id
$understandingTaskId = ($overview.timeline | Where-Object { $_.stage -eq "UNDERSTANDING" } | Select-Object -First 1).task_id
$trainingTaskId = ($overview.timeline | Where-Object { $_.stage -eq "TRAINING" } | Select-Object -First 1).task_id
$reflectionTaskId = ($overview.timeline | Where-Object { $_.stage -eq "REFLECTION" } | Select-Object -First 1).task_id

$structureTaskId
$understandingTaskId
$trainingTaskId
$reflectionTaskId
```

预期结果：

- `timeline` 有任务列表
- `next_task` 不为空
- 能拿到首个节点的四类任务 ID

### 5.5 执行阶段任务

```powershell
$structureRun = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/task/$structureTaskId/run" `
  -Headers $headers

$understandingRun = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/task/$understandingTaskId/run" `
  -Headers $headers

$trainingRun = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/task/$trainingTaskId/run" `
  -Headers $headers

$reflectionRun = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/task/$reflectionTaskId/run" `
  -Headers $headers
```

预期结果：

- 四次调用都返回 `status = SUCCEEDED`
- `generation_mode` 在规则兜底下通常不是 LLM
- `task_attempt` 新增成功记录
- `task.output_json` / `task_attempt.output_json` 有内容

### 5.6 触发训练题生成

```powershell
$quizStart = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/sessions/$sessionId/quiz/generate" `
  -Headers $headers

do {
  Start-Sleep -Seconds 2
  $quizStatus = Invoke-RestMethod `
    -Method Get `
    -Uri "$base/api/sessions/$sessionId/quiz/status" `
    -Headers $headers
  $quizStatus
} while ($quizStatus.generation_status -eq "PENDING" -or $quizStatus.generation_status -eq "RUNNING")
```

预期结果：

- 最终 `generation_status = SUCCEEDED`
- `quiz_status` 进入 `QUIZ_READY`
- `question_count` 大于 0

### 5.7 获取训练题

```powershell
$quiz = Invoke-RestMethod `
  -Method Get `
  -Uri "$base/api/sessions/$sessionId/quiz" `
  -Headers $headers

$quiz.questions
```

在规则兜底模式下，通常会得到 3 道题：

- 1 道单选
- 1 道判断
- 1 道简答

### 5.8 提交训练题答案

规则兜底题目是固定模板，可以直接构造答案：

```powershell
$q1 = $quiz.questions[0].question_id
$q2 = $quiz.questions[1].question_id
$q3 = $quiz.questions[2].question_id

$submitBody = @{
  answers = @(
    @{
      question_id = $q1
      answer = "Focus on definitions and boundary conditions before applying formulas."
    },
    @{
      question_id = $q2
      answer = "False"
    },
    @{
      question_id = $q3
      answer = "I will first define the concept clearly, then follow the key steps to apply it, and finally check boundary conditions and edge cases before confirming the result."
    }
  )
} | ConvertTo-Json -Depth 6

$feedback = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/sessions/$sessionId/quiz/submit" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $submitBody

$feedback
```

预期结果：

- 返回 `report_id`
- `report_status = SUCCEEDED`
- `question_results` 非空
- `recommended_action` 为 `REVIEW` 或 `NEXT_ROUND`

### 5.9 查询反馈与下一步动作

```powershell
$feedbackView = Invoke-RestMethod `
  -Method Get `
  -Uri "$base/api/sessions/$sessionId/feedback" `
  -Headers $headers

$feedbackView

$nextActionBody = @{
  action = $feedbackView.recommended_action
} | ConvertTo-Json

$nextAction = Invoke-RestMethod `
  -Method Post `
  -Uri "$base/api/sessions/$sessionId/next-action" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $nextActionBody

$nextAction
```

预期结果：

- `selected_action` 被记录
- quiz 状态进入 `REVIEWING` 或 `NEXT_ROUND`

### 5.10 查询诊断和成长看板

```powershell
$weakPoints = Invoke-RestMethod `
  -Method Get `
  -Uri "$base/api/session/$sessionId/learning-feedback/weak-points" `
  -Headers $headers

$report = Invoke-RestMethod `
  -Method Get `
  -Uri "$base/api/session/$sessionId/learning-feedback/report" `
  -Headers $headers

$dashboard = Invoke-RestMethod `
  -Method Get `
  -Uri "$base/api/session/$sessionId/growth-dashboard" `
  -Headers $headers

$weakPoints
$report
$dashboard
```

预期结果：

- `weak_points` 能看到弱项节点或诊断摘要
- `learning-feedback/report` 能看到题目结果、掌握度、下一步建议
- `growth-dashboard` 能看到节点掌握度聚合结果

## 6. 数据库验收点

如果接口都通了，建议再做一次数据库验收，确认是“真实业务闭环”而不是只返回接口 mock。

将下面 SQL 中的 `:sessionId`、`:taskId`、`:quizId`、`:userId` 替换成实际值。

### 6.1 会话与任务

```sql
select * from learning_session where id = :sessionId;
select * from task where session_id = :sessionId order by id;
select * from task_attempt where task_id in (select id from task where session_id = :sessionId) order by id;
```

预期：

- `learning_session` 有一条会话
- `task` 至少有 12 条左右任务（3 个知识点 x 4 阶段）
- `task_attempt` 有刚执行过的阶段记录

### 6.2 训练题与提交

```sql
select * from practice_quiz where session_id = :sessionId order by id desc;
select * from practice_item where quiz_id = :quizId order by id;
select * from practice_submission where quiz_id = :quizId order by id;
select * from practice_feedback_report where quiz_id = :quizId;
```

预期：

- `practice_quiz` 有 1 条记录
- `practice_item` 至少有 3 条题目
- `practice_submission` 有 3 条答案提交
- `practice_feedback_report` 有 1 条反馈报告

### 6.3 成长与事件沉淀

```sql
select * from node_mastery where user_id = :userId order by updated_at desc;
select * from learning_event where session_id = :sessionId order by id;
```

预期：

- `node_mastery` 有当前节点的掌握度记录
- `learning_event` 至少出现：
  - `PRACTICE_ITEMS_GENERATED`
  - `PRACTICE_ANSWER_SUBMITTED`
  - `PRACTICE_FEEDBACK_GENERATED`

## 7. 通过标准

满足以下条件即可认为后端业务全链路已跑通：

- 鉴权成功，受保护接口能正常访问
- `learning_session`、`task`、`task_attempt` 写入成功
- 训练题生成成功，`practice_quiz`、`practice_item` 写入成功
- 提交答案后 `practice_submission`、`practice_feedback_report` 写入成功
- `learning-feedback/report`、`growth-dashboard` 可读取聚合结果
- `node_mastery`、`learning_event` 有对应沉淀

## 8. 常见失败点

### 8.1 创建 session 失败

优先检查：

- PostgreSQL 是否可连接
- Flyway 是否执行成功
- `concept_node` 是否已有 `tcp` 章节数据

### 8.2 401 Unauthorized

优先检查：

- 是否忘记带 `Authorization` 头
- 格式是否为 `Bearer <token>`

### 8.3 quiz 一直生成失败

优先检查：

- 是否已关闭 LLM 并走规则兜底
- `trainingTaskId` 是否确实属于 `TRAINING` 阶段
- 后端日志中是否出现 `Practice quiz generation failed`

### 8.4 提交答案报缺少题目

优先检查：

- `/api/sessions/{sessionId}/quiz` 返回的 `questions` 是否完整
- `submit` 请求里是否为每个 `question_id` 都传了 `answer`

## 9. 最小结论

当前项目最适合做“后端业务全链路验收”的方式，不是逐个原子接口单测，而是按本文这条链路串行执行。它能够覆盖当前后端最重要的真实闭环：

`认证 -> session -> task -> quiz -> submission -> feedback -> insight -> growth`
