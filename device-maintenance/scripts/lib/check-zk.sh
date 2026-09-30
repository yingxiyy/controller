#!/bin/bash

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
SCRIPT_ROOT="$(cd "$LIB_DIR/.." && pwd)"

source "$LIB_DIR/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

if ! command -v nc >/dev/null 2>&1; then
    echo "❌ 未找到nc命令"
    exit 1
fi

status=0
IFS=',' read -ra servers <<< "$ZK_SERVERS"
for server in "${servers[@]}"; do
    host="${server%%:*}"
    port="${server##*:}"
    echo -n "🔍 检查 $host:$port ... "
    if nc -z -w 3 "$host" "$port" >/dev/null 2>&1; then
        echo "✅"
    else
        echo "❌"
        status=1
    fi

done

exit $status

