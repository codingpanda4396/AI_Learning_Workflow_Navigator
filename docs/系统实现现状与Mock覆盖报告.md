# 系统实现现状与 Mock 覆盖报告

## 1. 报告说明

本项目当前定位是一个 **AI 学习工作流系统**，而不是通用聊天产品。  
系统核心闭环为：

`目标 -> 诊断 -> 规划 -> 执行 -> 报告`

本报告基于当前代码仓库的实际实现情况整理，统计口径以仓库内现有前后端代码、接口、常量配置、测试与页面实现为准，不以未来规划或演示话术为准。

本报告重点回答四个问题：

- 目前系统已经覆盖了哪些业务知识点与学习流程能力
- 哪些能力已经真实打通，哪些仍属于演示态、mock、fallback 或 showcase 覆盖
- 前端、后端各自达到了什么工程成熟度
- 当前阶段适合如何对外表述，哪些部分需要明确说明并非全量真实生产能力

---

## 2. 当前已实现的知识点与学习覆盖

### 2.1 已配置的首页知识点入口

当前前端首页的知识点入口来自 [frontend/src/constants/homeQuickStart.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\constants\homeQuickStart.ts)，属于“已配置可发起学习入口”的知识点集合。

当前共配置：

- 4 个学科
- 每个学科 5 个主题
- 合计 20 个快速开始主题

四个学科分别为：

- 操作系统
- 计算机网络
- 数据结构
- 组成原理

20 个主题分别为：

- 操作系统：进程与线程、页面置换、虚拟内存、调度算法、死锁
- 计算机网络：TCP 三次握手、流量控制、拥塞控制、HTTP 缓存、路由转发
- 数据结构：DFS vs BFS、堆与优先队列、并查集、拓扑排序、二叉树遍历
- 组成原理：缓存与局部性、流水线冒险、中断处理、指令周期、总线与 DMA

需要明确的是，这里的“已覆盖”指的是：

- 前端已有对应主题入口
- 可从该入口发起目标创建
- 可进入后续诊断、规划、执行与报告流程

它**不等于**系统已经完成了全量 408 知识体系建模，也不等于后端拥有完整知识图谱或全学科精细化内容生产能力。

### 2.2 当前学习流程能力覆盖

从业务流程看，当前系统已经具备一条可运行的主链路：

- 目标创建：把学习意图组织成结构化目标输入
- 诊断：通过固定题组快速判断当前状态与主要卡点
- 规划：生成推荐策略、阶段与任务
- 执行：围绕当前任务进入受约束的学习交互
- 报告：汇总证据、判断结果并给出下一步建议

也就是说，系统当前的“覆盖”重点并不是海量知识点数量，而是**围绕一轮学习闭环的工作流能力**。

### 2.3 演示强化知识点

除通用主题入口外，当前规划页还存在一层专门用于演示表达的 showcase 编排，相关配置位于：

- [frontend/src/constants/showcaseKnowledgeConfig.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\constants\showcaseKnowledgeConfig.ts)
- [frontend/src/constants/goalShowcasePresets.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\constants\goalShowcasePresets.ts)

当前被重点强化展示的知识点包括：

- 数组 / 链表对比
- 二叉树基础
- DFS vs BFS

这些知识点在规划页具有更强的视觉编排与演示表达，但这属于**前端展示层增强**，不能直接等同于后端已经具备同等粒度的知识引擎。

---

## 3. 真实实现 vs Mock / 演示覆盖

## 3.1 已真实打通的能力

### 目标创建

前端通过 [frontend/src/api/goals.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\api\goals.ts) 真实调用 `/api/goals`。  
后端入口为 [backend/src/main/java/navigator/api/controller/GoalController.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\api\controller\GoalController.java)，由 `GoalApplicationService` 完成：

- 原始目标输入接收
- 规则推导为结构化目标
- 目标上下文快照生成
- `learning_goal` 数据落库

因此，目标创建不是纯前端假数据。

### 诊断会话与提交

前端通过 [frontend/src/api/diagnosis.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\api\diagnosis.ts) 真实调用：

- `/api/diagnosis/sessions`
- `/api/diagnosis/submissions`

后端对应 [backend/src/main/java/navigator/api/controller/DiagnosisController.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\api\controller\DiagnosisController.java) 与 `DiagnosisApplicationService`，已完成：

