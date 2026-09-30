#!/bin/bash

# 北京环境远程管理工具 - Device Maintenance Beijing Environment
# 专门用于北京环境的部署和管理

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"

# ⭐ 强制使用北京环境配置
export CONFIG_FILE="$SCRIPT_DIR/config/server-beijing-env.properties"

# 加载配置
source "$SCRIPT_DIR/lib/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 显示横幅
show_banner() {
    echo
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║      🌐 Device Maintenance 远程管理工具 - 北京环境           ║${NC}"
    echo -e "${CYAN}║         Remote Management Tool - Beijing Environment        ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo
    echo -e "${YELLOW}📍 环境: 北京 (Beijing)${NC}"
    echo -e "${YELLOW}🌐 Web界面: http://117.134.16.112:8088/${NC}"
    echo
}

# 显示配置信息
show_config_info() {
    echo -e "${BLUE}📋 北京环境配置:${NC}"
    echo "  远程服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "  部署目录: $DEPLOY_DIR"
    echo "  应用端口: $APP_PORT"
    echo "  数据库: $DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"
    echo "  ZooKeeper: $ZK_SERVERS"
    echo "  Kafka: $KAFKA_BOOTSTRAP_SERVERS"
    echo
}

# 显示主菜单
show_main_menu() {
    echo -e "${GREEN}🚀 远程管理功能:${NC}"
    echo
    echo -e "${YELLOW}📦 部署管理${NC}"
    echo "  1. 一键完整部署 (编译+上传+启动)"
    echo "  2. 编译打包项目"
    echo "  3. 上传应用到服务器"
    echo "  4. 上传脚本到服务器"
    echo
    echo -e "${YELLOW}🔧 服务管理${NC}"
    echo "  5. 启动应用服务 (普通模式)"
    echo "  6. 启动应用服务 (Debug模式 - 端口12345)"
    echo "  7. 停止应用服务"
    echo "  8. 重启应用服务 (普通模式)"
    echo "  9. 重启应用服务 (Debug模式)"
    echo " 10. 查看服务状态"
    echo
    echo -e "${YELLOW}💾 数据库管理${NC}"
    echo " 11. 数据库初始化"
    echo " 12. 应用数据库补丁"
    echo " 13. 应用所有待应用补丁"
    echo " 14. 查看已应用补丁历史"
    echo " 15. 查看数据库结构"
    echo
    echo -e "${YELLOW}📋 日志管理${NC}"
    echo " 16. 查看实时日志"
    echo " 17. 下载应用日志"
    echo " 18. 清理应用日志"
    echo " 27. 查看微服务日志 (neMgr/taskInfo等)"
    echo
    echo -e "${YELLOW}🔍 健康检查${NC}"
    echo " 19. 全面健康检查"
    echo " 20. 数据库连接检测"
    echo " 21. ZooKeeper连接检测"
    echo " 22. 微服务健康检查"
    echo
    echo -e "${YELLOW}⚙️  其他${NC}"
    echo " 23. 显示配置信息"
    echo " 24. 执行自定义远程命令"
    echo " 25. 打开远程Shell"
    echo " 26. 退出"
    echo
}

# 获取SSH命令
get_ssh() {
    eval "$(get_ssh_cmd)"
}

# 获取SCP命令
get_scp() {
    eval "$(get_scp_cmd)"
}

# 执行远程命令
exec_remote() {
    local cmd="$1"
    local ssh_cmd=$(get_ssh_cmd)
    eval "$ssh_cmd \"$cmd\""
}

# 执行远程脚本
exec_remote_script() {
    local script_name="$1"
    shift
    local args="$@"
    
    echo -e "${BLUE}▶ 执行远程脚本: $script_name $args${NC}"
    exec_remote "cd $DEPLOY_DIR/scripts && bash $script_name $args"
}

# 1. 一键完整部署
full_deploy() {
    echo -e "${CYAN}=== 一键完整部署 (北京环境) ===${NC}"
    echo
    
    # 编译
    compile_project || return 1
    echo
    
    # 上传脚本
    upload_scripts || return 1
    echo
    
    # 上传应用
    upload_application || return 1
    echo
    
    # 重启服务
    restart_service || return 1
    echo
    
    echo -e "${GREEN}✅ 北京环境一键部署完成！${NC}"
    echo -e "${YELLOW}🌐 Web界面: http://117.134.16.112:8088/${NC}"
}

