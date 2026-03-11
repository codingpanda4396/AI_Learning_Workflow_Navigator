# AI 个性化学习导航系统前端

## 启动

```bash
cd frontend
pnpm install
pnpm dev
```

构建：

```bash
pnpm build
```

## 环境变量

- `VITE_API_BASE_URL`：后端地址，默认 `http://localhost:8080`

## 页面职责

- `/login`：注册、登录、保存 token
- `/`：新建学习会话、继续最近学习
- `/sessions/:sessionId`：session 导航总览
- `/tasks/:taskId/run`：执行当前任务并查看内容
- `/sessions/:sessionId/quiz`：生成 quiz、轮询状态、作答提交
- `/sessions/:sessionId/report`：查看反馈报告并提交 next-action
- `/sessions/:sessionId/growth`：查看成长看板

## 接口依赖

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `POST /api/session/create`
- `POST /api/session/{sessionId}/plan?mode=adaptive`
- `GET /api/session/{sessionId}/overview`
- `GET /api/session/current`
- `GET /api/task/{taskId}`
- `POST /api/task/{taskId}/run`
- `POST /api/sessions/{sessionId}/quiz/generate`
- `GET /api/sessions/{sessionId}/quiz/status`
- `GET /api/sessions/{sessionId}/quiz`
- `POST /api/sessions/{sessionId}/quiz/submit`
- `GET /api/sessions/{sessionId}/feedback`
- `POST /api/sessions/{sessionId}/next-action`
- `GET /api/session/{sessionId}/learning-feedback/weak-points`
- `GET /api/session/{sessionId}/learning-feedback/report`
- `GET /api/session/{sessionId}/growth-dashboard`

## 兼容假设

- quiz 题目 `options` 可能是字符串数组，也可能是对象数组，前端统一转成文本选项。
- 报告页同时聚合 `/feedback`、`/learning-feedback/report`、`/weak-points`，任一接口缺失时页面仍尽量展示已有结果。
- task 输出结构不固定，首版直接按字符串或 JSON 展示，不在前端额外推导业务结论。
