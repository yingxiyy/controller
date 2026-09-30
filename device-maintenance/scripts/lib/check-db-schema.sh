#!/bin/bash

set -euo pipefail

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
SCRIPT_ROOT="$(cd "$LIB_DIR/.." && pwd)"
SQL_BASE_DIR="$SCRIPT_ROOT/sql"

source "$LIB_DIR/config-loader.sh"
if ! ensure_config_loaded; then
    exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
    echo "❌ 未找到 mysql 客户端，请先安装"
    exit 1
fi

if [[ -z "${DB_NAME:-}" ]]; then
    echo "❌ 未配置数据库名称 (DB_NAME)"
    exit 1
fi

IFS=' ' read -r -a MYSQL_ARGS <<< "$(get_mysql_cmd_args)"
MYSQL_CMD=(mysql "${MYSQL_ARGS[@]}")

mysql_query() {
    "${MYSQL_CMD[@]}" -e "$1"
}

if ! mysql_query "SELECT 1" >/dev/null 2>&1; then
    echo "❌ 无法连接数据库，请检查配置"
    exit 1
fi

status=0

check_table_exists() {
    local table="$1"
    local count
    count=$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table'") || return 1
    if [[ "$count" -eq 0 ]]; then
        echo "❌ 未找到数据表 $table"
        status=1
        return 1
    fi
    echo "✅ 数据表存在: $table"
    return 0
}

check_required_columns() {
    local table="$1"
    shift
    local required_cols=("$@")
    mapfile -t actual_cols < <(mysql_query "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '$table'")
    declare -A actual_map=()
    for col in "${actual_cols[@]}"; do
        actual_map["$col"]=1
    done
    for col in "${required_cols[@]}"; do
        if [[ -z "${actual_map[$col]+x}" ]]; then
            echo "  ❌ 缺少字段: $table.$col"
            status=1
        else
            echo "  ✅ 字段存在: $table.$col"
        fi
    done
}

check_forbidden_columns() {
    local table="$1"
    shift
    local forbidden_cols=("$@")
    for col in "${forbidden_cols[@]}"; do
        local exists
        exists=$(mysql_query "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '$table' AND column_name = '$col'")
        if [[ "$exists" -gt 0 ]]; then
            echo "  ⚠️ 发现已废弃字段: $table.$col"
            status=1
        fi
    done
}

check_indexes() {
    local table="$1"
    shift
    local required_indexes=("$@")
    mapfile -t actual_indexes < <(mysql_query "SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = '$table'")
    declare -A index_map=()
    for idx in "${actual_indexes[@]}"; do
        index_map["$idx"]=1
    done
    for idx in "${required_indexes[@]}"; do
        if [[ -z "${index_map[$idx]+x}" ]]; then
            echo "  ❌ 缺少索引: $table.$idx"
            status=1
        else
            echo "  ✅ 索引存在: $table.$idx"
        fi
    done
}

echo "🔍 开始检查数据库结构: $DB_NAME"

echo "— dm_device_task —"
if check_table_exists "dm_device_task"; then
    check_required_columns "dm_device_task" \
        task_id batch_id task_type device_id device_name sftp_server_id status \
        file_path backup_file_path backup_file_name restore_type force_restore \
        auto_restart restart_status current_version previous_version target_version auto_backup \
        force_upgrade timeout_minutes retry_count error_message created_time \
        updated_time started_time completed_time
    check_forbidden_columns "dm_device_task" \
        download_status backup_status upgrade_status commit_status restore_status \
        download_progress backup_progress upgrade_progress restore_progress
    check_indexes "dm_device_task" \
        PRIMARY idx_batch_id idx_task_type idx_device_id idx_status idx_task_type_status
fi

echo

echo "— dm_upgrade_batch —"
if check_table_exists "dm_upgrade_batch"; then
    check_required_columns "dm_upgrade_batch" \
        batch_id batch_name upgrade_file_path sftp_server_id operation_interval \
        device_count execution_mode scheduled_time max_retry_count status \
        success_count failed_count error_message created_by created_time updated_time \
        started_time completed_time
    check_indexes "dm_upgrade_batch" \
        PRIMARY uk_batch_name idx_status idx_execution_mode idx_scheduled_time idx_created_time idx_operation_interval idx_batch_status_time
fi

echo

echo "— dm_batch_device —"
if check_table_exists "dm_batch_device"; then
    check_required_columns "dm_batch_device" \
        id batch_id device_id device_name device_type vendor_type created_time
    check_indexes "dm_batch_device" \
        PRIMARY uk_batch_device idx_batch_id idx_device_id idx_batch_device_batch
fi

echo

echo "— dm_operation_history —"
if check_table_exists "dm_operation_history"; then
    check_required_columns "dm_operation_history" \
        history_id operation_type target_type target_id device_id device_name \
        operation_status operation_result error_message duration_ms operator created_time
    check_indexes "dm_operation_history" \
        PRIMARY idx_operation_type idx_target_type_id idx_device_id idx_operation_status idx_created_time idx_history_device_time
fi

echo

echo "— dm_task_execution_log —"
if check_table_exists "dm_task_execution_log"; then
    check_required_columns "dm_task_execution_log" \
        log_id task_id batch_id device_id step_name step_status progress_percent \
        log_message error_code error_message rpc_request rpc_response created_time
    check_indexes "dm_task_execution_log" \
        PRIMARY idx_task_id idx_batch_id idx_device_id idx_step_name idx_step_status idx_created_time idx_log_task_step
fi

echo

echo "— dm_device_software_info —"
if check_table_exists "dm_device_software_info"; then
    check_required_columns "dm_device_software_info" \
        info_id device_id device_name vendor_type current_version previous_version \
        available_versions last_upgrade_time last_backup_time backup_file_path status \
        created_time updated_time
    check_indexes "dm_device_software_info" \
        PRIMARY uk_device_id idx_vendor_type idx_current_version idx_status idx_last_upgrade_time idx_updated_time
fi

echo

echo "— dm_sftp_server —"
if check_table_exists "dm_sftp_server"; then
    check_required_columns "dm_sftp_server" \
        server_id server_name host port username password base_path description \
        connection_status last_check_time check_error_message is_enabled is_default \
        created_by created_time updated_time
    check_indexes "dm_sftp_server" \
        PRIMARY uk_server_name idx_host_port idx_connection_status idx_is_enabled idx_is_default idx_created_time idx_sftp_server_status_time
fi

echo

echo "— dm_sftp_connection_log —"
if check_table_exists "dm_sftp_connection_log"; then
    check_required_columns "dm_sftp_connection_log" \
        log_id server_id operation_type operation_path operation_status response_time_ms \
        error_message operation_result operator created_time
    check_indexes "dm_sftp_connection_log" \
        PRIMARY idx_server_id idx_operation_type idx_operation_status idx_created_time idx_sftp_log_server_time
fi

echo
if [[ $status -eq 0 ]]; then
    echo "✅ 数据库结构检查通过"
else
    echo "⚠️ 数据库结构存在问题，请根据上方提示修复"
fi

exit $status