# 2. 编译打包项目
compile_project() {
    echo -e "${CYAN}=== 编译打包项目 (北京环境) ===${NC}"
    echo "项目路径: $PROJECT_ROOT"
    echo -e "${YELLOW}📝 使用 Spring Profile: beijing${NC}"
    echo
    
    cd "$PROJECT_ROOT"
    
    # ⭐ 使用北京环境的 Spring Profile 编译
    echo -e "${BLUE}🔧 执行Maven清理和打包 (Profile: beijing)...${NC}"
    if mvn clean package -DskipTests -Dspring.profiles.active=beijing; then
        echo -e "${GREEN}✅ 项目编译打包成功 (北京环境配置)${NC}"
        echo "📦 JAR文件位置: target/device-maintenance-*.jar"
        echo "📝 配置文件: application-beijing.yml"
        return 0
    else
        echo -e "${RED}❌ 项目编译打包失败${NC}"
        echo "请检查代码错误或Maven配置"
        return 1
    fi
}

# 3. 上传应用
upload_application() {
    echo -e "${CYAN}=== 上传应用到北京服务器 ===${NC}"
    
    local jar_file=$(ls -t "$PROJECT_ROOT"/target/device-maintenance-*.jar 2>/dev/null | head -1)
    
    if [[ ! -f "$jar_file" ]]; then
        echo -e "${RED}❌ 未找到JAR文件，请先执行编译打包${NC}"
        return 1
    fi
    
    echo "📦 JAR文件: $jar_file"
    echo "📤 上传到: $SERVER_USER@$SERVER_HOST:$DEPLOY_DIR/target/"
    
    # 确保远程目录存在
    exec_remote "mkdir -p $DEPLOY_DIR/target"
    
    # 上传JAR
    local scp_cmd=$(get_scp_cmd)
    if eval "$scp_cmd \"$jar_file\" $SERVER_USER@$SERVER_HOST:$DEPLOY_DIR/target/"; then
        echo -e "${GREEN}✅ JAR文件上传成功${NC}"
    else
        echo -e "${RED}❌ JAR文件上传失败${NC}"
        return 1
    fi
    
    # ⭐ 创建服务器启动配置，指定使用北京环境 Profile
    echo -e "${BLUE}📝 配置服务器使用北京环境 Profile...${NC}"
    exec_remote "mkdir -p $DEPLOY_DIR/config"
    
    # 创建 application.yml，指定激活 beijing profile
    exec_remote "cat > $DEPLOY_DIR/config/application.yml << 'EOF'
spring:
  profiles:
    active: beijing
EOF"
    
    # 上传北京环境配置文件
    local beijing_config="$PROJECT_ROOT/src/main/resources/application-beijing.yml"
    if [[ -f "$beijing_config" ]]; then
        if eval "$scp_cmd \"$beijing_config\" $SERVER_USER@$SERVER_HOST:$DEPLOY_DIR/config/"; then
            echo -e "${GREEN}✅ 北京环境配置文件上传成功${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  未找到 application-beijing.yml${NC}"
    fi
    
    echo -e "${GREEN}✅ 服务器配置完成 (Profile: beijing)${NC}"
}

# 4. 上传脚本
upload_scripts() {
    echo -e "${CYAN}=== 上传脚本到北京服务器 ===${NC}"
    
    # 确保远程目录存在
    exec_remote "mkdir -p $DEPLOY_DIR/scripts"
    
    local scp_cmd=$(get_scp_cmd)
    
    # 上传整个scripts目录
    if eval "$scp_cmd -r \"$SCRIPT_DIR\"/* $SERVER_USER@$SERVER_HOST:$DEPLOY_DIR/scripts/"; then
        echo -e "${GREEN}✅ 脚本上传成功${NC}"
        
        # 设置执行权限
        exec_remote "find $DEPLOY_DIR/scripts -name '*.sh' -type f -exec chmod +x {} +"
        echo -e "${GREEN}✅ 脚本权限设置完成${NC}"
        return 0
    else
        echo -e "${RED}❌ 脚本上传失败${NC}"
        return 1
    fi
}

# 5. 启动服务 (普通模式)
start_service() {
    echo -e "${CYAN}=== 启动应用服务 (普通模式) ===${NC}"
    exec_remote_script "server-manager.sh" "start"
}

# 6. 启动服务 (Debug模式)
start_service_debug() {
    echo -e "${CYAN}=== 启动应用服务 (Debug模式) ===${NC}"
    echo -e "${YELLOW}🐛 Debug端口: 12345${NC}"
    echo -e "${YELLOW}📝 配置IDEA: Host=117.134.16.112, Port=12345, Mode=Attach${NC}"
    echo
    exec_remote_script "server-manager.sh" "start-debug"
}

