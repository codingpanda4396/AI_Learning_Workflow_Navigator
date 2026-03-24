# AGENTS.md

## Product framing
本仓库是一个 AI 学习工作流系统，不是通用聊天产品。
核心闭环是：目标 -> 诊断 -> 规划 -> 执行 -> 报告。

## Competition scope
当前迭代只优化面向演示的前端页面：
- 目标页
- 规划页
- 执行页

## Design principles
- 避免“聊天壳子”感
- 避免后台管理面板感
- 让用户一眼看懂学习工作流
- 突出四个阶段：STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION

## Implementation constraints
- 优先采用纯前端改动
- 为了演示效果可自由使用 mock 数据
- 非必要不要重定义后端契约
- 文案保持简短、产品化

## Goal page rules
- 优先提供快速开始
- 不要把自由文本输入作为主交互
- 展示 408 个学科以及未来扩展能力，并以灰态呈现

## Planning page rules
- 必须可视化四阶段学习流程
- 规划不是文字说明，而是学习编排

## Execution page rules
- AI Tutor 是受约束的教学助手，不是开放式聊天
- 任务脚手架才是中心，不是对话历史

## Validation
- 每次实现后都必须运行 build
- 前端改动优先做可视化验证
