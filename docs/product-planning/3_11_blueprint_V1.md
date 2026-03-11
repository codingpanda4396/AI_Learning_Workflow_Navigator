# 3.11 Blueprint V1：前后端可并行开发的系统约束方案

## 1. 文档目标

本文档用于把当前项目从“功能持续增加”收敛为“可分工、可约束、可并行开发、可答辩”的版本。

当前项目已经具备一条可运行的 MVP 主链路：

`学习目标 -> 会话创建 -> 学习路径规划 -> 分阶段任务执行 -> 训练作答 -> 结果反馈 -> 下一步学习建议`

也已经拥有较完整的核心对象与工程基础，包括：

- 学习会话 `learning_session`
- 知识节点 `concept_node`
- 分阶段任务 `task`
- 练习体系 `practice_item / practice_submission / practice_quiz`
- 反馈体系 `node_mastery / learning_feedback`
- AI Tutor 协同学习能力

因此，当前当务之急不是继续散点式加功能，而是建立一套稳定约束，让前端和后端都能围绕同一套产品语言、状态机和接口契约独立推进开发。

---

## 2. 当前项目判断

### 2.1 当前阶段定位

项目当前更准确的定位是：

> **MVP 主链路已成型，但产品闭环感、一致性和展示表达仍需强化。**

现阶段已经不是空框架，而是一个具备：

- 会话主链路
- 分阶段任务执行
- 训练与反馈
- Tutor 协同学习
- 历史记录与续学能力
- 前后端联动能力

的 AI 学习流程导航系统原型。

### 2.2 当前最大问题

现在最主要的问题不是“没有模块”，而是“协作边界不够清晰”：

- 前后端使用的术语还没有完全统一
- 阶段命名和状态流转还存在历史演进痕迹
- 前端页面有时在承担过多职责
- 后端已有能力没有沉淀成统一聚合接口
- 训练结果如何影响下一步学习动作，用户感知还不够强
- 文档、代码、接口、页面表达之间仍然依赖人工对齐

### 2.3 本文档要解决什么

本文档不解决所有实现细节，而是先解决最影响效率的 4 个问题：

1. **统一产品语言**：前后端、文档、答辩统一说法
2. **统一状态约束**：明确哪些状态存在，如何合法流转
3. **统一接口契约**：让前后端可以并行开发
4. **统一页面职责边界**：避免页面和组件继续失控扩张

---

## 3. 系统总原则

### 3.1 协作原则

后续所有开发都遵循以下原则：

1. **先收口主线，再扩展能力**
2. **先定契约，再写实现**
3. **后端负责业务结论，前端负责清晰呈现**
4. **页面只承担一种核心职责**
5. **所有“下一步怎么做”的建议必须可解释**

### 3.2 约束分层

建议将项目约束拆成 6 层：

1. 术语约束
2. 状态约束
3. 接口约束
4. 页面约束
5. 组件约束
6. 目录与 ownership 约束

这 6 层不是附属物，而是后续开发效率的基础设施。

---

## 4. 统一产品语言

### 4.1 对外核心业务对象

对外统一保留以下 8 个核心对象，前后端、文档、页面、答辩全部围绕这套对象展开：

- `LearningSession`：一次学习会话
- `LearningTask`：会话中的分步学习任务
- `PracticeBatch`：一次检测批次
- `PracticeItem`：单道题目
- `PracticeSubmission`：用户作答记录
- `LearningReport`：本轮学习报告
- `KnowledgeNodeProgress`：用户对知识点的掌握进度
- `TutorMessage`：Tutor 对话消息

约束：

- 前端不直接消费后端内部历史对象拼接结果
- 文档和答辩中不再使用过度底层化的数据表名称讲主故事
- 页面中出现的产品对象名称必须能映射到上述 8 个对象之一

### 4.2 统一阶段术语

对外阶段术语统一为：

- `STRUCTURE`：建立框架
- `UNDERSTANDING`：理解原理
- `TRAINING`：训练应用
- `REFLECTION`：评估反思

建议统一枚举：

```ts
type TaskStage =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION';
```

约束：

- API 只能返回这套枚举
- 前端页面文案必须通过统一映射表输出中文名
- 不允许继续混用 `EVALUATION` 与 `REFLECTION`
- 数据库内部若保留旧值，必须在应用层映射收口

### 4.3 统一产品主叙事

后续项目统一讲法：

