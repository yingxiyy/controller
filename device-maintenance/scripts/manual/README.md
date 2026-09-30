# Device Maintenance - 手动部署脚本（与标准化部署完全一致）

## 📋 概述

本目录包含与**标准化自动部署完全一致**的手动部署脚本。

手动部署和自动部署（`deploy-shanghai2.sh`）的结果**完全相同**：
- ✅ 使用相同的编译命令
- ✅ 生成相同的 tar.gz 包
- ✅ 部署到相同的目录结构
- ✅ 使用相同的 SystemD 服务配置
- ✅ 使用相同的环境变量配置方式
- ✅ 调用相同的 `install.sh` 脚本

## 📁 文件说明

```
manual/
├── README.md                        # 本文档
├── env-shanghai2.conf               # 上海2环境配置文件（可编辑）
├── shanghai2-manual-deploy.sh       # 上海2环境部署脚本（主脚本）
└── （未来可扩展）
    ├── env-beijing.conf             # 北京环境配置
    ├── beijing-manual-deploy.sh     # 北京环境部署脚本
    └── ...
```

## 🚀 快速开始

### 1️⃣ 使用默认配置部署（上海2环境）

```bash
cd /path/to/device-maintenance
./scripts/manual/shanghai2-manual-deploy.sh
```

脚本会自动：
1. ✅ 读取 `env-shanghai2.conf` 配置
2. ✅ 编译打包（`mvn clean package -DskipTests`）
3. ✅ 上传到远程服务器
4. ✅ 停止旧服务
5. ✅ 解压安装包
6. ✅ 创建环境配置文件
7. ✅ 运行标准 `install.sh`
8. ✅ 验证部署结果

### 2️⃣ 使用自定义配置文件

```bash
# 复制配置模板
cp scripts/manual/env-shanghai2.conf /tmp/my-custom-env.conf

# 编辑配置
vim /tmp/my-custom-env.conf

# 使用自定义配置部署
./scripts/manual/shanghai2-manual-deploy.sh /tmp/my-custom-env.conf
```

## ⚙️ 配置文件说明

### 配置文件格式（`env-shanghai2.conf`）

```bash
# 服务器连接配置
SERVER_HOST="116.128.204.12"        # 目标服务器IP
SERVER_USER="actsvt112"             # SSH用户名
SERVER_PASSWORD="pureg123!@#"       # SSH密码
SSH_PORT="13112"                    # SSH端口

# 部署配置
DEPLOY_BASE="/opt/dci/deploy"       # 部署基础目录
APP_NAME="device-maintenance"       # 应用名称
VERSION="1.0.0-SNAPSHOT"            # 应用版本

# 应用运行时配置（传递给 install.sh）
myIp="192.168.3.112"                # 服务注册IP
NAMESPACE="dciworld/clband"         # ZK命名空间 ⭐ 上海2专用

# 数据库、中间件配置
MYSQL_IP="127.0.0.1"
MYSQL_PORT="3309"
MONGO_SERVERS="127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012"
KAFKA_BROKER="127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"
pmcZooKeepers="127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192"
# ... 更多配置见配置文件
```

### 配置项说明

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `SERVER_HOST` | 目标服务器IP/域名 | `116.128.204.12` |
| `SERVER_USER` | SSH登录用户 | `actsvt112` |
| `SERVER_PASSWORD` | SSH密码（sudo密码） | `pureg123!@#` |
| `SSH_PORT` | SSH端口 | `13112` |
| `DEPLOY_BASE` | 部署基础目录 | `/opt/dci/deploy` |
| `myIp` | 服务注册到ZK的IP | `192.168.3.112` |
| `NAMESPACE` | ZK命名空间 ⭐ | `dciworld/clband` (上海2)<br>`dciworld/default` (其他) |
| `MYSQL_IP` | MySQL服务器IP | `127.0.0.1` |
| `MONGO_SERVERS` | MongoDB服务器列表 | `127.0.0.1:27010,...` |
| `KAFKA_BROKER` | Kafka Broker列表 | `127.0.0.1:9092,...` |
| `pmcZooKeepers` | ZooKeeper服务器列表 | `127.0.0.1:2190,...` |

## 🔍 配置方式对比

### ❌ 旧方式（不推荐）

```
静态配置写在脚本里 → 难以修改 → 不同环境需要不同脚本
```

### ✅ 新方式（推荐）

```
配置文件 → 脚本读取 → 同一个脚本 + 不同配置文件 = 支持多环境
```

**优点：**
- ✅ 配置和逻辑分离
- ✅ 易于维护和修改
- ✅ 支持多环境（只需创建不同的配置文件）
- ✅ 配置可版本控制（敏感信息除外）

