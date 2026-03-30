# 规划页改造计划

## 页面定位
- 页面对应：`frontend/src/views/LearningPlanDecisionView.vue`
- 核心目标：体现“学习决策”，而不是堆叠“学习陈述”
- 页面任务：回答“为什么从这里开始”“接下来会怎么学”“现在就该点什么”

## 设计结论
- 规划页要从“信息展示页”收束成“单决策确认页”
- 当前页面已经有 `PlanDecisionHero`、`PlanReasonBlock`、`PlanCausalChain`、`PlanFirstTaskCard`、`PlanPathPreview`、`PlanContrastBlock`
- 但从设计文档看，规划页应该进一步去卡片化、去说明化，把视觉中心压到一个明确的学习决策上

## 现状问题
- 信息块偏多，用户视线容易在多个模块之间游移
- “为什么这样规划”与“接下来怎么开始”没有形成足够强的单线叙事
- 四阶段路径虽然已存在，但还需要更像“即将进入的学习路线”，而不是补充展示
- 页面底部缺少足够强的大按钮收口，导致决策闭环不够干脆

## 改造目标
- 首屏直接让用户看见一个明确结论：这轮学习为什么从这个阶段开始
- 用最少文字解释诊断因果
- 清楚预告四阶段路径，但不把路径讲成复杂说明
- 页面最后收束到一个强主动作：`开始学习`

## 信息架构调整

### 1. 页面压缩为四段主结构
1. 顶部决策 Hero
2. 中部原因解释
3. 下部四阶段路径预览
4. 底部唯一主 CTA

### 2. 需要降级或折叠的信息
- 对比说明块只能作为辅助，不应与 Hero 抢中心
- 因果链若保留，应压缩为短句式判断链，而不是多个说明卡
- 首任务卡需要保留，但定位应从“另一张信息卡”转成“开始前的预告”

## 模块级改造计划

### A. 顶部决策 Hero 重做
- 重点组件：`PlanDecisionHero.vue`
- 目标：首屏一句话说清这轮学习的起点决策
- 建议内容结构：
  - 大标题：学习规划
  - 决策结论：先从某一阶段开始
  - 一句原因：基于诊断暴露出的关键卡点
  - 一个预期结果：完成这一步后你会获得什么
- 视觉上让 Hero 成为绝对主角，其他模块全部降级

### B. 原因解释区收敛
- 重点组件：`PlanReasonBlock.vue`、`PlanCausalChain.vue`
- 目标：只解释“为什么这样安排”，不展开长篇业务说明
- 建议改法：
  - 将当前原因内容压缩为 2 到 3 条短判断
  - 优先展示“当前卡点 -> 先做什么 -> 避免什么问题”
  - 如果 `PlanCausalChain` 保留，改为一条线性因果链，而非多块并列卡片

### C. 四阶段路径预览强化
- 重点组件：`PlanPathPreview.vue`
- 目标：让用户看到完整学习路线，但知道“当前只需要开始第一步”
- 建议改法：
  - 只保留四阶段名称、阶段目标、一行当前状态
  - 当前推荐起点高亮，其他阶段弱化
  - 路径应更像路线图，不是内容清单
- 这里要明确体现：`STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION`

### D. 首任务预告卡重定位
- 重点组件：`PlanFirstTaskCard.vue`
- 目标：告诉用户一进入执行页会先做什么
- 建议改法：
  - 不再展开很多细节
  - 只展示“第一步任务是什么、为什么先做它、预计多久”
  - 与 Hero 形成上下衔接，不变成并列主角

### E. 对比说明块降级
- 重点组件：`PlanContrastBlock.vue`
- 目标：保留“为什么不是另一种安排”的辅助证据
- 建议改法：
  - 放到页面靠后位置
  - 视觉降级为次级说明
  - 文案改成短句，避免论文化

### F. 底部主按钮强化
- 当前 `@start` 已存在，但页面需要更明确的大按钮收口
- 文案统一成 `开始学习`
- 按钮周围不再放多个同级动作，确保单一主动作成立

## 文案策略
- 全部使用短句、指令式、产品式中文
- 尽量避免“因为系统分析认为你目前存在……”这类 AI 解释腔
- 推荐文案结构：
  - 当前更适合先做：
  - 这是因为：
  - 做完你会先得到：
  - 接下来按四步推进：

## 实施顺序
1. 重构 `LearningPlanDecisionView.vue` 的页面顺序，压缩为四段主结构
2. 优先改 `PlanDecisionHero.vue`，建立首屏单中心
3. 收敛 `PlanReasonBlock.vue` 与 `PlanCausalChain.vue` 的文本量
4. 改 `PlanPathPreview.vue`，强化四阶段路线预览
5. 调整 `PlanFirstTaskCard.vue` 与 `PlanContrastBlock.vue` 的主次关系
6. 统一 CTA 文案与底部收口动作

## 验收标准
- 用户进入规划页后，5 秒内能回答：
  - 为什么从这里开始
  - 接下来会按哪四步学
  - 现在该点击什么
- 首屏只有一个明确结论，不再出现多个主卡并列竞争
- 页面文字显著减少，但决策感更强
- 四阶段路径预览清楚，且不会把用户带入复杂阅读

## 关联文件
- `frontend/src/views/LearningPlanDecisionView.vue`
- `frontend/src/components/plan/decision/PlanDecisionHero.vue`
- `frontend/src/components/plan/decision/PlanReasonBlock.vue`
- `frontend/src/components/plan/decision/PlanCausalChain.vue`
- `frontend/src/components/plan/decision/PlanFirstTaskCard.vue`
- `frontend/src/components/plan/decision/PlanPathPreview.vue`
- `frontend/src/components/plan/decision/PlanContrastBlock.vue`

## 备注
- 规划页这一轮建议以“收束信息架构”为主，不优先新增更多模块
- 如果后续只保留一个规划页入口，建议以 `LearningPlanDecisionView.vue` 作为主实现，避免与旧的 `LearningPlanView.vue` 形成双轨设计
