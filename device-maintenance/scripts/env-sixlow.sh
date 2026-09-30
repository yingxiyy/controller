#!/bin/bash
##############################################################################
# 环境变量配置 - sixlow 环境
# 服务器: 10.192.230.22
# 生成时间: 2025-12-08
##############################################################################

# ===== 网络配置 =====
export myIp="0.0.0.0"                    # 服务注册IP（0.0.0.0 表示监听所有接口）
export SERVER_PORT="18008"                # 服务端口

# ===== Kafka 配置 =====
export KAFKA_BROKER="10.242.111.32:9092"

# ===== MongoDB 配置 =====
export MONGO_SERVERS="127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012"
export MONGO_DATABASE="sotn"
export MONGO_USER="dci"
export MONGO_PWD="dciworld@iivi"

# ===== ZooKeeper 配置 =====
export pmcZooKeepers="127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192"
export NAMESPACE="clband"

# ===== MySQL 配置 =====
export MYSQL_IP="127.0.0.1"
export MYSQL_PORT="3307"                  # ⚠️ 注意：sixlow 使用 3307，不是 3309
export MYSQL_USER="root"
export MYSQL_PASSWORD="dciworld@iivi"

# ===== 部署配置 =====
export SERVICE_USER="actsvt111"
export ROOT_DIR="/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT"

# ===== 其他配置 =====
export swVersion="1.0.0-SNAPSHOT"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 环境变量已设置 - sixlow 环境"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  myIp: ${myIp}"
echo "  SERVER_PORT: ${SERVER_PORT}"
echo "  KAFKA_BROKER: ${KAFKA_BROKER}"
echo "  MONGO_SERVERS: ${MONGO_SERVERS}"
echo "  MYSQL_IP: ${MYSQL_IP}"
echo "  MYSQL_PORT: ${MYSQL_PORT}"
echo "  pmcZooKeepers: ${pmcZooKeepers}"
echo "  NAMESPACE: ${NAMESPACE}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