- 诊断会话创建
- 固定题组生成
- 诊断答案归一化
- 学习者画像生成
- 诊断证据摘要生成
- 学习策略画像生成
- 诊断会话、答案、画像等持久化

这里的诊断逻辑是真实存在的，但当前题组是**固定结构化题组**，不是开放式自适应题库。

### 规划预览与提交

前端通过 [frontend/src/api/learning-plan.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\api\learning-plan.ts) 真实调用：

- `/api/learning-plans/preview`
- `/api/learning-plans/commit`

后端对应 [backend/src/main/java/navigator/api/controller/LearningPlanController.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\api\controller\LearningPlanController.java) 与 `PlanningApplicationService`，已完成：

- 规划上下文组装
- 规划策略选择
- 阶段与任务生成
- 计划预览落库
- 计划提交
- 会话任务物化
- 可执行任务规格生成

因此，规划页不是纯文案展示，而是有真实后端规划结果承接。

### 执行主链路

前端通过 [frontend/src/api/task.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\api\task.ts) 与 [frontend/src/api/session.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\api\session.ts) 真实调用执行与报告相关接口。  
后端对应控制器为：

- [backend/src/main/java/navigator/api/controller/TaskController.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\api\controller\TaskController.java)
- [backend/src/main/java/navigator/api/controller/SessionController.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\api\controller\SessionController.java)

当前已打通的执行能力包括：

- 获取当前任务
- 获取当前任务引导信息
- 获取任务脚手架
- 发送任务消息
- 自我解释提交
- checkpoint 提交
- 任务完成提交
- 报告获取
- 下一步动作确认

因此，执行页和报告页都不是前端纯 mock 壳。

### 报告页

前端报告页 [frontend/src/views/ReportView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\ReportView.vue) 真实通过 `/api/sessions/{sessionId}/report` 获取数据。  
后端 `ReportApplicationService` 会聚合执行证据、学习方法画像并生成：

- 学习结果状态
- 已完成进展
- 未解决问题
- 证据摘要
- 总结
- 下一步建议

报告页因此属于真实业务链路的一部分。

## 3.2 当前存在的 Mock / Fallback / 演示层

### `FixedSampleData` 仍作为后端兜底样例来源

[backend/src/main/java/navigator/application/FixedSampleData.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\FixedSampleData.java) 仍保留了一批固定样例数据，用于若干场景的兜底。

这意味着当前系统中仍有部分内容带有固定样例色彩，例如：

- 部分任务蓝图兜底
- 部分说明文案兜底
- 部分报告 / 任务解释类内容兜底

因此，当前后端并非所有任务内容都来自完整的动态知识生成。

### LLM 不可用时会回退到 Mock 或模板

后端存在 [backend/src/main/java/navigator/application/llm/MockLlmGateway.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\llm\MockLlmGateway.java) 与 [backend/src/main/java/navigator/application/llm/TaskTutorOrchestrator.java](D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\navigator\application\llm\TaskTutorOrchestrator.java)。

当前 AI Tutor / 任务执行内 Tutor 的真实状态是：

- 可以接真实 LLM
- 当真实 LLM 不可用、关闭或报错时，会回退到 mock 回复或模板回复

因此，系统的 AI 能力是“**可接真实模型 + 保留稳定 fallback**”的工程实现，而不是强依赖真实模型在线返回。

### 规划页 showcase 属于展示层增强

规划页对数组/链表、二叉树基础、DFS vs BFS 的演示增强，主要来自前端常量配置与匹配逻辑，而不是后端单独提供的专业知识编排引擎。

这说明：

- 页面演示效果已较强
- 但其底层仍以配置驱动的展示增强为主
- 不能表述为“系统已具备全量知识图谱可视化编排能力”

### 首页的 4 学科 / 20 主题是可演示入口，不是全量学科建模

目标页当前确实具备学科与知识点选择入口，但当前真实可证实的事实是：

- 已配置 4 个学科
- 每学科 5 个主题
- 用于快速发起学习流程

因此对外更准确的说法应是“**系统当前已配置一组代表性学科与主题入口用于完整工作流演示**”，而不是“已完成 408 全量知识点建设”。

