# AI Learning Navigator 前端系统性修复执行 Prompt（审计版）

> 用途：本文件仅用于审计与评审，不执行代码修改。
> 日期：2026-03-08

## 1. 角色设定
你现在是一个资深前端工程师 + 交互设计工程师，负责在**保留现有产品方向与代码骨架**的前提下，对 AI Learning Navigator 前端 demo 做一次“面向比赛展示和后续后端联调”的系统性修复。

## 2. 项目背景
这是一个 AI Learning Navigator（自适应学习流程导航系统）的前端 demo。
- 首页：用户输入学习目标、选择课程与章节，然后进入四步学习流程。
- 产品核心：不是普通聊天，而是“将模糊学习目标转化为可执行的分步学习流程”。

## 3. 执行边界
- 不要重写整个项目。
- 优先基于现有代码增量重构。
- 不仅改样式，还要修复产品语义、交互逻辑和状态模型。
- 设计风格：暗色科技感、简洁克制、强调 AI 学习产品属性，不做花哨堆砌。

## 4. 首页修复目标
1. 优化视觉层级，解决标题过大、页面重心失衡、品牌表达不统一。
2. 统一 UI 语言：颜色、字号、圆角、间距、边框风格。
3. 表单流程化：`学习目标 -> 课程 -> 章节 -> 开始学习`，形成清晰步骤感。
4. 增加简洁价值说明：让用户一眼理解这是“学习流程导航系统”而非聊天页。
5. 增加四步流程提示：`诊断目标 -> 生成路径 -> 分步学习 -> 总结反馈`。
6. 优化“开始学习”按钮状态：disabled / hover / active / loading。
7. 增加基础校验：学习目标为空时不可开始，并提供轻量提示。
8. 做好移动端适配：避免键盘弹出遮挡、内容溢出、间距失衡。

## 5. 流程页修复目标
1. 必须有清晰 stepper/进度指示。
2. 每一步只聚焦一个任务，避免信息过载。
3. 每一步明确展示“当前输入”和“本步产出”。
4. 统一上一页/下一页/完成按钮风格与逻辑。
5. 支持将流程状态保存到前端 state。
6. 为后续后端联调预留清晰数据结构，避免纯静态写死。

## 6. 工程化要求
1. 保持现有结构，优先增量重构。
2. 抽离通用 UI Token/常量（颜色、圆角、阴影、间距、字号）。
3. 抽离可复用组件（至少）：
   - `PageHeader`
   - `GoalInputCard`
   - `StepProgress`
   - `PrimaryButton`
   - `CourseSelector`
4. 整理页面状态模型（至少包含）：
   - `goal`
   - `courseId`
   - `chapterId`
   - `workflowId`
   - `currentStep`
   - `stepData`
   - `loading`
5. 关键逻辑需有清晰注释，便于团队后续开发。

## 7. 数据模型建议（用于前后端联调预留）
建议前端统一使用如下结构，便于后续直接映射 API：

```ts
interface LearningWorkflowState {
  workflowId: string | null;
  goal: string;
  courseId: string | null;
  chapterId: string | null;
  currentStep: 1 | 2 | 3 | 4;
  loading: boolean;
  stepData: {
    step1?: {
      input: Record<string, unknown>;
      output: Record<string, unknown>;
      status: 'idle' | 'running' | 'done' | 'error';
    };
    step2?: {
      input: Record<string, unknown>;
      output: Record<string, unknown>;
      status: 'idle' | 'running' | 'done' | 'error';
    };
    step3?: {
      input: Record<string, unknown>;
      output: Record<string, unknown>;
      status: 'idle' | 'running' | 'done' | 'error';
    };
    step4?: {
      input: Record<string, unknown>;
      output: Record<string, unknown>;
      status: 'idle' | 'running' | 'done' | 'error';
    };
  };
}
```

## 8. 后端 API 对接预留建议
- 首屏提交：`POST /api/workflows` 创建流程并返回 `workflowId`。
- 步骤执行：`POST /api/workflows/{workflowId}/steps/{stepNo}`。
- 状态查询：`GET /api/workflows/{workflowId}`。
- 暂存能力：`PATCH /api/workflows/{workflowId}`（保存用户当前输入）。
- 完成总结：`POST /api/workflows/{workflowId}/complete`。

建议前端在 state 层预留：
- `requestId`（幂等或追踪）
- `updatedAt`（本地与服务端同步时间）
- `error`（统一错误结构）

## 9. 验收标准（DoD）
1. 首页可在 3 秒内让评委理解产品核心价值。
2. 首页交互完整：输入约束、按钮状态、轻提示、移动端可用。
3. 流程页步骤清晰：有 stepper、有输入/产出、有统一导航行为。
4. 状态模型可支持后端联调，不是纯静态演示页。
5. 组件与 token 已抽离，可复用、可维护。
6. 视觉风格统一：暗色科技感但不过度炫技。

## 10. 本轮输出格式要求
执行实现后，必须按以下结构输出：
1. 你发现的问题清单
2. 修复方案说明
3. 后续如何与后端 API 对接的建议

## 11. 重要提醒
- 本文件是“执行 Prompt 审计稿”。
- 在你收到“开始修改”的明确指令前，不进行任何代码变更。
