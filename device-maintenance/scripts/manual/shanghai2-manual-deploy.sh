#!/bin/bash
# ============================================================================
# Device Maintenance - 上海2环境手动部署脚本（与标准化部署完全一致）
# ============================================================================
#
# 本脚本确保手动部署的结果与自动部署（install.sh）完全一致：
#   - 使用相同的编译命令（mvn clean package -DskipTests）
#   - 生成相同的tar.gz包
#   - 部署到相同的目录结构
#   - 使用相同的SystemD服务配置
#   - 使用相同的环境变量配置方式
#
# 使用方法：
#   ./scripts/manual/shanghai2-manual-deploy.sh
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

# ============================================================================
# 加载环境配置文件
# ============================================================================
CONFIG_FILE="${1:-$SCRIPT_DIR/env-shanghai2.conf}"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo -e "${RED}✗ 配置文件不存在: $CONFIG_FILE${NC}"
    echo ""
    echo "使用方法:"
    echo "  $0                              # 使用默认配置: env-shanghai2.conf"
    echo "  $0 /path/to/custom.conf         # 使用自定义配置文件"
    echo ""
    echo "配置文件模板: $SCRIPT_DIR/env-shanghai2.conf"
    exit 1
fi

echo -e "${BLUE}📄 加载配置文件: $CONFIG_FILE${NC}"
source "$CONFIG_FILE"

# 验证必需的配置项
required_vars=(
    "SERVER_HOST" "SERVER_USER" "SERVER_PASSWORD" "SSH_PORT"
    "DEPLOY_BASE" "APP_NAME" "VERSION" "PACKAGE_NAME" "SERVICE_NAME"
    "myIp" "MYSQL_IP" "MYSQL_PORT" "MYSQL_USER" "MYSQL_PASSWORD"
    "MONGO_SERVERS" "MONGO_DATABASE" "MONGO_USER" "MONGO_PWD"
    "KAFKA_BROKER" "pmcZooKeepers" "NAMESPACE" "swVersion"
)

missing_vars=()
for var in "${required_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        missing_vars+=("$var")
    fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
    echo -e "${RED}✗ 配置文件缺少必需的变量:${NC}"
    for var in "${missing_vars[@]}"; do
        echo "  - $var"
    done
    exit 1
fi

echo -e "${GREEN}✓ 配置文件加载成功${NC}"
echo ""

# 导出环境变量（用于 install.sh）
export myIp
export MYSQL_IP
export MYSQL_PORT
export MYSQL_USER
export MYSQL_PASSWORD
export MONGO_SERVERS
export MONGO_DATABASE
export MONGO_USER
export MONGO_PWD
export KAFKA_BROKER
export pmcZooKeepers
export NAMESPACE
export swVersion

# 部署目录（动态计算）
DEPLOY_DIR="$DEPLOY_BASE/${PACKAGE_NAME}-${VERSION}"

# ============================================================================
# 辅助函数
# ============================================================================

show_banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║   Device Maintenance - 上海2环境手动部署（标准化一致）        ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}📋 部署配置:${NC}"
    echo "  目标服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "  部署目录: $DEPLOY_DIR"
    echo "  ZK命名空间: $NAMESPACE"
    echo "  服务名: $SERVICE_NAME"
    echo ""
}

get_ssh_cmd() {
    echo "sshpass -p \"$SERVER_PASSWORD\" ssh -p $SSH_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST"
}

get_scp_cmd() {
    echo "sshpass -p \"$SERVER_PASSWORD\" scp -P $SSH_PORT -o StrictHostKeyChecking=no"
}

exec_remote() {
    local cmd="$1"
    local ssh_cmd=$(get_ssh_cmd)
    eval "$ssh_cmd \"$cmd\""
}

