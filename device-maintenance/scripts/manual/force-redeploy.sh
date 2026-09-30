#!/bin/bash
# 强制重新部署脚本 - 确保上传新的可执行 jar

set -e

SERVER_HOST="116.128.204.12"
SERVER_USER="actsvt112"
SERVER_PASSWORD="pureg123!@#"
SSH_PORT="13112"
DEPLOY_BASE="/opt/dci/deploy"
VERSION="1.0.0-SNAPSHOT"
PACKAGE_NAME="device-maintenance"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目根目录（脚本所在位置的上上上级目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo -e "${CYAN}=== 强制重新部署 device-maintenance ===${NC}"
echo "项目目录: $PROJECT_ROOT"
echo ""

# Step 1: 验证本地 jar
echo -e "${CYAN}=== 步骤1: 验证本地 jar 文件 ===${NC}"
LOCAL_JAR="$PROJECT_ROOT/target/${PACKAGE_NAME}-${VERSION}.jar"
if [ ! -f "$LOCAL_JAR" ]; then
    echo -e "${RED}✗ 本地 jar 文件不存在: $LOCAL_JAR${NC}"
    echo "请先运行: mvn clean package -DskipTests"
    exit 1
fi

if [ -x "$LOCAL_JAR" ]; then
    echo -e "${GREEN}✓ 本地 jar 文件可执行${NC}"
    echo "文件大小: $(ls -lh "$LOCAL_JAR" | awk '{print $5}')"
    echo "文件头部:"
    head -3 "$LOCAL_JAR"
else
    echo -e "${RED}✗ 本地 jar 文件不可执行！${NC}"
    echo "请检查 pom.xml 中的 <executable>true</executable> 配置"
    exit 1
fi

# Step 2: 上传 tar.gz
echo ""
echo -e "${CYAN}=== 步骤2: 上传部署包 ===${NC}"
TARBALL="$PROJECT_ROOT/target/${PACKAGE_NAME}-${VERSION}-bin.tar.gz"
if [ ! -f "$TARBALL" ]; then
    echo -e "${RED}✗ tar.gz 文件不存在: $TARBALL${NC}"
    exit 1
fi

echo "上传: $TARBALL"
sshpass -p "$SERVER_PASSWORD" scp -P "$SSH_PORT" "$TARBALL" "${SERVER_USER}@${SERVER_HOST}:/tmp/" || {
    echo -e "${RED}✗ 上传失败${NC}"
    exit 1
}
echo -e "${GREEN}✓ 上传成功${NC}"

# Step 3: 远程部署
echo ""
echo -e "${CYAN}=== 步骤3: 远程部署 ===${NC}"

sshpass -p "$SERVER_PASSWORD" ssh -p "$SSH_PORT" "${SERVER_USER}@${SERVER_HOST}" bash << 'ENDSSH'
set -e

echo ">>> 停止旧服务"
echo "pureg123!@#" | sudo -S systemctl stop device-maintenance 2>/dev/null || true
sleep 2

echo ">>> 删除旧目录"
cd /opt/dci/deploy
echo "pureg123!@#" | sudo -S rm -rf device-maintenance-1.0.0-SNAPSHOT-backup 2>/dev/null || true
echo "pureg123!@#" | sudo -S mv device-maintenance-1.0.0-SNAPSHOT device-maintenance-1.0.0-SNAPSHOT-backup 2>/dev/null || true

echo ">>> 解压新包"
echo "pureg123!@#" | sudo -S tar -xzf /tmp/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz
echo "pureg123!@#" | sudo -S chown -R actsvt112:actsvt112 device-maintenance-1.0.0-SNAPSHOT

echo ">>> 验证解压后的 jar 文件"
cd device-maintenance-1.0.0-SNAPSHOT
if [ -x device-maintenance-1.0.0-SNAPSHOT.jar ]; then
    echo "✓ jar 文件可执行"
    head -3 device-maintenance-1.0.0-SNAPSHOT.jar
else
    echo "✗ jar 文件不可执行！"
    ls -l device-maintenance-1.0.0-SNAPSHOT.jar
    exit 1
fi

echo ">>> 创建环境文件"
cat > env-shanghai2.sh << 'EOF'
#!/bin/bash
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
EOF
chmod +x env-shanghai2.sh

echo ">>> 执行 install.sh"
source env-shanghai2.sh && echo "pureg123!@#" | sudo -S -E bash bin/install.sh

echo ">>> 等待服务启动"
sleep 5

echo ">>> 检查服务状态"
echo "pureg123!@#" | sudo -S systemctl status device-maintenance --no-pager | head -10

echo ""
echo ">>> 检查配置"
grep "^NAMESPACE=" config/zkclient_conf.properties

echo ""
echo ">>> 检查日志（最近10行）"
echo "pureg123!@#" | sudo -S journalctl -u device-maintenance -n 10 --no-pager

ENDSSH

echo ""
echo -e "${GREEN}=== 部署完成 ===${NC}"
echo ""
echo -e "${CYAN}下一步：检查健康状态${NC}"
echo "运行: curl http://116.128.204.12:18008/actuator/health"

