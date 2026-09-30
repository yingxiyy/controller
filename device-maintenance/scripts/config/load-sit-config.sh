#!/bin/bash

# SIT服务器配置加载脚本
# 用于加载SIT环境的微服务配置信息

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SIT_CONFIG_FILE="$SCRIPT_DIR/server-shanghai-env.properties"
SERVER_CONFIG_FILE="$SCRIPT_DIR/server-cloud-env.properties"

# 检查配置文件是否存在
if [[ ! -f "$SIT_CONFIG_FILE" ]]; then
    echo "❌ 错误: SIT配置文件不存在: $SIT_CONFIG_FILE"
    exit 1
fi

# 加载配置文件
echo "📋 加载SIT服务器配置..."
source "$SIT_CONFIG_FILE"

# 验证关键配置
validate_config() {
    local errors=0
    
    if [[ -z "$SIT_SERVER_HOST" ]]; then
        echo "❌ 错误: SIT_SERVER_HOST 未配置"
        ((errors++))
    fi
    
    if [[ -z "$NEMGR_PORT" ]]; then
        echo "❌ 错误: NEMGR_PORT 未配置"
        ((errors++))
    fi
    
    if [[ -z "$SFTPSERVER_PORT" ]]; then
        echo "❌ 错误: SFTPSERVER_PORT 未配置"
        ((errors++))
    fi
    
    if [[ $errors -gt 0 ]]; then
        echo "❌ 配置验证失败，发现 $errors 个错误"
        return 1
    fi
    
    echo "✅ 配置验证通过"
    return 0
}

# 显示配置摘要
show_config_summary() {
    echo ""
    echo "🖥️  SIT服务器配置摘要"
    echo "==============================================="
    echo "服务器地址: $SIT_SERVER_HOST"
    echo "SSH用户:   $SIT_SSH_USER"
    echo "SSH端口:   $SIT_SSH_PORT"
    echo "部署目录:  $SIT_DEPLOY_DIR"
    echo ""
    echo "📊 核心服务端口:"
    echo "  • neMgr:      $NEMGR_PORT"
    echo "  • sftpserver: $SFTPSERVER_PORT"
    echo "  • status:     $STATUS_PORT"
    echo "  • notifier:   $NOTIFIER_PORT"
    echo "  • allocate:   $ALLOCATE_PORT"
    echo ""
    echo "🌐 网关服务端口:"
    echo "  • gateway:    $GATEWAY_PORT"
    echo "  • auth:       $AUTH_PORT"
    echo "  • user-mgr:   $USER_MANAGER_PORT"
    echo ""
    echo "📋 任务服务端口:"
    echo "  • taskinfo:   $TASKINFO_PORT"
    echo "  • advance:    $ADVANCE_PORT"
    echo "  • discover2:  $DISCOVER2_PORT"
    echo "==============================================="
}

# 查询特定服务配置
query_service() {
    local service_name=$(echo "$1" | tr '[:lower:]' '[:upper:]')
    local host_var="${service_name}_HOST"
    local port_var="${service_name}_PORT"
    local health_var="${service_name}_HEALTH_PATH"
    
    local host=${!host_var}
    local port=${!port_var}
    local health_path=${!health_var}
    
    if [[ -n "$host" && -n "$port" ]]; then
        echo "🔍 服务: $service_name"
        echo "   地址: $host:$port"
        echo "   健康检查: http://$host:$port${health_path:-/actuator/health}"
    else
        echo "❌ 服务 '$service_name' 未找到配置"
        return 1
    fi
}

# 生成服务列表
list_all_services() {
    echo "📋 所有SIT微服务列表:"
    echo "==============================================="
    
    echo ""
    echo "🎯 核心业务服务:"
    IFS=',' read -ra SERVICES <<< "$CORE_SERVICES"
    for service in "${SERVICES[@]}"; do
        query_service "$service" | grep -E "地址:|健康检查:" | sed 's/^/  /'
    done
    
    echo ""
    echo "🌐 网关认证服务:"
    IFS=',' read -ra SERVICES <<< "$GATEWAY_SERVICES"
    for service in "${SERVICES[@]}"; do
        query_service "$service" | grep -E "地址:|健康检查:" | sed 's/^/  /'
    done
    
    echo ""
    echo "📋 任务调度服务:"
    IFS=',' read -ra SERVICES <<< "$TASK_SERVICES"
    for service in "${SERVICES[@]}"; do
        query_service "$service" | grep -E "地址:|健康检查:" | sed 's/^/  /'
    done
    
    echo ""
    echo "📊 监控管理服务:"
    IFS=',' read -ra SERVICES <<< "$MONITOR_SERVICES"
    for service in "${SERVICES[@]}"; do
        query_service "$service" | grep -E "地址:|健康检查:" | sed 's/^/  /'
    done
    
    echo "==============================================="
}

