1. 项目架构定位

本系统定位为：

面向大学生专业知识学习场景的 AI 学习流程导航系统

它不是传统聊天机器人，也不是单纯题库系统，而是将大语言模型嵌入到“目标设定—路径规划—分步学习—总结反馈”的完整学习闭环中，帮助用户从“零散提问”升级为“结构化学习”。

2. 总体架构分层

建议你在文档里写成这 5 层：

① 表现层（Presentation Layer）

负责用户交互与学习流程可视化。

包含：

Web 前端 / 小程序前端

首页（创建学习会话）

Session 学习流程页

Task 任务详情页

历史记录页

登录注册页

核心职责：

展示学习四步流程

展示任务清单、任务详情、进度状态

提供 AI 对话入口、继续学习入口、历史恢复入口

② 应用层（Application Layer）

负责流程编排和接口聚合，是“业务流程控制中心”。

核心模块：

Auth 应用服务

Session 应用服务

Learning Workflow 应用服务

Task 应用服务

Progress 应用服务

History 应用服务

Tutor 应用服务

Quiz 应用服务

Summary / Reflection 应用服务

核心职责：

接收前端请求

编排 session 创建、plan 生成、task 推进、学习反馈等过程

控制四步流程状态流转

调用 AI 能力层生成内容

聚合数据库与缓存数据返回前端

③ 领域层（Domain Layer）

负责承载系统核心业务对象与规则。

建议的核心领域对象：

AppUser

LearningSession

LearningTask

LearningEvent

LearningProgress

TaskQuiz

TutorMessage

KnowledgeNode

LearningRecommendation

核心业务规则：

一个用户可以有多个学习会话

一个学习会话对应一个学习目标和章节上下文

一个会话包含多个任务

任务按学习阶段推进

任务可附带 AI 讲解、小测、总结、推荐下一步

系统根据任务完成度与测验结果动态调整学习建议

④ AI 能力层（AI Capability Layer）

这是你后续最重要的亮点层。

建议拆成 4 个子模块：

A. 路径规划引擎

输入：

学习目标

课程标识 / 章节标识

用户当前阶段

输出：

学习路径

分步任务清单

每步任务目标与预估耗时

B. AI Tutor 引擎

输入：

当前任务内容

用户提问

当前 session 上下文

输出：

概念解释

例题讲解

提示式引导

错误纠正

C. Quiz / Check 引擎

输入：

当前任务知识点

难度级别

用户作答结果

输出：

练习题 / 小测题

自动判定结果

掌握情况反馈

D. Summary / Strategy 引擎

输入：

已完成任务

错题 / 小测结果

用户学习事件

输出：

阶段性总结

知识盲点分析

下一步推荐

是否回退补基础/继续进阶

⑤ 数据层（Data Layer）

建议在文档里明确分成：

关系型数据库（PostgreSQL）

存储：

用户

会话

任务

学习事件

测验记录

总结记录

历史学习状态

缓存层（Redis，可后续加入）

存储：

会话上下文缓存

AI 临时生成结果缓存

高频历史查询缓存

Tutor 对话上下文缓存

向量检索层（后续 RAG）

存储：

教材片段 embedding

知识点解释 embedding

题目与解析 embedding

3. 核心业务流程架构

你可以在文档里这样写：

流程一：创建学习会话

用户输入学习目标、课程标识、章节标识

系统创建 LearningSession

AI 路径规划引擎生成学习路径

系统生成分步任务清单

前端进入四步学习流程页

流程二：执行任务学习

用户进入当前任务详情

查看任务目标、关键点、总结

通过 AI Tutor 进行提问与讲解

系统生成小测或练习题

用户提交作答结果

系统记录学习事件并更新任务状态

流程三：学习反馈与继续学习

系统根据当前进度和测验结果计算掌握度

生成阶段总结与下一步建议

用户可继续当前推荐任务或恢复历史会话

系统维护完整学习轨迹

4. 推荐的核心模块图

你文档里可以直接写成：

[ Frontend / Mini Program ]
        |
        v
[ API Controller Layer ]
        |
        v
[ Application Services ]
  |- AuthService
  |- SessionService
  |- TaskService
  |- TutorService
  |- QuizService
  |- ProgressService
  |- SummaryService
        |
        +----------------------+
        |                      |
        v                      v
[ Domain Model ]          [ AI Capability Layer ]
                              |- Path Planner
                              |- AI Tutor
                              |- Quiz Generator
                              |- Summary Engine
        |                      |
        +----------+-----------+
                   |
                   v
            [ PostgreSQL / Redis / Vector Store ]
5. 当前 MVP 到下一阶段的演进路线
当前 MVP

登录注册

学习会话创建

四步流程页

历史记录

resume session

任务详情展示

下一阶段增强

AI Tutor 实时问答

每任务小测

学习统计面板

当前推荐任务

知识结构图

自适应学习建议

比赛增强版

RAG 教材支持

学习策略引擎

错误诊断与回退学习

多端支持（Web / 小程序 / RN）