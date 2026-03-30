# 执行页改造计划

## 页面定位

- 页面对应：`frontend/src/views/TaskRunView.vue`
- 核心目标：把执行页重构成“单主任务驱动的学习动作工作台”
- 页面任务：回答“我现在应该完成什么认知动作”

## 设计结论

- 这是整个产品的核心展示页，不能再保留聊天页气质
- 页面中心必须从“对话来回”切换到“任务卡 + 结构化产出 + 结构化反馈”
- AI 导师必须退居辅助位，不能常驻抢占主区

## 现状问题

- 当前 `TaskRunView.vue` 同时承载旧执行流、驾驶舱布局、脚手架引擎 UI，多套结构并存
- 页面内主区、侧栏、反馈、输入、浮动动作之间存在多中心竞争
- 用户会看到很多状态和模块，但不一定能快速知道“现在先做什么”
- 执行页还残留明显的聊天工作流痕迹，不够像教学型学习系统

## 本轮改造目标

- 默认状态下只突出一个主任务、一个输出区、一个下一步动作
- 构建“大阶段 + 小动作”的双层状态可视化
- 把 prompt 显性化为“思考支架”，而不是后台提示词
- 把用户输入区改造成结构化表达区
- 把反馈区改造成固定四块教学反馈
- 把 AI 导师改成右下角悬浮入口 + 侧边抽屉

## 总体信息架构

### 桌面端目标骨架

1. 顶部轻量栏
2. 左侧阶段轨
3. 中央主工作台
4. 右下角 AI 导师悬浮按钮

### 中央主工作台固定顺序

1. 当前任务卡
2. 系统给你的思考支架
3. 你的表达区
4. 本轮反馈
5. 底部动作条

## 页面重构策略

### 1. 先统一视图路线，去掉并存结构

- `TaskRunView.vue` 当前同时支持 `useScaffoldEngineUi` 与非脚手架布局
- 本轮要优先确定“单主任务工作台”是唯一主视图
- 旧的聊天驱动块、双栏驾驶舱块、散落反馈块都需要向统一主工作台收口
- 优先保留真正符合设计文档的组件，逐步淘汰重复表达的组件

### 2. 页面状态统一为双层

- 大阶段：`STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION`
- 小动作：`ORIENT / EXPLORE / SELF_EXPLAIN / CHECK / REMEDIAL / PASS`
- 表达方式：
  - 左侧阶段轨显示大阶段
  - 当前任务卡副标题显示小动作
  - 所有提示和按钮都围绕当前小动作展开

## 模块级改造计划

### A. 顶部轻量栏重做

- 重点组件：`ExecutionHeader.vue`
- 目标：只保留必要定位信息
- 建议信息：
  - 当前知识点
  - 当前阶段
  - 当前进度
  - 退出/返回
- 禁止放大段说明或复杂统计

### B. 左侧阶段轨强化

- 重点组件：`ExecutionProgressRail.vue`、`StageProgressMiniCard.vue`，必要时合并重做
- 目标：持续告诉用户这是一轮阶段化学习，不是自由问答
- 每个阶段只显示：
  - 阶段名
  - 一句话目标
  - 状态
- 当前阶段高亮，已完成阶段打勾，未开始阶段弱化

### C. 当前任务卡设为全页主角

- 重点组件：`PrimaryTaskCard.vue` 或 `ExecutionMainActionCard.vue`，建议收敛为单一主卡
- 固定五段结构：
  - 当前阶段 / 当前动作
  - 本步目标
  - 为什么先做这一步
  - 产出要求
  - 完成标准
- 任务卡必须成为视觉中心，其他模块全部为辅助

### D. Prompt 脚手架区 UI 化

- 重点组件：`ScaffoldGuideCard.vue`、`ScaffoldActionCard.vue`、`ScaffoldSectionCard.vue`
- 目标：把 prompt 拆成可执行认知支架，不直接展示长 prompt
- 固定为 3 个渐进式支架：
  - 先想什么
  - 再补什么
  - 最后落到哪里
- 每个支架支持三级帮助：
  - 轻提示
  - 标准提示
  - 加强提示

### E. 用户产出区改造成结构化表达区

- 重点组件：`ExpressionWorkspace.vue`、`UserExpressionPanel.vue`
- 目标：不能再是大聊天框
- 不同阶段使用不同表达结构：
  - `STRUCTURE`：对比式字段填写
  - `UNDERSTANDING`：问题 / 机制 / 结果
  - `TRAINING`：用自己的话解释 + 场景验证
  - `REFLECTION`：错因 / 规则 / 下次检查
