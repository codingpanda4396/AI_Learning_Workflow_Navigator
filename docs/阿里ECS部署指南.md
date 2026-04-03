# 阿里 ECS 部署指南（AI 学习工作流导航系统）

本文档面向在**阿里云 ECS** 上部署本仓库的**前端静态资源 + Spring Boot 后端 + PostgreSQL**，采用 **Nginx 同源反代**（浏览器只访问一个域名，Cookie 与 `/api` 请求最省心）。

适用假设：

- ECS 系统为 **Alibaba Cloud Linux 3** 或 **Ubuntu 22.04**（命令以 `dnf` / `apt` 二选一说明）。
- 你已有一个解析到 ECS 公网 IP 的**域名**（HTTPS 强烈建议开启）。
- 数据库使用 **本机 PostgreSQL**；若使用 **阿里云 RDS PostgreSQL**，将连接串换为 RDS 内网/外网地址即可。

---

## 1. 架构与端口

| 组件 | 说明 |
|------|------|
| 用户浏览器 | 仅访问 `https://你的域名` |
| Nginx | `443` 对外；静态文件 `/`；反向代理 `/api` → 本机 `127.0.0.1:8080` |
| Spring Boot | 仅监听 `127.0.0.1:8080`（不对公网直连） |
| PostgreSQL | 本机 `127.0.0.1:5432`（或 RDS） |

---

## 2. 阿里云侧准备

1. **ECS**：建议 2 vCPU / 4 GiB 及以上；系统盘 ≥ 40 GiB。
2. **安全组入方向**（按你实际方案调整）：
   - `22`：SSH（建议仅允许你的办公网 IP）。
   - `80`：HTTP（若用 Certbot 验证或 HTTP 跳转）。
   - `443`：HTTPS。
   - **不要**对 `0.0.0.0/0` 开放 `8080`；后端只给 Nginx 本机访问。
3. （可选）**弹性公网 IP** 绑定到 ECS。

---

## 3. 服务器基础环境

以 **root 或有 sudo 权限的用户**登录 ECS。

### 3.1 安装常用工具

**Alibaba Cloud Linux 3 / CentOS 系：**

```bash
sudo dnf update -y
sudo dnf install -y git tar gzip
```

**Ubuntu：**

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git tar gzip
```

### 3.2 安装 OpenJDK 17

**dnf：**

```bash
sudo dnf install -y java-17-openjdk-devel
java -version
```

**apt：**

```bash
sudo apt install -y openjdk-17-jdk
java -version
```

### 3.3 安装 PostgreSQL（本机库）

**dnf（示例 PostgreSQL 15，包名以实际仓库为准）：**

```bash
sudo dnf install -y postgresql-server postgresql-contrib
sudo postgresql-setup --initdb
sudo systemctl enable --now postgresql
```

**apt：**

```bash
sudo apt install -y postgresql postgresql-contrib
sudo systemctl enable --now postgresql
```

### 3.4 创建数据库与用户

```bash
sudo -u postgres psql <<'SQL'
CREATE USER ai_nav WITH PASSWORD '请替换为强密码';
CREATE DATABASE ai_learning_workflow_navigator OWNER ai_nav;
GRANT ALL PRIVILEGES ON DATABASE ai_learning_workflow_navigator TO ai_nav;
SQL
```

生产环境请把密码记入**仅你本人可读的**配置文件（见下文 `application.env`），勿写入 Git。

若使用 **RDS**：在 RDS 控制台创建库与用户，并放行 ECS 到 RDS 的安全组规则。

### 3.5 安装 Nginx

**dnf：**

```bash
sudo dnf install -y nginx
sudo systemctl enable --now nginx
```

**apt：**

```bash
sudo apt install -y nginx
sudo systemctl enable --now nginx
```

---

## 4. 获取代码与构建

### 4.1 克隆仓库

```bash
sudo mkdir -p /opt/apps
sudo chown "$USER:$USER" /opt/apps
cd /opt/apps
git clone <你的仓库地址> AI_Learning_Workflow_Navigator
cd AI_Learning_Workflow_Navigator
```

### 4.2 构建后端 JAR

```bash
cd /opt/apps/AI_Learning_Workflow_Navigator/backend
mvn -q clean package -DskipTests
```

需已安装 **Maven 3.8+** 与 **JDK 17**（与上文 `java` 一致即可）。

产物路径示例：

```text
backend/target/learning-workflow-navigator-backend-0.1.0-SNAPSHOT.jar
```

### 4.3 构建前端（同源：API 走当前站点 `/api`）

**推荐**：在 `frontend` 目录增加生产环境变量，使 `VITE_API_BASE` 为空字符串，这样打包后请求为相对路径 `/api/...`，由 Nginx 转发到后端。

```bash
cd /opt/apps/AI_Learning_Workflow_Navigator/frontend
printf '%s\n' 'VITE_API_BASE=' > .env.production
```

安装 Node（若尚未安装，可使用 nvm 或发行版自带 Node 20+）：

```bash
npm ci
npm run build
```

产物在 `frontend/dist/`。

> **说明**：若未设置 `VITE_API_BASE`，生产构建会默认指向 `http://localhost:8080`，在用户浏览器上会导致 API 请求失败。同源部署**必须**按上文设置空值或显式设为站点根路径策略。

---

## 5. 部署目录与 systemd

### 5.1 放置 JAR 与静态文件

```bash
sudo mkdir -p /opt/learning-nav/{bin,static}
sudo cp /opt/apps/AI_Learning_Workflow_Navigator/backend/target/learning-workflow-navigator-backend-0.1.0-SNAPSHOT.jar \
  /opt/learning-nav/bin/app.jar
sudo rm -rf /opt/learning-nav/static/*
sudo cp -r /opt/apps/AI_Learning_Workflow_Navigator/frontend/dist/* /opt/learning-nav/static/
sudo chown -R root:root /opt/learning-nav
```

