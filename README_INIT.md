# README_INIT

## 当前项目状态（2026-03-05）

本仓库已完成第一步：后端工程骨架与分层落地，目标是先保证可运行，再逐步接入真实业务。

## 目录与工程结构

- 仓库根目录下新增并启用子工程：`backend/`（Maven + Spring Boot）
- Maven 主工程文件：`backend/pom.xml`
- 代码入口主包：`com.pandanav.learning`

当前主要目录：

- `backend/src/main/java/com/pandanav/learning/api`
  - `controller`
  - `dto`
- `backend/src/main/java/com/pandanav/learning/application`
  - `service`
  - `command`
  - `query`
- `backend/src/main/java/com/pandanav/learning/domain`
  - `model`
  - `repository`
- `backend/src/main/java/com/pandanav/learning/infrastructure`
  - `persistence`
  - `repository`
  - `config`
  - `external`

说明：历史代码（`com.panda.ainavigator`）已随工程迁入 `backend/src/main/java/com/panda/...`，当前保留，后续按重构节奏处理。

## 已完成功能（本阶段）

- 主启动类：`com.pandanav.learning.LearningApplication`
- 健康检查接口：`GET /health`
  - 返回：`{"status":"ok"}`
- 统一响应体：`ApiResponse`
- OpenAPI/Swagger：已接入 `springdoc-openapi`
  - 可访问：`/swagger-ui.html`

## 配置现状

文件：`backend/src/main/resources/application.yml`

- 已提供基础配置占位：
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `DB_DRIVER`
- 默认值可直接本地启动（H2 内存库），不依赖真实数据库
- 当前 `flyway.enabled=false`

## 依赖现状（核心）

`backend/pom.xml` 已包含：

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-jdbc`
- `flyway-core`
- `postgresql`（runtime）
- `h2`（runtime）
- `springdoc-openapi-starter-webmvc-ui`

## 本地运行与验证

在仓库根目录执行：

```bash
cd backend
mvn spring-boot:run
```

验证：

```bash
curl http://127.0.0.1:8080/health
# {"status":"ok"}

curl -I http://127.0.0.1:8080/swagger-ui.html
# HTTP/1.1 200
```

## 当前边界

本阶段明确未做：

- 不做数据库建模与真实持久化实现
- 不做业务接口开发
- 仅保证工程骨架、分层结构、启动可用与基础探活/文档能力