> 一个面向大学课程场景的 AI 个性化学习导航与知识成长系统。
> 它通过能力分析、路径规划、任务学习、个性化检测、反馈决策和私人知识图谱沉淀，实现从单次学习到长期成长的动态闭环。

所有开发都要服务这条主叙事。

---

## 5. 核心业务闭环蓝图

### 5.1 系统主闭环

```text
学习目标输入
   ↓
用户能力分析
   ↓
个性化学习方案生成
   ↓
知识点拆解 + 任务体系生成
   ↓
用户与 AI Tutor 协同完成学习任务
   ↓
基于当前学习内容生成个性化检测题
   ↓
提交答案并自动评估
   ↓
生成学习报告 + 更新掌握度 + 记录薄弱点
   ↓
写入用户私人知识图谱
   ↓
系统决定下一步：推进 / 复习 / 强化 / 回退
```

### 5.2 系统总结构

```text
AI个性化学习导航系统
├─ A. 用户入口层
│  ├─ 目标输入
│  ├─ 历史会话续学
│  ├─ 个人学习主页
│  └─ 个人知识图谱入口
│
├─ B. 个性化决策层
│  ├─ 用户能力分析
│  ├─ 学习路径规划
│  └─ 下一步决策引擎
│
├─ C. 学习执行层
│  ├─ 学习会话 Session
│  ├─ 分阶段任务 Task
│  ├─ Task Run 页面
│  └─ AI Tutor 协同学习
│
├─ D. 训练评估层
│  ├─ 个性化出题
│  ├─ 作答提交
│  ├─ 自动评估
│  ├─ 薄弱点诊断
│  └─ 掌握度更新
│
├─ E. 成长沉淀层
│  ├─ 学习报告
│  ├─ 私人知识图谱
│  └─ 学习成长仪表盘
│
└─ F. 平台基础层
   ├─ 用户认证
   ├─ 历史记录
   ├─ LLM 调用审计
   ├─ 异步任务
   ├─ 状态流转
   └─ 配置 / 容错 / fallback
```

---

## 6. 状态约束设计

状态约束是前后端分工的核心。

### 6.1 会话状态机

```ts
type SessionStatus =
  | 'ANALYZING'
  | 'PLANNING'
  | 'LEARNING'
  | 'PRACTICING'
  | 'REPORT_READY'
  | 'COMPLETED';
```

状态含义：

- `ANALYZING`：能力分析中
- `PLANNING`：学习路径规划中
- `LEARNING`：任务学习中
- `PRACTICING`：检测中
- `REPORT_READY`：报告已生成
- `COMPLETED`：本轮学习完成

合法迁移：

```text
ANALYZING -> PLANNING -> LEARNING -> PRACTICING -> REPORT_READY -> COMPLETED
REPORT_READY -> LEARNING
REPORT_READY -> PRACTICING
```

约束：

- 前端页面切换只能依据 `session.status`
- 前端不得自行推断当前处于哪一步
- 后端必须校验状态是否合法迁移
- 所有跨阶段动作都必须经过应用层统一编排

### 6.2 任务状态机

```ts
type TaskStatus =
  | 'TODO'
  | 'IN_PROGRESS'
  | 'WAITING_PRACTICE'
  | 'DONE'
  | 'BLOCKED';
```

状态含义：

- `TODO`：待开始
- `IN_PROGRESS`：进行中
- `WAITING_PRACTICE`：等待进入检测
- `DONE`：已完成
- `BLOCKED`：被阻塞

约束：

- 训练型任务完成后必须进入 `WAITING_PRACTICE` 或 `DONE`
- 前端不得直接将任务标记为 `DONE`
- 任务完成结论只能由后端统一给出

### 6.3 练习状态机

```ts
type PracticeStatus =
  | 'NOT_GENERATED'
  | 'GENERATING'
  | 'READY'
  | 'SUBMITTED'
  | 'EVALUATING'
  | 'EVALUATED';
```

状态含义：

- `NOT_GENERATED`：未生成
- `GENERATING`：题目生成中
- `READY`：可开始作答
- `SUBMITTED`：已提交
- `EVALUATING`：评估中
- `EVALUATED`：已评估完成

约束：

- 练习相关页面必须严格依据该状态渲染
- 前端不能仅凭“有没有题”判断流程位置
- 报告页只能在 `EVALUATED` 之后进入

---

## 7. 接口契约约束

### 7.1 统一响应壳

