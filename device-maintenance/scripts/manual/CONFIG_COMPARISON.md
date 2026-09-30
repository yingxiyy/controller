# 配置处理方式对比说明

## 🔍 回答你的问题：配置内容是静态的还是读取替换的？

答案：**✅ 从配置文件读取的（动态）**

---

## 📊 配置流程详解

### 整体流程

```
┌──────────────────────────────────────────────────────────────┐
│ 1. 本地编译阶段（在你的Mac上）                                │
└──────────────────────────────────────────────────────────────┘
                            ↓
    脚本读取配置文件: env-shanghai2.conf
    ├── SERVER_HOST="116.128.204.12"
    ├── myIp="192.168.3.112"
    ├── NAMESPACE="dciworld/clband"
    └── ...其他配置
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. 编译打包（配置文件内容不影响编译）                         │
└──────────────────────────────────────────────────────────────┘
                            ↓
    mvn clean package -DskipTests
    └── 生成: Device Maintenance Microservice-1.0.0-SNAPSHOT-bin.tar.gz
        包含:
        ├── device-maintenance-1.0.0-SNAPSHOT.jar
        ├── bin/install.sh (plugin生成，包含sed替换逻辑)
        └── config/
            ├── application.yml (默认值)
            ├── zkclient_conf.properties (默认值: NAMESPACE=dciworld/default)
            ├── mongodb.properties (默认值)
            └── mysql.properties (默认值)
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. 上传到远程服务器                                           │
└──────────────────────────────────────────────────────────────┘
                            ↓
    scp tar.gz → 116.128.204.12:/tmp/
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. 解压到部署目录                                             │
└──────────────────────────────────────────────────────────────┘
                            ↓
    tar -xzf → /opt/dci/deploy/Device Maintenance Microservice-1.0.0-SNAPSHOT/
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. 在远程服务器创建环境配置文件                               │
└──────────────────────────────────────────────────────────────┘
                            ↓
    脚本将配置文件中的值写入远程服务器的 env-shanghai2.sh:
    
    #!/bin/bash
    export myIp="192.168.3.112"           ← 从配置文件读取
    export NAMESPACE="dciworld/clband"    ← 从配置文件读取
    export pmcZooKeepers="127.0.0.1:2190,..." ← 从配置文件读取
    export MYSQL_IP="127.0.0.1"           ← 从配置文件读取
    ... (所有配置都从配置文件读取)
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 6. 远程服务器执行 install.sh                                  │
└──────────────────────────────────────────────────────────────┘
                            ↓
    cd /opt/dci/deploy/Device Maintenance Microservice-1.0.0-SNAPSHOT/
    source env-shanghai2.sh              ← 加载环境变量
    ./bin/install.sh                     ← 执行安装
                            ↓
    install.sh 使用 sed 替换配置文件中的占位符:
    
    # zkclient_conf.properties
    sed -i s#NAMESPACE=.*#NAMESPACE=${NAMESPACE}#
    # 替换前: NAMESPACE=dciworld/default
    # 替换后: NAMESPACE=dciworld/clband ✅
    
    # mongodb.properties
    sed -i s#mongodb.servers=.*#mongodb.servers=${MONGO_SERVERS}#
    # 替换前: mongodb.servers=127.0.0.1:27017
    # 替换后: mongodb.servers=127.0.0.1:27010,127.0.0.1:27011,127.0.0.1:27012 ✅
    
    # mysql.properties
    sed -i "s#spring.datasource.url=.*#...jdbc:mysql://${MYSQL_IP}:${MYSQL_PORT}...#"
    # 替换前: spring.datasource.url=jdbc:mysql://127.0.0.1:3306/sotn...
    # 替换后: spring.datasource.url=jdbc:mysql://127.0.0.1:3309/sotn... ✅
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 7. 安装 SystemD 服务并启动                                    │
└──────────────────────────────────────────────────────────────┘
                            ↓
    systemctl enable Device\ Maintenance\ Microservice.service
    systemctl restart Device\ Maintenance\ Microservice.service
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 8. 服务运行时读取已替换的配置文件                             │
└──────────────────────────────────────────────────────────────┘
                            ↓
    应用启动后读取:
    - config/zkclient_conf.properties (NAMESPACE=dciworld/clband) ✅
    - config/mongodb.properties (已替换为实际值) ✅
    - config/mysql.properties (已替换为实际值) ✅
```

