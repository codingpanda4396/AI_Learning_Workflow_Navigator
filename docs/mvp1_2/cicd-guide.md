# CI/CD 傻瓜式操作手册

本文档为新手准备，按步骤操作即可完成从本地到生产的完整部署链路。

---

## 目录

- [A. 本地准备](#a-本地准备)
- [B. GitHub 侧操作](#b-github-侧操作)
- [C. ECS 服务器首次准备](#c-ecs-服务器首次准备)
- [D. 自动部署验证](#d-自动部署验证)
- [E. 常见错误排查](#e-常见错误排查)
- [最小落地路径](#最小落地路径)

---

## A. 本地准备

### A1. 确认本地环境

- **JDK 17**：`java -version` 应显示 17.x
- **Maven**：`mvn -v` 能正常执行
- **Docker**：`docker --version` 能正常执行

### A2. 验证 Maven 构建

在仓库根目录执行：

```bash
cd backend
mvn clean test
mvn clean package -DskipTests
```

若成功，`backend/target/` 下会出现 `learning-backend-0.0.1-SNAPSHOT.jar`。

### A3. 验证 Dockerfile 构建

**必须在仓库根目录**执行（不是 `backend/`）：

```bash
# 在仓库根目录
docker build -t ai-learning-backend:test .
```

若失败，常见原因：
- 在 `backend/` 下执行了 `docker build`（错误）
- 根目录下没有 `backend/pom.xml` 和 `backend/src`

### A4. 本地测试镜像（可选）

需先启动 PostgreSQL，例如：

```bash
docker run -d --name pg-test -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16-alpine
```

然后运行 backend 镜像：

```bash
docker run -d -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/postgres \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e SPRING_PROFILES_ACTIVE=prod \
  ai-learning-backend:test
```

访问 http://localhost:8080/actuator/health 应返回 `{"status":"UP"}`。

---

## B. GitHub 侧操作

### B1. 配置 Secrets 的入口

1. 打开你的 GitHub 仓库
2. 点击 **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**

### B2. 需要配置的 Secrets 清单

| Secret 名称 | 说明 | 示例值 |
|-------------|------|--------|
| `GHCR_PAT` | 推拉 GHCR 镜像的 Personal Access Token | `ghp_xxxx` |
| `ECS_HOST` | 阿里云 ECS 公网 IP 或域名 | `123.45.67.89` |
| `ECS_USER` | SSH 登录用户名 | `root` 或 `ubuntu` |
| `ECS_SSH_KEY` | SSH 私钥完整内容 | 见下方格式 |
| `ECS_SSH_PORT` | SSH 端口（默认 22 可填 22） | `22` |
| `ECS_DEPLOY_DIR` | 服务器上部署目录的**绝对路径** | `/home/ubuntu/deploy` |

### B3. 创建 GHCR_PAT

1. 右上角头像 → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)**
3. 勾选权限：`write:packages`、`read:packages`、`delete:packages`
4. 生成后复制，**只显示一次**，请妥善保存

### B4. ECS_SSH_KEY 格式

必须是**完整私钥**，包含首尾行：

```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAA...
...多行...
-----END OPENSSH PRIVATE KEY-----
```

- 复制时不要漏掉 `-----BEGIN...` 和 `-----END...`
- 不要有多余空格或换行
- 若使用 `id_rsa`，可执行 `cat ~/.ssh/id_rsa` 查看

---

## C. ECS 服务器首次准备

### C1. 安装 Docker 与 Docker Compose

若系统没有预装：

```bash
# 以 Ubuntu 为例
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 登出再登录后生效

# Docker Compose V2（随 Docker 安装，或单独安装）
sudo apt update && sudo apt install docker-compose-plugin -y
docker compose version  # 确认可用
```

### C2. 创建部署目录

```bash
mkdir -p ~/deploy
cd ~/deploy
```

请记住此路径，填入 GitHub Secret `ECS_DEPLOY_DIR`，例如 `/home/ubuntu/deploy`。

### C3. 上传部署文件

将以下两个文件放到 `ECS_DEPLOY_DIR`：

1. **docker-compose.prod.yml**  
   从本仓库 `deploy/docker-compose.prod.yml` 复制

2. **.env.prod**  
   复制 `deploy/.env.prod.example` 为 `.env.prod`，并填入真实值：

```bash
# 在服务器上
cat > .env.prod << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://postgres:5432/ai_learning
DB_USERNAME=postgres
DB_PASSWORD=你的数据库密码
POSTGRES_DB=ai_learning
BACKEND_IMAGE=ghcr.io/你的GitHub用户名/ai-learning-backend:latest
EOF
```

**务必修改**：`DB_PASSWORD` 和 `BACKEND_IMAGE` 中的 GitHub 用户名。

### C4. 首次手动登录 GHCR

在 ECS 上执行（将 `YOUR_GITHUB_USERNAME` 和 `YOUR_PAT` 替换为实际值）：

```bash
echo "YOUR_PAT" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

成功会显示 `Login Succeeded`。

### C5. 首次手动部署

```bash
cd $ECS_DEPLOY_DIR  # 即你创建的部署目录
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

### C6. 检查容器是否启动成功

```bash
docker compose -f docker-compose.prod.yml ps
curl http://localhost:8080/actuator/health
```

应返回 `{"status":"UP"}`。若服务器有公网 IP，可从本机访问 `http://<ECS_IP>:8080/actuator/health`。

---

## D. 自动部署验证

### D1. push 到 main 后会发生什么

1. GitHub Actions 触发 **Backend CD** 工作流
2. 构建 Docker 镜像并推送到 GHCR
3. SSH 登录 ECS，执行 `docker compose pull` 和 `up -d`
4. 清理无用镜像

### D2. 查看 Actions 日志

- 仓库顶部 **Actions** 标签
- 点击对应的 workflow run（如 "Backend CD"）
- 展开各步骤查看输出

### D3. 在 ECS 上查看日志

```bash
cd $ECS_DEPLOY_DIR
docker compose -f docker-compose.prod.yml logs -f backend
```

按 `Ctrl+C` 退出跟踪。

### D4. 失败时优先检查

1. **Actions 中 build-and-push 失败**：检查 `GHCR_PAT` 权限
2. **deploy-to-ecs 失败**：检查 `ECS_SSH_KEY`、`ECS_HOST`、`ECS_USER`、`ECS_DEPLOY_DIR`
3. **容器启动后立即退出**：`docker compose logs backend` 看应用错误
4. **健康检查不通过**：检查 DB 连接、Flyway 迁移是否成功

---

## E. 常见错误排查

### E1. Dockerfile 路径不对

**现象**：`docker build` 报错 `COPY failed: file not found`

**原因**：在错误目录执行 build，或 Dockerfile 中 COPY 路径与仓库结构不一致。

**修复**：必须在**仓库根目录**执行 `docker build -t xxx .`，且 Dockerfile 中为 `COPY backend/...`。

---

### E2. Maven 构建失败

**现象**：CI 中 `mvn clean test` 或 `mvn package` 失败

**原因**：测试失败、依赖下载失败、JDK 版本不对。

**修复**：
- 本地在 `backend/` 下执行 `mvn clean test` 复现
- 检查 `backend/pom.xml` 中 `java.version` 是否为 17
- 确认 GitHub Actions 使用 `actions/setup-java@v4` 且 `java-version: 17`

---

### E3. GHCR 权限问题

**现象**：`denied: permission_denied` 或 `unauthorized`

**原因**：PAT 权限不足，或镜像为私有但 ECS 未登录。

**修复**：
- PAT 需包含 `write:packages`、`read:packages`
- 若镜像设为私有，在 ECS 上执行 `docker login ghcr.io`
- 仓库 Settings → Packages 中确认该 PAT 有访问权限

---

### E4. SSH 连接失败

**现象**：`Permission denied (publickey)` 或 `Connection refused`

**原因**：私钥错误、用户错误、端口错误、安全组未开放 22。

**修复**：
- `ECS_SSH_KEY` 必须是完整私钥，含 `-----BEGIN...` 和 `-----END...`
- `ECS_USER` 与服务器实际用户名一致（如 `root`、`ubuntu`）
- `ECS_SSH_PORT` 与 sshd 监听端口一致（默认 22）
- 阿里云安全组放行 22 端口

---

### E5. ECS 上 docker compose 命令不可用

**现象**：`docker compose: command not found`

**原因**：未安装 Docker Compose 插件，或只有旧版 `docker-compose`。

**修复**：
```bash
sudo apt install docker-compose-plugin -y
# 或使用带连字符的旧命令（若已安装）：
docker-compose -f docker-compose.prod.yml up -d
```

---

### E6. Spring Boot 连不上 PostgreSQL

**现象**：日志中出现 `Connection refused` 或 `password authentication failed`

**原因**：DB_URL、用户名、密码错误；或 PostgreSQL 尚未就绪。

**修复**：
- 同一 compose 时，`DB_URL` 应为 `jdbc:postgresql://postgres:5432/ai_learning`（`postgres` 为服务名）
- `.env.prod` 中 `DB_USERNAME`、`DB_PASSWORD` 与 `POSTGRES_USER`、`POSTGRES_PASSWORD` 一致
- 使用 `depends_on` + `condition: service_healthy` 确保数据库先就绪

---

### E7. Flyway migration 失败

**现象**：应用启动报错 `Migration checksum mismatch` 或 `Found non-empty schema`

**原因**：已有数据库与迁移脚本不一致，或迁移文件被修改。

**修复**：
- 开发阶段可删除库重新创建：`docker compose down -v` 再 `up -d`
- 生产环境不要随意改已执行过的迁移文件
- 新增迁移使用更大版本号，如 `V8__xxx.sql`

---

## 最小落地路径

若时间有限，按下面**三步**即可跑通：

### 第 1 步：本地验证（约 5 分钟）

1. `cd backend && mvn clean package -DskipTests`
2. 在仓库根目录执行 `docker build -t ai-learning-backend:test .`
3. 两者都成功即可进入下一步

### 第 2 步：配置 GitHub Secrets（约 10 分钟）

1. 创建 `GHCR_PAT`（勾选 packages 权限）
2. 在仓库 Settings → Secrets 中配置：`GHCR_PAT`、`ECS_HOST`、`ECS_USER`、`ECS_SSH_KEY`、`ECS_SSH_PORT`、`ECS_DEPLOY_DIR`
3. push 到 `dev`，确认 **Backend CI** 通过
4. push 到 `main`，确认 **Backend CD** 通过

### 第 3 步：ECS 首次部署（约 15 分钟）

1. 在 ECS 上安装 Docker 与 Docker Compose
2. 创建部署目录，放入 `docker-compose.prod.yml` 和 `.env.prod`
3. 执行 `docker login ghcr.io` 和 `docker compose up -d`
4. 访问 `http://<ECS_IP>:8080/actuator/health` 验证

完成以上三步后，之后每次 push 到 `main` 都会自动部署。
