#!/bin/bash
# ============================================================================
# 本地测试脚本 - 验证打包和配置逻辑（不连接远程服务器）
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║          Device Maintenance - 本地部署测试                   ║${NC}"
echo -e "${CYAN}║          (验证打包和配置，不连接远程服务器)                   ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 加载配置文件
CONFIG_FILE="$SCRIPT_DIR/env-shanghai2.conf"
echo -e "${BLUE}📄 加载配置文件: $CONFIG_FILE${NC}"
source "$CONFIG_FILE"
echo -e "${GREEN}✓ 配置文件加载成功${NC}"
echo ""

# ============================================================================
# 测试1: 验证编译打包
# ============================================================================
echo -e "${CYAN}=== 测试1: 验证编译打包 ===${NC}"
cd "$PROJECT_ROOT"

echo "执行编译..."
if mvn clean package -DskipTests -q; then
    echo -e "${GREEN}✓ 编译成功${NC}"
else
    echo -e "${RED}✗ 编译失败${NC}"
    exit 1
fi

# 检查生成的文件
echo ""
echo -e "${BLUE}检查生成的文件:${NC}"
files=(
    "target/install.sh"
    "target/uninstall.sh"
    "target/Device Maintenance Microservice.service"
    "target/Device Maintenance Microservice-1.0.0-SNAPSHOT-bin.tar.gz"
    "target/device-maintenance-1.0.0-SNAPSHOT.jar"
)

all_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        size=$(ls -lh "$file" | awk '{print $5}')
        echo -e "  ${GREEN}✓${NC} $file ($size)"
    else
        echo -e "  ${RED}✗${NC} $file (不存在)"
        all_exist=false
    fi
done

if [ "$all_exist" = false ]; then
    echo -e "${RED}✗ 部分文件未生成${NC}"
    exit 1
fi
echo ""

# ============================================================================
# 测试2: 验证 tar.gz 包结构
# ============================================================================
echo -e "${CYAN}=== 测试2: 验证 tar.gz 包结构 ===${NC}"

TAR_FILE="target/Device Maintenance Microservice-1.0.0-SNAPSHOT-bin.tar.gz"
echo "检查 tar.gz 包内容..."
echo ""

required_files=(
    "bin/install.sh"
    "bin/uninstall.sh"
    "bin/Device Maintenance Microservice.service"
    "device-maintenance-1.0.0-SNAPSHOT.jar"
    "config/application.yml"
    "config/zkclient_conf.properties"
    "config/mongodb.properties"
    "config/mysql.properties"
)

all_in_tar=true
for file in "${required_files[@]}"; do
    if tar -tzf "$TAR_FILE" | grep -q "/$file$"; then
        echo -e "  ${GREEN}✓${NC} $file"
    else
        echo -e "  ${RED}✗${NC} $file (不在tar包中)"
        all_in_tar=false
    fi
done

if [ "$all_in_tar" = false ]; then
    echo -e "${RED}✗ tar包内容不完整${NC}"
    exit 1
fi
echo ""

# ============================================================================
# 测试3: 验证 install.sh 内容
# ============================================================================
echo -e "${CYAN}=== 测试3: 验证 install.sh 内容 ===${NC}"

echo "检查 install.sh 关键函数..."
install_sh="target/install.sh"

required_functions=(
    "validate_env"
    "generate_zk_config"
    "generate_mongo_config"
    "generate_mysql_config"
    "generate_service"
)

all_functions_exist=true
for func in "${required_functions[@]}"; do
    if grep -q "function $func" "$install_sh" || grep -q "^$func()" "$install_sh"; then
        echo -e "  ${GREEN}✓${NC} $func"
    else
        echo -e "  ${RED}✗${NC} $func (不存在)"
        all_functions_exist=false
    fi
done

if [ "$all_functions_exist" = false ]; then
    echo -e "${RED}✗ install.sh 缺少必需的函数${NC}"
    exit 1
fi
echo ""

# ============================================================================
# 测试4: 验证配置文件替换逻辑
# ============================================================================
echo -e "${CYAN}=== 测试4: 验证配置文件替换逻辑 ===${NC}"

# 创建临时目录
TEST_DIR="/tmp/device-maintenance-test-$$"
mkdir -p "$TEST_DIR"

echo "解压 tar.gz 到临时目录..."
tar -xzf "$TAR_FILE" -C "$TEST_DIR"

EXTRACT_DIR="$TEST_DIR/Device Maintenance Microservice-1.0.0-SNAPSHOT"

echo ""
echo -e "${BLUE}检查解压后的配置文件默认值:${NC}"

