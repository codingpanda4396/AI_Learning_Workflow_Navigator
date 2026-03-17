# Lumina AI 前端

## 技术栈

- Vue 3 + TypeScript
- Vite 5
- Vue Router
- Pinia
- Tailwind CSS 3
- Axios

## 开发

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 http://localhost:5173，API 请求通过 Vite 代理转发到 http://localhost:8080。

## 构建

```bash
npm run build
```

## 主链路

1. **目标输入** (`/goal`) - POST /api/goals
2. **诊断** (`/diagnosis`) - POST /api/diagnosis/sessions, POST /api/diagnosis/submissions
3. **计划** (`/plan`) - POST /api/learning-plans/preview, POST /api/learning-plans/commit
4. **任务执行** (`/task`) - GET /api/sessions/{id}/current-task, POST /api/tasks/{id}/complete
5. **报告** (`/report`) - GET /api/sessions/{id}/report, POST /api/sessions/{id}/next-action

## 设计系统

- 色彩：primary #4F46E5，背景 #F8FAFC
- 圆角：卡片 16px，输入/按钮 12px
- 详见 `docs/frontend-style-guide.md`
