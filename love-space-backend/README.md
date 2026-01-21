# 💕 Love Space 情侣空间

专属于 **李梦龙** ❤️ **曾凡芮** 的私密空间

## 功能特性

- 🏠 **首页仪表盘** - 恋爱计时器、纪念日提醒、最新动态
- 📝 **动态广场** - 发布文字、图片、视频，点赞评论
- 📔 **情侣日记** - 每日日记，心情记录，日历视图
- 💕 **纪念日管理** - 重要日子记录，倒计时提醒

## 技术栈

- 后端：Spring Boot 3.2 + MyBatis Plus + MySQL 8
- 认证：JWT Token
- 文件：本地存储

## 快速开始

### 1. 初始化数据库

```bash
mysql -u love -p love_space < src/main/resources/schema.sql
```

### 2. 编译运行

```bash
# 编译
./mvnw clean package -DskipTests

# 运行
java -jar target/love-space-backend-1.0.0.jar
```

### 3. 一键部署

```bash
chmod +x deploy.sh
./deploy.sh
```

## 默认账号

| 用户 | 用户名 | 密码 |
|------|--------|------|
| 李梦龙 | limenglong | love520 |
| 曾凡芮 | zengfanrui | love520 |

⚠️ 请登录后及时修改密码！

## API 文档

### 认证相关

```
POST /api/auth/login          登录
GET  /api/auth/me             获取当前用户
GET  /api/auth/partner        获取另一半信息
PUT  /api/auth/me             更新个人信息
POST /api/auth/change-password 修改密码
```

### 仪表盘

```
GET /api/dashboard            获取首页数据
```

### 动态

```
GET    /api/moments           获取动态列表
POST   /api/moments           发布动态
GET    /api/moments/:id       获取动态详情
DELETE /api/moments/:id       删除动态
POST   /api/moments/:id/like  点赞/取消
POST   /api/moments/:id/comments  添加评论
DELETE /api/moments/comments/:id  删除评论
```

### 日记

```
GET  /api/diaries             获取日记列表
POST /api/diaries             写日记
GET  /api/diaries/month       获取某月日记
GET  /api/diaries/date        获取某天日记
GET  /api/diaries/:id         获取日记详情
DELETE /api/diaries/:id       删除日记
```

### 纪念日

```
GET    /api/anniversaries           获取所有纪念日
POST   /api/anniversaries           添加纪念日
GET    /api/anniversaries/together  获取在一起天数
GET    /api/anniversaries/upcoming  获取即将到来的纪念日
PUT    /api/anniversaries/:id       更新纪念日
DELETE /api/anniversaries/:id       删除纪念日
```

### 文件上传

```
POST /api/files/upload        上传单个文件
POST /api/files/upload/batch  批量上传
DELETE /api/files             删除文件
```

## 目录结构

```
love-space-backend/
├── src/main/java/com/lovespace/
│   ├── LoveSpaceApplication.java   # 启动类
│   ├── common/                     # 通用类
│   ├── config/                     # 配置类
│   ├── controller/                 # 控制器
│   ├── dto/                        # 数据传输对象
│   ├── entity/                     # 实体类
│   ├── filter/                     # 过滤器
│   ├── mapper/                     # 数据访问层
│   ├── service/                    # 业务逻辑层
│   └── util/                       # 工具类
├── src/main/resources/
│   ├── application.yml             # 配置文件
│   └── schema.sql                  # 数据库脚本
├── deploy.sh                       # 部署脚本
└── pom.xml                         # Maven 配置
```

---

Made with ❤️ for Love
