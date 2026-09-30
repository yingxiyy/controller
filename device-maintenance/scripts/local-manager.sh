#!/bin/bash

# 本地管理工具 - Device Maintenance项目本地操作
# 主要用于项目编译、打包、上传、部署等本地执行的操作

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
source "$SCRIPT_DIR/lib/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_LOADER="$SCRIPT_DIR/config/load-sit-config.sh"

# 显示横幅
show_banner() {
    echo
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║              📦 Device Maintenance 本地管理工具              ║"
    echo "║                   Local Management Tool                      ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo
}

# 显示配置信息
show_config_info() {
    echo "📋 当前环境配置:"
    echo "  远程服务器: $SERVER_HOST"
    echo "  SSH用户: $SERVER_USER"
    echo "  部署目录: $DEPLOY_DIR"
    echo "  应用端口: $APP_PORT"
    echo
}

# 显示主菜单
show_main_menu() {
    echo "🚀 本地管理功能:"
    echo "  1. 编译打包项目"
    echo "  2. 上传脚本到远程服务器"
    echo "  3. 上传应用JAR和配置到远程服务器"
    echo "  4. 一键完整部署"
    echo "  5. 显示当前配置"
    echo "  6. 重新生成配置 (来自 config/server-*.properties)"
    echo "  7. 退出"
    echo
}

# 编译打包项目
compile_project() {
    echo "=== 编译打包项目 ==="
    echo "项目路径: $PROJECT_ROOT"
    echo
    
    cd "$PROJECT_ROOT"
    
    echo "🔧 执行Maven清理和打包..."
    if mvn clean package -DskipTests; then
        echo "✅ 项目编译打包成功"
        echo "📦 JAR文件位置: target/device-maintenance-*.jar"
    else
        echo "❌ 项目编译打包失败"
        echo "请检查代码错误或Maven配置"
    fi
    
    cd "$SCRIPT_DIR"
}

# 重新生成配置
regenerate_config() {
    echo "=== 重新生成配置 ==="
    echo "当前将基于 config/server-shanghai-env.properties 写入 scripts/config.properties"
    read -p "确认重新生成? (y/n): " confirm
    if [[ $confirm != "y" && $confirm != "Y" ]]; then
        echo "❌ 已取消"
        return
    fi

    if [[ ! -f "$CONFIG_LOADER" ]]; then
        echo "❌ 未找到配置加载脚本: $CONFIG_LOADER"
        return
    fi

    if "$CONFIG_LOADER" tool-config; then
        echo "✅ 配置已重新生成"
    else
        echo "❌ 生成失败，请检查配置文件"
    fi
}

show_environment_hint() {
    echo "请编辑 config/server-*.properties 完成环境参数调整，新配置需执行菜单6刷新生成"
}

# 上传脚本
upload_scripts() {
    echo "=== 上传脚本到远程服务器 ==="
    "$SCRIPT_DIR/lib/upload-scripts.sh"
}

# 上传应用
upload_app() {
    echo "=== 上传应用JAR和配置到远程服务器 ==="
    "$SCRIPT_DIR/lib/upload-app.sh"
}

# 一键完整部署
full_deployment() {
    echo "=== 一键完整部署 ==="
    echo "将依次执行：编译打包 → 上传脚本 → 上传应用"
    echo

    read -p "确认执行完整部署? (y/n): " confirm
    if [[ $confirm != "y" && $confirm != "Y" ]]; then
        echo "❌ 已取消部署"
        return
    fi

    echo "📦 步骤1: 编译打包项目..."
    compile_project
    echo

    echo "📤 步骤2: 上传脚本文件..."
    upload_scripts
    echo

    echo "📤 步骤3: 上传应用文件..."
    upload_app
    echo

    echo "✅ 完整部署流程执行完成！"
    echo
    echo "🎯 接下来请："
    echo "1. SSH到远程服务器: ssh $SERVER_USER@$SERVER_HOST"
    echo "2. 进入部署目录: cd $DEPLOY_DIR"
    echo "3. 执行服务器管理脚本: ./scripts/server-manager.sh"
}

# 主程序入口
main() {
    while true; do
        show_banner
        show_config_info
        show_main_menu
        
        read -p "请选择操作 (1-13): " choice
        echo
        
        case $choice in
            1)
                compile_project
                ;;
            2)
                upload_scripts
                ;;
            3)
                upload_app
                ;;
            4)
                full_deployment
                ;;
            5)
                echo "=== 显示当前配置 ==="
                show_config_info
                ;;
            6)
                regenerate_config
                ;;
            7)
                echo "退出程序"
                echo "远端检查、启停、补丁请在服务器上执行 ./scripts/server-manager.sh"
                exit 0
                ;;
            *)
                echo "❌ 无效选择，请重新输入"
                ;;
        esac
        
        echo
        read -p "按回车键继续..."
        echo
    done
}

# 运行主程序
main "$@"
