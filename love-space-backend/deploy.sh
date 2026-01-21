#!/bin/bash

# ==========================================
# Love Space 部署脚本
# ==========================================

echo "=========================================="
echo "  💕 Love Space 情侣空间 - 部署脚本"
echo "=========================================="

# 检查 Java 环境
echo "检查 Java 环境..."
java -version
if [ $? -ne 0 ]; then
    echo "❌ Java 未安装，请先安装 JDK 17"
    exit 1
fi

# 检查 MySQL 服务
echo "检查 MySQL 服务..."
systemctl is-active --quiet mysqld
if [ $? -ne 0 ]; then
    echo "❌ MySQL 未运行，请先启动 MySQL"
    exit 1
fi

# 进入项目目录
cd /data/love-space/backend

# 检查是否需要初始化数据库
echo ""
echo "是否需要初始化数据库？（首次部署请选 y）"
read -p "输入 y/n: " init_db

if [ "$init_db" = "y" ]; then
    echo "初始化数据库..."
    mysql -u love -p'Lml@200234' love_space < src/main/resources/schema.sql
    if [ $? -eq 0 ]; then
        echo "✅ 数据库初始化成功"
    else
        echo "❌ 数据库初始化失败"
        exit 1
    fi
fi

# 编译项目
echo ""
echo "编译项目..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"

# 停止旧进程
echo ""
echo "停止旧进程..."
PID=$(ps -ef | grep love-space-backend | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    kill -9 $PID
    echo "已停止旧进程 PID: $PID"
fi

# 启动新进程
echo ""
echo "启动服务..."
nohup java -jar target/love-space-backend-1.0.0.jar > /data/love-space/logs/app.log 2>&1 &

sleep 5

# 检查是否启动成功
PID=$(ps -ef | grep love-space-backend | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo ""
    echo "=========================================="
    echo "  ✅ 部署成功！"
    echo "=========================================="
    echo ""
    echo "  后端地址: http://localhost:8080"
    echo "  进程 PID: $PID"
    echo "  日志文件: /data/love-space/logs/app.log"
    echo ""
    echo "  默认账号："
    echo "    用户名: limenglong / zengfanrui"
    echo "    密码: love520"
    echo ""
    echo "=========================================="
else
    echo "❌ 启动失败，请查看日志："
    echo "tail -f /data/love-space/logs/app.log"
    exit 1
fi
