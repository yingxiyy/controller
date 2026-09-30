#!/bin/bash
##############################################################################
# 环境变量配置 - 自动生成
# 生成时间: 2025-12-08 09:39:10
# 参考服务: adapter-1.0.0-SNAPSHOT
# SSH来源: actsvt112@116.128.204.12:13114
##############################################################################

# ===== 网络配置 =====
export myIp="0.0.0.0"                    # 服务注册IP（0.0.0.0 表示监听所有接口）
export SERVER_PORT="18008"                # 服务端口

# ===== Kafka 配置 =====
# 默认使用本地端口转发地址
export KAFKA_BROKER="127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"
# 其他可用的 Kafka 地址（如需切换，取消注释并注释上面的行）:
# export KAFKA_BROKER="10.242.111.39:9092"  # 10 个服务使用: 10.242.111.39:9092
# export KAFKA_BROKER="172.18.86.38:9092"  # 4 个服务使用: 172.18.86.38:9092
# export KAFKA_BROKER="10.242.111.32:9092"  # 2 个服务使用: 10.242.111.32:9092

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
export MYSQL_PORT="3309"
export MYSQL_USER="root"
export MYSQL_PASSWORD="dciworld@iivi"

# ===== 部署配置 =====
export SERVICE_USER="actsvt112"
export ROOT_DIR="/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT"

# ===== 其他配置 =====
export swVersion="1.0.0-SNAPSHOT"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 环境变量已设置"
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