## 📊 部署流程详解

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 加载配置文件                                              │
│    - 读取 env-shanghai2.conf                                │
│    - 验证必需配置项                                          │
│    - 导出环境变量                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. 编译打包                                                  │
│    - mvn clean package -DskipTests                          │
│    - 生成 tar.gz（包含 install.sh）                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. 上传到远程服务器                                          │
│    - scp tar.gz → /tmp/                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. 停止旧服务（如果存在）                                    │
│    - systemctl stop Device\ Maintenance\ Microservice       │
│    - pkill 手动启动的进程                                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. 准备部署目录                                              │
│    - 备份/删除旧目录                                         │
│    - 创建新部署目录                                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. 解压安装包                                                │
│    - tar -xzf → /opt/dci/deploy/Device Maintenance...       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. 创建环境配置文件                                          │
│    - 生成 env-shanghai2.sh（在远程服务器）                  │
│    - 包含所有环境变量（从配置文件读取）                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. 运行标准 install.sh                                       │
│    - source env-shanghai2.sh                                │
│    - ./bin/install.sh                                       │
│    - install.sh 使用 sed 替换配置文件:                      │
│      * zkclient_conf.properties (NAMESPACE=dciworld/clband) │
│      * mongodb.properties                                   │
│      * mysql.properties                                     │
│    - 复制 systemd service 文件                              │
│    - systemctl enable + restart                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. 验证部署                                                  │
│    - systemctl status                                       │
│    - curl health check                                      │
│    - 检查 ZK namespace                                       │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 常见操作

### 修改配置后重新部署

```bash
# 1. 编辑配置文件
vim scripts/manual/env-shanghai2.conf

# 2. 重新部署（会自动停止旧服务）
./scripts/manual/shanghai2-manual-deploy.sh
```

### 部署到不同环境

```bash
# 方式1: 创建新的配置文件
cp scripts/manual/env-shanghai2.conf scripts/manual/env-beijing.conf
vim scripts/manual/env-beijing.conf  # 修改配置

# 方式2: 使用自定义配置部署
./scripts/manual/shanghai2-manual-deploy.sh scripts/manual/env-beijing.conf
```

### 查看部署结果

```bash
# SSH登录到服务器
ssh -p 13112 actsvt112@116.128.204.12

# 查看服务状态
sudo systemctl status "Device Maintenance Microservice"

# 查看配置文件
cat /opt/dci/deploy/Device\ Maintenance\ Microservice-1.0.0-SNAPSHOT/config/zkclient_conf.properties | grep NAMESPACE

# 查看日志
sudo journalctl -u "Device Maintenance Microservice" -f
```

## ⚠️ 重要说明

### 手动部署 vs 自动部署

| 特性 | 手动部署（本脚本） | 自动部署（deploy-shanghai2.sh） |
|------|-------------------|--------------------------------|
| **编译命令** | ✅ `mvn clean package -DskipTests` | ✅ `mvn clean package -DskipTests` |
| **打包方式** | ✅ tar.gz（含install.sh） | ✅ tar.gz（含install.sh） |
| **配置方式** | ✅ 配置文件（可编辑） | ❌ 硬编码在脚本中 |
| **部署目录** | ✅ `/opt/dci/deploy/Device Maintenance...` | ✅ `/opt/dci/deploy/Device Maintenance...` |
| **SystemD服务** | ✅ 自动安装 | ✅ 自动安装 |
| **ZK Namespace** | ✅ `dciworld/clband` | ✅ `dciworld/clband` |
| **灵活性** | ⭐⭐⭐⭐⭐ 高（可自定义配置） | ⭐⭐⭐ 中（需修改脚本） |
| **交互性** | ⭐⭐⭐⭐⭐ 高（有确认提示） | ⭐⭐⭐ 低（自动执行） |

### 与旧的 shanghai2-manager.sh 的区别

| 维度 | 新手动部署（本脚本） | 旧脚本（shanghai2-manager.sh） |
|------|---------------------|------------------------------|
| **部署方式** | ✅ 标准化（tar.gz + install.sh） | ❌ 非标准（只上传jar） |
| **配置来源** | ✅ 配置文件 | ❌ 硬编码 |
| **SystemD** | ✅ 支持 | ❌ 不支持 |
| **部署结果** | ✅ 与标准部署一致 | ❌ 不一致 |
| **推荐度** | ⭐⭐⭐⭐⭐ | ⭐ |

## 🎯 最佳实践

1. **✅ 使用配置文件**
   ```bash
   # 为每个环境创建独立的配置文件
   scripts/manual/
   ├── env-shanghai.conf
   ├── env-shanghai2.conf
   ├── env-beijing.conf
   └── env-production.conf
   ```

2. **✅ 配置文件版本控制**
   ```bash
   # 敏感信息使用环境变量或密钥管理
   SERVER_PASSWORD="${SERVER_PASSWORD:-default_password}"
   MYSQL_PASSWORD="${MYSQL_PASSWORD:-default_password}"
   ```

3. **✅ 部署前检查**
   ```bash
   # 脚本会自动检查必需的配置项
   # 脚本会显示部署配置供确认
   # 脚本会询问是否备份旧部署
   ```

4. **✅ 部署后验证**
   ```bash
   # 脚本会自动执行验证步骤：
   # - SystemD服务状态
   # - 进程检查
   # - 端口监听检查
   # - 配置文件检查
   # - 健康检查API
   # - ZooKeeper连接检查
   ```

## 🔗 相关文档

- [部署指南](../../docs/DEPLOYMENT_GUIDE.md)
- [环境配置](../../docs/ENVIRONMENT_SETUP_COMPLETE.md)
- [标准化打包](../../docs/ASSEMBLY_PACKAGING.md)

## 📞 问题反馈

如有问题，请检查：
1. 配置文件是否正确
2. 服务器是否可连接
3. 依赖是否已安装（sshpass, expect）
4. 日志输出中的错误信息

---

**最后更新**: 2025-11-28
**版本**: 1.0.0

