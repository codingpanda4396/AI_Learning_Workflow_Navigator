# 本地开发指南

在本地运行前后端，无需 push 到远程即可测试。

## 前置条件

- Node.js 18+
- Java 17+
- Maven 3.6+
- PostgreSQL（或用 `application-local.yml` 配置的远程库）

## 1. 一键启动（推荐）

```bash
# 首次需安装依赖
npm install

# 启动前端 + 后端
npm run dev
```

- 前端：http://localhost:3000（Vue 热更新）
- 后端：http://localhost:8080

## 2. 分别启动

```bash
# 终端 1：后端
npm run dev:backend

# 终端 2：前端
npm run dev:frontend
```

## 3. 本地 API 配置

前端通过 **Vite 代理** 将 `/api` 转发到 `localhost:8080`，无需改后端地址。

已创建 `frontend/.env.local`，内容为：

```
VITE_API_BASE_URL=/api
```

这样请求会走 `localhost:3000/api/...`，由 Vite 代理到 `localhost:8080/api/...`。

若没有 `.env.local`，可复制模板：

```bash
cp frontend/.env.local.example frontend/.env.local
```

## 4. 热更新

| 层级 | 行为 |
|------|------|
| **前端** | Vite HMR，保存 Vue/TS 后即时刷新 |
| **后端** | Spring Boot DevTools，在 IDE 中运行时会随重编译自动重启 |

后端热重载建议用 IDE（IntelliJ / VS Code + Java 扩展）运行 `LearningApplication`，修改 Java 后保存即可触发重启。

## 5. LLM 配置

后端使用 `backend/application-local.yml` 的 LLM 配置（已加入 .gitignore）。确保其中：

- `app.llm.enabled: true`
- `base-url`、`api-key`、`model` 已填写

详见 [LLM_MVP2_USAGE_GUIDE.md](./LLM_MVP2_USAGE_GUIDE.md)。

## 6. 切回远程环境

若要测试远程部署的接口，删除或重命名 `frontend/.env.local`，前端会使用 `frontend/.env` 中的 `VITE_API_BASE_URL`（指向远程）。
