#!/bin/bash

# 远程环境测试脚本
# 使用方法: ssh actsvt@116.128.204.13 'bash -s' < remote-test.sh

echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                    🧪 远程环境通知系统测试                                    ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo ""

# 配置
DB_HOST="116.128.204.13"
DB_PORT="3309"
DB_USER="root"
DB_PASS="dciworld@2025"
DB_NAME="sotn"
API_BASE="http://localhost:18008"
KAFKA_BROKER="116.128.204.13:9092"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1️⃣  检查应用运行状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if netstat -tln | grep -q ":18008 "; then
    echo "✅ 应用正在运行 (端口 18008)"
else
    echo "❌ 应用未运行！"
    echo "   请先启动应用: cd /path/to/app && java -jar xxx.jar"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2️⃣  验证Bug 1修复: getAllBatches查询"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "📊 查询数据库中的UPGRADE批次..."
UPGRADE_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -sN \
    -e "SELECT COUNT(*) FROM dm_batch WHERE batch_type = 'UPGRADE';" 2>/dev/null)

if [ -z "$UPGRADE_COUNT" ]; then
    echo "⚠️  无法连接数据库"
else
    echo "数据库中UPGRADE批次数量: $UPGRADE_COUNT"
    
    if [ "$UPGRADE_COUNT" -gt 0 ]; then
        echo ""
        echo "批次列表:"
        mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -e \
            "SELECT batch_id, batch_name, status, created_time FROM dm_batch WHERE batch_type = 'UPGRADE' ORDER BY created_time DESC LIMIT 5;" 2>/dev/null
    fi
fi

echo ""
echo "📡 调用getAllBatches API..."
API_RESULT=$(curl -s -X POST "$API_BASE/restconf/operations/device-maintenance:get-all-batch-upgrades" \
    -H "Content-Type: application/json" \
    -d '{}')

API_COUNT=$(echo "$API_RESULT" | jq '.data | length' 2>/dev/null || echo "0")
echo "API返回批次数量: $API_COUNT"

if [ "$UPGRADE_COUNT" -eq "$API_COUNT" ]; then
    echo "✅ Bug 1已修复! 数据库和API数量一致"
else
    echo "⚠️  数据库($UPGRADE_COUNT) 与 API($API_COUNT) 不一致"
    echo "详细信息:"
    echo "$API_RESULT" | jq '.' 2>/dev/null || echo "$API_RESULT"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3️⃣  检查数据库中的RUNNING任务"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
RUNNING_TASKS=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -sN \
    -e "SELECT COUNT(*) FROM dm_device_task WHERE status = 'RUNNING';" 2>/dev/null)

echo "RUNNING状态的任务数: $RUNNING_TASKS"

if [ "$RUNNING_TASKS" -gt 0 ]; then
    echo ""
    echo "RUNNING任务详情:"
    mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -e \
        "SELECT task_id, device_id, task_type, batch_id, created_time, updated_time 
         FROM dm_device_task 
         WHERE status = 'RUNNING' 
         ORDER BY created_time DESC 
         LIMIT 5;" 2>/dev/null
    
    # 获取第一个RUNNING任务的详细信息
    FIRST_TASK=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -sN \
        -e "SELECT task_id, batch_id, device_id FROM dm_device_task WHERE status = 'RUNNING' LIMIT 1;" 2>/dev/null)
    
    if [ -n "$FIRST_TASK" ]; then
        TASK_ID=$(echo "$FIRST_TASK" | awk '{print $1}')
        BATCH_ID=$(echo "$FIRST_TASK" | awk '{print $2}')
        DEVICE_ID=$(echo "$FIRST_TASK" | awk '{print $3}')
        
        echo ""
        echo "⚠️  发现RUNNING任务!"
        echo "   Task ID: $TASK_ID"
        echo "   Batch ID: $BATCH_ID"
        echo "   Device ID: $DEVICE_ID"
    fi
else
    echo "✅ 没有RUNNING状态的任务"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4️⃣  检查通知接收状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "📡 查询通知统计..."
NOTIF_STATS=$(curl -s "$API_BASE/api/notifications/stats")
echo "$NOTIF_STATS" | jq '.' 2>/dev/null || echo "$NOTIF_STATS"

NOTIF_COUNT=$(echo "$NOTIF_STATS" | jq -r '.totalNotifications' 2>/dev/null || echo "0")

if [ "$NOTIF_COUNT" -eq 0 ]; then
    echo ""
    echo "⚠️  通知数量为0!"
    echo "   可能原因:"
    echo "   1. 应用刚启动，还没收到通知"
    echo "   2. Kafka未连接成功"
    echo "   3. 设备未发送system-change通知"
else
    echo ""
    echo "✅ 已接收 $NOTIF_COUNT 条通知"
    
    echo ""
    echo "最近的通知:"
    curl -s "$API_BASE/api/notifications/recent?limit=3" | jq '.notifications' 2>/dev/null
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5️⃣  手动发送测试通知"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "🧪 准备发送测试通知到Kafka..."

