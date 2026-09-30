#!/bin/bash

# 服务器管理工具 - Device Maintenance项目服务器端操作
# 主要用于数据库初始化、服务启动、重启、停止等服务器端操作

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
source "$SCRIPT_DIR/lib/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

# 应用相关配置
APP_NAME="device-maintenance"
JAR_FILE="$DEPLOY_DIR/target/device-maintenance-*.jar"
PID_FILE="/tmp/$APP_NAME.pid"
LOG_FILE="$DEPLOY_DIR/logs/$APP_NAME.log"

# 显示横幅
show_banner() {
    echo
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║              🚀 Device Maintenance 服务器管理工具            ║"
    echo "║                   Server Management Tool                     ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo
}

# 显示配置信息
show_config_info() {
    echo "📋 服务器环境配置:"
    echo "  服务器: $(hostname) ($(whoami))"
    echo "  工作目录: $DEPLOY_DIR"
    echo "  应用端口: $APP_PORT"
    echo "  数据库: $DB_HOST:$DB_PORT/$DB_NAME"
    echo "  ZooKeeper: $ZK_SERVERS"
    echo
}

# 显示主菜单
show_main_menu() {
    echo "🚀 服务器管理功能:"
    echo "  1. 数据库初始化"
    echo "  2. 执行数据库补丁"
    echo "  3. 启动应用服务"
    echo "  4. 停止应用服务"
    echo "  5. 重启应用服务"
    echo "  6. 查看服务状态"
    echo "  7. 查看应用日志"
    echo "  8. 清理应用日志"
    echo "  9. 数据库连接检测"
    echo " 10. ZooKeeper连接检测"
    echo " 11. 微服务健康检查"
    echo " 12. Kafka连接检测"
    echo " 13. 显示配置信息"
    echo " 14. 检查数据库结构"
    echo " 15. 退出"
    echo
}

# 数据库初始化
init_database() {
    echo "=== 数据库初始化 ==="
    echo "加载当前配置后运行 readme/schema.sql"
    echo

    local schema_file="$SCRIPT_DIR/sql/schema.sql"
    if [[ ! -f "$schema_file" ]]; then
        echo "❌ 未找到schema文件: $schema_file"
        return 1
    fi

    echo "🔍 连接预检查"
    "$SCRIPT_DIR/lib/check-db.sh" || return 1
    "$SCRIPT_DIR/lib/check-zk.sh" || return 1
    if [[ -n "${KAFKA_BOOTSTRAP_SERVERS:-}" ]]; then
        "$SCRIPT_DIR/lib/check-kafka.sh" || return 1
    fi

    if ! command -v mysql >/dev/null 2>&1; then
        echo "❌ 未检测到mysql客户端，请先安装"
        return 1
    fi

    echo "📋 目标数据库: $DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"
    echo "🚧 正在执行SQL..."
    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$schema_file"; then
        echo "✅ 数据库初始化完成"
    else
        echo "❌ SQL执行失败"
        return 1
    fi
}

# 获取应用进程ID
get_app_pid() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "$pid"
        else
            rm -f "$PID_FILE"
            echo ""
        fi
    else
        echo ""
    fi
}

# 检查应用是否运行
is_app_running() {
    local pid=$(get_app_pid)
    [ -n "$pid" ]
}

# 启动应用服务 (普通模式)
start_service() {
    _start_service_internal ""
}

# 启动应用服务 (Debug模式)
start_service_debug() {
    _start_service_internal "debug"
}