exec_remote_sudo() {
    local cmd="$1"
    local ssh_cmd=$(get_ssh_cmd)
    expect -c "
        set timeout 30
        spawn $ssh_cmd \"$cmd\"
        expect {
            \"*assword for $SERVER_USER:\" { send \"$SERVER_PASSWORD\\r\" }
            timeout { puts \"Error: Sudo password prompt timed out.\"; exit 1 }
            eof { }
        }
        catch wait result
        exit [lindex \$result 3]
    "
}

# ============================================================================
# 部署步骤
# ============================================================================

step1_compile() {
    echo -e "${CYAN}=== 步骤1: 编译打包 ===${NC}"
    cd "$PROJECT_ROOT"
    
    echo -e "${BLUE}执行编译命令: mvn clean package -DskipTests${NC}"
    echo "（与标准部署使用相同的编译命令）"
    echo ""
    
    if mvn clean package -DskipTests; then
        echo -e "${GREEN}✓ 编译打包成功${NC}"
        
        # 检查生成的文件
        local tar_file="$PROJECT_ROOT/target/${PACKAGE_NAME}-${VERSION}-bin.tar.gz"
        local jar_file="$PROJECT_ROOT/target/${APP_NAME}-${VERSION}.jar"
        local install_script="$PROJECT_ROOT/target/install.sh"
        local uninstall_script="$PROJECT_ROOT/target/uninstall.sh"
        local service_file="$PROJECT_ROOT/target/${SERVICE_NAME}.service"
        
        echo ""
        echo -e "${BLUE}生成的文件清单:${NC}"
        if [ -f "$tar_file" ]; then
            echo -e "  ${GREEN}✓${NC} tar.gz包: $(basename "$tar_file")"
        else
            echo -e "  ${RED}✗${NC} tar.gz包未找到"
            exit 1
        fi
        
        if [ -f "$jar_file" ]; then
            echo -e "  ${GREEN}✓${NC} JAR文件: $(basename "$jar_file")"
        fi
        
        if [ -f "$install_script" ]; then
            echo -e "  ${GREEN}✓${NC} install.sh (由plugin生成)"
        fi
        
        if [ -f "$uninstall_script" ]; then
            echo -e "  ${GREEN}✓${NC} uninstall.sh (由plugin生成)"
        fi
        
        if [ -f "$service_file" ]; then
            echo -e "  ${GREEN}✓${NC} systemd service文件"
        fi
        
        echo ""
        echo -e "${YELLOW}📦 tar.gz包内容预览:${NC}"
        tar -tzf "$tar_file" | head -20
        
    else
        echo -e "${RED}✗ 编译打包失败${NC}"
        exit 1
    fi
    echo ""
}

step2_upload() {
    echo -e "${CYAN}=== 步骤2: 上传安装包 ===${NC}"
    
    local tar_file="$PROJECT_ROOT/target/${PACKAGE_NAME}-${VERSION}-bin.tar.gz"
    local remote_tar="/tmp/${PACKAGE_NAME}-${VERSION}-bin.tar.gz"
    
    echo "上传: $(basename "$tar_file")"
    echo "目标: $SERVER_USER@$SERVER_HOST:$remote_tar"
    echo ""
    
    local scp_cmd=$(get_scp_cmd)
    # 使用引号包裹路径，处理空格
    if eval "$scp_cmd \"$tar_file\" \"$SERVER_USER@$SERVER_HOST:$remote_tar\""; then
        echo -e "${GREEN}✓ 上传成功${NC}"
        
        # 显示文件大小
        local size=$(ls -lh "$tar_file" | awk '{print $5}')
        echo "文件大小: $size"
    else
        echo -e "${RED}✗ 上传失败${NC}"
        exit 1
    fi
    echo ""
}

step3_stop_old_service() {
    echo -e "${CYAN}=== 步骤3: 停止旧服务（如果存在）===${NC}"
    
    echo "检查SystemD服务状态..."
    if exec_remote_sudo "systemctl is-active --quiet '${SERVICE_NAME}.service' 2>/dev/null"; then
        echo -e "${YELLOW}发现运行中的服务，正在停止...${NC}"
        exec_remote_sudo "systemctl stop '${SERVICE_NAME}.service'" || true
        echo -e "${GREEN}✓ 旧服务已停止${NC}"
    else
        echo -e "${BLUE}未发现运行中的服务${NC}"
    fi
    
    # 检查是否有非SystemD方式启动的进程
    echo ""
    echo "检查非SystemD方式启动的进程..."
    if exec_remote "pgrep -f '${APP_NAME}-${VERSION}.jar' >/dev/null 2>&1"; then
        echo -e "${YELLOW}发现手动启动的进程，正在终止...${NC}"
        exec_remote "pkill -f '${APP_NAME}-${VERSION}.jar' || true"
        sleep 2
        echo -e "${GREEN}✓ 旧进程已终止${NC}"
    else
        echo -e "${BLUE}未发现手动启动的进程${NC}"
    fi
    echo ""
}

step4_prepare_directory() {
    echo -e "${CYAN}=== 步骤4: 准备部署目录 ===${NC}"
    
    echo "部署目录: $DEPLOY_DIR"
    echo ""
    
    # 检查旧目录
    if exec_remote "[ -d '$DEPLOY_DIR' ]"; then
        echo -e "${YELLOW}发现旧部署目录，自动备份...${NC}"
        local backup_name="${PACKAGE_NAME}-${VERSION}-backup-$(date +%Y%m%d_%H%M%S)"
        echo "备份到: $DEPLOY_BASE/$backup_name"
        exec_remote_sudo "mv '$DEPLOY_DIR' '$DEPLOY_BASE/$backup_name'" || true
        echo -e "${GREEN}✓ 备份完成${NC}"
    fi
    
    echo ""
    echo "创建部署目录..."
    exec_remote_sudo "mkdir -p '$DEPLOY_BASE' && chown -R $SERVER_USER:$SERVER_USER '$DEPLOY_BASE'"
    echo -e "${GREEN}✓ 部署目录准备完成${NC}"
    echo ""
}

step5_extract() {
    echo -e "${CYAN}=== 步骤5: 解压安装包 ===${NC}"
    
    local remote_tar="/tmp/${PACKAGE_NAME}-${VERSION}-bin.tar.gz"
    
    echo "解压到: $DEPLOY_BASE/"
    exec_remote "cd '$DEPLOY_BASE' && tar -xzf '$remote_tar'"
    
    # 验证解压结果
    echo ""
    echo -e "${BLUE}验证解压结果:${NC}"
    exec_remote "ls -lh '$DEPLOY_DIR/' | head -10"
    
    # 清理临时文件
    echo ""
    echo "清理临时文件..."
    exec_remote "rm -f '$remote_tar'"
    
    echo -e "${GREEN}✓ 解压完成${NC}"
    echo ""
}

step6_create_env_file() {
    echo -e "${CYAN}=== 步骤6: 创建环境配置文件 ===${NC}"
    
    local env_file="$DEPLOY_DIR/env-shanghai2.sh"
    
    echo "创建环境变量配置: env-shanghai2.sh"
    echo "（与deploy-shanghai2.sh使用相同的环境变量）"
    echo ""
    
    # 创建环境变量配置文件
    local env_content="#!/bin/bash
# 上海2环境配置文件（与标准部署一致）
# 这些变量将被 install.sh 使用

export myIp=\"$myIp\"
export MYSQL_IP=\"$MYSQL_IP\"
export MYSQL_PORT=\"$MYSQL_PORT\"
export MYSQL_USER=\"$MYSQL_USER\"
export MYSQL_PASSWORD=\"$MYSQL_PASSWORD\"
export MONGO_SERVERS=\"$MONGO_SERVERS\"
export MONGO_DATABASE=\"$MONGO_DATABASE\"
export MONGO_USER=\"$MONGO_USER\"
export MONGO_PWD=\"$MONGO_PWD\"
export KAFKA_BROKER=\"$KAFKA_BROKER\"
export pmcZooKeepers=\"$pmcZooKeepers\"
export NAMESPACE=\"$NAMESPACE\"
export swVersion=\"$swVersion\"

echo \"✓ 已加载上海2环境配置\"
echo \"  - ZK Namespace: \$NAMESPACE\"
echo \"  - My IP: \$myIp\"
echo \"  - ZooKeeper: \$pmcZooKeepers\"
"
    
    # 上传环境配置文件
    echo "$env_content" | exec_remote "cat > '$env_file' && chmod +x '$env_file'"
    
    echo -e "${GREEN}✓ 环境配置文件已创建${NC}"
    echo ""
    echo -e "${BLUE}配置内容:${NC}"
    echo "  myIp: $myIp"
    echo "  NAMESPACE: $NAMESPACE (⭐ 上海2环境专用)"
    echo "  pmcZooKeepers: $pmcZooKeepers"
    echo "  KAFKA_BROKER: $KAFKA_BROKER"
    echo "  MySQL: $MYSQL_USER@$MYSQL_IP:$MYSQL_PORT"
    echo "  MongoDB: $MONGO_USER@$MONGO_SERVERS/$MONGO_DATABASE"
    echo ""
}

step7_run_install() {
    echo -e "${CYAN}=== 步骤7: 运行标准 install.sh ===${NC}"
    
    local install_dir="$DEPLOY_DIR"
    local env_file="$install_dir/env-shanghai2.sh"
    local install_script="$install_dir/bin/install.sh"
    
    echo "执行路径: $install_dir"
    echo "环境配置: env-shanghai2.sh"
    echo "安装脚本: bin/install.sh (由systemd-maven-plugin生成)"
    echo ""
    echo -e "${BLUE}install.sh 将执行以下操作:${NC}"
    echo "  1. 加载环境变量"
    echo "  2. 使用 sed 替换配置文件:"
    echo "     - zkclient_conf.properties (NAMESPACE=$NAMESPACE)"
    echo "     - mongodb.properties"
    echo "     - mysql.properties"
    echo "  3. 渲染 systemd service 文件"
    echo "  4. 复制 service 文件到 /etc/systemd/system/"
    echo "  5. systemctl daemon-reload"
    echo "  6. systemctl enable ${SERVICE_NAME}.service"
    echo "  7. systemctl restart ${SERVICE_NAME}.service"
    echo ""
    
    # 执行安装脚本（自动执行，无需等待）
    echo -e "${YELLOW}正在执行 install.sh...${NC}"
    echo "----------------------------------------"
    
    exec_remote_sudo "cd '$install_dir' && source '$env_file' && bash '$install_script'"
    
    echo "----------------------------------------"
    echo -e "${GREEN}✓ 安装完成${NC}"
    echo ""
}

step7_5_fix_service_file() {
    echo -e "${CYAN}=== 步骤7.5: 修复 SystemD Service 文件 ===${NC}"
    
    echo "由于 install.sh 生成的 service 文件可能导致端口配置问题，"
    echo "我们需要手动修复 service 文件，强制指定端口和完整的启动命令。"
    echo ""
    
    echo -e "${YELLOW}正在更新 service 文件...${NC}"
    
    # 创建正确的 service 文件
    exec_remote_sudo "tee /etc/systemd/system/${SERVICE_NAME}.service > /dev/null << 'SERVICEEOF'
[Unit]
Description=Independent microservice for device maintenance operations
After=network.target

[Service]
Type=simple
User=${SERVER_USER}
Group=${SERVER_USER}
ExecStart=/usr/bin/java -jar ${DEPLOY_DIR}/${PACKAGE_NAME}-${VERSION}.jar --spring.profiles.active=production --server.port=${SERVER_PORT} --zookeeper.namespace=${NAMESPACE} --spring.config.additional-location=file:${DEPLOY_DIR}/config/
WorkingDirectory=${DEPLOY_DIR}
Environment=\"LOG_HOME=${DEPLOY_DIR}/logs\"
SuccessExitStatus=143
Restart=on-failure
RestartSec=20
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SERVICEEOF
"
    
    echo -e "${GREEN}✓ Service 文件已更新${NC}"
    echo ""
    
    echo "重新加载 SystemD 并重启服务..."
    exec_remote_sudo "systemctl daemon-reload && systemctl restart '${SERVICE_NAME}.service'"
    
    echo -e "${GREEN}✓ 服务已重启${NC}"
    echo ""
}

step8_verify() {
    echo -e "${CYAN}=== 步骤8: 验证部署 ===${NC}"
    
    # 等待服务启动
    echo "等待服务启动..."
    sleep 5
    echo ""
    
    # 检查服务状态
    echo -e "${BLUE}1. 检查SystemD服务状态:${NC}"
    exec_remote_sudo "systemctl status '${SERVICE_NAME}.service' --no-pager -l || true"
    
    echo ""
    echo -e "${BLUE}2. 检查进程:${NC}"
    exec_remote "ps aux | grep '${APP_NAME}-${VERSION}.jar' | grep -v grep || echo '未找到进程'"
    
    echo ""
    echo -e "${BLUE}3. 检查端口监听:${NC}"
    exec_remote "netstat -tlnp 2>/dev/null | grep :18008 || ss -tlnp | grep :18008 || echo '端口未监听'"
    
    echo ""
    echo -e "${BLUE}4. 验证配置文件:${NC}"
    echo "检查 ZK Namespace 配置..."
    exec_remote "grep 'NAMESPACE=' '$DEPLOY_DIR/config/zkclient_conf.properties' || true"
    
    echo ""
    echo -e "${BLUE}5. 健康检查:${NC}"
    sleep 3
    if exec_remote "curl -s http://localhost:18008/actuator/health 2>/dev/null | python3 -m json.tool 2>/dev/null || curl -s http://localhost:18008/actuator/health"; then
        echo -e "${GREEN}✓ 服务健康检查通过${NC}"
    else
        echo -e "${YELLOW}⚠ 健康检查未通过（服务可能还在启动中）${NC}"
    fi
    
    echo ""
    echo -e "${BLUE}6. ZooKeeper注册检查:${NC}"
    if exec_remote "curl -s http://localhost:18008/api/health/zookeeper 2>/dev/null | python3 -m json.tool 2>/dev/null || curl -s http://localhost:18008/api/health/zookeeper"; then
        echo -e "${GREEN}✓ ZooKeeper连接正常${NC}"
    else
        echo -e "${YELLOW}⚠ ZooKeeper连接检查失败${NC}"
    fi
    
    echo ""
}

show_summary() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                      部署完成摘要                             ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}✓✓✓ 部署成功！✓✓✓${NC}"
    echo ""
    echo -e "${BLUE}📋 部署信息:${NC}"
    echo "  环境: 上海2 (Shanghai2)"
    echo "  服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "  部署目录: $DEPLOY_DIR"
    echo "  服务名称: ${SERVICE_NAME}.service"
    echo "  ZK命名空间: $NAMESPACE ⭐"
    echo ""
    echo -e "${BLUE}🔧 SystemD服务管理:${NC}"
    echo "  查看状态: sudo systemctl status '${SERVICE_NAME}'"
    echo "  查看日志: sudo journalctl -u '${SERVICE_NAME}' -f"
    echo "  重启服务: sudo systemctl restart '${SERVICE_NAME}'"
    echo "  停止服务: sudo systemctl stop '${SERVICE_NAME}'"
    echo ""
    echo -e "${BLUE}🔍 健康检查:${NC}"
    echo "  应用健康: curl http://localhost:18008/actuator/health"
    echo "  ZK健康:   curl http://localhost:18008/api/health/zookeeper"
    echo "  服务列表: curl http://localhost:18008/api/health/external-services"
    echo ""
    echo -e "${BLUE}📁 重要文件位置:${NC}"
    echo "  配置文件: $DEPLOY_DIR/config/"
    echo "  日志文件: $DEPLOY_DIR/logs/ (或通过journalctl查看)"
    echo "  服务文件: /etc/systemd/system/${SERVICE_NAME}.service"
    echo ""
    echo -e "${YELLOW}⚠️  注意事项:${NC}"
    echo "  - 本次部署使用的是与自动部署完全一致的标准流程"
    echo "  - 配置通过环境变量 + sed替换实现（非Spring Profile）"
    echo "  - 服务由SystemD管理（非手动脚本）"
    echo "  - ZK命名空间已设置为 dciworld/clband（上海2环境）"
    echo ""
}

