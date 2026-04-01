# 执行页页面蓝图（DFS/BFS）

## 页面总原则

- 执行页是一个路由、同一工作台内四个阶段切换，不是四个分散页面。
- 页面固定骨架：顶部阶段信息 + 中部双栏 + 底部下一步动作。
- 禁止聊天式结构：不出现气泡流、对话时间线、会话历史堆叠。

## 路由约束

- 主路由：`/tasks/:taskId/run?sessionId=xxx&phase=...`
- `phase` 仅允许：`structure | understanding | training | reflection`

## 固定页面骨架

### 顶部固定区

- 组件：`StageWorkbenchHeader`
- 必须展示：当前阶段、当前目标、总进度（阶段条）

### 中部双栏

- 左栏主学习区（固定顺序）
  1. `StageIntroCard`
  2. `ExplanationBlockList`
  3. `PhaseInteractionHost`
  4. `PhaseFeedbackCard`（仅 `renderState=feedback` 显示）
- 右栏认知动作库
  - 组件：`PromptScaffoldPanel`
  - 行为：点击仅追加 explanation，不改输入框，不产生聊天回复

### 底部动作栏

- 组件：`BottomActionBar`
- 仅负责阶段推进 CTA

## 四阶段首屏定义

### STRUCTURE

- 目标：建立 DFS/BFS 轮廓差异
- 主交互：单题选择题（选后锁定）
- 禁止：文本输入、发送按钮、聊天记录

### UNDERSTANDING

- 目标：解释现象背后的机制
- 主交互：机制类选择题（为什么）
- 禁止：重复结构辨析题语义

### TRAINING

- 目标：用户自主表达
- 主交互：单任务表达卡（标题 + 要求 + 单输入框 + 提交）
- 唯一允许文本输入的阶段

### REFLECTION

- 目标：收束混淆点，提炼迁移策略
- 主交互：`SystemSummaryCard` + `ReflectionQuestionCard` + `ReflectionStrategyCard`
- 禁止：引入新教学任务

## 禁止项清单

- 禁止 assistant/user 聊天气泡流
- 禁止 scaffold 直接填充输入框
- 禁止把旧 task-driven 主容器继续作为执行页主路径
- 禁止在入口页拼装完整阶段 UI