### 前端流程恢复主要依赖关键 ID

[frontend/src/stores/workflow.ts](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\stores\workflow.ts) 当前主要通过 `sessionStorage` 持久化：

- `goalId`
- `diagnosisId`
- `planId`
- `sessionId`
- `structuredGoal`

这意味着前端可恢复的是**关键流程标识与少量上下文**，并不是完整后端会话快照恢复能力。当前的流程连续性更多是“前端状态恢复 + 后端接口补取”，而不是成熟的全量会话恢复机制。

---

## 4. 前端技术报告

## 4.1 技术栈

当前前端技术栈为：

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Tailwind CSS
- Axios

整体属于轻量、适合快速迭代的前端方案，没有引入重型 UI 框架或复杂前端中台体系。

## 4.2 当前前端结构

前端当前大致采用以下分层：

- `views/`：承载流程页与核心业务页面
- `components/`：承载页面编排、计划展示、执行交互、AI Tutor 等复用组件
- `api/`：统一封装后端请求
- `stores/`：维护工作流状态、认证状态、AI Tutor 状态等
- `utils/`：视图模型转换、诊断提交映射、执行页模型拼装等辅助逻辑
- `types/`：DTO、枚举和类型定义

这种结构已经明显围绕“学习流程产品”而不是“聊天容器”展开。

## 4.3 当前前端能力重点

### 目标页

目标页 [frontend/src/views/GoalInputView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\GoalInputView.vue) 已经不是自由文本输入主导，而是“快速开始优先”的交互形态，符合当前仓库中的产品规则：

- 快速开始优先
- 学科与知识点可选
- 自动拼装目标请求
- 登录后可继续上次学习

### 规划页

规划页 [frontend/src/views/LearningPlanView.vue](D:\Panda_Code\AI_Learning_Workflow_Navigator\frontend\src\views\LearningPlanView.vue) 已经具备：

- 四阶段学习流程可视化表达
- 推荐阶段与当前问题解释
- 时间线展示
- 阶段展开与开始动作

也就是说，规划在前端已经被表现为“学习编排”，而不只是说明文字。

### 执行页

执行相关页面与组件当前强调“任务脚手架中心”，而不是开放式聊天中心，符合仓库当前产品原则。  
核心表现包括：

- 当前任务目标
- 推荐提问模板
- 自我解释
- checkpoint
- 反馈与补救
- 任务收束

AI Tutor 虽然存在，但它是嵌入式辅导能力，不是主页面的唯一核心。

### 报告页

报告页已支持：

- 学习结果展示
- 目标回顾
- 已完成进展
- 待解决问题
- 证据摘要
- 学习方法画像
- 下一步确认

因此，前端已经能够完整承接一次学习闭环的终点展示。

## 4.4 当前前端边界

当前前端仍有几个明显边界：

- 真实接口数据与展示型常量并存
- 工作流状态较强依赖 `workflow` store 与 `sessionStorage`
- 历史会话、跨端连续性、多会话并存能力仍较弱
- 自动化前端测试痕迹有限，当前验证方式仍偏 `build + 手工可视化检查`

整体判断：前端已经达到**演示级成熟度较高、流程表达较完整**的状态，但还未进入复杂前端平台化阶段。

---

## 5. 后端技术报告

## 5.1 技术栈

当前后端技术栈为：

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring WebFlux
- MyBatis-Plus
- PostgreSQL
- Flyway
- Caffeine
- H2（测试）

这说明后端已经具备同步接口、流式输出、数据库迁移、缓存与测试环境支撑等基本工程能力。

## 5.2 当前后端分层

后端当前采用较清晰的分层组织：

- `api`：控制器、DTO、统一响应与异常处理
- `application`：业务服务、规则、规划、执行、报告、LLM orchestration
- `domain`：领域模型与枚举
- `infrastructure`：持久化、缓存、LLM 接入、配置、内存态

这套结构已经不是简单 CRUD 项目结构，而是较明显地围绕业务流程进行组织。

## 5.3 当前已实现的核心业务模块

### Goal

`GoalApplicationService` 已实现：

- 目标结构化
- 上下文快照生成
- 目标数据持久化

### Diagnosis

`DiagnosisApplicationService` 已实现：

