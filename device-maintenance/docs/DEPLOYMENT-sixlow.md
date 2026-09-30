# Device Maintenance 服务部署文档（sixlow 环境）

## 服务信息
- **服务名称**: device-maintenance
- **版本**: 1.0.0-SNAPSHOT
- **主机**: 10.192.230.22 (sixlow)
- **端口**: 18008
- **网关访问**: 116.128.204.12:13668
- **跳板机**: 116.128.204.12:13102 (actsvt / pureg@1n)
- **目标主机**: 10.192.230.22:22 (actsvt111 / pureg123!@#)

## 部署日期
2025-12-08

## ✅ 成功配置（重要！）

### ZooKeeper 配置
```properties
ZOOKEEPER_SERVERS=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
NAMESPACE=clband
```

⚠️ **注意**: 必须使用 2190,2191,2192 端口，不是 2181！  
⚠️ **注意**: NAMESPACE 必须是 clband，不是 sdnotn！

### 服务配置
```properties
myIp=0.0.0.0
hostIp=10.243.67.158
port=18008
MODULE=device-maintenance
sw_version=1.0.0
```

### Kafka 配置
```properties
KAFKA_BROKER=127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094
```

### MySQL 配置
```properties
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3307
MYSQL_DATABASE=sotn
MYSQL_USER=root
MYSQL_PASSWORD=dciworld@iivi
```

### MongoDB 配置
```properties
MONGO_SERVERS=127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012
MONGO_DATABASE=sotn
MONGO_USER=dci
MONGO_PWD=dciworld@iivi
```

## 环境变量文件 (env-sixlow.sh)

```bash
#!/bin/bash

# ================== 服务基本配置 ==================
export myIp="0.0.0.0"
export myPort="18008"
export NAMESPACE="clband"
export swVersion="1.0.0"

# ================== Kafka 配置 ==================
export KAFKA_BROKER="127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"

# ================== MySQL 配置 ==================
export MYSQL_HOST="127.0.0.1"
export MYSQL_PORT="3307"
export MYSQL_DATABASE="sotn"
export MYSQL_USER="root"
export MYSQL_PASSWORD="dciworld@iivi"

# ================== MongoDB 配置 ==================
export MONGO_HOST="127.0.0.1"
export MONGO_PORT="27010"
export MONGO_DATABASE="sotn"
export MONGO_USER="dci"
export MONGO_PWD="dciworld@iivi"
export MONGO_PASSWORD="dciworld@iivi"
export MONGO_SERVERS="127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012"

# ================== ZooKeeper 配置 ==================
export ZK_ADDRESS="127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192"
export ZK_NAMESPACE="clband"
export pmcZooKeepers="127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192"

# ================== 其他配置 ==================
export DEVICE_MAINTENANCE_ENABLED="true"
```

## 部署步骤

### 前提条件
- 已编译生成 `target/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz`
- 可以通过跳板机访问目标服务器

### 1. 上传安装包
```bash
cd /Users/orca/PRODS/devicebe/controller/device-maintenance

# 上传到跳板机
sshpass -p 'pureg@1n' scp -P 13102 \
  target/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz \
  actsvt@116.128.204.12:/tmp/

# 从跳板机传输到 sixlow
sshpass -p 'pureg@1n' ssh -p 13102 actsvt@116.128.204.12 \
  "sshpass -p 'pureg123!@#' scp /tmp/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz \
   actsvt111@10.192.230.22:/tmp/"
```

### 2. 停止旧服务
```bash
sudo systemctl stop device-maintenance.service
```

### 3. 备份旧版本
```bash
cd /home/dci/deploy
sudo mv device-maintenance-1.0.0-SNAPSHOT \
  device-maintenance-1.0.0-SNAPSHOT.backup.$(date +%Y%m%d_%H%M%S)
```

### 4. 解压新版本
```bash
cd /home/dci/deploy
tar -xzf /tmp/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz
```

### 5. 使用环境变量安装（关键步骤！）
```bash
cd /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/bin

# 方式1: 如果 env-sixlow.sh 已存在
source /home/dci/deploy/env-sixlow.sh && sudo -E ./install.sh

# 方式2: 如果需要重新创建环境变量文件，参考上面的 env-sixlow.sh 内容
```

⚠️ **重要**: 
- 必须使用 `source env-sixlow.sh` + `sudo -E ./install.sh`
- `-E` 参数保留环境变量给 sudo

### 6. 修复 systemd 文件路径
install.sh 生成的 systemd 文件路径不正确（ExecStart 指向错误的路径），需要手动修复：

```bash
sudo tee /etc/systemd/system/device-maintenance.service > /dev/null << 'EOF'
[Unit]
Description=Independent microservice for device maintenance operations
After=network.target

[Service]
ExecStart=/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/device-maintenance-1.0.0-SNAPSHOT.jar --spring.profiles.active=production
WorkingDirectory=/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT
SuccessExitStatus=143
Restart=on-failure
RestartSec=20

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
```

### 7. 启动服务
```bash
sudo systemctl start device-maintenance.service
```

### 8. 验证部署

#### 8.1 检查服务状态
```bash
sudo systemctl status device-maintenance.service
```
期望：`Active: active (running)`

#### 8.2 检查端口
```bash
ss -tlnp | grep 18008
```
期望：显示 `LISTEN` 状态

#### 8.3 检查 ZooKeeper 注册
```bash
tail -100 /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log | \
  grep "register.*success\|connectString="
```
期望：
- `connectString=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192`
- `success to register the node :/STATE/device-maintenance/device-maintenance_10.192.230.22`

#### 8.4 检查应用启动
```bash
tail -100 /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log | \
  grep "Started DeviceMaintenanceApplication"
```

#### 8.5 测试 API
```bash
wget -q -O- --header="Content-Type: application/json" --post-data='{}' \
  http://127.0.0.1:18008/restconf/operations/device-maintenance:get-all-devices
```
期望：返回设备列表 JSON

#### 8.6 验证微服务注册
在前端页面查看"控制器组件"列表，确认：
- `device-maintenance_10.192.230.22` 显示为"运行中"
- IP 地址显示为 `10.243.67.158`

## 验证清单
- [ ] Systemd 服务状态为 `active (running)`
- [ ] 端口 18008 监听正常
- [ ] ZooKeeper 注册成功（connectString=127.0.0.1:2190,2191,2192）
- [ ] NAMESPACE 为 clband
- [ ] API 返回正常
- [ ] 微服务列表显示"运行中"状态

## 常见问题

### Q1: 服务显示"已停止"
**原因**: ZooKeeper 配置错误，可能使用了错误的端口（2181）或 NAMESPACE（sdnotn）

**解决**: 
1. 检查 `/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/config/zkclient_conf.properties`
2. 确保：
   - `ZOOKEEPER_SERVERS=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192`
   - `NAMESPACE=clband`
3. 如果不正确，重新运行步骤 5-7

### Q2: systemd 启动失败 (203/EXEC)
**原因**: install.sh 生成的 systemd 文件路径不正确

**错误的路径**: `/device-maintenance-1.0.0-SNAPSHOT.jar`  
**正确的路径**: `/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/device-maintenance-1.0.0-SNAPSHOT.jar`

**解决**: 按照步骤 6 手动修复 systemd 文件

### Q3: API 404 错误
**原因**: 
- 服务未完全启动
- 访问的 batchId 不存在
- 网关配置问题

**解决**: 
1. 等待服务完全启动（约30秒）
2. 检查日志中的 `Started DeviceMaintenanceApplication`
3. 确认访问的 batchId 存在

### Q4: TaskInfo 显示任务刚创建就"已完成"
**状态**: ✅ 已修复（2025-12-08）

**修复内容**: 移除了批次创建通知中的 `successfully=true` 设置

**验证**: 创建备份任务后，状态应显示为 `SCHEDULED`，而不是"已完成"

## 重要文件位置

### 服务器上
- 环境变量: `/home/dci/deploy/env-sixlow.sh`
- 环境变量备份: `/home/dci/deploy/env-sixlow.sh.backup`
- 配置文件目录: `/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/config/`
- 日志文件: `/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log`
- systemd 文件: `/etc/systemd/system/device-maintenance.service`
- 部署文档: `/home/dci/deploy/device-maintenance-DEPLOYMENT.md`

### 本地
- 环境变量模板: `controller/device-maintenance/scripts/manual/env-sixlow.sh`
- 部署文档: `controller/device-maintenance/DEPLOYMENT-sixlow.md`

## 快速部署脚本

创建 `deploy-to-sixlow.sh` 快速部署脚本：

```bash
#!/bin/bash
set -e

echo "🚀 开始部署 device-maintenance 到 sixlow..."

# 1. 编译
echo "1️⃣ 编译项目..."
mvn clean package -DskipTests

# 2. 上传
echo "2️⃣ 上传安装包..."
sshpass -p 'pureg@1n' scp -P 13102 \
  target/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz \
  actsvt@116.128.204.12:/tmp/

# 3. 部署
echo "3️⃣ 部署到 sixlow..."
sshpass -p 'pureg@1n' ssh -p 13102 actsvt@116.128.204.12 bash << 'OUTER_EOF'
sshpass -p 'pureg123!@#' ssh actsvt111@10.192.230.22 bash << 'INNER_EOF'
# 传输文件
sshpass -p 'pureg@1n' ssh -p 13102 actsvt@116.128.204.12 \
  "sshpass -p 'pureg123!@#' scp /tmp/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz \
   actsvt111@10.192.230.22:/tmp/"

# 停止服务
sudo systemctl stop device-maintenance.service

# 备份旧版本
cd /home/dci/deploy
[ -d device-maintenance-1.0.0-SNAPSHOT ] && \
  sudo mv device-maintenance-1.0.0-SNAPSHOT \
    device-maintenance-1.0.0-SNAPSHOT.backup.$(date +%Y%m%d_%H%M%S)

# 解压新版本
tar -xzf /tmp/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz

# 安装（使用环境变量）
cd device-maintenance-1.0.0-SNAPSHOT/bin
source /home/dci/deploy/env-sixlow.sh
sudo -E ./install.sh

# 修复 systemd 文件
sudo tee /etc/systemd/system/device-maintenance.service > /dev/null << 'SVCEOF'
[Unit]
Description=Independent microservice for device maintenance operations
After=network.target

[Service]
ExecStart=/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/device-maintenance-1.0.0-SNAPSHOT.jar --spring.profiles.active=production
WorkingDirectory=/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT
SuccessExitStatus=143
Restart=on-failure
RestartSec=20

[Install]
WantedBy=multi-user.target
SVCEOF

sudo systemctl daemon-reload
sudo systemctl start device-maintenance.service

echo "✅ 部署完成！"
INNER_EOF
OUTER_EOF

echo "4️⃣ 等待服务启动..."
sleep 30

echo "5️⃣ 验证部署..."
# 这里可以添加验证逻辑

echo "🎉 部署完成！请检查微服务列表确认状态。"
```

## SFTP 配置（重要！）

### SFTP 服务器设置
如果需要配置 SFTP 服务器供设备下载使用：

```bash
# 1. 安装必要工具
sudo apt-get install -y openssh-server sshpass

# 2. 创建 SFTP 根目录
sudo mkdir -p /sftp
sudo chown root:root /sftp
sudo chmod 755 /sftp

# 3. 创建 SFTP 用户
sudo useradd -m -d /sftp/sftpuser -s /usr/sbin/nologin sftpuser
echo "sftpuser:dci123456" | sudo chpasswd

# 4. 设置目录权限（关键！）
sudo chown root:root /sftp/sftpuser        # ← Chroot 要求：root 拥有
sudo chmod 755 /sftp/sftpuser

sudo mkdir -p /sftp/sftpuser/{upload,download,backup}
sudo chown -R sftpuser:sftpuser /sftp/sftpuser/{upload,download,backup}
sudo chmod 755 /sftp/sftpuser/{upload,download,backup}

# 5. 配置 SSHD（关键！）
sudo tee -a /etc/ssh/sshd_config > /dev/null << 'EOF'

# SFTP Chroot Configuration
Match User sftpuser
    ChrootDirectory /sftp/%u           # ← 使用 %u 占位符！
    ForceCommand internal-sftp
    AllowTcpForwarding no
    X11Forwarding no
    PermitTTY no
EOF

# 6. 测试并重启
sudo sshd -t                           # 测试配置
sudo systemctl reload ssh              # 重新加载配置

# 7. 验证
sshpass -p 'dci123456' sftp sftpuser@localhost << 'EOF'
ls /upload
bye
EOF
```

**⚠️ 常见错误**:
- ❌ `ChrootDirectory /sftp` - 路径映射错误
- ✅ `ChrootDirectory /sftp/%u` - 正确配置
- ❌ `/sftp/sftpuser` 所有者是 sftpuser - 违反 chroot 安全要求
- ✅ `/sftp/sftpuser` 所有者是 root - 正确配置

**详细排查文档**: 
- 📖 [TROUBLESHOOTING-SFTP-DOWNLOAD-FAILURE.md](./TROUBLESHOOTING-SFTP-DOWNLOAD-FAILURE.md)

---

## 修复历史
- **2025-12-08**: 修复 TaskInfo 批次创建通知（移除 successfully=true）
- **2025-12-08**: 修正 ZooKeeper 配置（2181 → 2190,2191,2192）
- **2025-12-08**: 修正 NAMESPACE（sdnotn → clband）
- **2025-12-08**: 修复 SFTP ChrootDirectory 配置和目录权限问题
- **2025-12-08**: 修正 myIp 配置（10.192.230.22 → 0.0.0.0，与其他服务保持一致）
- **2025-12-08**: 添加 systemd 文件路径修复步骤

## 相关文档
- [网络备份配置](./NETWORK-BACKUP.md)
- [轮询机制说明](./POLLING-MECHANISM.md)
- [TaskInfo 通知修复](./TASKINFO-FIX.md)

