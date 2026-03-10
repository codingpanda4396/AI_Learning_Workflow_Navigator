# AI学习流程导航系统｜当前可直接开工的 Codex Prompt

> 本文件综合以下三份材料收敛而成：
> - MVP 需求文档
> - 技术方案 Prompt 版
> - MVP 现状差距与技术实现方案
>
> 目标不是继续发散设计，而是让你**现在就能开工**，并且让 Codex / Cursor 在你当前项目结构上做**增量实现**。

---

## 0. 你这轮开发的唯一目标

把当前已经完成的：

- 目标输入
- LLM 评估与建议
- 任务规划
- Tutor 分步学习

补成一个**最小可演示闭环**：

```text
目标输入
-> 评估报告
-> 任务规划
-> Tutor 分步学习
-> 异步生成检测题
-> 用户答题
-> LLM 评估答案
-> 反馈报告
-> 进入复习 / 下一轮学习
```

这轮**不要继续扩展**：

- 不做 RAG
- 不做教材上传
- 不做复杂知识图谱
- 不做题库后台
- 不做 MQ
- 不做大规模 UI 重构

---

## 1. 你现在最该先做的事情

按优先级，只做下面 5 件事：

1. **统一训练主链路**：以 `practice_item + practice_submission` 或你现有等价结构作为唯一训练事实来源。
2. **补齐异步出题能力**：学习开始后后台生成检测题，不阻塞 Tutor。
3. **补齐答题与反馈能力**：支持提交整套答案，并返回结构化反馈。
4. **把反馈变成下一步动作入口**：反馈页必须能进入 `review` 或 `next_round`。
5. **前端补齐状态展示**：生成中 / 已生成 / 失败可重试 / 反馈可查看。

---

## 2. 推荐你直接投喂给 Codex 的总控 Prompt

复制下面整段，先发给 Codex：

```text
你现在的角色是：
1. 高级 Java 后端架构师
2. Spring Boot 3 / JDK17 / PostgreSQL / Flyway / MyBatis-Plus 工程顾问
3. Vue 前端联调与 API 契约设计专家
4. 面向 MVP 收敛的产品技术实现负责人

项目背景：
我正在开发一个“AI 学习流程导航系统”MVP。当前已经完成：
- 用户输入目标 + 课程 + 章节
- LLM 初始评估与学习建议
- 用户确认评估后，LLM 生成学习任务
- Tutor 已接入分步学习主链路

当前还未补齐：
- 学习阶段异步生成检测题
- 用户答题提交
- LLM 评估答案并生成反馈报告
- 反馈后进入 review / next_round

当前项目目标不是重构整套系统，而是：
基于现有代码做增量实现，补齐最小学习闭环。

这轮开发的产品闭环是：
目标输入 -> 评估报告 -> 任务规划 -> Tutor 学习 -> 异步生成题目 -> 用户答题 -> 反馈报告 -> 进入复习或下一轮学习。

实现约束：
- 不要推翻当前架构
- 不要重做已有评估、规划、Tutor 链路
- 不要引入 MQ，MVP 用 Spring @Async 或线程池即可
- 不要设计复杂题库系统，只服务当前 session
- 不要过度抽象，优先能跑通主链路
- 优先统一命名、状态、接口契约
- 所有 LLM 输出必须结构化 JSON，并进行 schema / 业务校验
- 所有失败场景要有 fallback

本轮最重要的 5 件事：
1. 统一训练事实来源，避免旧提交链路和新 practice 链路双轨并存
2. 新增 quiz/question/answer/feedback 的最小数据承载
3. 打通异步出题 -> 拉题 -> 提交答案 -> 反馈生成
4. 反馈页提供 review / next_round 两个动作
5. 前后端状态和术语保持一致

输出要求：
- 先输出实现方案摘要
- 再输出本轮修改文件清单
- 再按 backend / frontend / api contract / db migration / test 五部分输出
- 尽量给出 unified diff 或接近可直接落地的代码
- 如果发现现有代码与目标有冲突，优先给出“最小补充方案”，不要大重构
```

---

## 3. 第一阶段：数据库与状态模型 Prompt

先做这一步，因为不先落数据结构，后续接口容易反复返工。