- 输入区底部保留一句低门槛引导语

### F. 反馈区改造成教学反馈板

- 重点组件：`FeedbackSummary.vue`、`ExecutionSystemFeedback.vue`、`FeedbackPanel.vue`
- 固定成四块：
  - 你已经说对了什么
  - 你漏了什么
  - 你混淆了什么
  - 下一步该怎么修
- 不再返回大段模型作文式反馈
- 反馈内容要能精确指向结构化字段，而不是泛泛评价

### G. 底部动作条压缩

- 重点组件：`StickyActionBar.vue`、`BottomActionBar.vue`
- 页面任意时刻不超过 3 个主动作
- 推荐只保留：
  - 查看提示
  - 提交本轮表达
  - 进入下一步
- 其他动作做次级入口或折叠处理

### H. AI 导师降级为辅助位

- 重点组件：`AiTutorFloating.vue`、`AiTutorPanel.vue`、`TutorActionPanel.vue`
- 默认只展示一个悬浮按钮：`不懂这一步？`
- 打开后进入右侧抽屉，不再常驻大面板
- 顶部只提供三类快捷帮助：
  - 帮我解释这一步要求
  - 给我一个更容易理解的提示
  - 这个术语是什么意思
- 禁止开放成“你可以问我任何问题”的聊天主区

## 四阶段差异化计划

### STRUCTURE

- 关键词：区分、归位、建立边界
- 页面气质：冷静、清晰、框架感
- 交互重点：对比式输入、概念位置、小步确认

### UNDERSTANDING

- 关键词：因果、过程、机制
- 页面气质：推演感、过程感
- 交互重点：因果链、步骤排序、没有它会怎样

### TRAINING

- 关键词：复述、验证、暴露错误
- 页面气质：参与感更强，接近答辩
- 交互重点：自我解释、举例验证、判断错误说法

### REFLECTION

- 关键词：错因、规则、迁移
- 页面气质：收束、沉淀、方法化
- 交互重点：提取错误模式、写下判断规则、总结可迁移策略

## 实施拆分建议

### 第一阶段：统一骨架

1. 整理 `TaskRunView.vue`，明确唯一主视图
2. 固定页面总骨架：顶部栏 + 阶段轨 + 主工作台 + AI 悬浮入口
3. 压缩旧侧栏和重复反馈模块

### 第二阶段：重建主工作台

1. 收敛成一个主任务卡
2. 重做脚手架区呈现逻辑
3. 将输入区改为结构化表达区
4. 重做反馈区四块结构

### 第三阶段：阶段差异化

1. 为四阶段配置差异化文案模板
2. 为四阶段配置差异化输入结构
3. 为四阶段配置差异化视觉语义

### 第四阶段：辅助系统收口

1. AI 导师改抽屉
2. 历史消息、补充资料、长反馈改为折叠内容
3. 底部动作条统一到最多 3 个主动作

## 优先修改文件

- `frontend/src/views/TaskRunView.vue`
- `frontend/src/components/task-run/ExecutionHeader.vue`
- `frontend/src/components/task-run/PrimaryTaskCard.vue`
- `frontend/src/components/task-run/ScaffoldGuideCard.vue`
- `frontend/src/components/task-run/ExpressionWorkspace.vue`
- `frontend/src/components/task-run/UserExpressionPanel.vue`
- `frontend/src/components/task-run/FeedbackSummary.vue`
- `frontend/src/components/task-run/StickyActionBar.vue`
- `frontend/src/components/ai-tutor/AiTutorFloating.vue`
- `frontend/src/components/ai-tutor/AiTutorPanel.vue`

## 验收标准

- 用户进入执行页后，第一眼只能看到一个明确主任务
- 用户能快速回答：
  - 我当前在哪个阶段
  - 我当前要完成什么动作
  - 我要产出什么
  - 提交后会看到什么反馈
- 页面不再像聊天产品，不再出现三栏信息平台感
- AI 导师退居辅助位，不抢主区
- 四阶段在页面气质、输入方式、反馈方式上都有明显差异

## 备注

- 执行页是本轮最重的改造点，建议后续实施时先做信息架构收束，再做视觉打磨
- 现有组件较多，优先策略应是“合并和收敛”，不是继续新增平行组件

