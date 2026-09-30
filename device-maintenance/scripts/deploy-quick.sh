#!/bin/bash
# ============================================================================
# Device Maintenance - 快速部署脚本
# ============================================================================
#
# 功能：
#   1. 编译打包
#   2. 上传到远程服务器
#   3. 解压并使用 service 命令重启服务
#
# 使用方法：
#   ./scripts/deploy-quick.sh
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
# 服务器配置
# ============================================================================
SERVER_HOST="116.128.204.12"
SERVER_USER="actsvt112"
SERVER_PASSWORD='dciUser123!@#'
SSH_PORT="13112"
DEPLOY_DIR="/opt/dci/deploy"
APP_NAME="device-maintenance"
VERSION="1.0.0-SNAPSHOT"

# ============================================================================
# 辅助函数
# ============================================================================

show_banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║         Device Maintenance - 快速部署脚本                    ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

exec_remote() {
    local cmd="$1"
    sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST "$cmd"
}

exec_remote_sudo() {
    local cmd="$1"
    sshpass -p "$SERVER_PASSWORD" ssh -p $SSH_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST "echo '$SERVER_PASSWORD' | sudo -S $cmd"
}

upload_file() {
    local src="$1"
    local dst="$2"
    sshpass -p "$SERVER_PASSWORD" scp -P $SSH_PORT -o StrictHostKeyChecking=no "$src" "$SERVER_USER@$SERVER_HOST:$dst"
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

    if upload_file "$tar_file" "$remote_tar"; then
        echo -e "${GREEN}✓ 上传成功${NC}"
    else
        echo -e "${RED}✗ 上传失败${NC}"
        exit 1
    fi
    echo ""
}

extract_and_deploy() {
    echo -e "${CYAN}=== 步骤3: 解压并部署 ===${NC}"

    local remote_tar="/tmp/${APP_NAME}-${VERSION}-bin.tar.gz"
    local extract_dir="$DEPLOY_DIR/${APP_NAME}-${VERSION}"

    # 停止服务
    echo "停止服务..."
    exec_remote_sudo "service device-maintenance stop" || true

    # 备份旧版本（可选）
    echo "备份旧版本..."
    exec_remote "if [ -d $extract_dir ]; then mv $extract_dir ${extract_dir}.bak.\$(date +%Y%m%d%H%M%S); fi" || true

    # 解压新版本
    echo "解压到: $extract_dir"
    exec_remote "cd $DEPLOY_DIR && tar -xzf $remote_tar"

    # 清理临时文件
    exec_remote "rm -f $remote_tar"

    echo -e "${GREEN}✓ 解压完成${NC}"
    echo ""
}

restart_service() {
    echo -e "${CYAN}=== 步骤4: 重启服务 ===${NC}"

    # 重启服务
    echo "重启服务..."
    exec_remote_sudo "service device-maintenance restart"

    # 等待服务启动
    echo "等待服务启动..."
    sleep 5

    # 检查服务状态
    echo "检查服务状态..."
    exec_remote_sudo "service device-maintenance status" || true

    echo -e "${GREEN}✓ 服务重启完成${NC}"
    echo ""
}

verify_deployment() {
    echo -e "${CYAN}=== 步骤5: 验证部署 ===${NC}"

    # 检查健康接口
    echo "检查健康接口..."
    if exec_remote "curl -s http://localhost:18008/actuator/health"; then
        echo ""
        echo -e "${GREEN}✓ 服务健康检查通过${NC}"
    else
        echo -e "${YELLOW}⚠ 健康检查未通过（服务可能还在启动中）${NC}"
    fi

    echo ""
}

show_summary() {
    echo -e "${CYAN}=== 部署摘要 ===${NC}"
    echo "服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "部署目录: $DEPLOY_DIR/${APP_NAME}-${VERSION}"
    echo ""
    echo "常用命令:"
    echo "  查看状态: sudo service device-maintenance status"
    echo "  查看日志: sudo journalctl -u device-maintenance -f"
    echo "  重启服务: sudo service device-maintenance restart"
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

    # 执行部署
    compile_project
    upload_package
    extract_and_deploy
    restart_service
    verify_deployment
    show_summary
}

# 运行主流程
main "$@"
