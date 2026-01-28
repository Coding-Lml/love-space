## 现状结论
- 项目结构完整：前端为 Vue3 + Vite + Pinia + Vant；后端为 Spring Boot + Spring Security + MyBatis-Plus + JWT + WebSocket。
- 目前存在几处“会导致无法正常跑起来 / 生产事故风险 / 明显影响体验”的点，需要在后续阶段直接修改代码与配置。

## 重点问题（已定位到文件）
1. 启动即重置所有用户密码（高危、影响可用性）
   - [DataInitializer.java](file:///Users/aloong/Documents/My%20Web/love-space/love-space-backend/src/main/java/com/lovespace/config/DataInitializer.java)
   - 当前逻辑会在特定条件下把所有用户密码改为同一个默认值 love520，线上非常危险。
2. 默认 JWT_SECRET 为空，后端可能直接启动失败（影响“能否正常运行”）
   - [application.yml](file:///Users/aloong/Documents/My%20Web/love-space/love-space-backend/src/main/resources/application.yml) 与 [JwtUtil.java](file:///Users/aloong/Documents/My%20Web/love-space/love-space-backend/src/main/java/com/lovespace/util/JwtUtil.java)
3. WebSocket 放开所有 Origin 且 token 放在 URL query（安全与体验风险；代理/日志容易泄漏）
   - [WebSocketConfig.java](file:///Users/aloong/Documents/My%20Web/love-space/love-space-backend/src/main/java/com/lovespace/config/WebSocketConfig.java)
   - 前端 [chat.js](file:///Users/aloong/Documents/My%20Web/love-space/love-space-frontend/src/stores/chat.js) 目前拼接 `?token=...`
4. 前端“owner/guest”判断硬编码用户名，容易与后端配置漂移（导致跳转/权限体验异常）
   - 前端 [user.js](file:///Users/aloong/Documents/My%20Web/love-space/love-space-frontend/src/stores/user.js)；后端 owner 来源配置 [RoleService.java](file:///Users/aloong/Documents/My%20Web/love-space/love-space-backend/src/main/java/com/lovespace/security/RoleService.java)

## 拟定修改（确认后我会直接改代码）
### 1) 禁止危险的密码初始化逻辑
- 将 DataInitializer 改为：默认不运行；仅在 dev 环境 + 显式开关开启时运行。
- 同时把“重置所有用户为同一默认密码”的策略替换为更安全的处理（例如：只在空库/首次初始化时创建示例用户；不覆盖已存在用户密码）。

### 2) 让后端默认可启动，并对生产更严格
- 调整 JWT 配置：
  - 本地/默认环境提供一个明确的 dev-only 默认 secret（保证开箱能跑）。
  - prod 环境继续强制要求显式 JWT_SECRET，并额外禁止使用 dev 默认值（避免生产误用）。
- 在 JwtUtil 启动期校验 secret 长度与空值，给出明确错误信息。

### 3) WebSocket：收紧 Origin + 去掉 URL token
- WebSocket Origin 改为读取现有 cors 配置项（或新增专用 ws.allowed-origins），不再 `*`。
- WebSocket 鉴权改为“连接建立后第一条消息进行 auth”（避免 token 出现在 URL），前端 chat store 在 onopen 后立即发送 auth 包；后端收到后再把 session 绑定到 userId，否则关闭连接。

### 4) 统一 owner/guest 判断来源（提升体验稳定性）
- 后端登录/注册返回里增加 `isOwner`（或 role 字段），并在 `/api/auth/me` 返回中也带上。
- 前端移除硬编码用户名：
  - `userStore.isOwner` 改为使用后端返回的 `isOwner/role`；
  - 路由守卫与登录跳转逻辑同步更新；
  - 做兼容：若老数据里没有该字段，才临时 fallback 到旧逻辑（避免老 localStorage 立刻坏）。

## 验证方式（修改后会执行）
- 后端：启动检查 + 关键接口冒烟（login/register/me、动态列表、文件上传、WebSocket 聊天连接与收发）。
- 前端：登录/注册跳转、访客与主人页面互斥跳转、头像上传、聊天断线重连。

如果你确认这个计划，我将退出计划模式并开始逐项修改与验证。