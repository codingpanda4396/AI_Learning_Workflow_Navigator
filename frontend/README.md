# AI Learning Navigator - Frontend

Vue 3 前端项目，自适应学习流程导航系统的用户界面。

## 技术栈

- Vue 3 (Composition API + `<script setup>`)
- TypeScript
- Pinia (状态管理)
- Vue Router 4
- Axios
- Vite

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

## 配置

环境变量配置在 `.env` 文件中（参考 `.env.example`）：

```
VITE_API_BASE_URL=http://localhost:8080/api
```

## 项目结构

```
src/
├── api/          # API 客户端
├── components/   # 公共组件
├── router/       # 路由配置
├── stores/       # Pinia 状态管理
├── types/        # TypeScript 类型定义
└── views/        # 页面组件
```
