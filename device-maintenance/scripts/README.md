# Device Maintenance 部署脚本说明

## 📋 目录结构

```
scripts/
├── README.md                           # 本文件
├── deploy-shanghai2.sh                 # 上海2环境一键部署脚本（推荐）
├── shanghai2-manager.sh                # 上海2环境管理脚本（旧版，保留用于兼容）
├── shanghai-manager.sh                 # 上海环境管理脚本（旧版，保留用于兼容）
├── device-maintenance-manager.sh       # 环境选择器（旧版）
├── server-manager.sh                   # 服务器端管理脚本（旧版）
├── config/
│   └── server-shanghai2-env.properties # 上海2环境配置（旧版）
└── lib/
    └── config-loader.sh                # 配置加载库（旧版）
```

---

## 🚀 推荐部署方式（标准化）

### 方式1：使用一键部署脚本（最简单）

适用于从本地机器部署到远程服务器。

```bash
# 直接运行
./scripts/deploy-shanghai2.sh
```

**这个脚本会自动完成：**
1. ✅ 编译打包
2. ✅ 上传到远程服务器
3. ✅ 解压
4. ✅ 创建环境配置
5. ✅ 运行 `install.sh` 安装
6. ✅ 验证部署

---

### 方式2：手动部署（标准install.sh方式）

适用于直接在服务器上操作。

#### **步骤1：编译打包**
```bash
cd /path/to/devicebe/controller/device-maintenance
mvn clean package -DskipTests
```

#### **步骤2：上传到服务器**
```bash
scp target/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz \
    user@server:/path/to/deploy/
```

#### **步骤3：在服务器上解压**
```bash
ssh user@server
cd /path/to/deploy
tar -xzf device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz
cd device-maintenance-1.0.0-SNAPSHOT
```

#### **步骤4：配置环境变量**

创建环境配置文件：
```bash
vi env-shanghai2.sh
```

写入配置（参考下面的环境配置示例）：
```bash
#!/bin/bash
export myIp=192.168.3.112
export MYSQL_IP=127.0.0.1
export MYSQL_PORT=3309
export MYSQL_USER=root
export MYSQL_PASSWORD='dciworld@iivi'
export MONGO_SERVERS=127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012
export MONGO_DATABASE=sotn
export MONGO_USER=dci
export MONGO_PWD='dciworld@iivi'
export KAFKA_BROKER=127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094
export pmcZooKeepers=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
export NAMESPACE=dciworld/clband
```

#### **步骤5：运行安装脚本**
```bash
source env-shanghai2.sh
./bin/install.sh
```

#### **步骤6：验证部署**
```bash
# 检查服务状态
sudo systemctl status device-maintenance

# 检查健康接口
curl http://localhost:18008/actuator/health
curl http://localhost:18008/api/health/zookeeper
```

---

## 📝 环境配置示例

### 上海环境（Shanghai）
```bash
export myIp=192.168.3.206
export MYSQL_IP=127.0.0.1
export MYSQL_PORT=3308
export MYSQL_USER=root
export MYSQL_PASSWORD='dciworld@2025'
export MONGO_SERVERS=127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012,127.0.0.1:27013,127.0.0.1:27014
export MONGO_DATABASE=sotn
export MONGO_USER=dciworld
export MONGO_PWD='dciworld@2025'
export KAFKA_BROKER=127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094,127.0.0.1:9095,127.0.0.1:9096
export pmcZooKeepers=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192,127.0.0.1:2193,127.0.0.1:2194
export NAMESPACE=dciworld/default
```

### 上海2环境（Shanghai2）
```bash
export myIp=192.168.3.112
export MYSQL_IP=127.0.0.1
export MYSQL_PORT=3309
export MYSQL_USER=root
export MYSQL_PASSWORD='dciworld@iivi'
export MONGO_SERVERS=127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012
export MONGO_DATABASE=sotn
export MONGO_USER=dci
export MONGO_PWD='dciworld@iivi'
export KAFKA_BROKER=127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094
export pmcZooKeepers=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
export NAMESPACE=dciworld/clband
```

---

## 🔧 服务管理命令

```bash
# 启动服务
sudo systemctl start device-maintenance

# 停止服务
sudo systemctl stop device-maintenance

# 重启服务
sudo systemctl restart device-maintenance

# 查看状态
sudo systemctl status device-maintenance

# 查看日志
sudo journalctl -u device-maintenance -f

# 查看最近100行日志
sudo journalctl -u device-maintenance -n 100

# 禁用开机自启
sudo systemctl disable device-maintenance

# 启用开机自启
sudo systemctl enable device-maintenance
```

---

## 🗑️ 卸载服务

   ```bash
cd /path/to/deploy/device-maintenance-1.0.0-SNAPSHOT
./bin/uninstall.sh
```

---

## ⚠️ 旧版脚本说明

以下脚本为旧版部署方式，保留用于向后兼容，**不推荐新部署使用**：

| 脚本 | 说明 | 状态 |
|------|------|------|
| `shanghai2-manager.sh` | 上海2环境管理脚本 | ⚠️ 已废弃，建议使用 `deploy-shanghai2.sh` |
| `shanghai-manager.sh` | 上海环境管理脚本 | ⚠️ 已废弃 |
| `device-maintenance-manager.sh` | 环境选择器 | ⚠️ 已废弃 |
| `server-manager.sh` | 服务器端管理脚本 | ⚠️ 已废弃 |

**旧版脚本的问题：**
- ❌ 使用 Spring Profiles 管理多环境（复杂）
- ❌ 需要维护多套配置文件
- ❌ 与其他微服务部署方式不一致
- ❌ 不支持标准 SystemD 服务管理

---

## 🆚 新旧部署方式对比

| 特性 | 旧方式 | 新方式（标准化） |
|------|--------|----------------|
| **配置方式** | Spring Profiles + 多套YAML | 环境变量 + 单套YAML |
| **部署脚本** | 自定义脚本（多套） | 标准 install.sh |
| **环境切换** | 修改 `--spring.profiles.active` | 修改环境变量 |
| **服务管理** | 自定义启停脚本 | SystemD 标准命令 |
| **与其他微服务一致性** | ❌ 不一致 | ✅ 完全一致 |
| **运维友好度** | 中等 | ✅ 高 |

---

## 📚 相关文档

- [部署指南](../docs/DEPLOYMENT_GUIDE.md) - 完整的部署说明
- [StandardD配置](../docs/SYSTEMD_SERVICE.md) - SystemD 服务配置说明
- [环境配置](../docs/ENVIRONMENT_CONFIG.md) - 环境变量配置详解

---

## 🆘 故障排查

### 问题1：服务启动失败

```bash
# 查看详细日志
sudo journalctl -u device-maintenance -n 100 --no-pager

# 检查配置文件
ls -la config/

# 检查端口占用
sudo netstat -tuln | grep 18008
```

### 问题2：数据库连接失败

```bash
# 检查MySQL配置
cat config/mysql.properties | grep datasource

# 测试数据库连接
mysql -h 127.0.0.1 -P 3309 -u root -p
```

### 问题3：ZooKeeper注册失败

```bash
# 检查ZooKeeper配置
cat config/zkclient_conf.properties

# 测试ZooKeeper连接
echo stat | nc 127.0.0.1 2190

# 检查服务注册
curl http://localhost:18008/api/health/zookeeper
```

---

**版本**: 2.0.0 (标准化版本)  
**更新时间**: 2025-11-28  
**维护者**: Device Maintenance Team
