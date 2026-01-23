
# love-space

**我和 zfr 的 love-space**  
线上地址：<http://117.72.146.190>  

- 2026.01.21 初始化该网站  
- 2026.01.23 新增主页 / 个人中心功能：上传头像、修改昵称、修改密码、上传视频

---

# 💕 Love Space

情侣空间：动态、日记、纪念日与首页仪表盘的一体化小站（前后端分离，前后端都在同一台服务器上部署）。

本 README 既面向人类，也面向后续协助维护的 AI，用来说明：

- 功能与数据模型概览
- 现在线上部署形态（目录结构 / systemd / Nginx）
- 关键技术路线与约定（认证、文件存储、安全配置）

---

## 功能概览

- **登录与会话**
  - 基于 JWT 的账号登录，两人账号固定（limenglong / zengfanrui）
  - 前端将 token 存在 localStorage，后端使用过滤器统一校验

- **首页仪表盘**
  - 在一起计时器：天数 + 小时/分钟/秒实时累加
  - 恋爱开始日期：固定配置在后端
  - 即将到来的纪念日：剩余天数提示
  - 最近动态：展示最新几条图文/视频动态摘要

- **动态（Moments）**
  - 发布文字、图片、**视频**
  - 点赞 / 取消赞
  - 评论 / 删除评论
  - 删除动态时会同时清理该动态下的媒体、点赞、评论，并尝试删除对应文件

- **情侣日记（Diary）**
  - 每日一条，可多次修改
  - 列表视图：按时间倒序
  - 日历视图：按月查看每天是否有日记
  - 可见性：仅自己（self）或双方（both）

- **纪念日（Anniversary）**
  - 增删改查纪念日
  - 每年重复
  - 首页显示最近要到来的纪念日与倒计时

- **个人中心（Profile）**
  - 通过底部 Tab「我的」或首页右上角齿轮进入 `/profile`
  - 修改头像（上传图片）
  - 修改昵称
  - 修改密码（修改成功后强制重新登录）
  - 关于页面：展示项目版本与情侣信息
  - 退出登录

---

## 技术栈与架构

- **前端**
  - Vue 3 + Vite
  - 状态管理：Pinia
  - UI：Vant Mobile
  - 路由：vue-router（`createWebHistory`，SPA 模式）

- **后端**
  - Spring Boot 3.2
  - 持久化：MyBatis-Plus + MySQL 8
  - 认证：JWT（自定义过滤器）
  - 构建：Maven wrapper（`./mvnw`）

- **网关 / Web 服务器**
  - Nginx 1.26
  - 同域部署：`/` 前端静态，`/api` 反代后端，`/uploads` 静态文件

- **运行环境（线上示例）**
  - 京东云 CentOS 7，2c4g，约 60G 系统盘
  - Java 17（/usr/local/jdk17/bin/java）
  - systemd 管理后端服务

---

## 数据模型与主要接口（后端概览）

### 用户 / 认证

- 表：`user`
  - 字段：id, username, password(BCrypt), nickname, avatar, created_at, updated_at
  - 初始账号（由 schema.sql 初始化）：
    - 用户名：`limenglong` / `zengfanrui`
    - 默认密码：`love520`（BCrypt 存储）

- 主要接口
  - `POST /api/auth/login`：登录，返回 token + 用户信息
  - `GET /api/auth/me`：当前用户信息
  - `GET /api/auth/partner`：另一半信息
  - `PUT /api/auth/me`：更新当前用户信息
  - `POST /api/auth/change-password`：修改密码（需旧密码）

- JWT 认证
  - 过滤器：`JwtAuthenticationFilter`
  - 白名单：`/api/auth/login`, `/api/health`, `/uploads/**`, `/favicon.ico`, `/error`
  - 其余接口需要 `Authorization: Bearer <token>`

- 登录限流
  - 组件：`LoginRateLimiter`
  - 维度：IP + 用户名
  - 默认配置：5 分钟内最多 10 次尝试（可通过配置修改）

### 动态 / 评论

- 表：`moment`, `moment_media`, `moment_like`, `comment`
- 主要接口
  - `POST /api/moments`：发布动态，表单字段：
    - `content`（可选）
    - `location`（可选）
    - `files`（MultipartFile[]，图片或视频）
  - `GET /api/moments`：分页获取动态列表
  - `GET /api/moments/{id}`：动态详情
  - `DELETE /api/moments/{id}`：删除动态（带级联清理）
  - `POST /api/moments/{id}/like`：点赞 / 取消点赞
  - `POST /api/moments/{id}/comments`：添加评论
  - `DELETE /api/moments/comments/{commentId}`：删除评论

### 日记

- 表：`diary`
- 主要接口
  - `POST /api/diaries`：写 / 更新当天日记
  - `GET /api/diaries`：分页日记列表
  - `GET /api/diaries/month`：某月所有日记
  - `GET /api/diaries/date`：某天日记
  - `GET /api/diaries/{id}`：日记详情
  - `DELETE /api/diaries/{id}`：删除日记

### 纪念日 / 仪表盘

- 表：`anniversary`
- 主要接口
  - `GET /api/anniversaries`：全部纪念日
  - `POST /api/anniversaries`：新增
  - `PUT /api/anniversaries/{id}`：更新
  - `DELETE /api/anniversaries/{id}`：删除
  - `GET /api/anniversaries/upcoming`：即将到来的纪念日
  - `GET /api/dashboard`：首页仪表盘数据（在一起时间、即将到来的纪念日、最近动态）

