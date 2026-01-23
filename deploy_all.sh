#!/bin/bash

# ==========================================
# Love Space 全栈部署脚本
# ==========================================

# 设置基础路径
BASE_DIR="/data/love-space"
FRONTEND_DIR="$BASE_DIR/frontend"
BACKEND_DIR="$BASE_DIR/backend"
UPLOADS_DIR="$BASE_DIR/uploads"
LOGS_DIR="$BASE_DIR/logs"

# 加载环境变量（如果存在）
if [ -f .env ]; then
    export $(cat .env | xargs)
fi

echo "=========================================="
echo "  💕 Love Space 部署脚本"
echo "=========================================="

# 1. 准备目录
echo "Checking directories..."
mkdir -p $FRONTEND_DIR
mkdir -p $BACKEND_DIR
mkdir -p $UPLOADS_DIR
mkdir -p $LOGS_DIR

# 2. 停止旧服务
echo "Stopping old services..."
PID=$(ps -ef | grep love-space-backend | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    kill -9 $PID
    echo "✅ 已停止旧后端进程 PID: $PID"
else
    echo "ℹ️ 没有发现运行中的后端进程"
fi

# 3. 部署前端
echo ""
echo "Deploying Frontend..."

# 尝试进入前端目录 (优先匹配 server 上的 frontend，其次是本地的 love-space-frontend)
if [ -d "frontend" ]; then
    cd frontend
elif [ -d "love-space-frontend" ]; then
    cd love-space-frontend
else
    echo "❌ 找不到前端目录 (frontend 或 love-space-frontend)"
    exit 1
fi

# 检查是否安装了 node_modules，如果没有则安装
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

echo "Building frontend..."
npm run build

if [ $? -eq 0 ]; then
    echo "✅ 前端构建成功"
    # 清理旧文件并复制新文件
    # 注意：如果当前在 frontend 目录，且 FRONTEND_DIR 指向 ../frontend，需要小心处理
    # 实际上，通常构建出的 dist 就在当前目录下
    
    # 如果我们在 frontend 目录中，而目标目录也是 frontend，我们只需要保留 dist 即可
    # 但这里的逻辑原本是将 dist 复制到部署目录。
    # 假设服务器上的结构是：源代码就在 /data/love-space/frontend 中
    # 那么构建后的 dist 就在 /data/love-space/frontend/dist
    # Nginx 指向的也是这个 dist。
    
    echo "✅ 前端构建完成，静态文件位于 $(pwd)/dist"
else
    echo "❌ 前端构建失败"
    exit 1
fi

cd ..

# 4. 部署后端
echo ""
echo "Deploying Backend..."

# 尝试进入后端目录
if [ -d "backend" ]; then
    cd backend
elif [ -d "love-space-backend" ]; then
    cd love-space-backend
else
    echo "❌ 找不到后端目录 (backend 或 love-space-backend)"
    exit 1
fi

echo "Building backend..."
chmod +x mvnw
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ 后端编译成功"
    
    # 启动后端
    echo "Starting backend service..."
    # 使用 nohup 后台运行，并指定日志路径
    nohup java -jar target/love-space-backend-1.0.0.jar \
        --spring.profiles.active=prod \
        > $LOGS_DIR/app.log 2>&1 &
        
    sleep 5
    
    NEW_PID=$(ps -ef | grep love-space-backend | grep -v grep | awk '{print $2}')
    if [ -n "$NEW_PID" ]; then
        echo "✅ 后端服务启动成功! PID: $NEW_PID"
        echo "📜 日志文件: $LOGS_DIR/app.log"
    else
        echo "❌ 后端服务启动失败，请检查日志"
        exit 1
    fi
else
    echo "❌ 后端编译失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "  🎉 部署完成！"
echo "=========================================="
echo "  前端: 请确保 Nginx 已配置并重新加载 (nginx -s reload)"
echo "  后端: 运行在 8080 端口"
echo "=========================================="