# 7. 停止服务
stop_service() {
    echo -e "${CYAN}=== 停止应用服务 ===${NC}"
    exec_remote_script "server-manager.sh" "stop"
}

# 8. 重启服务 (普通模式)
restart_service() {
    echo -e "${CYAN}=== 重启应用服务 (普通模式) ===${NC}"
    exec_remote_script "server-manager.sh" "restart"
}

# 9. 重启服务 (Debug模式)
restart_service_debug() {
    echo -e "${CYAN}=== 重启应用服务 (Debug模式) ===${NC}"
    echo -e "${YELLOW}🐛 Debug端口: 12345${NC}"
    echo -e "${YELLOW}📝 配置IDEA: Host=117.134.16.112, Port=12345, Mode=Attach${NC}"
    echo
    exec_remote_script "server-manager.sh" "restart-debug"
}

# 10. 查看服务状态
check_service_status() {
    echo -e "${CYAN}=== 查看服务状态 ===${NC}"
    exec_remote_script "server-manager.sh" "status"
}

# 11. 数据库初始化
init_database() {
    echo -e "${CYAN}=== 数据库初始化 (北京环境) ===${NC}"
    echo -e "${YELLOW}⚠️  警告: 此操作会重新创建数据库表结构${NC}"
    echo -e "${YELLOW}⚠️  数据库: $DB_HOST:$DB_PORT/$DB_NAME${NC}"
    read -p "确认执行数据库初始化? (y/n): " confirm
    if [[ $confirm != "y" && $confirm != "Y" ]]; then
        echo -e "${YELLOW}❌ 已取消${NC}"
        return
    fi
    
    exec_remote_script "server-manager.sh" "init-db"
}

# 12. 应用数据库补丁
apply_patch() {
    echo -e "${CYAN}=== 应用数据库补丁 ===${NC}"
    echo
    echo "可用的补丁文件:"
    
    # 列出远程补丁文件
    local patches=$(exec_remote "ls -1 $DEPLOY_DIR/scripts/sql/patch_*.sql 2>/dev/null || echo ''")
    
    if [[ -z "$patches" ]]; then
        echo -e "${YELLOW}未找到任何补丁文件${NC}"
        return
    fi
    
    echo "$patches" | nl
    echo
    read -p "请输入补丁编号 (或输入补丁文件名): " patch_input
    
    local patch_file=""
    if [[ "$patch_input" =~ ^[0-9]+$ ]]; then
        # 数字输入，选择对应行
        patch_file=$(echo "$patches" | sed -n "${patch_input}p")
    else
        # 文件名输入
        patch_file="$patch_input"
    fi
    
    if [[ -z "$patch_file" ]]; then
        echo -e "${RED}❌ 无效的选择${NC}"
        return
    fi
    
    # 只取文件名
    patch_file=$(basename "$patch_file")
    
    echo
    echo -e "${BLUE}将应用补丁: $patch_file${NC}"
    read -p "确认执行? (y/n): " confirm
    if [[ $confirm != "y" && $confirm != "Y" ]]; then
        echo -e "${YELLOW}❌ 已取消${NC}"
        return
    fi
    
    # 确保 applied 目录存在
    exec_remote "mkdir -p $DEPLOY_DIR/scripts/sql/applied"
    
    # 执行补丁
    local cmd="cd $DEPLOY_DIR/scripts/sql && mysql -h$DB_HOST -P$DB_PORT -u$DB_USERNAME -p'$DB_PASSWORD' $DB_NAME < $patch_file"
    
    if exec_remote "$cmd"; then
        echo -e "${GREEN}✅ 补丁应用成功${NC}"
        
        # 移动补丁到 applied 目录
        local timestamp=$(date +%Y%m%d_%H%M%S)
        local applied_name="${patch_file%.sql}_applied_${timestamp}.sql"
        
        if exec_remote "cd $DEPLOY_DIR/scripts/sql && mv $patch_file applied/$applied_name"; then
            echo -e "${GREEN}📦 补丁已移动到 applied/$applied_name${NC}"
        else
            echo -e "${YELLOW}⚠️  补丁应用成功，但移动文件失败（可能需要手动清理）${NC}"
        fi
    else
        echo -e "${RED}❌ 补丁应用失败${NC}"
    fi
}

