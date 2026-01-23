
# love-space
**我和zfr的love-space**   
## http://117.72.146.190
## 2026.1.21 初始化该网站

## 2026.1.23 新增主页功能 
- 上传头像
- 修改昵称
- 修改密码
- 上传视频


# 💕 Love Space

情侣空间：动态、日记、纪念日与首页仪表盘的一体化小站（前后端分离）。

## 功能

- 登录与会话：JWT
- 首页：在一起计时、即将到来的纪念日、最近动态
- 动态：图文/视频发布、点赞、评论、删除
- 日记：按日期写/改、列表/日历视图、可见性（仅自己/双方）
- 纪念日：增删改查、每年重复、倒计时展示
- 个人中心：改昵称、改密码、换头像

## 技术栈

- 前端：Vue 3 + Vite + Pinia + Vant
- 后端：Spring Boot 3.2 + MyBatis-Plus + MySQL 8 + JWT
- 网关：Nginx（建议同域反代 `/api` 与静态资源）

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

## 云服务器部署要点（推荐同域）

### 1) 必需环境

- Java 17
- Node.js（用于构建前端）
- MySQL 8
- Nginx

### 2) 目录约定

- `/data/love-space/frontend/dist`：前端构建产物
- `/data/love-space/backend`：后端代码与 jar
- `/data/love-space/uploads`：上传文件
- `/data/love-space/logs`：后端日志目录

### 3) 环境变量与 prod 启动校验

后端以 `prod` profile 启动时会校验关键配置项是否存在（DB/JWT/路径）。可参考：

- 环境变量示例：deploy/systemd/love-space.env.example
- systemd 服务模板：deploy/systemd/love-space-backend.service

将环境文件放置在 `/data/love-space/.env`，并确保设置了 `DB_PASSWORD` 与足够强度的 `JWT_SECRET`（至少 32 字节）。

### 4) Nginx

前端目录已提供 Nginx 示例配置（含 `/api` 反代与 `/uploads` 静态映射）：love-space-frontend/nginx.conf

建议启用 HTTPS（Let’s Encrypt）并将 `/uploads` 交给 Nginx 直接提供静态访问。

### 5) 备份

- MySQL：定期备份（按天/按周）
- 上传目录：备份 `/data/love-space/uploads`
>>>>>>> Stashed changes