# 生成Postman环境配置
generate_postman_env() {
    echo "🔄 生成Postman环境变量配置..."
    
    cat > "$SCRIPT_DIR/sit-postman-env.json" << EOF
{
  "id": "sit-environment",
  "name": "SIT Environment",
  "values": [
    {
      "key": "sit_server_host",
      "value": "$SIT_SERVER_HOST",
      "description": "SIT服务器主机地址",
      "enabled": true
    },
    {
      "key": "nemgr_host",
      "value": "$NEMGR_HOST",
      "description": "NE Manager微服务主机地址",
      "enabled": true
    },
    {
      "key": "nemgr_port",
      "value": "$NEMGR_PORT",
      "description": "NE Manager微服务端口",
      "enabled": true
    },
    {
      "key": "sftpserver_host",
      "value": "$SFTPSERVER_HOST",
      "description": "SFTP Server微服务主机地址",
      "enabled": true
    },
    {
      "key": "sftpserver_port",
      "value": "$SFTPSERVER_PORT",
      "description": "SFTP Server微服务端口",
      "enabled": true
    },
    {
      "key": "status_host",
      "value": "$STATUS_HOST",
      "description": "Status微服务主机地址",
      "enabled": true
    },
    {
      "key": "status_port",
      "value": "$STATUS_PORT",
      "description": "Status微服务端口",
      "enabled": true
    },
    {
      "key": "taskinfo_host",
      "value": "$TASKINFO_HOST",
      "description": "TaskInfo微服务主机地址",
      "enabled": true
    },
    {
      "key": "taskinfo_port",
      "value": "$TASKINFO_PORT",
      "description": "TaskInfo微服务端口",
      "enabled": true
    }
  ]
}
EOF
    
    echo "✅ Postman环境配置已生成: $SCRIPT_DIR/sit-postman-env.json"
}

# 生成本地工具 config.properties
generate_tool_config() {
    echo "🔄 生成本地工具配置 (config.properties)..."

    if [[ ! -f "$SERVER_CONFIG_FILE" ]]; then
        echo "⚠️  未找到旧环境配置: $SERVER_CONFIG_FILE"
    fi

    cat > "$ROOT_DIR/config/config.properties" << EOF
# 当前激活环境配置 - 自动生成，请勿手工编辑
# 生成时间: $(date '+%Y-%m-%d %H:%M:%S')

# 服务器配置
SERVER_HOST=$SIT_SERVER_HOST
SERVER_USER=$SIT_SSH_USER
SERVER_PASSWORD=$SIT_SSH_PASSWORD
SSH_PORT=$SIT_SSH_PORT
DEPLOY_DIR=$SIT_DEPLOY_DIR

# 数据库配置
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=${DB_NAME:-sotn}
DB_USERNAME=${DB_USERNAME:-root}
DB_PASSWORD=${DB_PASSWORD:-dciworld@2025}

# ZooKeeper
ZK_SERVERS=$ZK_SERVERS
ZK_NAMESPACE=$ZK_NAMESPACE

# Kafka
KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-}
KAFKA_CONSUMER_GROUP=${KAFKA_CONSUMER_GROUP:-}

# 应用端口
APP_PORT=${DEVICE_MAINTENANCE_PORT:-18008}

EOF

    echo "✅ config.properties 已生成于: $ROOT_DIR/config/config.properties"
}

# 主函数
main() {
    case "$1" in
        "validate")
            validate_config
            ;;
        "summary")
            show_config_summary
            ;;
        "query")
            if [[ -n "$2" ]]; then
                query_service "$2"
            else
                echo "用法: $0 query <service_name>"
                echo "例如: $0 query nemgr"
            fi
            ;;
        "list")
            list_all_services
            ;;
        "postman")
            generate_postman_env
            ;;
        "tool-config")
            generate_tool_config
            ;;
        "")
            echo "🔧 SIT配置加载脚本"
            echo ""
            echo "用法: $0 <command>"
            echo ""
            echo "可用命令:"
            echo "  validate  - 验证配置文件"
            echo "  summary   - 显示配置摘要"
            echo "  query     - 查询特定服务配置"
            echo "  list      - 列出所有服务"
            echo "  postman   - 生成Postman环境配置"
            echo "  tool-config - 生成scripts/config.properties"
            echo ""
            echo "示例:"
            echo "  $0 summary"
            echo "  $0 query nemgr"
            echo "  $0 list"
            echo "  $0 tool-config"
            ;;
        *)
            echo "❌ 未知命令: $1"
            echo "运行 '$0' 查看可用命令"
            exit 1
            ;;
    esac
}

# 如果直接执行脚本，调用主函数
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