- 固定诊断题组生成
- 诊断答案归一化
- 学习者画像生成
- 证据摘要生成
- 学习策略画像生成
- 诊断结果持久化

### Planning

`PlanningApplicationService` 已实现：

- 规划上下文组装
- 规划策略选择
- 阶段与任务生成
- 计划预览落库
- 计划提交
- 任务物化
- 可执行任务规格生成

### Execution

`ExecutionApplicationService`、`TaskExecutionFlowService` 等模块已实现：

- 任务状态推进
- 执行交互处理
- 自我解释与 checkpoint
- 执行证据累积
- 任务完成
- 方法画像沉淀

### Report

`ReportApplicationService` 已实现：

- 会话证据聚合
- 学习报告生成
- 学习方法表现汇总
- 下一步动作建议

## 5.4 当前后端边界

当前后端虽然已经形成完整业务主链路，但仍存在一些典型的 MVP / 过渡期特征：

- `InMemoryStore` 仍承担一部分运行态
- 部分 ID 处理与状态推进仍依赖字符串规则与内存映射
- 固定样例数据仍用于部分兜底
- LLM 接入是“可接真实模型 + 可回退 mock”，并非所有场景都依赖真实在线模型

整体判断：后端已经具备**完整工作流后端骨架与核心服务能力**，但尚未完全摆脱原型期遗留的内存态与固定样例依赖。

---

## 6. 业务报告

## 6.1 当前最成熟的业务资产

从业务价值角度看，当前最成熟的部分不是某个单点页面，而是以下三项：

### 学习流程闭环

系统已经能够把学习过程组织成完整闭环：

- 目标进入
- 当前状态识别
- 规划生成
- 任务执行
- 结果总结
- 下一步建议

这是项目当前最核心的业务资产。

### 任务执行状态机 / 引导机制

执行阶段当前已经不是简单聊天，而是更接近“受约束的教学执行流”：

- 有任务目标
- 有推荐问法
- 有自我解释
- 有 checkpoint
- 有补救与收束

这部分构成了项目相较普通聊天产品最有辨识度的地方。

### 报告与下一步决策

报告模块不是简单的完成提示，而是尝试基于执行证据对本轮学习作出判断，并提供下一步建议。  
这让系统从“过程推动器”开始向“学习策略调节器”演进。

## 6.2 当前仍偏 MVP / 演示态的部分

当前系统仍有一些部分更适合以“已完成 MVP、适合演示验证”的口径来表达：

- 知识点覆盖深度仍有限，目前以代表性主题入口为主
- 多数规划与诊断仍是规则驱动，而不是开放式智能生成
- 部分任务内容、Tutor 响应与说明文本仍存在模板 / mock / fallback 支撑
- 长周期学习资产、跨 session 复用、完整历史学习体系尚未真正成熟
- 首页展示能力强于真实知识体系覆盖广度

## 6.3 当前阶段的对外表述建议

当前更适合对外使用的表述是：

> 当前系统已完成核心学习闭环与演示级前端体验，部分教学内容与 AI 响应仍采用规则、模板和 mock/fallback 混合支撑。

如果要进一步展开，可补充为：

> 项目当前最大的完成度不在“海量知识点覆盖”，而在“把 AI 学习过程组织成结构化工作流”。前后端主链路已经打通，但在知识深度、长期资产沉淀与全量内容真实化方面，仍处于 MVP 向工程化系统过渡阶段。

---

## 7. 结论

综合当前代码仓库现状，可以给出一个较稳妥的判断：

- 该项目已经不是单纯的演示页面集合
- 它已经形成了真实可运行的学习工作流主链路
- 前后端在目标、诊断、规划、执行、报告五段上均已有实际实现
- 但系统中仍同时存在展示增强层、固定样例层、模板层与 mock/fallback 层

因此，当前最准确的项目定位应是：

**一个已经打通核心学习闭环、具备较强演示能力与初步工程化基础的 AI 学习工作流 MVP 系统。**

它最值得强调的不是“已经覆盖全部知识点”，而是：

- 已经把学习过程拆成可组织、可引导、可记录、可复盘的流程
- 已经完成面向演示和评审场景所需的主链路实现
- 已经具备继续向更深知识覆盖和更强工程稳定性演进的基础
