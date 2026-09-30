#!/bin/bash

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
SCRIPT_ROOT="$(cd "$LIB_DIR/.." && pwd)"

source "$LIB_DIR/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

if ! command -v mysql >/dev/null 2>&1 && ! command -v mariadb >/dev/null 2>&1; then
    echo "❌ 未找到MySQL/MariaDB客户端"
    exit 1
fi

mysql_cmd="mysql"
command -v mysql >/dev/null 2>&1 || mysql_cmd="mariadb"

echo "🔍 测试数据库连接: $DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"
if echo "SELECT 1" | $mysql_cmd -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" >/dev/null 2>&1; then
    echo "✅ 数据库连接正常"
else
    echo "❌ 数据库连接失败"
    exit 1
fi

