#!/bin/bash

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
SCRIPT_ROOT="$(cd "$LIB_DIR/.." && pwd)"

source "$SCRIPT_ROOT/lib/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

remote_package="$DEPLOY_DIR/device-maintenance-scripts.tar"
remote_scripts_dir="$DEPLOY_DIR/scripts"
local_temp_dir="$(mktemp -d)"
package_path="$local_temp_dir/device-maintenance-scripts.tar"

echo "📦 打包脚本与配置..."
entries=(
    "scripts/local-manager.sh"
    "scripts/server-manager.sh"
    "scripts/lib"
    "scripts/config"
    "scripts/sql"
)
if [[ -d "$SCRIPT_ROOT/datapatch" ]]; then
    entries+=("datapatch")
fi

tar_cmd=(tar --no-xattrs --no-acls --no-mac-metadata)
if command -v gtar >/dev/null 2>&1; then
    tar_cmd=(gtar --no-xattrs --no-acls)
fi

COPYFILE_DISABLE=1 "${tar_cmd[@]}" -C "$SCRIPT_ROOT/.." -cf "$package_path" "${entries[@]}"

scp_cmd=$(get_scp_cmd)
ssh_cmd=$(get_ssh_cmd)

echo "🚚 上传打包文件到 $SERVER_HOST:$remote_package"
eval "$scp_cmd \"$package_path\" \"$SERVER_USER@$SERVER_HOST:$remote_package\""

echo "📂 远程解压脚本..."
remote_cmd="mkdir -p $remote_scripts_dir && cd $DEPLOY_DIR && tar -xf \$(basename '$remote_package') && rm -f \$(basename '$remote_package')"
eval "$ssh_cmd \"bash -lc '$remote_cmd'\""

rm -rf "$local_temp_dir"
echo "✅ 脚本上传完成"

