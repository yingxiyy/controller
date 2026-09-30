#!/bin/bash

# Device Maintenance 多环境管理工具
# Multi-Environment Management Tool for Device Maintenance

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 显示横幅
show_banner() {
    echo
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║           🌐 Device Maintenance 多环境管理工具               ║${NC}"
    echo -e "${CYAN}║         Multi-Environment Management Tool                   ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo
}

# 显示环境选择菜单
show_environment_menu() {
    echo -e "${GREEN}🚀 请选择目标环境:${NC}"
    echo
    echo -e "${YELLOW}📍 可用环境列表:${NC}"
    echo "  1. 上海环境 (Shanghai)    - 116.128.204.13 (actsvt/pureg123!@#$)"
    echo "  2. 上海2环境 (Shanghai2)  - 116.128.204.12:13112 (actsvt112/pureg123!@#)"
    echo "  3. 退出"
    echo
}

# 主函数
main() {
    show_banner
    
    while true; do
        show_environment_menu
        read -p "请选择环境 [1-3]: " choice
        echo
        
        case $choice in
            1)
                echo -e "${BLUE}🌐 启动上海环境管理工具...${NC}"
                exec "$SCRIPT_DIR/shanghai-manager.sh"
                ;;
            2)
                echo -e "${BLUE}🌐 启动上海2环境管理工具...${NC}"
                exec "$SCRIPT_DIR/shanghai2-manager.sh"
                ;;
            3)
                echo -e "${GREEN}👋 再见！${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}❌ 无效的选择，请输入 1-3${NC}"
                echo
                ;;
        esac
    done
}

# 检查脚本是否存在
if [[ ! -f "$SCRIPT_DIR/shanghai-manager.sh" ]]; then
    echo -e "${RED}❌ 错误: shanghai-manager.sh 不存在${NC}"
    exit 1
fi

if [[ ! -f "$SCRIPT_DIR/shanghai2-manager.sh" ]]; then
    echo -e "${RED}❌ 错误: shanghai2-manager.sh 不存在${NC}"
    exit 1
fi

# 设置脚本执行权限
chmod +x "$SCRIPT_DIR/shanghai-manager.sh"
chmod +x "$SCRIPT_DIR/shanghai2-manager.sh"

# 运行主函数
main