所有业务接口统一返回以下结构：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "uuid",
  "data": {}
}
```

错误示例：

```json
{
  "code": "PRACTICE_NOT_READY",
  "message": "practice batch is still generating",
  "requestId": "uuid",
  "data": null
}
```

约束：

- 禁止部分接口直接返回裸对象，部分接口返回包装结构
- 前端逻辑判断只能基于 `code` 和 `data`，不能依赖 `message`
- 所有异步型接口都必须返回 `requestId`

### 7.2 契约优先原则

开发流程统一为：

1. 先定义接口契约
2. 前端基于 mock 开发
3. 后端按契约补实现
4. 联调只验证一致性，不临时改字段语义

约束：

- 不允许联调时口头修改字段名
- 字段变更必须先更新契约文档
- 所有枚举值必须在文档中明确定义

### 7.3 高优先级聚合接口

当前优先定义以下 5 个接口：

1. `GET /api/sessions/{sessionId}/overview`
2. `GET /api/sessions/{sessionId}/report`
3. `GET /api/users/me/knowledge-graph`
4. `GET /api/users/me/growth-stats`
5. `GET /api/sessions/{sessionId}/next-action`

这些接口的价值在于：它们不是底层 CRUD，而是前端页面真正需要的聚合视图。

---

## 8. 关键接口草案

### 8.1 会话总览接口

`GET /api/sessions/{sessionId}/overview`

用途：提供 Session 页所需全部核心摘要。

返回示例：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "req-001",
  "data": {
    "sessionId": "s1",
    "status": "LEARNING",
    "currentNode": {
      "nodeId": "n1",
      "title": "链表基础结构"
    },
    "currentTask": {
      "taskId": "t1",
      "stage": "STRUCTURE",
      "title": "理解链表节点与指针"
    },
    "progress": {
      "completedTasks": 2,
      "totalTasks": 4,
      "percent": 50
    },
    "latestPractice": {
      "status": "EVALUATED",
      "score": 78,
      "weakPoints": ["头指针与头节点混淆"]
    },
    "nextAction": {
      "type": "GO_TO_REPORT",
      "label": "查看学习报告"
    }
  }
}
```

约束：

- 前端不能自己拼装 Session 页摘要
- Session 页仅消费该聚合模型

### 8.2 学习报告接口

`GET /api/sessions/{sessionId}/report`

用途：提供学习报告页完整聚合信息。

返回示例：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "req-002",
  "data": {
    "sessionId": "s1",
    "report": {
      "summary": "你已掌握链表基础结构，但在空指针判断上仍不稳定",
      "mastery": {
        "level": "PARTIAL",
        "score": 72
      },
      "questionEvaluations": [
        {
          "itemId": "p1",
          "result": "CORRECT",
          "comment": "理解了节点连接方式"
        }
      ],
      "weakPoints": [
        {
          "tag": "NULL_POINTER",
          "label": "空指针判断"
        }
      ],
      "nextStep": {
        "type": "REVIEW",
        "label": "进入强化复习"
      },
      "reason": "基础概念已具备，但关键边界处理不稳定"
    }
  }
}
```

约束：

- 报告页禁止自己调用多个底层接口拼数据
- `nextStep` 与 `reason` 必须由后端给出
- 前端只负责展示，不负责决策

### 8.3 私人知识图谱接口

`GET /api/users/me/knowledge-graph`

用途：为知识图谱页提供节点与关系视图。

返回示例：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "req-003",
  "data": {
    "nodes": [
      {
        "nodeId": "n1",
        "title": "链表基础结构",
        "status": "LEARNING",
        "masteryScore": 72
      }
    ],
    "edges": [
      {
        "from": "n0",
        "to": "n1",
        "type": "PREREQUISITE"
      }
    ],
    "recommendedPath": ["n1", "n2", "n3"]
  }
}
```

### 8.4 成长统计接口

`GET /api/users/me/growth-stats`

返回示例：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "req-004",
  "data": {
    "learnedNodeCount": 18,
    "courseMastery": 61,
    "recentPracticeTrend": [62, 68, 75, 73, 81],
    "topWeakAreas": ["空指针判断", "边界条件处理"]
  }
}
```

### 8.5 下一步动作接口

`GET /api/sessions/{sessionId}/next-action`

返回示例：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "req-005",
  "data": {
    "type": "REVIEW",
    "label": "进入强化复习",
    "reason": "你已掌握主体概念，但边界处理错误率仍偏高"
  }
}
```

---

## 9. 页面职责约束

页面约束的目标是：每个页面只承担一种核心职责。

### 9.1 首页 Home

职责：

- 新建学习
- 继续学习
- 查看最近学习摘要
- 进入成长中心