# 13. 应用所有待应用补丁
apply_all_patches() {
    echo -e "${CYAN}=== 应用所有待应用补丁 ===${NC}"
    echo
    
    local patches=$(exec_remote "ls -1 $DEPLOY_DIR/scripts/sql/patch_*.sql 2>/dev/null | sort || echo ''")
    
    if [[ -z "$patches" ]]; then
        echo -e "${YELLOW}未找到任何补丁文件${NC}"
        return
    fi
    
    echo "待应用的补丁:"
    echo "$patches" | nl
    echo
    
    read -p "确认应用所有补丁? (y/n): " confirm
    if [[ $confirm != "y" && $confirm != "Y" ]]; then
        echo -e "${YELLOW}❌ 已取消${NC}"
        return
    fi
    
    # 确保 applied 目录存在
    exec_remote "mkdir -p $DEPLOY_DIR/scripts/sql/applied"
    
    # 逐个应用补丁
    local count=0
    local success=0
    local failed=0
    
    while IFS= read -r patch_file; do
        ((count++))
        local patch_name=$(basename "$patch_file")
        echo
        echo -e "${BLUE}[$count] 应用补丁: $patch_name${NC}"
        
        local cmd="cd $DEPLOY_DIR/scripts/sql && mysql -h$DB_HOST -P$DB_PORT -u$DB_USERNAME -p'$DB_PASSWORD' $DB_NAME < $patch_name"
        
        if exec_remote "$cmd"; then
            echo -e "${GREEN}✅ $patch_name 应用成功${NC}"
            
            # 移动补丁到 applied 目录
            local timestamp=$(date +%Y%m%d_%H%M%S)
            local applied_name="${patch_name%.sql}_applied_${timestamp}.sql"
            
            if exec_remote "cd $DEPLOY_DIR/scripts/sql && mv $patch_name applied/$applied_name"; then
                echo -e "${GREEN}📦 已移动到 applied/$applied_name${NC}"
                ((success++))
            else
                echo -e "${YELLOW}⚠️  补丁应用成功，但移动文件失败${NC}"
                ((success++))
            fi
        else
            echo -e "${RED}❌ $patch_name 应用失败（保留在原位置）${NC}"
            ((failed++))
        fi
    done <<< "$patches"
    
    echo
    echo -e "${CYAN}=== 补丁应用完成 ===${NC}"
    echo "总计: $count, 成功: $success, 失败: $failed"
}

# 14. 查看已应用补丁历史
view_applied_patches() {
    echo -e "${CYAN}=== 查看已应用补丁历史 ===${NC}"
    echo
    
    local applied_patches=$(exec_remote "ls -lt $DEPLOY_DIR/scripts/sql/applied/*.sql 2>/dev/null || echo ''")
    
    if [[ -z "$applied_patches" ]]; then
        echo -e "${YELLOW}📭 尚未应用任何补丁${NC}"
        return
    fi
    
    echo -e "${GREEN}📦 已应用的补丁:${NC}"
    echo
    echo "$applied_patches"
}

# 15. 查看数据库结构
check_database_structure() {
    echo -e "${CYAN}=== 查看数据库结构 ===${NC}"
    exec_remote_script "server-manager.sh" "check-db-structure"
}

# 16. 查看实时日志
view_logs() {
    echo -e "${CYAN}=== 查看实时日志 ===${NC}"
    echo "按 Ctrl+C 退出日志查看"
    echo
    
    local ssh_cmd=$(get_ssh_cmd)
    eval "$ssh_cmd \"tail -f $DEPLOY_DIR/logs/device-maintenance.log 2>/dev/null || echo '日志文件不存在'\""
}

# 17. 下载应用日志
download_logs() {
    echo -e "${CYAN}=== 下载应用日志 ===${NC}"
    
    local local_log_dir="$SCRIPT_DIR/../logs_downloaded/beijing"
    mkdir -p "$local_log_dir"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local log_file="device-maintenance_beijing_${timestamp}.log"
    
    echo "📥 下载日志到: $local_log_dir/$log_file"
    
    local scp_cmd=$(get_scp_cmd)
    if eval "$scp_cmd $SERVER_USER@$SERVER_HOST:$DEPLOY_DIR/logs/device-maintenance.log \"$local_log_dir/$log_file\""; then
        echo -e "${GREEN}✅ 日志下载成功${NC}"
        echo "文件位置: $local_log_dir/$log_file"
        
        # 显示最后50行
        echo
        echo -e "${BLUE}最后50行日志:${NC}"
        tail -50 "$local_log_dir/$log_file"
    else
        echo -e "${RED}❌ 日志下载失败${NC}"
    fi
}

