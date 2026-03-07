
根据项目文档和代码结构，整理如下项目状态和功能概览：

---

## 项目阶段概览

**AI Learning Workflow Navigator** 是一个面向微信小程序的 AI 学习流程导航后端，当前处于 **MVP 后端核心功能基本完成** 阶段。README 仍标注为“制作中”。

---

## 已完成功能

### 1. 工程与分层结构
- 后端使用 **Spring Boot + Maven**，包名为 `com.pandanav.learning`
- DDD-lite 分层：
  - **api**：Controller、DTO
  - **application**：Service、UseCase、Command/Query
  - **domain**：Model、Repository
  - **infrastructure**：JdbcTemplate 持久化、异常、配置

### 2. 基础设施
- 健康检查：`GET /health`
- Swagger 文档：`/swagger-ui.html`（springdoc-openapi）
- DB 调试：`GET /debug/db`
- Flyway + PostgreSQL 迁移（V1–V7）
- 统一异常处理与错误响应格式

### 3. 会话相关 API（已打通真实业务流）
| 接口 | 说明 |
|------|------|
| `POST /api/session/create` | 创建学习会话，设定 STRUCTURE 阶段，写入首个 concept_node |
| `POST /api/session/{sessionId}/plan` | 按章节 concept_node 生成 4 类任务（STRUCTURE/UNDERSTANDING/TRAINING/REFLECTION） |
| `GET /api/session/{sessionId}/overview` | 返回时间线、next_task、mastery_summary 等 |

### 4. 任务相关 API（已打通真实业务流）
| 接口 | 说明 |
|------|------|
| `POST /api/task/{taskId}/run` | 执行任务并持久化结构化输出，实现幂等（SUCCEEDED 时直接返回缓存） |
| `POST /api/task/{taskId}/submit` | 提交 TRAINING 题答案，评估 → 更新 mastery → 策略决策 → 返回 next_action 与 next_task |

### 5. Submit 训练提交流程（最新完成）
- **EvaluatorService**：规则式评分，输出 score、error_tags、feedback（diagnosis、fixes）
- **MasteryUpdateService**：更新节点 mastery（公式：`newMastery = clamp(oldMastery * 0.7 + (score/100) * 0.3, 0, 1)`）
- **NextActionPolicyService**：根据评分决定下一步动作  
  - `< 60` → 补救理解  
  - `60–80` → 训练变式  
  - `80–90` → 巩固训练  
  - `≥ 90` → 进入下一节点
- **Evidence**：V7 迁移，将评估证据持久化到 `evidence` 表

### 6. 规则式生成（非 LLM）
- 任务 Run 输出按 Stage 使用固定 JSON 结构：STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION 各有模板
- MVP 阶段不接入 LLM，均为规则或模板驱动

---

## 技术栈与数据

- **持久化**：PostgreSQL + JdbcTemplate
- **迁移**：Flyway（V1–V7，含 evidence）
- **Domain 模型**：LearningSession、ConceptNode、Task、Mastery、Evidence
- **测试**：已有 `SubmitTrainingAnswerServiceTest` 等单元测试

---

## 简要总结

| 维度 | 状态 |
|------|------|
| Session 流程 | 已实现 create / plan / overview |
| Task Run | 已实现，带幂等与结构化输出 |
| Task Submit | 已实现，评估、mastery、策略、Evidence 全链路 |
| 前端 | 未见，面向微信小程序，应由前端单独项目承载 |
| LLM | 未接入，当前均为规则实现 |

整体上，后端 MVP 核心链路已打通，适合继续联调、补充测试或接入前端/微信小程序。