---

## 🎯 关键点总结

### 1️⃣ **配置文件是如何被使用的？**

```bash
本地Mac上:
├── env-shanghai2.conf (你可以编辑的配置文件)
│   └── 包含: SERVER_HOST, myIp, NAMESPACE, MYSQL_IP, 等等
│
├── shanghai2-manual-deploy.sh (部署脚本)
│   └── 第一步: source env-shanghai2.conf (读取配置)
│   └── 将配置值导出为环境变量
│
└── 远程服务器上创建 env-shanghai2.sh
    └── 内容来自本地配置文件的值
    └── install.sh 会 source 这个文件并使用环境变量
```

### 2️⃣ **配置替换的三个阶段**

| 阶段 | 位置 | 配置状态 | 工具 |
|------|------|---------|------|
| **阶段1: 编译时** | 本地Mac | 配置文件中的值是**硬编码默认值** | Maven |
| **阶段2: 部署时** | 远程服务器 | 配置文件被**sed替换为实际值** | install.sh |
| **阶段3: 运行时** | 远程服务器 | 应用读取**已替换的配置文件** | Spring Boot |

### 3️⃣ **配置文件的三种形态**

#### 形态A: 源码中的配置文件（编译前）

```properties
# src/main/resources/zkclient_conf.properties
ZOOKEEPER_SERVERS=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
MODULE=device-maintenance
NAMESPACE=dciworld/default          ← 默认值（占位符）
myIp=192.168.1.100                  ← 默认值（占位符）
```

#### 形态B: tar.gz包中的配置文件（编译后，部署前）

```properties
# Device Maintenance Microservice-1.0.0-SNAPSHOT-bin.tar.gz
# → config/zkclient_conf.properties
ZOOKEEPER_SERVERS=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
MODULE=device-maintenance
NAMESPACE=dciworld/default          ← 仍然是默认值
myIp=192.168.1.100                  ← 仍然是默认值
```

#### 形态C: 服务器上的配置文件（install.sh替换后）

```properties
# /opt/dci/deploy/Device Maintenance.../config/zkclient_conf.properties
ZOOKEEPER_SERVERS=127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192
MODULE=device-maintenance
NAMESPACE=dciworld/clband           ← ✅ 已替换为实际值（从配置文件读取）
myIp=192.168.3.112                  ← ✅ 已替换为实际值（从配置文件读取）
```

---

## 📝 详细示例：NAMESPACE配置的完整流程

### 步骤1: 你修改配置文件

```bash
# 编辑 scripts/manual/env-shanghai2.conf
vim scripts/manual/env-shanghai2.conf

# 修改内容
NAMESPACE="dciworld/clband"  ← 你在这里修改
```

### 步骤2: 运行部署脚本

```bash
./scripts/manual/shanghai2-manual-deploy.sh
```

### 步骤3: 脚本读取配置

```bash
# shanghai2-manual-deploy.sh 第一步
source scripts/manual/env-shanghai2.conf

# 此时 NAMESPACE 变量的值是 "dciworld/clband"
echo $NAMESPACE  # 输出: dciworld/clband
```

### 步骤4: 脚本导出环境变量

```bash
# shanghai2-manual-deploy.sh
export NAMESPACE="dciworld/clband"  ← 从配置文件读取的值
```

### 步骤5: 脚本在远程服务器创建环境文件

```bash
# 脚本在远程服务器创建 env-shanghai2.sh
cat > /opt/dci/deploy/.../env-shanghai2.sh << EOF
export NAMESPACE="dciworld/clband"  ← 从本地配置文件传递过来的值
EOF
```

### 步骤6: install.sh 使用 sed 替换

