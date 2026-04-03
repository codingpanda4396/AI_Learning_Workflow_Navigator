# AGENTS.md

## Product framing
本仓库是一个 AI 学习工作流系统，不是通用聊天产品。
核心闭环是：目标 -> 诊断 -> 规划 -> 执行 -> 报告。

# Project Working Agreement

## Product identity
This project is an AI-guided learning workflow product, not a generic chatbot UI.
Every page must help the user know:
- where they are
- what to do now
- why this step matters
- what comes next

## UI priorities
- Reduce cognitive load first
- One page, one primary action
- Scaffold is the product highlight
- Current task must dominate the first screen
- Explanatory text must not overpower guidance

## Design consistency
- Reuse shared visual tokens and components
- Avoid ad-hoc colors, spacing, radius, and shadows
- Keep page hierarchy clear and stable across flows

## Copywriting
- Use short, directive, product-style Chinese copy
- Avoid “AI explanation tone”
- Avoid long business-logic explanations
- Buttons should start with clear verbs

## Engineering workflow
- For complex UI refactors, inspect existing structure first, then propose a concise plan, then implement
- Reuse before creating new abstractions
- After changes, self-check hierarchy, copy, responsiveness, loading, empty, and error states
- Task execution page data loading: prefer extending `composables/useTaskRunSession.ts` over duplicating fetch/sync logic in the view

## Special attention
The task execution page is the core showcase page.
It must make the scaffold feel like a real guided learning system rather than a chat wrapper.