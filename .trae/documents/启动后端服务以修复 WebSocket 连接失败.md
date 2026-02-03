# 彻底删除聊天室功能计划

## 1. 前端清理 (Frontend Cleanup)
### 删除文件
- [x] 删除 `src/views/Chat.vue`
- [x] 删除 `src/stores/chat.js`

### 修改代码
- [x] **`src/router/index.js`**: 删除 `/chat` 路由配置。
- [x] **`src/api/index.js`**: 删除 `chat` 相关的 API 定义 (`chat.history`, `chat.markRead`)。
- [x] **`src/views/Profile.vue`**: 删除“聊天”入口链接/按钮。
- [x] **`src/views/Home.vue`**: 检查并删除可能存在的聊天入口。
- [x] **`src/App.vue`**: 检查是否有全局 WebSocket 连接逻辑（如果在 App.vue 而不是 Store 里），予以移除。

## 2. 后端清理 (Backend Cleanup)
### 删除文件
- [x] **Controller**: `src/main/java/com/lovespace/controller/ChatController.java`
- [x] **Service**: `src/main/java/com/lovespace/service/ChatMessageService.java`
- [x] **Entity**: `src/main/java/com/lovespace/entity/ChatMessage.java`
- [x] **Mapper**: `src/main/java/com/lovespace/mapper/ChatMessageMapper.java`
- [x] **WebSocket Handler**: `src/main/java/com/lovespace/websocket/ChatWebSocketHandler.java`
- [x] **WebSocket Config**: `src/main/java/com/lovespace/config/WebSocketConfig.java`
- [x] **Interceptor**: `src/main/java/com/lovespace/config/WebSocketOriginInterceptor.java`

### 修改代码
- [x] **`src/main/java/com/lovespace/config/SecurityConfig.java`**:
    - 移除 `/ws/**`, `/api/ws/**`, `/chat` 的 `permitAll()` 放行规则。
    - 移除对 `WebSocketOriginInterceptor` 的任何引用（如果 SecurityConfig 里有的话，通常是在 WebConfig 或 WebSocketConfig 里）。

## 3. 数据库清理 (Database Cleanup)
- [ ] (可选) 如果需要，我可以为您生成 SQL 语句来删除 `chat_messages` 表。如果不执行，它只会成为数据库里的僵尸表，不影响运行。

## 4. 验证
- [ ] 重新构建前端 (`npm run build`) 并部署。
- [ ] 重启后端服务。
- [ ] 验证页面无报错，且聊天入口已消失。