### 5.2 环境变量文件（勿提交到 Git）

```bash
sudo install -m 600 /dev/null /opt/learning-nav/application.env
sudo nano /opt/learning-nav/application.env
```

示例内容（请替换密码；域名仅在 Nginx 使用）：

```bash
# 必填：禁止省略。仓库默认 SPRING_PROFILES_ACTIVE 为 local，会加载 application-local.yml（开发用远端库等），
# 生产必须改为非 local，例如 prod。仓库未提供 application-prod.yml 时，仅使用 application.yml + 环境变量即可。
SPRING_PROFILES_ACTIVE=prod

SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/ai_learning_workflow_navigator
SPRING_DATASOURCE_USERNAME=ai_nav
SPRING_DATASOURCE_PASSWORD=你的数据库密码

# LLM：演示环境可关闭，仅规则与模板
NAVIGATOR_LLM_ENABLED=false

# 若开启 LLM，再配置下列项（示例）
# NAVIGATOR_LLM_BASE_URL=https://api.openai.com
# NAVIGATOR_LLM_API_KEY=你的密钥
# NAVIGATOR_LLM_MODEL=gpt-4.1-mini
```

**注意**：`SPRING_DATASOURCE_*` 与 `NAVIGATOR_LLM_*` 会覆盖 `application.yml` 中的占位默认值；务必保证 **`SPRING_PROFILES_ACTIVE` 不是 `local`**，否则会误用 `application-local.yml`。

### 5.3 systemd 服务

```bash
sudo tee /etc/systemd/system/learning-nav.service <<'UNIT'
[Unit]
Description=Learning Workflow Navigator Backend
After=network.target postgresql.service

[Service]
User=root
WorkingDirectory=/opt/learning-nav/bin
EnvironmentFile=/opt/learning-nav/application.env
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/learning-nav/bin/app.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now learning-nav
sudo systemctl status learning-nav
```

确认本机可访问：

```bash
curl -sS http://127.0.0.1:8080/api/auth/me | head
```

---

## 6. Nginx 配置（HTTPS + 静态 + /api 反代）

### 6.1 HTTP 站点（可选：仅用于跳转 HTTPS）

将 `your-domain.com` 换成你的域名。

```bash
sudo tee /etc/nginx/conf.d/learning-nav.conf <<'NGINX'
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
NGINX
```

### 6.2 HTTPS 站点

建议使用 **Let’s Encrypt**（`certbot`）签发证书；首次可先用手动放置证书路径，或让 certbot 自动改 Nginx。

示例（证书路径需按 certbot 实际输出修改）：

```bash
sudo tee /etc/nginx/conf.d/learning-nav-ssl.conf <<'NGINX'
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    root /opt/learning-nav/static;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINX
```

```bash
sudo nginx -t && sudo systemctl reload nginx
```

**Certbot 简要步骤（Ubuntu 示例）：**

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

---

## 7. 发布新版本流程

```bash
cd /opt/apps/AI_Learning_Workflow_Navigator
git pull

cd backend && mvn -q clean package -DskipTests
cd ../frontend
printf '%s\n' 'VITE_API_BASE=' > .env.production
npm ci && npm run build

sudo cp backend/target/learning-workflow-navigator-backend-0.1.0-SNAPSHOT.jar /opt/learning-nav/bin/app.jar
sudo rm -rf /opt/learning-nav/static/*
sudo cp -r frontend/dist/* /opt/learning-nav/static/

sudo systemctl restart learning-nav
sudo systemctl status learning-nav
```

---

## 8. 验证清单（评委演示前自测）

1. 浏览器打开 `https://你的域名`，能加载首页。
2. **注册 / 登录** 成功（依赖 Cookie；同源下一般无跨域问题）。
3. 完成：**目标 → 诊断 → 计划 → 开始学习 → 任务执行 → 完成全部任务**。
4. 进入 **报告页**，无 `SESSION_NOT_COMPLETED` 类错误。
5. 服务器上：`journalctl -u learning-nav -n 200 --no-pager` 无持续报错；PostgreSQL 连接正常。

---

## 9. 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 前端请求 `localhost:8080` | 生产构建未设置 `VITE_API_BASE=` | 按第 4.3 节重建前端 |
| 登录后接口 401 | 跨域或 Cookie 未带上 | 优先采用本文同源 Nginx 方案；检查是否混用 IP 与域名 |
| 后端启动失败：数据库连接拒绝 | PostgreSQL 未监听或未授权 | 检查 `pg_hba.conf`、密码、安全组（RDS 白名单） |
| 后端连到错误的数据库或加载了开发配置 | 未设置 `SPRING_PROFILES_ACTIVE`，默认成了 `local` | 在 `application.env` 中设置 `SPRING_PROFILES_ACTIVE=prod`（或任意非 `local`） |
| Flyway 迁移失败 | 库版本与迁移脚本不一致 | 查看日志；勿随意改生产库手工结构 |

---

## 10. 安全建议

- 修改 SSH 默认策略、使用密钥登录；安全组收紧 `22`。
- 数据库密码、API Key **仅**放在服务器权限 600 的配置文件中。
- 定期备份 PostgreSQL（`pg_dump` 或 RDS 自动备份）。
- 生产环境将 `application-local.yml` 中的任何**示例远端库地址**视为无效，**绝不**在服务器上指向开发用共享库。

---

文档版本：与仓库 `backend/pom.xml` 中 `artifactId` / `version` 对应 JAR 名称一致即可；若版本号变更，请同步修改文中 JAR 路径。
