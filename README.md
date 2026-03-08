# AI Learning Workflow Navigator

Spring Boot + PostgreSQL + Flyway 学习流程导航后端。

## 项目结构

```
├── backend/              # Java 后端（Maven）
│   ├── pom.xml
│   └── src/
├── deploy/               # 生产部署配置
│   ├── docker-compose.prod.yml
│   └── .env.prod.example
├── docs/                 # 文档
│   └── cicd-guide.md     # CI/CD 操作手册
├── Dockerfile
└── .github/workflows/    # GitHub Actions
    ├── backend-ci.yml    # push dev / PR 触发
    └── backend-cd.yml    # push main 触发
```

## 快速开始

- **本地运行**：`cd backend && mvn spring-boot:run`（需配置 PostgreSQL）
- **Docker 构建**：在仓库根目录执行 `docker build -t ai-learning-backend .`
- **CI/CD 部署**：详见 [docs/cicd-guide.md](docs/cicd-guide.md)
