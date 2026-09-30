#!/bin/bash

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
SCRIPT_ROOT="$(cd "$LIB_DIR/.." && pwd)"

source "$SCRIPT_ROOT/lib/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

JAR_SOURCE="$SCRIPT_ROOT/../target/device-maintenance-1.0.0-SNAPSHOT.jar"
if [[ ! -f "$JAR_SOURCE" ]]; then
    echo "❌ 未找到JAR: $JAR_SOURCE"
    exit 1
fi

scp_cmd=$(get_scp_cmd)
ssh_cmd=$(get_ssh_cmd)
remote_target_dir="$DEPLOY_DIR/target"
remote_jar="$remote_target_dir/device-maintenance-1.0.0-SNAPSHOT.jar"

echo "🚚 上传JAR到 $remote_target_dir"
eval "$ssh_cmd \"mkdir -p $remote_target_dir\""
eval "$scp_cmd \"$JAR_SOURCE\" \"$SERVER_USER@$SERVER_HOST:$remote_jar\""

echo "✅ 应用上传完成"