# 检查是否有RUNNING任务可以用于测试
if [ -n "$DEVICE_ID" ]; then
    TEST_MESSAGE="{\"neId\":\"$DEVICE_ID\",\"download.download-state\":\"COMPLETE\",\"download.file-name\":\"test.tar\"}"
    
    echo "测试消息: $TEST_MESSAGE"
    echo ""
    
    # 尝试发送测试消息
    if command -v kafka-console-producer.sh >/dev/null 2>&1; then
        echo "发送测试通知..."
        echo "$TEST_MESSAGE" | kafka-console-producer.sh \
            --bootstrap-server $KAFKA_BROKER \
            --topic system-change 2>/dev/null
        
        echo "✅ 测试通知已发送"
        echo ""
        echo "等待3秒，让应用处理通知..."
        sleep 3
        
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "6️⃣  验证Bug 2修复: 检查任务状态是否更新"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        echo ""
        echo "📊 查询任务状态..."
        TASK_STATUS=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -sN \
            -e "SELECT status FROM dm_device_task WHERE task_id = '$TASK_ID';" 2>/dev/null)
        
        echo "任务状态: $TASK_STATUS"
        
        if [ "$TASK_STATUS" = "COMPLETED" ]; then
            echo "✅ Bug 2已修复! 任务状态已更新为COMPLETED"
            
            # 检查批次状态
            if [ -n "$BATCH_ID" ] && [ "$BATCH_ID" != "NULL" ]; then
                echo ""
                echo "📊 查询批次状态..."
                BATCH_STATUS=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME -sN \
                    -e "SELECT status, success_count FROM dm_batch WHERE batch_id = '$BATCH_ID';" 2>/dev/null)
                
                echo "批次状态: $BATCH_STATUS"
                
                BATCH_STATE=$(echo "$BATCH_STATUS" | awk '{print $1}')
                SUCCESS_COUNT=$(echo "$BATCH_STATUS" | awk '{print $2}')
                
                if [ "$BATCH_STATE" = "COMPLETED" ] && [ "$SUCCESS_COUNT" -gt 0 ]; then
                    echo "✅ 批次状态也已更新! status=$BATCH_STATE, successCount=$SUCCESS_COUNT"
                else
                    echo "⚠️  批次状态未完全更新: status=$BATCH_STATE, successCount=$SUCCESS_COUNT"
                fi
            fi
        else
            echo "⚠️  任务状态仍为: $TASK_STATUS"
            echo "   请检查日志: tail -f logs/application.log | grep 'system-change'"
        fi
        
        echo ""
        echo "📊 查询通知数量..."
        NEW_NOTIF_COUNT=$(curl -s "$API_BASE/api/notifications/stats" | jq -r '.totalNotifications' 2>/dev/null || echo "0")
        echo "通知数量: $NOTIF_COUNT → $NEW_NOTIF_COUNT"
        
        if [ "$NEW_NOTIF_COUNT" -gt "$NOTIF_COUNT" ]; then
            echo "✅ 新通知已被接收和存储"
        fi
        
    else
        echo "⚠️  kafka-console-producer.sh 未安装"
        echo "   请手动发送测试消息:"
        echo ""
        echo "   kafka-console-producer.sh --bootstrap-server $KAFKA_BROKER --topic system-change"
        echo "   $TEST_MESSAGE"
    fi
else
    echo "ℹ️  没有RUNNING任务可用于测试"
    echo "   你可以创建一个新的批次任务，然后再次运行此脚本"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "7️⃣  检查应用日志"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "🔍 查找Kafka相关日志..."

# 尝试查找日志文件
LOG_PATHS=(
    "logs/application.log"
    "/var/log/device-maintenance/application.log"
    "~/logs/application.log"
)

LOG_FILE=""
for path in "${LOG_PATHS[@]}"; do
    if [ -f "$path" ]; then
        LOG_FILE="$path"
        break
    fi
done

if [ -n "$LOG_FILE" ]; then
    echo "日志文件: $LOG_FILE"
    echo ""
    
    echo "--- Kafka监听器启动日志 ---"
    grep -i "Kafka监听器配置已启用" "$LOG_FILE" | tail -3
    grep -i "System-Change 通知监听器已初始化" "$LOG_FILE" | tail -3
    
    echo ""
    echo "--- 最近接收的通知 (最近10条) ---"
    grep -i "接收到 system-change 通知" "$LOG_FILE" | tail -10
    
    echo ""
    echo "--- 批次更新触发日志 (最近5条) ---"
    grep -i "已调度批次更新任务" "$LOG_FILE" | tail -5
    
else
    echo "⚠️  未找到日志文件"
    echo "   请手动检查: tail -f logs/application.log | grep -iE 'kafka|system-change'"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                          测试完成                                             ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo ""

echo "📋 总结:"
echo "  - Bug 1 (getAllBatches): 数据库有 $UPGRADE_COUNT 个UPGRADE批次, API返回 $API_COUNT 个"
echo "  - Bug 2 (状态更新): 需要手动测试 (发送Kafka消息)"
echo "  - Bug 3 (通知为空): 当前有 $NEW_NOTIF_COUNT 条通知"
echo ""
echo "💡 建议:"
echo "  1. 检查日志文件确认Kafka连接状态"
echo "  2. 手动发送测试通知验证Bug 2修复"
echo "  3. 如需查看实时日志: tail -f logs/application.log | grep -E 'Kafka|system-change|批次更新'"
echo ""