```text
请先只处理数据库与状态模型，不要展开写前端。

目标：
为“AI 学习流程导航系统”MVP 补齐最小数据承载，以支持：
- 学习阶段异步生成检测题
- 用户答题提交
- 反馈报告查询
- 基于反馈选择 review / next_round

技术栈约束：
- PostgreSQL
- Flyway
- Spring Boot 3 / JDK17
- 尽量兼容现有 learning_session、task、llm_call_record、practice 相关表
- 优先少表、少改动、可快速落地

请基于我当前项目结构，输出最小数据库变更方案。

要求：
1. 先说明设计意图
2. 给出 Flyway migration SQL
3. 给出索引建议
4. 给出状态字段枚举建议
5. 标明哪些字段是后续可扩展预留

最低覆盖对象：
- quiz_generation（或等价命名）
- question
- answer_submission
- feedback_report

状态至少支持：
- PENDING
- RUNNING
- SUCCEEDED
- FAILED

请注意：
- 不要设计复杂题库系统
- 不要脱离当前 session 闭环泛化
- 如果现有 practice_item / practice_submission 已可复用，请明确说明哪些复用、哪些补充
- 输出结果以“可直接创建 migration 文件”的形式给我
```

---

## 4. 第二阶段：后端主链路 Prompt

数据库结构出来后，马上补后端主链路。

```text
请基于当前项目代码，增量实现后端主链路，不要做前端。

本轮目标：
补齐“异步出题 + 答题评估 + 反馈决策”能力，并与现有 session / task / tutor 主链路兼容。

请完成以下能力：
1. 用户进入学习阶段后，可触发异步出题，不阻塞 Tutor 返回
2. 系统有最小状态流：PENDING / RUNNING / SUCCEEDED / FAILED
3. 可查询题目生成状态
4. 题目生成成功后，可获取题目列表
5. 用户可一次性提交整套答案
6. 后端调用 LLM 或规则评估答案，生成结构化反馈报告
7. 用户可基于反馈选择 next action：review / next_round
8. 所有 LLM 调用都记录模型名、任务类型、状态、耗时、输入输出
9. 所有失败场景都有 fallback：可重试、可读错误、幂等处理

接口至少包括：
- POST /sessions/{id}/quiz/generate
- GET /sessions/{id}/quiz/status
- GET /sessions/{id}/quiz
- POST /sessions/{id}/quiz/submit
- GET /sessions/{id}/feedback
- POST /sessions/{id}/next-action

实现约束：
- 不要引入 MQ，用 Spring @Async 或线程池 + 数据库存状态
- 不要推翻现有 llm service / prompt provider / task domain
- 优先复用现有 practice 相关服务与表结构
- 如果当前存在 SubmitTrainingAnswerService 和 PracticeSubmission 双轨，请收敛职责，明确唯一训练事实来源
- 反馈结果必须结构化，不要只返回大段自然语言

LLM 出题结构至少包含：
- type
- stem
- referenceAnswer
- rubric 或 evaluationFocus

LLM 反馈结构至少包含：
- overallSummary
- questionResults[]
- weaknesses[]
- suggestedNextAction

输出要求：
- 先给后端实现方案摘要
- 再给修改文件清单
- 再给核心代码改动
- 最后给你建议新增的测试点
```

---

## 5. 第三阶段：API 契约收敛 Prompt

这一步是为了避免你和前端继续错位。

```text
请为当前“AI 学习流程导航系统”MVP 收敛最小 API 契约文档。

背景：
当前已有目标输入、评估、任务规划、Tutor 主链路。
现在新增了异步出题、答题提交、反馈查询、下一步动作能力。

请输出：
1. API 列表
2. 每个接口的 method、path、request、response
3. 状态码与失败场景
4. 前后端字段命名统一建议
5. 建议的数据流顺序

接口至少包括：
- POST /sessions/{id}/quiz/generate
- GET /sessions/{id}/quiz/status
- GET /sessions/{id}/quiz
- POST /sessions/{id}/quiz/submit
- GET /sessions/{id}/feedback
- POST /sessions/{id}/next-action

要求：
- request / response 尽量简洁
- 输出 JSON 示例
- 明确哪些字段适合前端直接渲染
- 明确哪些字段仅用于内部诊断
- 状态命名必须和后端一致
- 不要泛化成平台级开放 API，只围绕当前项目收敛
```

---

## 6. 第四阶段：前端联动 Prompt

后端接口收敛后，再让 Codex 改前端，不要一开始就前后端一起乱改。

