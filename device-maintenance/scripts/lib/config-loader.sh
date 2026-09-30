#!/bin/bash

# ⭐ 优先使用环境变量中已设置的 CONFIG_FILE
if [[ -n "${CONFIG_FILE:-}" && -f "$CONFIG_FILE" ]]; then
    # 环境变量已设置且文件存在，直接使用
    :
else
    # 否则查找默认配置文件
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )/.." && pwd)"
    CONFIG_CANDIDATES=(
        "$SCRIPT_DIR/config/config.properties"
        "$SCRIPT_DIR/config.properties"
    )
    CONFIG_FILE=""
    for candidate in "${CONFIG_CANDIDATES[@]}"; do
        if [[ -f "$candidate" ]]; then
            CONFIG_FILE="$candidate"
            break
        fi
    done

    if [[ -z "$CONFIG_FILE" ]]; then
        echo "❌ 配置文件未找到: ${CONFIG_CANDIDATES[0]}"
        return 1 2>/dev/null || exit 1
    fi
fi

load_config() {
    while IFS='=' read -r key value || [[ -n "$key" ]]; do
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        if [[ -n "$key" && -n "$value" ]]; then
            export "$key"="$value"
        fi
    done < "$CONFIG_FILE"
}

validate_config() {
    local errors=()
    [[ -z "$SERVER_HOST" ]] && errors+=("SERVER_HOST未配置")
    [[ -z "$SERVER_USER" ]] && errors+=("SERVER_USER未配置")
    [[ -z "$DEPLOY_DIR" ]] && errors+=("DEPLOY_DIR未配置")
    [[ -z "$DB_HOST" ]] && errors+=("DB_HOST未配置")
    [[ -z "$ZK_SERVERS" ]] && errors+=("ZK_SERVERS未配置")

    if [[ ${#errors[@]} -gt 0 ]]; then
        echo "❌ 配置验证失败:"
        for error in "${errors[@]}"; do
            echo "   - $error"
        done
        return 1
    fi
    return 0
}

show_config() {
    echo "📋 当前配置:"
    echo "  服务器: $SERVER_USER@$SERVER_HOST:$SSH_PORT"
    echo "  部署目录: $DEPLOY_DIR"
    echo "  数据库: $DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"
    echo "  ZooKeeper: $ZK_SERVERS"
    echo "  应用端口: $APP_PORT"
}

get_db_url() {
    echo "jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8"
}

get_ssh_cmd() {
    if [[ -n "$SERVER_PASSWORD" ]]; then
        echo "sshpass -p \"$SERVER_PASSWORD\" ssh -p $SSH_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST"
    else
        echo "ssh -p $SSH_PORT -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST"
    fi
}

# 获取支持sudo的SSH命令
get_ssh_sudo_cmd() {
    if [[ -n "$SERVER_PASSWORD" ]]; then
        echo "sshpass -p \"$SERVER_PASSWORD\" ssh -p $SSH_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST"
    else
        echo "ssh -p $SSH_PORT -o ConnectTimeout=10 $SERVER_USER@$SERVER_HOST"
    fi
}

get_scp_cmd() {
    if [[ -n "$SERVER_PASSWORD" ]]; then
        echo "sshpass -p \"$SERVER_PASSWORD\" scp -P $SSH_PORT -o StrictHostKeyChecking=no"
    else
        echo "scp -P $SSH_PORT -o StrictHostKeyChecking=no"
    fi
}

get_mysql_cmd_args() {
    local args=("-h$DB_HOST" "-P$DB_PORT" "-u$DB_USERNAME" "-D" "$DB_NAME" "-N" "-B")
    if [[ -n "${DB_PASSWORD:-}" ]]; then
        args+=("-p$DB_PASSWORD")
    fi
    echo "${args[@]}"
}

ensure_config_loaded() {
    if [[ ${CONFIG_LOADED:-0} -eq 1 ]]; then
        return 0
    fi
    if ! load_config; then
        return 1
    fi
    if ! validate_config; then
        return 1
    fi
    export CONFIG_LOADED=1
    return 0
}