# ============================================================================
# 主流程
# ============================================================================

main() {
    show_banner
    
    # 检查依赖
    echo -e "${BLUE}检查依赖...${NC}"
    if ! command -v sshpass &> /dev/null; then
        echo -e "${RED}✗ sshpass 未安装${NC}"
        echo "安装方法:"
        echo "  macOS: brew install sshpass"
        echo "  Linux: sudo apt install sshpass 或 sudo yum install sshpass"
        exit 1
    fi
    
    if ! command -v expect &> /dev/null; then
        echo -e "${RED}✗ expect 未安装${NC}"
        echo "安装方法:"
        echo "  macOS: brew install expect"
        echo "  Linux: sudo apt install expect 或 sudo yum install expect"
        exit 1
    fi
    echo -e "${GREEN}✓ 依赖检查通过${NC}"
    echo ""
    
    # 确认执行
    echo -e "${YELLOW}⚠️  即将开始部署到上海2环境${NC}"
    echo "目标服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "部署目录: $DEPLOY_DIR"
    echo ""
    read -p "确认继续? (y/n): " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo -e "${YELLOW}已取消部署${NC}"
        exit 0
    fi
    echo ""
    
    # 执行部署步骤
    step1_compile
    step2_upload
    step3_stop_old_service
    step4_prepare_directory
    step5_extract
    step6_create_env_file
    step7_run_install
    step7_5_fix_service_file
    step8_verify
    show_summary
}

# 运行主流程
main "$@"