```text
请基于当前前端（Vue）页面结构，增量实现最小前端联动，不要重构页面体系。

当前前端已有：
- SessionView.vue
- TaskRunView.vue
- TaskSubmitView.vue
- 历史页
- session / tutor / practice 等 store

本轮目标：
在不推翻现有 Session / Tutor 页面结构的前提下，新增“检测题”和“反馈页”能力，形成可演示闭环。

要求：
1. 在学习阶段增加题目生成状态展示：
   - 生成中
   - 已生成，可开始检测
   - 失败，可重试
2. 支持拉取题目列表并展示答题区
3. 用户可逐题填写答案，并一次性提交
4. 提交后进入反馈页或反馈区块，展示：
   - overallSummary
   - questionResults
   - weaknesses
   - suggestedNextAction
5. 反馈页提供两个按钮：
   - 进入复习
   - 下一轮学习
6. 前端状态命名必须与后端一致
7. 只修改必要的 view / store / api 文件，不做大规模 UI 重构
8. 如果已有 practice store，可优先复用

输出要求：
- 先给页面流转说明
- 再给修改文件清单
- 再给可以直接应用的代码修改建议
- 尽量保持你当前项目页面结构不变
```

---

## 7. 第五阶段：测试与验收 Prompt

最后补测试，不然这轮很容易“能演示但不稳定”。

```text
请为本次增量功能生成测试清单与最小测试代码框架。

目标：
验证“异步出题 -> 拉取题目 -> 提交答案 -> 生成反馈 -> 进入下一步”的主链路。

请输出：
1. 单元测试建议
2. 集成测试建议
3. Controller 层接口测试建议
4. LLM JSON 解析与校验测试建议
5. 关键异常场景测试建议
6. 如果可以，请直接给出 JUnit 5 测试骨架代码

最少覆盖场景：
- 题目生成成功
- 题目生成失败
- 重复触发生成的幂等处理
- 提交空答案或非法 session
- 反馈生成成功
- 反馈生成失败
- next action 选择 review / next_round

请注意：
- 测试目标是保障当前 MVP 主链路，而不是做全量测试体系
- 优先覆盖 controller/service/llm JSON parse 三层
```

---

## 8. 你可以直接开工的最小执行顺序

不要乱，按这个顺序干：

### Step 1：先让 Codex看总控 Prompt
让它理解你的项目边界，不然它很容易发散重构。

### Step 2：先做数据库 migration
因为题目、答案、反馈、状态都需要数据承载。

### Step 3：再做后端接口与服务
重点是：
- 异步出题
- 状态查询
- 题目拉取
- 答题提交
- 反馈生成
- next action

### Step 4：再收敛 API 文档
确保前端知道怎么接，不要边猜边改。

### Step 5：最后挂前端页面
只做最小联动，把状态和反馈展示出来。

### Step 6：补测试
至少把主链路和失败场景兜住。

---

## 9. 这轮开发的验收标准

做完后，你至少要能演示下面这条链：

```text
输入目标 + 课程 + 章节
-> 生成评估报告
-> 用户确认
-> 生成学习任务
-> 进入 Tutor 学习
-> 后台异步生成题目
-> 前端展示“题目已生成”
-> 用户开始检测并提交答案
-> 系统返回结构化反馈
-> 用户点击“进入复习”或“下一轮学习”
```

并满足以下条件：

- 题目生成不阻塞 Tutor 主流程
- 反馈结果是结构化字段，不是聊天大段文本
- review / next_round 两个动作真实可点、可流转
- 前后端状态名一致
- 失败场景可重试、可提示

---

## 10. 最后给你的执行提醒

你这轮最容易犯的错有三个：

### 错误 1：继续扩展，而不是收敛
你现在不缺想法，缺的是闭环落地。先把这轮做透。

### 错误 2：前后端一起乱改
一定是：**数据结构 -> 后端 -> API -> 前端 -> 测试**。

### 错误 3：反馈做成聊天文本
反馈必须结构化，不然前端没法稳定展示，也没法支撑后续路径调整。

---

## 11. 一句话版本

如果你现在只想复制一段最短提示给 Codex，就用这个：

```text
基于我当前的 AI 学习流程导航系统项目，做增量开发，不要重构。当前已完成目标输入、LLM 评估、任务规划、Tutor 主链路。现在请补齐最小学习闭环：异步生成检测题、查询生成状态、获取题目、提交答案、结构化反馈、进入 review / next_round。技术栈是 Spring Boot 3 + JDK17 + PostgreSQL + Flyway + Vue。要求优先复用现有 practice / llm / session / task 结构，不引入 MQ，不做复杂题库，不做大 UI 重构。请先输出实现方案摘要、修改文件清单，再分别输出 backend、api contract、frontend、db migration、test 的可落地修改方案。所有状态和命名必须统一，所有 LLM 输出必须结构化 JSON 并可校验，失败场景必须有 fallback 和幂等处理。
```

---

这份文件的作用只有一个：**让你今天就能开始干活，并且不跑偏。**
