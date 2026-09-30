#!/bin/bash

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_ROOT="$(cd "$LIB_DIR/.." && pwd)"

source "$LIB_DIR/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

to_lower() { printf '%s' "$1" | tr '[:upper:]' '[:lower:]'; }
to_upper() { printf '%s' "$1" | tr '[:lower:]' '[:upper:]'; }
strip_delimiters() { printf '%s' "$1" | tr -d '_-'; }
replace_delimiters_with_space() { printf '%s' "$1" | tr '_-' '  '; }

check_local_endpoint() {
    local url="$1"
    local name="$2"
    if command -v curl >/dev/null 2>&1; then
        if curl -fsS --max-time 3 "$url" >/dev/null; then
            echo "✅ $name 可访问 ($url)"
            return 0
        else
            echo "❌ $name 无法访问 ($url)"
            return 1
        fi
    fi
    echo "⚠️  未安装 curl，跳过 $name"
    return 0
}

status=0
APP_HEALTH_URL="http://127.0.0.1:${APP_PORT:-18008}/device-maintenance/actuator/health"
if ! check_local_endpoint "$APP_HEALTH_URL" "Device Maintenance"; then
    status=1
fi

call_health_api() {
    local services="$1"
    local url="http://127.0.0.1:${APP_PORT:-18008}/device-maintenance/health/zookeeper?services=${services}"
    curl -fsS --max-time 8 "$url"
}

if [[ -z "${ALL_SERVICES:-}" ]]; then
    echo "ℹ️  ALL_SERVICES 未在配置中定义，使用内置列表: device-maintenance,neMgr,taskInfo,ftpServer,notifier,allocator"
    ALL_SERVICES="device-maintenance,neMgr,taskInfo,ftpServer,notifier,allocator"
fi

if ! command -v curl >/dev/null 2>&1; then
    echo "⚠️  缺少 curl，无法继续微服务检查"
    exit 1
fi

echo
IFS=',' read -ra service_list <<< "$ALL_SERVICES"
normalized_services=()
for svc in "${service_list[@]}"; do
    svc_trimmed="$(echo "$svc" | xargs)"
    [[ -z "$svc_trimmed" ]] && continue

    svc_lower="$(to_lower "$svc_trimmed")"
    svc_upper="$(to_upper "$svc_trimmed")"

    normalized_services+=("$svc_trimmed" "$svc_lower" "$svc_upper")
    normalized_services+=("$(strip_delimiters "$svc_trimmed")")
    normalized_services+=("$(strip_delimiters "$svc_lower")" "$(strip_delimiters "$svc_upper")")
    normalized_services+=("$(replace_delimiters_with_space "$svc_trimmed")")

    var_name="$(to_upper "$svc_trimmed")_SERVICE_NAME"
    if [[ -n "${!var_name:-}" ]]; then
        normalized_services+=("${!var_name}")
    fi
 done

unique_services=()
declare -A seen
for svc in "${normalized_services[@]}"; do
    svc_candidate="$(echo "$svc" | xargs)"
    [[ -z "$svc_candidate" ]] && continue
    key="$(to_lower "$svc_candidate")"
    if [[ -z "${seen[$key]:-}" ]]; then
        seen[$key]=1
        unique_services+=("$svc_candidate")
    fi
 done

if [[ ${#unique_services[@]} -eq 0 ]]; then
    echo "⚠️  服务列表为空"
    exit $status
fi

services_param=$(IFS=','; echo "${unique_services[*]}")
response="$(call_health_api "$services_param" || true)"
if [[ -z "$response" ]]; then
    echo "❌ 无法获取健康检查结果"
    exit 1
fi

echo "🔍 健康检查结果:"
if command -v jq >/dev/null 2>&1; then
    echo "$response" | jq '.otherServices + .self' 2>/dev/null || echo "$response"
else
    echo "$response"
fi

declare -i failures=0
if command -v jq >/dev/null 2>&1; then
    failures=$(echo "$response" | jq -r '[.self[], .otherServices[]] | map(select(.zkStatus != "REGISTERED" or (.healthStatus != "UP" and .healthStatus != "UNKNOWN"))) | length' 2>/dev/null || echo 0)
else
    if [[ "$response" != *"REGISTERED"* || "$response" == *"DOWN"* ]]; then
        failures=1
    fi
fi

if (( failures > 0 )); then
    echo "❌ 部分服务状态异常"
    status=1
else
    echo "✅ 所有服务状态正常"
fi

exit $status
