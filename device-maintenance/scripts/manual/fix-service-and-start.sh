#!/bin/bash
# 修复 service 文件并启动服务

DEPLOY_DIR="/opt/dci/deploy/device-maintenance-1.0.0-SNAPSHOT"

echo "修复 service 文件..."
cat > /etc/systemd/system/device-maintenance.service << EOF
[Unit]
Description=Independent microservice for device maintenance operations
After=network.target

[Service]
Type=simple
User=actsvt112
Group=actsvt112
ExecStart=/usr/bin/java -jar ${DEPLOY_DIR}/device-maintenance-1.0.0-SNAPSHOT.jar --spring.profiles.active=production --spring.config.additional-location=file:${DEPLOY_DIR}/config/
WorkingDirectory=${DEPLOY_DIR}
Environment="LOG_HOME=${DEPLOY_DIR}/logs"
SuccessExitStatus=143
Restart=on-failure
RestartSec=20
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo "重新加载 systemd..."
systemctl daemon-reload

echo "启动服务..."
systemctl restart device-maintenance

echo "等待5秒..."
sleep 5

echo "检查服务状态..."
systemctl status device-maintenance --no-pager -l

echo ""
echo "检查配置..."
grep "^NAMESPACE=" ${DEPLOY_DIR}/config/zkclient_conf.properties

echo ""
echo "检查健康..."
sleep 5
curl -s http://localhost:18008/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:18008/actuator/health

