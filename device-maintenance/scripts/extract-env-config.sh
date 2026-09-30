#!/bin/bash
##############################################################################
# 自动提取环境配置脚本
# 
# 功能：从已部署的服务中提取配置信息，生成 env-{环境名}.sh 文件
# 
# 使用方法：
#   ./extract-env-config.sh <SSH_HOST> <SSH_PORT> <SSH_USER> <SSH_PASSWORD> [REFERENCE_SERVICE] [OUTPUT_FILE]
#
# 示例：
#   ./extract-env-config.sh 116.128.204.12 13112 actsvt112 'password' adapter-1.0.0-SNAPSHOT env-shanghai2.sh
#
# 依赖：sshpass（用于非交互式SSH）
##############################################################################

set -e

# 参数检查
if [ $# -lt 4 ]; then
    echo "用法: $0 <SSH_HOST> <SSH_PORT> <SSH_USER> <SSH_PASSWORD> [REFERENCE_SERVICE] [OUTPUT_FILE]"
    echo ""
    echo "参数说明："
    echo "  SSH_HOST         - SSH服务器地址"
    echo "  SSH_PORT         - SSH端口"
    echo "  SSH_USER         - SSH用户名"
    echo "  SSH_PASSWORD     - SSH密码"
    echo "  REFERENCE_SERVICE - 参考服务名（默认: adapter-1.0.0-SNAPSHOT）"
    echo "  OUTPUT_FILE      - 输出文件名（默认: env-extracted.sh）"
    echo ""
    echo "示例："
    echo "  $0 116.128.204.12 13112 actsvt112 'mypassword'"
    echo "  $0 116.128.204.12 13112 actsvt112 'mypassword' adapter-1.0.0-SNAPSHOT env-shanghai2.sh"
    exit 1
fi

SSH_HOST=$1
SSH_PORT=$2
SSH_USER=$3
SSH_PASSWORD=$4
REFERENCE_SERVICE=${5:-"adapter-1.0.0-SNAPSHOT"}
OUTPUT_FILE=${6:-"env-extracted.sh"}

DEPLOY_BASE="/home/dci/deploy"
CONFIG_DIR="${DEPLOY_BASE}/${REFERENCE_SERVICE}/config"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 开始提取环境配置"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  SSH目标: ${SSH_USER}@${SSH_HOST}:${SSH_PORT}"
echo "  参考服务: ${REFERENCE_SERVICE}"
echo "  输出文件: ${OUTPUT_FILE}"
echo ""

# SSH命令前缀
SSH_CMD="sshpass -p '${SSH_PASSWORD}' ssh -p ${SSH_PORT} -o StrictHostKeyChecking=no ${SSH_USER}@${SSH_HOST}"

# 检查参考服务是否存在
echo "📁 检查参考服务目录..."
if ! eval "${SSH_CMD} 'test -d ${CONFIG_DIR}'"; then
    echo "❌ 错误: 参考服务目录不存在: ${CONFIG_DIR}"
    echo ""
    echo "可用的服务列表:"
    eval "${SSH_CMD} 'ls -1 ${DEPLOY_BASE} | grep -E \".*-SNAPSHOT$\"'"
    exit 1
fi
echo "  ✅ 找到配置目录: ${CONFIG_DIR}"
echo ""

# 提取ZooKeeper配置
echo "🔧 提取 ZooKeeper 配置..."
ZK_CONFIG=$(eval "${SSH_CMD} 'cat ${CONFIG_DIR}/zkclient_conf.properties 2>/dev/null || echo \"\"'")
ZOOKEEPER_SERVERS=$(echo "$ZK_CONFIG" | grep "^ZOOKEEPER_SERVERS=" | cut -d'=' -f2)
NAMESPACE=$(echo "$ZK_CONFIG" | grep "^NAMESPACE=" | cut -d'=' -f2)
echo "  ZOOKEEPER_SERVERS: ${ZOOKEEPER_SERVERS}"
echo "  NAMESPACE: ${NAMESPACE}"

# 提取MongoDB配置
echo ""
echo "🔧 提取 MongoDB 配置..."
MONGO_CONFIG=$(eval "${SSH_CMD} 'cat ${CONFIG_DIR}/mongodb.properties 2>/dev/null || echo \"\"'")
MONGO_SERVERS=$(echo "$MONGO_CONFIG" | grep "^mongodb.servers=" | cut -d'=' -f2)
MONGO_DATABASE=$(echo "$MONGO_CONFIG" | grep "^mongodb.database=" | cut -d'=' -f2)
MONGO_USER=$(echo "$MONGO_CONFIG" | grep "^mongodb.user=" | cut -d'=' -f2)
MONGO_PWD=$(echo "$MONGO_CONFIG" | grep "^mongodb.pwd=" | cut -d'=' -f2)
echo "  MONGO_SERVERS: ${MONGO_SERVERS}"
echo "  MONGO_DATABASE: ${MONGO_DATABASE}"
echo "  MONGO_USER: ${MONGO_USER}"
echo "  MONGO_PWD: ${MONGO_PWD:0:3}***"

# 提取MySQL配置
echo ""
echo "🔧 提取 MySQL 配置..."
MYSQL_CONFIG=$(eval "${SSH_CMD} 'cat ${CONFIG_DIR}/mysql.properties 2>/dev/null || echo \"\"'")
MYSQL_URL=$(echo "$MYSQL_CONFIG" | grep "^spring.datasource.url=" | cut -d'=' -f2-)
MYSQL_USER=$(echo "$MYSQL_CONFIG" | grep "^spring.datasource.username=" | cut -d'=' -f2)
MYSQL_PASSWORD=$(echo "$MYSQL_CONFIG" | grep "^spring.datasource.password=" | cut -d'=' -f2)

# 解析MySQL URL获取IP和端口
if [ -n "$MYSQL_URL" ]; then
    # jdbc:mysql://127.0.0.1:3309/sotn?... -> 127.0.0.1:3309
    MYSQL_HOST_PORT=$(echo "$MYSQL_URL" | sed -n 's/.*\/\/\([^\/]*\)\/.*/\1/p')
    MYSQL_IP=$(echo "$MYSQL_HOST_PORT" | cut -d':' -f1)
    MYSQL_PORT=$(echo "$MYSQL_HOST_PORT" | cut -d':' -f2)
fi
echo "  MYSQL_IP: ${MYSQL_IP}"
echo "  MYSQL_PORT: ${MYSQL_PORT}"
echo "  MYSQL_USER: ${MYSQL_USER}"
echo "  MYSQL_PASSWORD: ${MYSQL_PASSWORD:0:3}***"

# 提取Kafka配置（默认使用本地地址，收集其他可选地址）
echo ""
echo "🔧 提取 Kafka 配置..."
echo "  扫描所有服务的 Kafka 配置..."

# 默认使用本地地址
KAFKA_BROKER="127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"

# 收集所有外部Kafka地址（用于注释）
KAFKA_ALTERNATIVES=$(eval "${SSH_CMD} '
for dir in ${DEPLOY_BASE}/*-SNAPSHOT/config; do
    grep -h \"bootstrap-servers:\" \$dir/application.yml 2>/dev/null | head -1 | sed \"s/.*bootstrap-servers://\" | tr -d \" \"
done | grep -v \"^$\" | grep -v \"127.0.0.1\" | grep -v \"localhost\" | sort | uniq -c | sort -rn | awk \"{print \\\$1 \\\" 个服务使用: \\\" \\\$2}\"
'")

# 显示所有发现的Kafka配置
echo "  发现的 Kafka 配置分布:"
eval "${SSH_CMD} '
for dir in ${DEPLOY_BASE}/*-SNAPSHOT/config; do
    kafka=\$(grep \"bootstrap-servers:\" \$dir/application.yml 2>/dev/null | head -1 | sed \"s/.*bootstrap-servers://\" | tr -d \" \")
    if [ -n \"\$kafka\" ]; then
        echo \"    \$kafka\"
    fi
done | sort | uniq -c | sort -rn
'"
echo "  ✅ 默认使用本地地址: ${KAFKA_BROKER}"

# 生成输出文件
echo ""
echo "📝 生成配置文件: ${OUTPUT_FILE}"
cat > "${OUTPUT_FILE}" << EOF
#!/bin/bash
##############################################################################
# 环境变量配置 - 自动生成
# 生成时间: $(date '+%Y-%m-%d %H:%M:%S')
# 参考服务: ${REFERENCE_SERVICE}
# SSH来源: ${SSH_USER}@${SSH_HOST}:${SSH_PORT}
##############################################################################

# ===== 网络配置 =====
export myIp="0.0.0.0"                    # 服务注册IP（0.0.0.0 表示监听所有接口）
export SERVER_PORT="18008"                # 服务端口

# ===== Kafka 配置 =====
# 默认使用本地端口转发地址
export KAFKA_BROKER="${KAFKA_BROKER}"
# 其他可用的 Kafka 地址（如需切换，取消注释并注释上面的行）:
EOF

# 添加可选的Kafka地址作为注释
if [ -n "$KAFKA_ALTERNATIVES" ]; then
    echo "$KAFKA_ALTERNATIVES" | while read line; do
        echo "# export KAFKA_BROKER=\"$(echo $line | awk '{print $NF}')\"  # $line" >> "${OUTPUT_FILE}"
    done
fi

cat >> "${OUTPUT_FILE}" << EOF

# ===== MongoDB 配置 =====
export MONGO_SERVERS="${MONGO_SERVERS}"
export MONGO_DATABASE="${MONGO_DATABASE}"
export MONGO_USER="${MONGO_USER}"
export MONGO_PWD="${MONGO_PWD}"

# ===== ZooKeeper 配置 =====
export pmcZooKeepers="${ZOOKEEPER_SERVERS}"
export NAMESPACE="${NAMESPACE}"

# ===== MySQL 配置 =====
export MYSQL_IP="${MYSQL_IP}"
export MYSQL_PORT="${MYSQL_PORT}"
export MYSQL_USER="${MYSQL_USER}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD}"

# ===== 部署配置 =====
export SERVICE_USER="${SSH_USER}"
export ROOT_DIR="/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT"

# ===== 其他配置 =====
export swVersion="1.0.0-SNAPSHOT"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 环境变量已设置"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  myIp: \${myIp}"
echo "  SERVER_PORT: \${SERVER_PORT}"
echo "  KAFKA_BROKER: \${KAFKA_BROKER}"
echo "  MONGO_SERVERS: \${MONGO_SERVERS}"
echo "  MONGO_DATABASE: \${MONGO_DATABASE}"
echo "  MONGO_USER: \${MONGO_USER}"
echo "  pmcZooKeepers: \${pmcZooKeepers}"
echo "  NAMESPACE: \${NAMESPACE}"
echo "  SERVICE_USER: \${SERVICE_USER}"
echo "  ROOT_DIR: \${ROOT_DIR}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
EOF

chmod +x "${OUTPUT_FILE}"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 配置提取完成!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "输出文件: ${OUTPUT_FILE}"
echo ""
echo "使用方法:"
echo "  1. 复制到服务器: scp -P ${SSH_PORT} ${OUTPUT_FILE} ${SSH_USER}@${SSH_HOST}:${DEPLOY_BASE}/device-maintenance-1.0.0-SNAPSHOT/bin/"
echo "  2. 在服务器上执行安装:"
echo "     cd ${DEPLOY_BASE}/device-maintenance-1.0.0-SNAPSHOT/bin"
echo "     source ${OUTPUT_FILE}"
echo "     export myIp=\"实际IP地址\"  # 如果需要指定IP"
echo "     sudo -E bash install.sh"
echo ""

