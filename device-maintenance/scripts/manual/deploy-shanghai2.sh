#!/bin/bash
##############################################################################
# Device Maintenance 手动部署脚本
# 用途：上传 JAR 包和配置文件到服务器，并重启服务
##############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 服务器配置
SERVER_HOST="116.128.204.12"
SERVER_PORT="13112"
SERVER_USER="actsvt112"
SERVER_PASS="pureg123!@#"

# 远程路径
REMOTE_BASE="/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT"
REMOTE_CONFIG_DIR="${REMOTE_BASE}/config"

# 本地文件路径（相对于项目根目录）
PROJECT_ROOT="/Users/orca/PRODS/devicebe/controller/device-maintenance"
LOCAL_JAR="${PROJECT_ROOT}/target/device-maintenance-1.0.0-SNAPSHOT.jar"
LOCAL_CONFIG="${PROJECT_ROOT}/src/main/resources/application.yml"

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Device Maintenance 部署脚本${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 1. 检查本地文件是否存在
echo -e "${YELLOW}[1/5] 检查本地文件...${NC}"
if [ ! -f "$LOCAL_JAR" ]; then
    echo -e "${RED}✗ 错误: JAR 包不存在: $LOCAL_JAR${NC}"
    echo -e "${YELLOW}提示: 请先运行 mvn clean package -DskipTests 编译项目${NC}"
    exit 1
fi
if [ ! -f "$LOCAL_CONFIG" ]; then
    echo -e "${RED}✗ 错误: 配置文件不存在: $LOCAL_CONFIG${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 本地文件检查完成${NC}"
echo ""

# 2. 测试服务器连接
echo -e "${YELLOW}[2/5] 测试服务器连接...${NC}"
if ! sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o ConnectTimeout=10 -o StrictHostKeyChecking=no \
    ${SERVER_USER}@${SERVER_HOST} "echo 'Connection OK'" > /dev/null 2>&1; then
    echo -e "${RED}✗ 错误: 无法连接到服务器 ${SERVER_HOST}:${SERVER_PORT}${NC}"
    echo -e "${YELLOW}提示: 请检查网络连接或 VPN${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 服务器连接成功${NC}"
echo ""

# 3. 上传 JAR 包
echo -e "${YELLOW}[3/5] 上传 JAR 包...${NC}"
echo "  源文件: $LOCAL_JAR"
echo "  目标路径: ${SERVER_USER}@${SERVER_HOST}:${REMOTE_BASE}/"
sshpass -p "$SERVER_PASS" scp -P $SERVER_PORT -o StrictHostKeyChecking=no \
    "$LOCAL_JAR" "${SERVER_USER}@${SERVER_HOST}:${REMOTE_BASE}/"
echo -e "${GREEN}✓ JAR 包上传成功${NC}"
echo ""

# 4. 上传配置文件
echo -e "${YELLOW}[4/5] 上传配置文件...${NC}"
echo "  源文件: $LOCAL_CONFIG"
echo "  目标路径: ${SERVER_USER}@${SERVER_HOST}:${REMOTE_CONFIG_DIR}/"
sshpass -p "$SERVER_PASS" scp -P $SERVER_PORT -o StrictHostKeyChecking=no \
    "$LOCAL_CONFIG" "${SERVER_USER}@${SERVER_HOST}:${REMOTE_CONFIG_DIR}/"
echo -e "${GREEN}✓ 配置文件上传成功${NC}"
echo ""

# 5. 重启服务
echo -e "${YELLOW}[5/5] 重启服务...${NC}"
sshpass -p "$SERVER_PASS" ssh -p $SERVER_PORT -o StrictHostKeyChecking=no \
    ${SERVER_USER}@${SERVER_HOST} << 'ENDSSH'
    # 切换到 root 用户重启服务
    echo "pureg123!@#" | sudo -S systemctl restart device-maintenance
    sleep 2
    echo "pureg123!@#" | sudo -S systemctl status device-maintenance --no-pager
ENDSSH

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✓ 部署完成！${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "查看服务日志："
echo "  ssh -p $SERVER_PORT ${SERVER_USER}@${SERVER_HOST}"
echo "  sudo journalctl -u device-maintenance -f"
echo ""

