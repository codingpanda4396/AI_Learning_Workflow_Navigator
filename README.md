# AI Learning Workflow Navigator

面向 AI 学习流程导航的前后端一体项目，当前仓库包含：

- `backend/`：Spring Boot 3.4 + PostgreSQL + Flyway 后端
- `frontend/`：Vue 3 + Pinia + Vite 前端
- `docs/`：架构、开发、规划、排障与历史沉淀文档
- `deploy/`：部署配置与生产环境样例

## 快速开始

### 一键本地启动

```bash
npm install
npm run dev
```

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`

### 分别启动

```bash
npm run dev:backend
npm run dev:frontend
```

## 常用验证

- 后端：`mvn -q -DskipTests compile`
- 前端：`pnpm build`

## 文档入口

- 文档总索引：[`docs/README.md`](docs/README.md)
- 本地开发：[`docs/devops/local-dev.md`](docs/devops/local-dev.md)
- 系统架构：[`docs/architecture/system-architecture.md`](docs/architecture/system-architecture.md)
- 当前项目状态：[`docs/architecture/3.11_projstatus.md`](docs/architecture/3.11_projstatus.md)
- 前端说明：[`frontend/README.md`](frontend/README.md)

## 目录概览

```text
.
├── backend/
├── frontend/
├── docs/
│   ├── architecture/
│   ├── backend/
│   ├── devops/
│   ├── frontend/
│   ├── implementation-plans/
│   ├── product-planning/
│   ├── prompts/
│   └── archive/
├── deploy/
└── spec/
```