# 内部启动函数
_start_service_internal() {
    local mode="$1"
    local debug_params=""
    
    if [ "$mode" = "debug" ]; then
        echo "=== 启动应用服务 (Debug模式) ==="
        debug_params="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:12345"
        echo "🐛 Remote Debug: 端口 12345"
        echo "📝 IDEA配置: Host=$(hostname -I | awk '{print $1}'), Port=12345, Mode=Attach"
    else
        echo "=== 启动应用服务 (普通模式) ==="
    fi
    
    echo "📋 使用默认配置 (包含数据库和ZooKeeper)"
    
    if is_app_running; then
        echo "⚠️  应用已经在运行中 (PID: $(get_app_pid))"
        return
    fi
    
    # 查找JAR文件
    cd "$DEPLOY_DIR"
    local jar_files=(target/device-maintenance-*.jar)
    if [ ! -f "${jar_files[0]}" ]; then
        echo "❌ 错误: 未找到应用JAR文件"
        echo "请确保已上传应用文件: target/device-maintenance-*.jar"
        return
    fi
    
    local jar_file="${jar_files[0]}"
    echo "📦 使用JAR文件: $jar_file"
    echo "📝 日志文件: $LOG_FILE"
    echo
    
    echo "🚀 启动应用服务..."
    
    # 检查是否有外部配置文件
    local config_params=""
    if [ -f "$DEPLOY_DIR/config/application.yml" ]; then
        # ⭐ 使用外部配置目录，Spring Boot会自动加载 application.yml 和 application-{profile}.yml
        config_params="--spring.config.additional-location=file:$DEPLOY_DIR/config/"
        echo "📋 使用外部配置目录: $DEPLOY_DIR/config/"
        
        # 检查配置文件中的 profile 设置
        if grep -q "active:" "$DEPLOY_DIR/config/application.yml" 2>/dev/null; then
            local active_profile=$(grep "active:" "$DEPLOY_DIR/config/application.yml" | awk '{print $2}' | tr -d '\r')
            echo "📝 激活的 Profile: $active_profile"
        fi
    else
        echo "📋 使用JAR内置配置"
    fi
    
    # ⭐ 不再硬编码 spring.profiles.active，让配置文件决定
    LOG_HOME="$DEPLOY_DIR/logs" nohup java $debug_params -jar "$jar_file" \
        $config_params \
        --server.port="$APP_PORT" \
        --spring.application.name=device-maintenance \
        --spring.kafka.bootstrap-servers="${KAFKA_BOOTSTRAP_SERVERS:-127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096}" \
        --zookeeper.connect-string="${ZK_SERVERS}" \
        --zookeeper.namespace="${ZK_NAMESPACE:-default}" \
        > "$LOG_FILE" 2>&1 &
    
    local pid=$!
    echo "$pid" > "$PID_FILE"
    
    echo "⏳ 等待服务启动..."
    sleep 8
    
    if is_app_running; then
        echo "✅ 应用服务启动成功！"
        echo "🔗 访问地址: http://$(hostname -I | awk '{print $1}'):$APP_PORT/device-maintenance/"
        echo "📊 健康检查: http://$(hostname -I | awk '{print $1}'):$APP_PORT/device-maintenance/actuator/health"
        echo "📝 查看日志: tail -f $LOG_FILE"
        
        if [ "$mode" = "debug" ]; then
            # 检查调试端口
            sleep 2
            if netstat -tuln 2>/dev/null | grep -q ":12345 "; then
                echo "🐛 Debug端口 12345 已开启"
            else
                echo "⚠️  Debug端口 12345 未检测到（可能需要稍等片刻）"
            fi
        fi
    else
        echo "❌ 应用服务启动失败"
        echo "📝 查看错误日志: tail -20 $LOG_FILE"
    fi
}

# 停止应用服务
stop_service() {
    echo "=== 停止应用服务 ==="

    if ! is_app_running; then
        echo "ℹ️  应用服务未运行"
    else
        local pid=$(get_app_pid)
        echo "🛑 停止应用服务 (PID: $pid)..."
        kill "$pid"
        local count=0
        while is_app_running && [ $count -lt 30 ]; do
            sleep 1
            count=$((count + 1))
        done
        if is_app_running; then
            echo "⚠️  正常停止失败，强制终止..."
            kill -9 "$pid"
            sleep 2
        fi
        rm -f "$PID_FILE"
        echo "✅ 应用服务已停止"
    fi

    echo "🔍 检查端口占用: $APP_PORT"
    killers=$(lsof -t -i:"$APP_PORT" 2>/dev/null || true)
    if [[ -n "$killers" ]]; then
        echo "⚠️  端口仍被以下进程占用: $killers"
        read -p "是否强制杀进程? (y/n): " confirm
        if [[ $confirm == "y" || $confirm == "Y" ]]; then
            echo "$killers" | xargs kill -9
            echo "✅ 端口占用进程已杀掉"
        fi
    fi
}

# 重启应用服务
restart_service() {
    echo "=== 重启应用服务 (普通模式) ==="
    stop_service
    echo
    start_service
}

restart_service_debug() {
    echo "=== 重启应用服务 (Debug模式) ==="
    stop_service
    echo
    start_service_debug
}

# 查看服务状态
show_service_status() {
    echo "=== 查看服务状态 ==="
    
    if is_app_running; then
        local pid=$(get_app_pid)
        echo "🟢 应用服务状态: 运行中"
        echo "📋 进程ID: $pid"
        echo "💾 内存使用:"
        ps -p "$pid" -o pid,ppid,%mem,%cpu,cmd --no-headers
        echo
        echo "🌐 端口监听:"
        netstat -tuln | grep ":$APP_PORT " || echo "端口 $APP_PORT 未监听"
    else
        echo "🔴 应用服务状态: 未运行"
    fi
    
    echo
    echo "🔍 相关Java进程:"
    ps aux | grep java | grep -v grep || echo "无Java进程运行"
}

# 查看应用日志
show_app_logs() {
    echo "=== 查看应用日志 ==="
    echo "日志文件: $LOG_FILE"
    echo "==============================================="
    
    if [ -f "$LOG_FILE" ]; then
        echo "📝 最近50行日志:"
        tail -50 "$LOG_FILE"
        echo
        echo "==============================================="
        echo "💡 实时查看日志: tail -f $LOG_FILE"
    else
        echo "❌ 日志文件不存在: $LOG_FILE"
    fi
}

