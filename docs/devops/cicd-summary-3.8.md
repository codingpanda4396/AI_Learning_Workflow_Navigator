很好，这一步其实是一个**完整的 DevOps 闭环**：
你已经从 **本地开发 → 自动构建 → 自动发布 → ECS 部署 → API 验证** 全流程跑通了。
我帮你整理成一个**可以直接放进项目 README / 技术文档 的 CI/CD 总结**。

---

# AI Learning Workflow Navigator

## CI/CD 自动部署流程（GitHub Actions + Docker + ECS）

本文档总结本项目后端服务的 **CI/CD 自动部署流程**。

目标：

```
本地开发
   ↓
Git push
   ↓
GitHub Actions 自动构建
   ↓
Docker 镜像推送 GHCR
   ↓
ECS 拉取最新镜像
   ↓
Docker Compose 启动
   ↓
Spring Boot + PostgreSQL 运行
```

整体架构：

```
                GitHub
                  │
                  │ push
                  ▼
        GitHub Actions CI
       ┌─────────────────┐
       │ Maven build     │
       │ Docker build    │
       │ Push to GHCR    │
       └─────────────────┘
                  │
                  ▼
        GHCR (Docker Registry)
                  │
                  │ docker pull
                  ▼
        ECS (阿里云服务器)
       ┌─────────────────────┐
       │ Docker Compose      │
       │ backend container   │
       │                     │
       │ Spring Boot API     │
       │        │            │
       │        ▼            │
       │   PostgreSQL (ECS)  │
       └─────────────────────┘
```

---

# 一、GitHub Actions 自动构建

当代码 push 到 GitHub 时：

```
push -> main
```

GitHub Actions 自动执行：

### CI 流程

1️⃣ 拉取代码

```
actions/checkout
```

2️⃣ 构建 Java 项目

```
mvn clean package
```

生成：

```
target/app.jar
```

3️⃣ 构建 Docker 镜像

```
docker build
```

Dockerfile：

```dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
```

4️⃣ 登录 GHCR

```
docker login ghcr.io
```

5️⃣ 推送镜像

```
docker push ghcr.io/<username>/ai-learning-backend:latest
```

最终镜像：

```
ghcr.io/codingpanda4396/ai-learning-backend:latest
```

---

# 二、ECS 服务器部署

服务器环境：

```
阿里云 ECS
OS: Alibaba Cloud Linux
Docker
Docker Compose
PostgreSQL
```

---

# 三、生产环境配置

## `.env.prod`

生产环境变量：

```env
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:postgresql://8.145.55.7:5432/ai_learning_workflow_navigator
DB_USERNAME=postgres
DB_PASSWORD=439695

POSTGRES_DB=ai_learning_workflow_navigator

BACKEND_IMAGE=ghcr.io/codingpanda4396/ai-learning-backend:latest
```

注意：

```
.env.prod 不提交 Git
```

---

# 四、Docker Compose 部署

文件：

```
docker-compose.prod.yml
```

配置：

```yaml
services:
  backend:
    image: ${BACKEND_IMAGE}
    container_name: ai-learning-backend
    restart: unless-stopped
    env_file:
      - .env.prod
    ports:
      - "8081:8080"
```

说明：

```
backend 容器运行 Spring Boot
数据库使用 ECS 本机 PostgreSQL
```

架构：

```
ECS
 ├─ PostgreSQL (宿主机)
 └─ Docker container
      └─ Spring Boot backend
```

---

# 五、部署流程

在 ECS 服务器执行：

### 1 拉取最新代码

```
git pull
```

### 2 拉取最新 Docker 镜像

```
docker pull ghcr.io/codingpanda4396/ai-learning-backend:latest
```

### 3 启动服务

```
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

---

# 六、运行验证

查看容器：

```
docker ps
```

查看日志：

```
docker logs -f ai-learning-backend
```

---

## API 测试

测试接口：

```
curl -i -X POST http://localhost:8081/api/session/create \
  -H "Content-Type: application/json" \
  -d '{
        "userId":"test_001",
        "courseId":"computer_network",
        "chapterId":"tcp",
        "goalText":"测试连接"
      }'
```

成功返回：

```
HTTP 200
```

---

# 七、今天踩到的关键坑

## 1 Docker Compose 覆盖 env

错误写法：

```yaml
environment:
  DB_URL: jdbc:postgresql://postgres:5432/xxx
```

导致：

```
UnknownHostException: postgres
```

因为：

```
.env.prod 中的 DB_URL 被 compose 覆盖
```

解决：

```
删除 compose 中 DB_URL
统一使用 .env.prod
```

---

## 2 Flyway 启动失败

Flyway 会在 **Spring Boot 启动阶段连接数据库**：

```
FlywayMigrationInitializer
```

如果数据库连不上：

```
Spring Boot 直接启动失败
```

表现：

```
ApplicationContext run failed
```

---

## 3 容器网络与数据库地址

如果数据库不在 compose 内：

```
不能使用 postgres:5432
```

需要使用：

```
ECS IP
或 host.docker.internal
或 172.17.0.1
```

---

# 八、当前部署架构

最终架构：

```
GitHub
   │
   ▼
GitHub Actions
   │
   ▼
GHCR (Docker Registry)
   │
   ▼
ECS
 ├─ PostgreSQL
 └─ Docker
      └─ Spring Boot Backend
```

访问：

```
http://ECS_IP:8081
```

---

# 九、下一步可以升级的 DevOps

你现在是 **手动 CD**：

```
push -> build -> pull -> compose up
```

可以升级为：

### 自动 CD

```
GitHub Actions
    │
    ▼
SSH ECS
    │
docker pull
docker compose up -d
```

实现：

```
push = 自动部署
```

---

如果你愿意，我可以再帮你补一份**真正工业级的 CI/CD 方案**（10分钟改完那种），包括：

* GitHub Actions 自动部署 ECS
* 零停机更新
* 自动 rollback
* docker image versioning

那套结构会比你现在这套 **再先进一代**。