禁止承担：

- 展示完整知识图谱
- 展示完整学习报告
- 展示复杂路径细节

### 9.2 能力分析与方案确认页

职责：

- 呈现当前能力分析
- 展示推荐节奏
- 展示学习重点
- 展示本轮方案预览
- 引导确认开始学习

### 9.3 Session 页

职责：作为“当前会话导航中枢”。

必须展示：

- 当前知识点
- 当前任务
- 任务整体进度
- 最近训练摘要
- 当前薄弱点摘要
- 下一步推荐入口

禁止展示：

- 全文学习报告
- 逐题详细解析
- 全量知识图谱

### 9.4 Task Run 页

职责：学习执行页。

必须展示：

- 当前任务卡片
- 主学习内容区
- Tutor 弹出助手
- 当前阶段提示
- 进入检测按钮

禁止展示：

- 大量历史数据
- 全局复杂导航
- 成长仪表盘型信息

### 9.5 检测答题页

职责：做题与提交。

必须展示：

- 题目列表
- 做题进度
- 提交答案入口
- 评估中反馈状态

### 9.6 学习报告页

职责：结果导航页，而不是单纯结果展示页。

必须展示：

- 学会了什么
- 哪些点薄弱
- 当前节点掌握情况
- 系统建议下一步
- 推荐原因
- 知识图谱已更新提示

### 9.7 个人学习中心 / Dashboard

职责：长期成长视图。

必须展示：

- 私人知识图谱
- 学习成长曲线
- 历史报告档案
- 当前薄弱领域
- 推荐后续学习方向

---

## 10. 组件职责约束

### 10.1 组件分类

所有前端组件分为三类：

#### 1）展示组件

特点：

- 纯 props
- 不直接发请求
- 不直接修改 store

示例：

- `ProgressCard`
- `WeakPointList`
- `ReportSummaryCard`

#### 2）容器组件

特点：

- 拉取页面级数据
- 负责布局编排
- 组织子组件

示例：

- `SessionOverviewPanel`
- `LearningReportPanel`

#### 3）交互组件

特点：

- 有明确交互边界
- 触发页面动作
- 不负责大面积数据拼装

示例：

- `TutorDrawer`
- `PracticeSubmitButton`
- `NextActionPanel`

### 10.2 组件约束规则

- 展示组件禁止直接 import store
- 展示组件禁止直接调 API
- 页面级接口请求只能发生在 view 或 container 层
- 复杂业务判断尽量落在 store 或后端，不下沉到纯 UI 组件

### 10.3 命名约束

- 页面：`XxxView.vue`
- 容器：`XxxPanel.vue`
- 展示：`XxxCard.vue`
- 空态：`EmptyStatePanel.vue`
- 行为入口：`XxxActionBar.vue`

---

## 11. 前后端职责边界

### 11.1 后端负责什么

后端只对外负责三件事：

1. **业务事实是否成立**
2. **状态是否可流转**
3. **下一步应该做什么**

后端重点职责：

- 会话与任务编排
- 训练评估聚合
- 掌握度与薄弱点更新
- 用户知识图谱聚合
- 成长统计
- 能力分析与个性化方案
- 接口契约与状态机收口
- 关键链路测试与可观测性

### 11.2 前端负责什么

前端只对外负责三件事：

1. **用户现在在哪**
2. **用户现在该做什么**
3. **为什么这样展示更清楚**

前端重点职责：

- Session 页闭环增强
- TaskRun 页学习体验优化
- 学习报告页重构
- 知识图谱页
- 成长仪表盘
- 能力分析与方案确认页
- Tutor 助手体验优化
- 整体视觉与演示表现

### 11.3 一句约束

> **后端负责结论，前端负责呈现。**

前端不得自行推导业务结论，后端不得把页面展示逻辑塞进接口设计。

---

## 12. 目录与 ownership 建议

### 12.1 前端目录 ownership 建议

```text
frontend/src
├─ views/                 # 页面级视图，前端主负责人管理
├─ components/
│  ├─ cards/              # 纯展示卡片
│  ├─ panels/             # 容器组件
│  ├─ actions/            # 行为组件
│  └─ tutor/              # Tutor 相关组件
├─ stores/                # session / practice / tutor / feedback
├─ api/                   # API 调用封装
├─ constants/             # 枚举、文案映射
├─ utils/                 # 纯工具函数
└─ types/                 # 前端 DTO / ViewModel 类型
```

### 12.2 后端目录 ownership 建议