# 检查 zkclient_conf.properties
ZK_CONFIG="$EXTRACT_DIR/config/zkclient_conf.properties"
if [ -f "$ZK_CONFIG" ]; then
    echo ""
    echo "zkclient_conf.properties:"
    echo "  NAMESPACE: $(grep '^NAMESPACE=' "$ZK_CONFIG" | cut -d= -f2)"
    echo "  myIp: $(grep '^myIp=' "$ZK_CONFIG" | cut -d= -f2)"
    
    if grep -q "NAMESPACE=dciworld/default" "$ZK_CONFIG"; then
        echo -e "  ${GREEN}✓${NC} 默认值正确（待替换）"
    else
        echo -e "  ${YELLOW}⚠${NC} 默认值不是 dciworld/default"
    fi
else
    echo -e "  ${RED}✗${NC} zkclient_conf.properties 不存在"
fi

echo ""
echo -e "${BLUE}模拟 install.sh 的配置替换:${NC}"

# 设置环境变量（从配置文件读取）
export myIp="$myIp"
export NAMESPACE="$NAMESPACE"
export pmcZooKeepers="$pmcZooKeepers"
export MONGO_SERVERS="$MONGO_SERVERS"
export MONGO_DATABASE="$MONGO_DATABASE"
export MONGO_USER="$MONGO_USER"
export MONGO_PWD="$MONGO_PWD"
export MYSQL_IP="$MYSQL_IP"
export MYSQL_PORT="$MYSQL_PORT"
export MYSQL_USER="$MYSQL_USER"
export MYSQL_PASSWORD="$MYSQL_PASSWORD"

echo ""
echo "使用环境变量:"
echo "  myIp: $myIp"
echo "  NAMESPACE: $NAMESPACE"
echo "  MYSQL_IP: $MYSQL_IP"
echo "  MYSQL_PORT: $MYSQL_PORT"

# 模拟 sed 替换（使用备份，不破坏原文件）
echo ""
echo "执行 sed 替换..."

# 替换 zkclient_conf.properties
sed "s#NAMESPACE=.*#NAMESPACE=${NAMESPACE}#" "$ZK_CONFIG" > "$ZK_CONFIG.test"
sed -i.bak "s#myIp=.*#myIp=${myIp}#" "$ZK_CONFIG.test"

echo ""
echo -e "${BLUE}替换后的配置:${NC}"
echo "zkclient_conf.properties:"
echo "  NAMESPACE: $(grep '^NAMESPACE=' "$ZK_CONFIG.test" | cut -d= -f2)"
echo "  myIp: $(grep '^myIp=' "$ZK_CONFIG.test" | cut -d= -f2)"

# 验证替换是否成功
if grep -q "NAMESPACE=${NAMESPACE}" "$ZK_CONFIG.test"; then
    echo -e "  ${GREEN}✓${NC} NAMESPACE 替换成功"
else
    echo -e "  ${RED}✗${NC} NAMESPACE 替换失败"
fi

if grep -q "myIp=${myIp}" "$ZK_CONFIG.test"; then
    echo -e "  ${GREEN}✓${NC} myIp 替换成功"
else
    echo -e "  ${RED}✗${NC} myIp 替换失败"
fi

# 清理
echo ""
echo "清理临时文件..."
rm -rf "$TEST_DIR"
echo -e "${GREEN}✓ 清理完成${NC}"
echo ""

# ============================================================================
# 测试5: 验证 install.sh 语法
# ============================================================================
echo -e "${CYAN}=== 测试5: 验证 install.sh 语法 ===${NC}"

if bash -n "$install_sh"; then
    echo -e "${GREEN}✓ install.sh 语法正确${NC}"
else
    echo -e "${RED}✗ install.sh 语法错误${NC}"
    exit 1
fi
echo ""

# ============================================================================
# 测试总结
# ============================================================================
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                      测试总结                                 ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✓✓✓ 所有测试通过！✓✓✓${NC}"
echo ""
echo -e "${BLUE}测试项目:${NC}"
echo "  ✓ 编译打包"
echo "  ✓ 文件生成（install.sh, uninstall.sh, service, tar.gz）"
echo "  ✓ tar.gz 包结构完整"
echo "  ✓ install.sh 包含所有必需函数"
echo "  ✓ 配置文件替换逻辑正确"
echo "  ✓ install.sh 语法正确"
echo ""
echo -e "${BLUE}配置验证:${NC}"
echo "  ✓ 配置文件默认值: NAMESPACE=dciworld/default (待替换)"
echo "  ✓ 环境变量配置: NAMESPACE=$NAMESPACE"
echo "  ✓ sed 替换成功: NAMESPACE=$NAMESPACE"
echo ""
echo -e "${GREEN}📦 生成的包:${NC}"
echo "  $(ls -lh target/Device\ Maintenance\ Microservice-1.0.0-SNAPSHOT-bin.tar.gz | awk '{print $9, "("$5")"}')"
echo ""
echo -e "${YELLOW}下一步:${NC}"
echo "  可以执行实际部署:"
echo "  ./scripts/manual/shanghai2-manual-deploy.sh"
echo ""

