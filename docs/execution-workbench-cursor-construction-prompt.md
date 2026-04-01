# 可直接投喂 Cursor 的执行页施工 Prompt

你要改造执行页，目标是把它固定为 **phase-driven 学习工作台**，并严格避免退化为聊天页或 task-driven 拼装页。

## 一、硬约束（必须满足）

1. 执行页只有一个主路由：`/tasks/:taskId/run?sessionId=xxx&phase=...`
2. `phase` 仅允许：`structure | understanding | training | reflection`
3. 页面固定骨架：顶部阶段信息 + 中部双栏 + 底部 CTA
4. 左栏固定顺序：`StageIntroCard -> ExplanationBlockList -> PhaseInteractionHost -> PhaseFeedbackCard(条件显示)`
5. 右栏固定：`PromptScaffoldPanel`（语义是认知动作库）
6. `renderState` 只在页面状态中流转：`prompt | think | output | feedback`，禁止写入 URL

## 二、禁止项（绝对不能出现）

- 聊天气泡流、assistant/user 消息时间线
- scaffold 自动填充输入框
- 发送按钮 + 会话历史组合的聊天壳
- 在 `TaskRunView.vue` 里继续拼整页 UI
- 旧 task-driven 主容器回到执行页主路径

## 三、文件职责边界

- `frontend/src/views/TaskRunView.vue`
  - 只做：数据获取、phase query 同步、挂载 `ExecutionWorkbenchPage`
- `frontend/src/components/task-run/ExecutionWorkbenchPage.vue`
  - 只做：页面骨架布局和组件编排
- `frontend/src/components/task-run/PhaseInteractionHost.vue`
  - 只做：phase 到子组件映射
- `frontend/src/components/task-run/PromptScaffoldPanel.vue`
  - 只做：认知动作项展示 + `append-explanation`

## 四、四阶段首屏要求

- `structure`：结构辨析单选题，选后锁定
- `understanding`：机制理解单选题（为什么）
- `training`：单任务表达卡（唯一允许文本输入）
- `reflection`：`SystemSummaryCard + ReflectionQuestionCard + ReflectionStrategyCard`

## 五、状态与跳转时序

- 阶段内：`prompt -> think -> output -> feedback`
- 阶段间：
  - `structure -> understanding`
  - `understanding -> training`
  - `training -> reflection`
  - `reflection -> report`
- 只有当前阶段反馈出现后，才允许下一阶段 CTA 可点击

## 六、文案固定（不要自由发挥）

- STRUCTURE：建立基本轮廓 / 先分清 DFS 和 BFS 分别是什么，不用担心答错。
- UNDERSTANDING：理解背后的机制 / 这一步要弄清楚，为什么 DFS 会回溯，为什么 BFS 会分层推进。
- TRAINING：用你自己的话讲清楚 / 试着自己解释一次，系统会帮你发现表达缺口。
- REFLECTION：把这次学习收住 / 最后看一眼，你刚才最容易混淆什么，下次怎么更快判断。

## 七、验收标准

1. 首屏 1 秒内能看懂阶段、目标、主交互入口
2. 页面骨架在四阶段保持一致
3. 只有 training 阶段出现文本输入
4. phase 通过 query 切换，renderState 不入 URL
5. 从 structure 到 reflection 能完整走通