```text
backend/src/main/java/.../
├─ api/                   # Controller + DTO
├─ application/           # UseCase / Query / Command / Service
├─ domain/                # 模型 / 枚举 / 规则 / 仓储接口
├─ infrastructure/        # JDBC 仓储 / 配置 / 外部集成
└─ test/                  # 接口测试、状态流转测试、聚合测试
```

### 12.3 ownership 约束

- 前端不得绕过 API 直接依赖数据库语义
- 后端不得把页面结构知识硬编码进业务层
- 枚举和字段定义优先统一在契约文档中，再同步到前后端代码

---

## 13. 最小可落地开发顺序

### 第一优先级：做实训练驱动闭环

优先完成：

1. 学习反馈报告聚合接口
2. Session 页闭环增强
3. 学习报告页重构

目标：形成一个用户能强烈感知的闭环：

`学习 -> 检测 -> 反馈 -> 下一步建议`

### 第二优先级：补私人知识图谱与成长视图

优先完成：

1. 私人知识图谱接口与页面
2. 成长统计接口与仪表盘页面
3. 报告页中的图谱更新提示

目标：让系统从单次学习工具升级为长期成长系统。

### 第三优先级：补能力分析与方案确认页

优先完成：

1. 用户能力分析接口
2. 个性化学习方案接口
3. 学前分析页与方案确认页

目标：增强个性化感与系统高度。

---

## 14. 双人协作排期建议

### Phase 0：统一基线（1~2 天）

#### 后端

- 统一阶段枚举与术语输出
- 梳理现有 API 清单和字段定义
- 标记历史兼容点与待收口点

#### 前端

- 统一页面、路由、组件命名
- 梳理页面信息结构
- 建立阶段文案映射表

#### 产出

- 统一术语表
- 主流程图
- 接口与页面映射表

### Phase 1：训练驱动闭环（3~5 天）

#### 后端

- 完成 `overview` 聚合接口
- 完成 `report` 聚合接口
- 收口 `practice -> mastery -> feedback -> nextStep`

#### 前端

- 重构 Session 页
- 重构学习报告页
- 完成训练提交后的回流体验

#### 产出

- 可演示的闭环 MVP

### Phase 2：知识图谱与成长视图（4~6 天）

#### 后端

- 完成知识图谱查询接口
- 完成成长统计接口
- 补用户节点状态更新逻辑

#### 前端

- 完成知识图谱页
- 完成长仪表盘
- 在报告页嵌入图谱更新提示

#### 产出

- 知识图谱
- 成长仪表盘

### Phase 3：能力分析与方案确认（3~5 天）

#### 后端

- 完成能力分析接口
- 完成个性化学习方案接口

#### 前端

- 完成能力分析页
- 完成方案确认页

#### 产出

- 个性化起点体验

### Phase 4：竞赛展示打磨（2~4 天）

#### 后端

- 补关键链路测试
- 补日志与异常兜底
- 准备演示数据

#### 前端

- 统一视觉风格
- 强化首页、报告页、图谱页展示
- 增加必要动效与演示路径

#### 产出

- 稳定 demo
- 可答辩版本

---

## 15. 当前必须立即落地的交付物

为了真正提高开发效率，建议今天就先生成以下 3 份文件：

### 15.1 `CONTRACT.md`

内容包括：

- 统一术语表
- 状态机定义
- 核心对象定义
- 5 个高优先级接口契约
- 错误码表

### 15.2 `FRONTEND_PAGE_SPEC.md`

内容包括：

- 页面列表
- 每个页面职责
- 页面所需接口
- 页面禁止承担的职责
- 组件拆分建议

### 15.3 `BACKEND_AGGREGATION_PLAN.md`

内容包括：

- 现有底层数据源
- 需要聚合出的页面视图模型
- 聚合接口清单
- 状态流转触发点
- 规则决策初版

---

## 16. 最终结论

你们现在最需要的“约束”，不是更细的编码规范，而是：

> **一套稳定的产品语言 + 一套清晰的状态机 + 一套前端可直接消费的聚合接口 + 一套明确的页面职责边界。**

对你们这种两人协作小队而言，最优分工不是两边都各自补功能，而是：

- **后端做聚合、决策、图谱、画像**
- **前端做闭环、报告、图谱、展示**

一句话概括：

- **后端负责把系统变聪明**
- **前端负责把系统变清楚、变好看、变有说服力**

这就是当前阶段最靠谱、最能提高开发效率、也最适合竞赛推进的 Blueprint V1。
