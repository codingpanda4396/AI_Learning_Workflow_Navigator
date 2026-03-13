# Docs Index

`docs/` 按“当前可用文档优先、历史文档归档”来组织，先看入口，再按主题深入。

## 建议阅读顺序

1. [`architecture/3.11_projstatus.md`](./architecture/3.11_projstatus.md)：项目当前状态、能力边界、主要差距
2. [`architecture/system-architecture.md`](./architecture/system-architecture.md)：系统架构与模块关系
3. [`devops/local-dev.md`](./devops/local-dev.md)：本地开发与环境配置
4. [`backend/backend-implementation-overview-3.8.md`](./backend/backend-implementation-overview-3.8.md)：后端实现总览
5. [`frontend/ai-tutor-mvp-summary-2026-03-08.md`](./frontend/ai-tutor-mvp-summary-2026-03-08.md)：前端阶段性总结

## 按目录查找

- `architecture/`：稳定架构、系统设计、阶段状态总结
- `backend/`：后端实现说明、链路分析、测试与交付记录
- `frontend/`：前端实现说明、页面与交互相关总结
- `devops/`：本地开发、部署、CI/CD
- `product-planning/`：需求分析、MVP 范围、接口与重构方案
- `implementation-plans/`：具体任务的设计稿与执行计划
- `prompts/`：Prompt 资产、清单与使用说明
- `issues/`：临时问题记录与待处理事项
- `BV1003/`：专项审查与系统分析材料
- `archive/`：历史交付、旧需求、一次性过程文档
- `todo/`：预留目录，当前无有效文档

## 当前高频文档

- 接口契约：[`contract.md`](./contract.md)
- 本地开发：[`devops/local-dev.md`](./devops/local-dev.md)
- CI/CD：[`devops/cicd-guide.md`](./devops/cicd-guide.md)
- 后端全链路测试：[`backend/backend-full-chain-test-guide.md`](./backend/backend-full-chain-test-guide.md)
- Prompt 清单：[`prompts/prompt-inventory.md`](./prompts/prompt-inventory.md)
- Quiz API/MVP 方案：[`product-planning/session-quiz-api-contract-mvp.md`](./product-planning/session-quiz-api-contract-mvp.md)

## 维护约定

- 新文档优先放入对应主题目录，不要堆在 `docs/` 根目录
- 阶段性方案、问题记录优先使用 `YYYY-MM-DD-` 前缀
- 已失效但需要保留的文档移动到 `archive/`
- 根目录 `docs/` 下仅保留跨主题入口文档或通用说明