# 18. 清理日志
clear_logs() {
    echo -e "${CYAN}=== 清理应用日志 ===${NC}"
    exec_remote_script "server-manager.sh" "clear-logs"
}

# 19. 全面健康检查
full_health_check() {
    echo -e "${CYAN}=== 全面健康检查 (北京环境) ===${NC}"
    exec_remote_script "server-manager.sh" "health-check"
}

# 20. 数据库连接检测
check_database() {
    echo -e "${CYAN}=== 数据库连接检测 ===${NC}"
    echo "数据库: $DB_HOST:$DB_PORT/$DB_NAME"
    echo
    exec_remote_script "server-manager.sh" "check-db"
}

# 21. ZooKeeper连接检测
check_zookeeper() {
    echo -e "${CYAN}=== ZooKeeper连接检测 ===${NC}"
    echo "ZooKeeper: $ZK_SERVERS"
    echo
    exec_remote_script "server-manager.sh" "check-zk"
}

# 22. 微服务健康检查
check_microservices() {
    echo -e "${CYAN}=== 微服务健康检查 ===${NC}"
    exec_remote_script "server-manager.sh" "check-services"
}

# 27. 查看微服务日志
view_microservice_logs() {
    echo -e "${CYAN}=== 查看微服务日志 ===${NC}"
    echo
    echo -e "${BLUE}可用的微服务:${NC}"
    echo "  1. neMgr (网元管理器)"
    echo "  2. taskInfo (任务信息服务)"
    echo "  3. nms (网络管理服务)"
    echo "  4. notifier (通知服务)"
    echo "  5. sftpserver (SFTP服务器)"
    echo "  6. adapter (适配器)"
    echo "  7. impl-app (实现服务)"
    echo "  8. user-manager (用户管理)"
    echo "  9. gateway-rest (网关)"
    echo "  0. 返回主菜单"
    echo
    read -p "请选择微服务 (0-9): " service_choice
    
    local service_dir=""
    local log_file=""
    
    case $service_choice in
        1)
            service_dir="/opt/dci/deploy/neManager-1.0.0-SNAPSHOT"
            log_file="ne-manager.log"
            ;;
        2)
            service_dir="/opt/dci/deploy/taskinfo-1.0.0-SNAPSHOT"
            log_file="taskinfo.log"
            ;;
        3)
            service_dir="/opt/dci/deploy/nms-rest-1.0.0-SNAPSHOT"
            log_file="nms-rest.log"
            ;;
        4)
            service_dir="/opt/dci/deploy/notifier-1.0.0-SNAPSHOT"
            log_file="notifier.log"
            ;;
        5)
            service_dir="/opt/dci/deploy/sftpserver-1.0.0-SNAPSHOT"
            log_file="sftpserver.log"
            ;;
        6)
            service_dir="/opt/dci/deploy/adapter-1.0.0-SNAPSHOT"
            log_file="adapter.log"
            ;;
        7)
            service_dir="/opt/dci/deploy/impl-app-1.0.0-SNAPSHOT"
            log_file="impl-app.log"
            ;;
        8)
            service_dir="/opt/dci/deploy/user-manager-1.0.0-SNAPSHOT"
            log_file="user-manager.log"
            ;;
        9)
            service_dir="/opt/dci/deploy/gateway-rest-1.0.0-SNAPSHOT"
            log_file="gateway-rest.log"
            ;;
        0)
            return
            ;;
        *)
            echo -e "${RED}❌ 无效的选择${NC}"
            return
            ;;
    esac
    
    echo
    echo -e "${BLUE}日志查看选项:${NC}"
    echo "  1. 查看最后100行"
    echo "  2. 查看最后50行 (含错误)"
    echo "  3. 搜索关键词"
    echo "  4. 实时查看 (tail -f)"
    echo
    read -p "请选择 (1-4): " log_option
    
    local ssh_cmd=$(get_ssh_cmd)
    
    case $log_option in
        1)
            echo -e "${CYAN}━━━ 最后100行日志 ━━━${NC}"
            eval "$ssh_cmd \"tail -100 $service_dir/log/$log_file 2>/dev/null || echo '日志文件不存在'\""
            ;;
        2)
            echo -e "${CYAN}━━━ 最后50行日志（含错误） ━━━${NC}"
            eval "$ssh_cmd \"tail -50 $service_dir/log/$log_file 2>/dev/null | grep -E 'ERROR|Exception|WARN' --color=always || tail -50 $service_dir/log/$log_file 2>/dev/null || echo '日志文件不存在'\""
            ;;
        3)
            read -p "请输入搜索关键词: " keyword
            if [[ -n "$keyword" ]]; then
                echo -e "${CYAN}━━━ 搜索关键词: $keyword ━━━${NC}"
                eval "$ssh_cmd \"grep -n --color=always '$keyword' $service_dir/log/$log_file 2>/dev/null | tail -50 || echo '未找到匹配内容'\""
            fi
            ;;
        4)
            echo -e "${CYAN}━━━ 实时查看日志（Ctrl+C退出） ━━━${NC}"
            eval "$ssh_cmd \"tail -f $service_dir/log/$log_file 2>/dev/null || echo '日志文件不存在'\""
            ;;
        *)
            echo -e "${RED}❌ 无效的选择${NC}"
            ;;
    esac
}