### 文件上传

- 表：`moment_media` 引用文件 URL
- 统一文件服务
  - `POST /api/files/upload`：单文件上传（图片 / 视频）
  - `POST /api/files/upload/batch`：多文件上传
  - `DELETE /api/files?url=...`：删除文件（同时会做路径安全检查）

- 头像上传
  - `POST /api/users/avatar`（通过前端 Profile 页面调用）

---

## 文件存储与限制

- **存储位置**
  - 配置项：`upload.path`，默认 `/data/love-space/uploads`
  - Nginx 暴露路径：`/uploads/**` → `alias /data/love-space/uploads/`

- **目录结构（示例）**
  - 图片：`/data/love-space/uploads/images/yyyy/MM/dd/<uuid>.<ext>`
  - 视频：`/data/love-space/uploads/videos/yyyy/MM/dd/<uuid>.<ext>`

- **类型与大小限制**
  - 图片 MIME：`image/jpeg`, `image/png`, `image/gif`, `image/webp`
  - 视频 MIME：`video/mp4`, `video/quicktime`, `video/x-msvideo`, `video/webm`
  - 单文件最大：100MB（后端 + Nginx 一致）

- **安全处理**
  - 删除文件时做路径归一化与越权检查，防止删除任意系统文件
  - 删除动态时尝试删除对应媒体文件（如果文件已不存在会忽略）

---

## 本地开发

### 后端

1. 初始化数据库

```bash
mysql -u love -p love_space < love-space-backend/src/main/resources/schema.sql
```

2. 启动（可选通过环境变量覆盖配置）

```bash
cd love-space-backend
./mvnw spring-boot:run
```

### 前端

```bash
cd love-space-frontend
npm install
npm run dev
```

---

## 云服务器部署（当前推荐方案）

### 1) 必需环境

- Java 17（示例：`/usr/local/jdk17/bin/java`）
- Node.js（仅用于构建前端）
- MySQL 8
- Nginx 1.2x
- systemd（管理后端服务）

### 2) 目录约定（线上）

在服务器上克隆仓库到 `/data/love-space`，最终结构示例：

- `/data/love-space/love-space-frontend/dist`：前端构建产物
- `/data/love-space/love-space-backend`：后端代码与 jar
- `/data/love-space/uploads`：上传文件（图片 / 视频）
- `/data/love-space/logs`：后端日志目录（由配置决定）
- `/data/love-space/.env`：后端 prod 环境变量

### 3) 环境变量与 prod 启动校验

后端以 `prod` profile 启动时，会在启动阶段校验关键配置项是否存在，不满足则直接抛错终止：

- 必填配置（示例）：  
  - `DB_URL`  
  - `DB_USERNAME`  
  - `DB_PASSWORD`  
  - `JWT_SECRET`（至少 32 字节）  
  - `UPLOAD_PATH`（推荐 `/data/love-space/uploads`）  
  - `LOG_PATH`（推荐 `/data/love-space/logs`）

可参考：

- 环境变量示例：`deploy/systemd/love-space.env.example`
- systemd 服务模板：`deploy/systemd/love-space-backend.service`

在服务器上创建 `/data/love-space/.env` 并配置上述键值。

### 4) systemd 服务（示例）

`/etc/systemd/system/love-space-backend.service`：

```ini
[Unit]
Description=Love Space Backend
After=network.target mysqld.service
Wants=mysqld.service

[Service]
Type=simple
WorkingDirectory=/data/love-space/love-space-backend
EnvironmentFile=/data/love-space/.env
ExecStart=/usr/local/jdk17/bin/java -jar /data/love-space/love-space-backend/target/love-space-backend-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启用与重启：

```bash
systemctl daemon-reload
systemctl enable love-space-backend
systemctl restart love-space-backend
systemctl status love-space-backend --no-pager
```

### 5) Nginx 配置（同域）

`/etc/nginx/conf.d/love-space.conf` 示例：

```nginx
server {
    listen 80;
    server_name _;

    # 前端静态资源
    root /data/love-space/love-space-frontend/dist;
    index index.html;

    # 上传文件访问
    location ^~ /uploads/ {
        alias /data/love-space/uploads/;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size 100M;
    }

    # SPA 路由兜底
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

检查并重载：

```bash
nginx -t
systemctl reload nginx
```

### 6) 前端构建与部署

在服务器上（或本地构建后上传）：

```bash
cd /data/love-space/love-space-frontend
npm install
npm run build
```

确认 `dist/index.html` 已生成并与 Nginx 配置的 root 一致。

### 7) 备份策略

- MySQL：定期备份 `love_space` 数据库（按天/按周）
- 上传目录：备份 `/data/love-space/uploads`

---

## 备注

- 当前仍采用 **本地磁盘存储图片/视频**，后续如需迁移到阿里云 OSS，可在文件服务层增加适配，不影响数据库与前端 URL 使用方式。
- 所有密码在数据库中以 BCrypt 形式存储，后端通过 `PasswordUtil` 进行校验。  
  如需强制重置，可在 MySQL 中将密码字段写为明文，后端启动时的初始化逻辑会统一转换为默认密码的 BCrypt。
