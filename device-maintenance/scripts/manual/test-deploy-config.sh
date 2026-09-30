#!/bin/bash
# 快速测试部署配置是否正确（不实际部署）

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/env-shanghai2.conf"

echo "=== 加载配置 ==="
source "$CONFIG_FILE"

echo ""
echo "=== 关键配置检查 ==="
echo "部署路径: $DEPLOY_BASE/${PACKAGE_NAME}-${VERSION}"
echo "myIp: $myIp"
echo "NAMESPACE: $NAMESPACE"
echo "SERVER_PORT: $SERVER_PORT"

echo ""
echo "=== 对比标准配置（其他微服务） ==="
echo "预期："
echo "  DEPLOY_BASE=/home/dci/deploy"
echo "  myIp=0.0.0.0"
echo "  NAMESPACE=clband"
echo ""
echo "实际："
echo "  DEPLOY_BASE=$DEPLOY_BASE"
echo "  myIp=$myIp"
echo "  NAMESPACE=$NAMESPACE"

echo ""
if [[ "$DEPLOY_BASE" == "/home/dci/deploy" ]] && \
   [[ "$myIp" == "0.0.0.0" ]] && \
   [[ "$NAMESPACE" == "clband" ]]; then
    echo "✅ 配置正确！与其他微服务一致"
else
    echo "❌ 配置有误！"
    exit 1
fi

echo ""
echo "=== 验证远程服务器上的其他微服务配置 ==="
sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT $SERVER_USER@$SERVER_HOST "
    echo '检查 neManager 配置:'
    grep -E 'NAMESPACE|myIp|port=' /home/dci/deploy/neManager-1.0.0-SNAPSHOT/config/zkclient_conf.properties 2>/dev/null || echo '未找到'
    
    echo ''
    echo '检查 taskinfo 配置:'
    grep -E 'NAMESPACE|myIp|port=' /home/dci/deploy/taskinfo-1.0.0-SNAPSHOT/config/zkclient_conf.properties 2>/dev/null || echo '未找到'
"

echo ""
echo "✅ 配置验证完成！"
echo ""
echo "如需部署，执行："
echo "  bash scripts/manual/shanghai2-manual-deploy.sh"

