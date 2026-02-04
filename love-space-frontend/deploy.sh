#!/bin/bash

# ==========================================
# Love Space 前端部署脚本
# ==========================================

echo "=========================================="
echo "  💕 Love Space 前端 - 部署脚本"
echo "=========================================="

cd /data/love-space/frontend

# 检查 Node.js
echo "检查 Node.js..."
node -v
if [ $? -ne 0 ]; then
    echo "❌ Node.js 未安装"
    exit 1
fi

# 安装依赖
echo ""
echo "安装依赖..."
npm install

if [ $? -ne 0 ]; then
    echo "❌ 依赖安装失败"
    exit 1
fi

# 构建
echo ""
echo "构建项目..."
npm run build

if [ $? -ne 0 ]; then
    echo "❌ 构建失败"
    exit 1
fi

# 配置 Nginx
echo ""
echo "配置 Nginx..."

# 备份默认配置
if [ -f /etc/nginx/conf.d/default.conf ]; then
    mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak
fi

# 复制配置
cp nginx.conf /etc/nginx/conf.d/love-space.conf

# 测试 Nginx 配置
nginx -t
if [ $? -ne 0 ]; then
    echo "❌ Nginx 配置有误"
    exit 1
fi

# 清除 Nginx 缓存（如果有）
echo ""
echo "清除缓存..."
find /var/cache/nginx -type f -delete 2>/dev/null || true

# 重载 Nginx
systemctl reload nginx

echo ""
echo "=========================================="
echo "  ✅ 前端部署成功！"
echo "==========================================="
echo ""
echo "  访问地址: http://你的服务器IP"
echo ""
echo "  ⚠️  手机浏览器请清除缓存后访问："
echo "    - Safari: 设置 > Safari > 清除历史记录与网站数据"
echo "    - Chrome: 设置 > 隐私和安全 > 清除浏览数据"
echo "    - 或使用无痕/隐私模式访问"
echo ""
echo "  默认账号："
echo "    用户名: limenglong / zengfanrui"
echo "    密码: love520"
echo ""
echo "=========================================="
