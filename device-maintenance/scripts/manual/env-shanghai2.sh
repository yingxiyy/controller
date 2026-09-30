#!/bin/bash
##############################################################################
# Device Maintenance - Shanghai2 环境变量配置
# 用途：设置部署所需的环境变量
# 基于：scripts/manual/env-shanghai2.conf
##############################################################################

# ===== 网络配置 =====
export myIp="0.0.0.0"                    # 服务注册IP（0.0.0.0 表示自动检测）
export SERVER_PORT="18008"                # 服务端口

# ===== Kafka 配置 =====
export KAFKA_BROKER="127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"

# ===== MongoDB 配置 =====
export MONGO_SERVERS="127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012"
export MONGO_DATABASE="sotn"
export MONGO_USER="dci"
export MONGO_PWD="dciworld@iivi"

# ===== ZooKeeper 配置 =====
export pmcZooKeepers="127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192"
export NAMESPACE="clband"                 # 上海2环境使用 clband 命名空间

# ===== MySQL 配置 =====
export MYSQL_IP="127.0.0.1"
export MYSQL_PORT="3309"
export MYSQL_USER="root"
export MYSQL_PASSWORD="dciworld@iivi"

# ===== 部署配置 =====
export SERVICE_USER="actsvt112"           # 运行服务的用户
export ROOT_DIR="/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT"

# ===== 其他配置 =====
export swVersion="1.0.0-SNAPSHOT"         # 软件版本

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Shanghai2 环境变量已设置"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  myIp: ${myIp}"
echo "  SERVER_PORT: ${SERVER_PORT}"
echo "  KAFKA_BROKER: ${KAFKA_BROKER}"
echo "  MONGO_SERVERS: ${MONGO_SERVERS}"
echo "  MONGO_DATABASE: ${MONGO_DATABASE}"
echo "  MONGO_USER: ${MONGO_USER}"
echo "  pmcZooKeepers: ${pmcZooKeepers}"
echo "  NAMESPACE: ${NAMESPACE}"
echo "  SERVICE_USER: ${SERVICE_USER}"
echo "  ROOT_DIR: ${ROOT_DIR}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
