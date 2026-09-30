#!/bin/bash
# 完成部署（从上传的tar.gz开始）

set -e

SERVER_HOST="116.128.204.12"
SERVER_USER="actsvt112"
SERVER_PASSWORD="pureg123!@#"
SSH_PORT="13112"
DEPLOY_BASE="/opt/dci/deploy"
PACKAGE_NAME="Device Maintenance Microservice"
VERSION="1.0.0-SNAPSHOT"
DEPLOY_DIR="$DEPLOY_BASE/${PACKAGE_NAME}-${VERSION}"

# 颜色定义
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m'

# 环境变量
export myIp="192.168.3.112"
export MYSQL_IP="127.0.0.1"
export MYSQL_PORT="3309"
export MYSQL_USER="root"
export MYSQL_PASSWORD="dciworld@iivi"
export MONGO_SERVERS="127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012"
export MONGO_DATABASE="sotn"
export MONGO_USER="dci"
export MONGO_PWD="dciworld@iivi"
export KAFKA_BROKER="127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"
export pmcZooKeepers="127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192"
export NAMESPACE="dciworld/clband"
export swVersion="1.0.0-SNAPSHOT"

echo -e "${CYAN}=== 继续完成部署 ===${NC}"

# 1. 解压
echo -e "${BLUE}解压tar.gz...${NC}"
sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT actsvt112@116.128.204.12 << 'ENDSSH'
cd /opt/dci/deploy
tar -xzf /tmp/Device\ Maintenance\ Microservice-1.0.0-SNAPSHOT-bin.tar.gz
rm -f /tmp/Device\ Maintenance\ Microservice-1.0.0-SNAPSHOT-bin.tar.gz
echo "✓ 解压完成"
ENDSSH

# 2. 创建环境文件
echo -e "${BLUE}创建环境文件...${NC}"
sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT actsvt112@116.128.204.12 << ENDSSH
cat > '$DEPLOY_DIR/env-shanghai2.sh' << 'EOF'
#!/bin/bash
export myIp="$myIp"
export MYSQL_IP="$MYSQL_IP"
export MYSQL_PORT="$MYSQL_PORT"
export MYSQL_USER="$MYSQL_USER"
export MYSQL_PASSWORD="$MYSQL_PASSWORD"
export MONGO_SERVERS="$MONGO_SERVERS"
export MONGO_DATABASE="$MONGO_DATABASE"
export MONGO_USER="$MONGO_USER"
export MONGO_PWD="$MONGO_PWD"
export KAFKA_BROKER="$KAFKA_BROKER"
export pmcZooKeepers="$pmcZooKeepers"
export NAMESPACE="$NAMESPACE"
export swVersion="$swVersion"
EOF
chmod +x '$DEPLOY_DIR/env-shanghai2.sh'
echo "✓ 环境文件已创建"
ENDSSH

# 3. 执行 install.sh
echo -e "${BLUE}执行 install.sh...${NC}"
expect << ENDEXPECT
set timeout 60
spawn sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT actsvt112@116.128.204.12 "cd '$DEPLOY_DIR' && source env-shanghai2.sh && sudo bash bin/install.sh"
expect {
    "*assword*" {
        send "$SERVER_PASSWORD\r"
        exp_continue
    }
    eof
}
ENDEXPECT

echo -e "${GREEN}✓ 部署完成${NC}"

# 4. 验证
echo ""
echo -e "${CYAN}=== 验证部署 ===${NC}"
sleep 5

sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT actsvt112@116.128.204.12 << 'ENDSSH'
echo "1. SystemD服务状态:"
sudo systemctl status "Device Maintenance Microservice.service" --no-pager -l || true

echo ""
echo "2. 检查进程:"
ps aux | grep device-maintenance-1.0.0-SNAPSHOT.jar | grep -v grep || echo "未找到进程"

echo ""
echo "3. 检查配置:"
grep "^NAMESPACE=" '/opt/dci/deploy/Device Maintenance Microservice-1.0.0-SNAPSHOT/config/zkclient_conf.properties'

echo ""
echo "4. 健康检查:"
sleep 5
curl -s http://localhost:18008/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:18008/actuator/health
ENDSSH

echo ""
echo -e "${GREEN}部署和验证完成！${NC}"