# 24. 执行自定义远程命令
exec_custom_command() {
    echo -e "${CYAN}=== 执行自定义远程命令 ===${NC}"
    echo "当前工作目录将是: $DEPLOY_DIR"
    echo
    read -p "请输入要执行的命令: " custom_cmd
    
    if [[ -z "$custom_cmd" ]]; then
        echo -e "${YELLOW}❌ 命令不能为空${NC}"
        return
    fi
    
    echo
    echo -e "${BLUE}执行命令: $custom_cmd${NC}"
    exec_remote "cd $DEPLOY_DIR && $custom_cmd"
}

# 25. 打开远程Shell
open_remote_shell() {
    echo -e "${CYAN}=== 打开远程Shell (北京环境) ===${NC}"
    echo "即将登录到北京服务器，输入 'exit' 退出"
    echo
    
    local ssh_cmd=$(get_ssh_cmd)
    eval "$ssh_cmd \"cd $DEPLOY_DIR && exec bash -l\""
}

# 主循环
main() {
    show_banner
    show_config_info
    
    while true; do
        show_main_menu
        read -p "请选择操作 [1-27]: " choice
        echo
        
        case $choice in
            1) full_deploy ;;
            2) compile_project ;;
            3) upload_application ;;
            4) upload_scripts ;;
            5) start_service ;;
            6) start_service_debug ;;
            7) stop_service ;;
            8) restart_service ;;
            9) restart_service_debug ;;
            10) check_service_status ;;
            11) init_database ;;
            12) apply_patch ;;
            13) apply_all_patches ;;
            14) view_applied_patches ;;
            15) check_database_structure ;;
            16) view_logs ;;
            17) download_logs ;;
            18) clear_logs ;;
            19) full_health_check ;;
            20) check_database ;;
            21) check_zookeeper ;;
            22) check_microservices ;;
            23) show_config_info ;;
            24) exec_custom_command ;;
            25) open_remote_shell ;;
            26)
                echo -e "${GREEN}👋 再见！${NC}"
                exit 0
                ;;
            27) view_microservice_logs ;;
            *)
                echo -e "${RED}❌ 无效的选择，请重新输入${NC}"
                ;;
        esac
        
        echo
        read -p "按回车键继续..."
        clear
        show_banner
        show_config_info
    done
}

# 如果有参数，执行对应操作后退出
if [[ $# -gt 0 ]]; then
    case $1 in
        deploy) full_deploy ;;
        compile) compile_project ;;
        upload-app) upload_application ;;
        upload-scripts) upload_scripts ;;
        start) start_service ;;
        start-debug) start_service_debug ;;
        stop) stop_service ;;
        restart) restart_service ;;
        restart-debug) restart_service_debug ;;
        status) check_service_status ;;
        init-db) init_database ;;
        patch) shift; apply_patch "$@" ;;
        logs) view_logs ;;
        download-logs) download_logs ;;
        health) full_health_check ;;
        shell) open_remote_shell ;;
        *)
            echo "用法: $0 [deploy|compile|upload-app|upload-scripts|start|start-debug|stop|restart|restart-debug|status|init-db|patch|logs|download-logs|health|shell]"
            echo "或不带参数启动交互式菜单"
            echo ""
            echo "北京环境专用部署脚本"
            echo "服务器: 117.134.16.112"
            echo "Web界面: http://117.134.16.112:8088/"
            exit 1
            ;;
    esac
else
    # 无参数，启动交互式菜单
    main
fi

