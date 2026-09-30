# Device Maintenance Service - 部署指南

## 📋 目录

- [概述](#概述)
- [部署架构](#部署架构)
- [环境准备](#环境准备)
- [配置说明](#配置说明)
- [部署步骤](#部署步骤)
- [服务管理](#服务管理)
- [故障排查](#故障排查)

---

## 概述

`device-maintenance` 微服务采用**标准化部署方式**，与其他微服务（如 `neMgr`、`taskInfo`）保持一致：

- ✅ 使用 `install.sh` 脚本进行自动化部署
- ✅ 通过环境变量配置，无需维护多套配置文件
- ✅ 支持 SystemD 服务管理
- ✅ 配置文件外置，便于运维调整

---

## 部署架构

### 打包结构

```
device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz
├── device-maintenance-1.0.0-SNAPSHOT.jar    # 可执行JAR
├── device-maintenance-1.0.0-SNAPSHOT.conf   # 服务配置
├── config/                                  # 外部配置目录
│   ├── application.yml                      # 主配置文件
│   ├── logback-spring.xml                   # 日志配置
│   ├── zkclient_conf.properties             # ZooKeeper配置
│   ├── mongodb.properties                   # MongoDB配置
│   └── mysql.properties                     # MySQL配置
└── bin/                                     # 脚本目录
    ├── install.sh                           # 安装脚本
    ├── uninstall.sh                         # 卸载脚本
    └── device-maintenance.service           # SystemD服务文件
```

### 配置加载顺序

```
命令行参数 (最高优先级)
   ↓
外部配置文件 (/path/to/deploy/config/*.yml)
   ↓
JAR内置配置 (BOOT-INF/classes/*.yml)
   ↓
默认值 (最低优先级)
```

---

## 环境准备

### 系统要求

- **操作系统**: Linux (推荐 Ubuntu 20.04+, CentOS 7+)
- **Java**: JDK 11+
- **SystemD**: 用于服务管理
- **权限**: 需要 sudo 权限（用于安装 systemd 服务）

### 依赖服务

确保以下服务已部署并可访问：

| 服务 | 默认端口 | 说明 |
|------|---------|------|
| **MySQL** | 3309 | 关系型数据库 |
| **MongoDB** | 27010-27012 | NoSQL数据库（副本集） |
| **Kafka** | 9092-9094 | 消息队列 |
| **ZooKeeper** | 2190-2192 | 服务注册与发现 |

---

## 配置说明

### 必需环境变量

在运行 `install.sh` 前，必须设置以下环境变量：

```bash
# ============================================================================
# 服务器信息
# ============================================================================
export myIp=192.168.3.112                    # 本机IP（用于服务注册）

# ============================================================================
# MySQL 配置
# ============================================================================
export MYSQL_IP=127.0.0.1                    # MySQL服务器IP
export MYSQL_PORT=3309                       # MySQL端口
export MYSQL_USER=root                       # MySQL用户名
export MYSQL_PASSWORD='dciworld@iivi'        # MySQL密码

# ============================================================================
# MongoDB 配置
# ============================================================================
export MONGO_SERVERS=127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012
export MONGO_DATABASE=sotn                   # 数据库名
export MONGO_USER=dci                        # 用户名
export MONGO_PWD='dciworld@iivi'             # 密码

# ============================================================================
# Kafka 配置
# ============================================================================
export KAFKA_BROKER=127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094

# ============================================================================
# ZooKeeper 配置
# ============================================================================
export pmcZooKeepers=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
export NAMESPACE=dciworld/clband             # ZooKeeper命名空间
```

### 环境配置示例

#### 上海环境（Shanghai）
```bash
#!/bin/bash
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

#### 上海2环境（Shanghai2）
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

---

## 部署步骤

### 1. 编译打包

```bash
cd /path/to/devicebe/controller/device-maintenance
mvn clean package -DskipTests
```

生成的文件：`target/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz`

### 2. 上传到目标服务器

```bash
scp target/device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz user@server:/path/to/deploy/
```

### 3. 解压

```bash
cd /path/to/deploy
tar -xzf device-maintenance-1.0.0-SNAPSHOT-bin.tar.gz
cd device-maintenance-1.0.0-SNAPSHOT
```

### 4. 配置环境变量

创建环境配置文件（或直接 export）：

```bash
# 创建环境配置文件
vi env-shanghai2.sh

# 写入环境变量（参考上面的配置示例）
# ...

# 加载环境变量
source env-shanghai2.sh
```

### 5. 运行安装脚本

```bash
./bin/install.sh
```

安装脚本会自动：
1. ✅ 验证所有必需的环境变量
2. ✅ 生成配置文件（替换占位符）
3. ✅ 安装 SystemD 服务
4. ✅ 启动服务

### 6. 验证部署

```bash
# 检查服务状态
sudo systemctl status device-maintenance

# 检查服务健康
curl http://localhost:18008/actuator/health

# 检查ZooKeeper注册
curl http://localhost:18008/api/health/zookeeper
```

---

## 服务管理

### SystemD 命令

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

### 卸载服务

```bash
cd /path/to/deploy/device-maintenance-1.0.0-SNAPSHOT
./bin/uninstall.sh
```

---

## 故障排查

### 常见问题

#### 1. 服务启动失败

**症状**: `systemctl status` 显示 `failed` 或 `inactive`

**排查步骤**:
```bash
# 查看详细日志
sudo journalctl -u device-maintenance -n 100 --no-pager

# 检查配置文件
ls -la config/

# 检查端口占用
sudo netstat -tuln | grep 18008
```

**常见原因**:
- ✗ 端口被占用
- ✗ 数据库连接失败
- ✗ ZooKeeper连接失败
- ✗ 配置文件格式错误

#### 2. 数据库连接失败

**症状**: 日志中出现 `Access denied` 或 `Connection refused`

**排查步骤**:
```bash
# 检查MySQL配置
cat config/mysql.properties | grep datasource

# 测试数据库连接
mysql -h 127.0.0.1 -P 3309 -u root -p
```

**解决方案**:
- 确认 MySQL 用户权限
- 确认密码正确（特殊字符需转义）
- 确认防火墙规则

#### 3. ZooKeeper注册失败

**症状**: 服务启动成功但无法被其他服务发现

**排查步骤**:
```bash
# 检查ZooKeeper配置
cat config/zkclient_conf.properties

# 测试ZooKeeper连接
echo stat | nc 127.0.0.1 2190

# 检查ZooKeeper命名空间
curl http://localhost:18008/api/health/zookeeper
```

**解决方案**:
- 确认 ZooKeeper 服务正常
- 确认 `NAMESPACE` 配置正确
- 确认 `myIp` 配置为正确的服务器IP

#### 4. Kafka连接失败

**症状**: 日志中出现 `Unable to connect to Kafka broker`

**排查步骤**:
```bash
# 检查Kafka配置
cat config/application.yml | grep bootstrap-servers

# 测试Kafka连接
telnet 127.0.0.1 9092
```

---

## 与旧部署方式的对比

| 特性 | 旧方式（自定义脚本） | 新方式（标准install.sh） |
|------|---------------------|------------------------|
| **配置方式** | Spring Profiles + 多套YAML | 环境变量 + 单套YAML |
| **环境切换** | 修改 `--spring.profiles.active` | 修改环境变量 |
| **脚本维护** | 多套环境脚本 | 统一脚本 + 环境变量 |
| **配置复杂度** | 高（需维护多套配置） | 低（统一配置模板） |
| **与其他微服务一致性** | 不一致 | ✅ 一致 |
| **运维友好度** | 中等 | ✅ 高 |

---

## 附录

### A. 完整的环境变量清单

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `myIp` | 本机IP | `192.168.3.112` |
| `MYSQL_IP` | MySQL服务器IP | `127.0.0.1` |
| `MYSQL_PORT` | MySQL端口 | `3309` |
| `MYSQL_USER` | MySQL用户名 | `root` |
| `MYSQL_PASSWORD` | MySQL密码 | `dciworld@iivi` |
| `MONGO_SERVERS` | MongoDB服务器列表 | `127.0.0.1:27010,127.0.0.1:27011` |
| `MONGO_DATABASE` | MongoDB数据库名 | `sotn` |
| `MONGO_USER` | MongoDB用户名 | `dci` |
| `MONGO_PWD` | MongoDB密码 | `dciworld@iivi` |
| `KAFKA_BROKER` | Kafka服务器列表 | `127.0.0.1:9092,127.0.0.1:9093` |
| `pmcZooKeepers` | ZooKeeper服务器列表 | `127.0.0.1:2190,127.0.0.1:2191` |
| `NAMESPACE` | ZooKeeper命名空间 | `dciworld/clband` |

### B. 目录权限说明

```bash
# 推荐的目录权限设置
/opt/dci/deploy/device-maintenance/
├── config/              # 644 (rw-r--r--)
├── bin/                 # 755 (rwxr-xr-x)
├── logs/                # 755 (rwxr-xr-x)
└── *.jar                # 755 (rwxr-xr-x)
```

---

**版本**: 1.0.0  
**更新时间**: 2025-11-28  
**维护者**: Device Maintenance Team