# 清理应用日志
clean_app_logs() {
    echo "=== 清理应用日志 ==="
    
    if [ -f "$LOG_FILE" ]; then
        local log_size=$(du -h "$LOG_FILE" | cut -f1)
        echo "当前日志文件大小: $log_size"
        echo
        
        read -p "确认清理日志文件? (y/n): " confirm
        if [[ $confirm == "y" || $confirm == "Y" ]]; then
            > "$LOG_FILE"
            echo "✅ 日志文件已清理"
        else
            echo "❌ 已取消清理"
        fi
    else
        echo "ℹ️  日志文件不存在，无需清理"
    fi
}

# 执行数据库补丁
apply_db_patches() {
    echo "=== 执行数据库补丁 ==="

    local patch_dir="$SCRIPT_DIR/sql/patches"
    if [[ ! -d "$patch_dir" ]]; then
        echo "ℹ️  未找到补丁目录: $patch_dir"
        return 0
    fi

    mapfile -t patches < <(find "$patch_dir" -name "*.sql" | sort)
    if [[ ${#patches[@]} -eq 0 ]]; then
        echo "ℹ️  没有可执行的SQL补丁"
        return 0
    fi

    echo "发现以下补丁:"
    local idx=1
    for patch in "${patches[@]}"; do
        printf "  %2d. %s\n" "$idx" "${patch##*/}"
        ((idx++))
    done

    read -p "输入要执行的编号(逗号分隔，all执行全部，q退出): " selection
    [[ -z "$selection" || "$selection" == "q" ]] && return 0

    if [[ "$selection" == "all" ]]; then
        to_run=($(seq 1 ${#patches[@]}))
    else
        IFS=',' read -ra to_run <<< "$selection"
    fi

    for i in "${to_run[@]}"; do
        if ! [[ "$i" =~ ^[0-9]+$ ]] || (( i < 1 || i > ${#patches[@]} )); then
            echo "⚠️  编号 $i 无效，跳过"
            continue
        fi
        patch_file="${patches[$((i-1))]}"
        echo "🚧 执行补丁: $(basename "$patch_file")"
        if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$patch_file"; then
            echo "✅ 补丁执行成功"
        else
            echo "❌ 补丁执行失败: $patch_file"
            read -p "是否继续执行后续补丁? (y/n): " cont
            [[ $cont == "y" || $cont == "Y" ]] || break
        fi
    done
}

# 主程序入口
main() {
    while true; do
        show_banner
        show_config_info
        show_main_menu
        
        read -p "请选择操作 (1-15): " choice
        echo
        
        case $choice in
            1)
                init_database
                ;;
            2)
                apply_db_patches
                ;;
            3)
                start_service
                ;;
            4)
                stop_service
                ;;
            5)
                restart_service
                ;;
            6)
                show_service_status
                ;;
            7)
                show_app_logs
                ;;
            8)
                clean_app_logs
                ;;
            9)
                echo "=== 数据库连接检测 ==="
                "$SCRIPT_DIR/lib/check-db.sh"
                ;;
            10)
                echo "=== ZooKeeper连接检测 ==="
                "$SCRIPT_DIR/lib/check-zk.sh"
                ;;
            11)
                echo "=== 微服务健康检查 ==="
                "$SCRIPT_DIR/lib/check-services.sh"
                ;;
            12)
                echo "=== Kafka连接检测 ==="
                "$SCRIPT_DIR/lib/check-kafka.sh"
                ;;
            13)
                echo "=== 显示配置信息 ==="
                show_config_info
                ;;
            14)
                echo "=== 数据库结构检查 ==="
                "$SCRIPT_DIR/lib/check-db-schema.sh"
                ;;
            15)
                echo "退出程序"
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

# 如果有参数，执行对应操作后退出（用于remote-manager.sh调用）
if [[ $# -gt 0 ]]; then
    case $1 in
        start) start_service ;;
        start-debug) start_service_debug ;;
        stop) stop_service ;;
        restart) restart_service ;;
        restart-debug) restart_service_debug ;;
        status) check_service_status ;;
        init-db) init_database ;;
        check-db) check_database ;;
        check-zk) check_zookeeper ;;
        check-kafka) check_kafka ;;
        check-services) check_microservices ;;
        health-check) full_health_check ;;
        check-db-structure) check_database_structure ;;
        logs) view_logs ;;
        clear-logs) clear_logs ;;
        *)
            echo "用法: $0 [start|start-debug|stop|restart|restart-debug|status|init-db|check-db|check-zk|check-kafka|check-services|health-check|check-db-structure|logs|clear-logs]"
            echo "或不带参数启动交互式菜单"
            echo ""
            echo "Debug模式说明:"
            echo "  start-debug / restart-debug  - 启动/重启服务并开启远程调试 (端口 12345)"
            exit 1
            ;;
    esac
else
    # 无参数，启动交互式菜单
    main
fi
