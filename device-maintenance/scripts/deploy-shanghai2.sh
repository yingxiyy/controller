#!/bin/bash
# ============================================================================
# Device Maintenance - 上海2环境部署脚本（标准化版本）
# ============================================================================
#
# 功能：
#   1. 编译打包
#   2. 上传到远程服务器
#   3. 解压并使用标准 install.sh 进行安装
#
# 使用方法：
#   ./scripts/deploy-shanghai2.sh
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================================================
# 上海2环境配置
# ============================================================================
SERVER_HOST="116.128.204.12"
SERVER_USER="actsvt112"
SERVER_PASSWORD="pureg123!@#"
SSH_PORT="13112"
DEPLOY_DIR="/opt/dci/deploy"
APP_NAME="device-maintenance"
VERSION="1.0.0-SNAPSHOT"

# ============================================================================
# 环境变量配置（用于 install.sh）
# ============================================================================
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
export NAMESPACE="clband"

# ============================================================================
# 辅助函数
# ============================================================================

show_banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║    Device Maintenance - 上海2环境部署脚本（标准化版本）       ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
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

compile_project() {
    echo -e "${CYAN}=== 步骤1: 编译打包 ===${NC}"
    cd "$PROJECT_ROOT"
    
    if mvn clean package -DskipTests; then
        echo -e "${GREEN}✓ 编译打包成功${NC}"
        
        local tar_file="$PROJECT_ROOT/target/${APP_NAME}-${VERSION}-bin.tar.gz"
        if [ -f "$tar_file" ]; then
            echo -e "${GREEN}✓ 打包文件: $tar_file${NC}"
        else
            echo -e "${RED}✗ 打包文件不存在${NC}"
            exit 1
        fi
    else
        echo -e "${RED}✗ 编译打包失败${NC}"
        exit 1
    fi
    echo ""
}

upload_package() {
    echo -e "${CYAN}=== 步骤2: 上传安装包 ===${NC}"
    
    local tar_file="$PROJECT_ROOT/target/${APP_NAME}-${VERSION}-bin.tar.gz"
    local remote_tar="/tmp/${APP_NAME}-${VERSION}-bin.tar.gz"
    
    echo "上传: $tar_file"
    echo "目标: $SERVER_USER@$SERVER_HOST:$remote_tar"
    
    local scp_cmd=$(get_scp_cmd)
    if eval "$scp_cmd \"$tar_file\" $SERVER_USER@$SERVER_HOST:$remote_tar"; then
        echo -e "${GREEN}✓ 上传成功${NC}"
    else
        echo -e "${RED}✗ 上传失败${NC}"
        exit 1
    fi
    echo ""
}

extract_package() {
    echo -e "${CYAN}=== 步骤3: 解压安装包 ===${NC}"
    
    local remote_tar="/tmp/${APP_NAME}-${VERSION}-bin.tar.gz"
    local extract_dir="$DEPLOY_DIR/${APP_NAME}-${VERSION}"
    
    # 创建部署目录
    echo "创建部署目录: $extract_dir"
    exec_remote_sudo "mkdir -p $DEPLOY_DIR && chown -R $SERVER_USER:$SERVER_USER $DEPLOY_DIR"
    
    # 解压
    echo "解压到: $extract_dir"
    exec_remote "cd $DEPLOY_DIR && tar -xzf $remote_tar"
    
    # 清理临时文件
    exec_remote "rm -f $remote_tar"
    
    echo -e "${GREEN}✓ 解压完成${NC}"
    echo ""
}

create_env_file() {
    echo -e "${CYAN}=== 步骤4: 创建环境配置文件 ===${NC}"
    
    local env_file="$DEPLOY_DIR/${APP_NAME}-${VERSION}/env-shanghai2.sh"
    
    # 创建环境变量配置文件
    local env_content="#!/bin/bash
# 上海2环境配置文件
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
"
    
    # 上传环境配置文件
    echo "$env_content" | exec_remote "cat > $env_file && chmod +x $env_file"
    
    echo -e "${GREEN}✓ 环境配置文件已创建: $env_file${NC}"
    echo ""
}

run_install() {
    echo -e "${CYAN}=== 步骤5: 运行安装脚本 ===${NC}"
    
    local install_dir="$DEPLOY_DIR/${APP_NAME}-${VERSION}"
    local env_file="$install_dir/env-shanghai2.sh"
    
    # 执行安装脚本
    exec_remote "cd $install_dir && source $env_file && ./bin/install.sh"
    
    echo -e "${GREEN}✓ 安装完成${NC}"
    echo ""
}

verify_deployment() {
    echo -e "${CYAN}=== 步骤6: 验证部署 ===${NC}"
    
    # 等待服务启动
    echo "等待服务启动..."
    sleep 5
    
    # 检查服务状态
    echo "检查服务状态..."
    exec_remote_sudo "systemctl status device-maintenance --no-pager || true"
    
    # 检查健康接口
    echo ""
    echo "检查健康接口..."
    if exec_remote "curl -s http://localhost:18008/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:18008/actuator/health"; then
        echo -e "${GREEN}✓ 服务健康检查通过${NC}"
    else
        echo -e "${YELLOW}⚠ 健康检查未通过（服务可能还在启动中）${NC}"
    fi
    
    echo ""
}

show_summary() {
    echo -e "${CYAN}=== 部署摘要 ===${NC}"
    echo "环境: 上海2 (Shanghai2)"
    echo "服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "部署目录: $DEPLOY_DIR/${APP_NAME}-${VERSION}"
    echo ""
    echo "常用命令:"
    echo "  查看状态: sudo systemctl status device-maintenance"
    echo "  查看日志: sudo journalctl -u device-maintenance -f"
    echo "  重启服务: sudo systemctl restart device-maintenance"
    echo ""
    echo "健康检查:"
    echo "  curl http://localhost:18008/actuator/health"
    echo "  curl http://localhost:18008/api/health/zookeeper"
    echo ""
    echo -e "${GREEN}✓✓✓ 部署完成！✓✓✓${NC}"
    echo ""
}

# ============================================================================
# 主流程
# ============================================================================

main() {
    show_banner
    
    # 检查依赖
    if ! command -v sshpass &> /dev/null; then
        echo -e "${RED}✗ sshpass 未安装，请先安装: brew install sshpass (macOS) 或 apt install sshpass (Linux)${NC}"
        exit 1
    fi
    
    if ! command -v expect &> /dev/null; then
        echo -e "${RED}✗ expect 未安装，请先安装: brew install expect (macOS) 或 apt install expect (Linux)${NC}"
        exit 1
    fi
    
    # 执行部署
    compile_project
    upload_package
    extract_package
    create_env_file
    run_install
    verify_deployment
    show_summary
}

# 运行主流程
main "$@"