```bash
# 远程服务器执行
source env-shanghai2.sh              # NAMESPACE="dciworld/clband"
./bin/install.sh

# install.sh 内部执行
sed -i s#NAMESPACE=.*#NAMESPACE=${NAMESPACE}# config/zkclient_conf.properties

# 替换前
NAMESPACE=dciworld/default

# 替换后
NAMESPACE=dciworld/clband  ← ✅ 成功替换
```

### 步骤7: 应用读取配置

```bash
# 应用启动后
ZkClientConfig.java 读取 zkclient_conf.properties
  └── NAMESPACE = "dciworld/clband" ✅

# 注册到 ZooKeeper
/dciworld/clband/STATE/device-maintenance/...  ✅
```

---

## 🔄 配置文件 vs 硬编码

### ❌ 旧方式（硬编码在脚本中）

```bash
# deploy-shanghai2.sh
export NAMESPACE="dciworld/clband"  ← 硬编码
export myIp="192.168.3.112"         ← 硬编码

# 缺点：
# - 修改配置需要修改脚本
# - 难以维护
# - 不同环境需要不同脚本
```

### ✅ 新方式（从配置文件读取）

```bash
# env-shanghai2.conf
NAMESPACE="dciworld/clband"         ← 配置文件（可编辑）
myIp="192.168.3.112"                ← 配置文件（可编辑）

# shanghai2-manual-deploy.sh
source env-shanghai2.conf           ← 脚本读取配置
export NAMESPACE                    ← 动态使用配置值

# 优点：
# ✅ 配置和逻辑分离
# ✅ 修改配置不需要改脚本
# ✅ 同一个脚本支持多环境
# ✅ 配置可以版本控制
```

---

## 🎯 最终答案

### Q: 配置内容是静态的还是读取替换的？

**A: 是从配置文件动态读取的！**

| 配置层级 | 存储位置 | 读取方式 | 何时替换 |
|---------|---------|---------|---------|
| **1. 本地配置** | `env-shanghai2.conf` | 脚本 `source` | - |
| **2. 环境变量** | 脚本导出 | `export` | - |
| **3. 远程环境文件** | `env-shanghai2.sh` | 脚本创建 | 部署时 |
| **4. 应用配置文件** | `*.properties` | `sed` 替换 | install.sh执行时 |
| **5. 运行时配置** | 应用读取 | Spring加载 | 应用启动时 |

### 配置流程图

```
你编辑配置文件 (env-shanghai2.conf)
    ↓
脚本读取配置文件 (source)
    ↓
脚本导出环境变量 (export)
    ↓
脚本上传并创建远程环境文件 (env-shanghai2.sh)
    ↓
install.sh 读取环境变量
    ↓
install.sh 使用 sed 替换配置文件
    ↓
应用启动时读取已替换的配置文件
    ↓
✅ 配置生效！
```

---

## 💡 实际使用示例

### 场景：需要部署到不同的ZK命名空间

```bash
# 1. 复制配置模板
cp scripts/manual/env-shanghai2.conf /tmp/my-test-env.conf

# 2. 修改配置
vim /tmp/my-test-env.conf
# 修改: NAMESPACE="dciworld/test"  ← 改成测试命名空间

# 3. 使用新配置部署
./scripts/manual/shanghai2-manual-deploy.sh /tmp/my-test-env.conf

# 结果：
# ✅ 服务会注册到 /dciworld/test/STATE/device-maintenance/...
# ✅ 所有其他配置也会使用 /tmp/my-test-env.conf 中的值
```

### 场景：需要修改数据库连接

```bash
# 1. 编辑配置文件
vim scripts/manual/env-shanghai2.conf

# 2. 修改数据库配置
MYSQL_IP="192.168.100.50"     ← 改成新的数据库IP
MYSQL_PORT="3307"             ← 改成新的端口

# 3. 重新部署
./scripts/manual/shanghai2-manual-deploy.sh

# 结果：
# ✅ mysql.properties 会被替换为新的数据库连接信息
# ✅ 应用会连接到新的数据库
```

---

**总结：配置完全是从配置文件动态读取和替换的，不是硬编码的！** ✅

