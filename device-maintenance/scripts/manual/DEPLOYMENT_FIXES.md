# Device Maintenance 部署配置修正说明

## 🎯 **关键修正**

通过分析其他微服务的实际部署配置，发现了以下关键差异并已修正：

### **1. 部署路径**

**❌ 错误：** `/opt/dci/deploy/`
**✅ 正确：** `/home/dci/deploy/`

**原因：** 所有其他微服务（neMgr, taskInfo, sftpserver, notifier等）都部署在 `/home/dci/deploy/`

### **2. ZooKeeper Namespace**

**❌ 错误：** `NAMESPACE=dciworld/clband`
**✅ 正确：** `NAMESPACE=clband`

**原因：**
- 其他微服务的配置文件中只写 `clband`，不带 `dciworld/` 前缀
- `zkclient4boot` 框架会自动将 namespace 包装为完整路径：`/dciworld/{NAMESPACE}/STATE/...`
- 如果配置文件中写 `dciworld/clband`，最终路径会变成 `/dciworld/dciworld/clband/STATE/...` （错误）

### **3. myIp 配置**

**❌ 错误：** `myIp=192.168.3.112` （硬编码IP）
**✅ 正确：** `myIp=0.0.0.0` （自动检测）

**原因：**
- 其他微服务都配置为 `myIp=0.0.0.0`
- `ConfLoader` 会自动检测本机IP（参见 `ConfLoader.getMyIp()` 方法）
- 这样更灵活，无需为每台机器单独配置

## 📋 **对比表**

| 配置项 | 错误配置（旧） | 正确配置（新） | 参考微服务 |
|--------|--------------|--------------|-----------|
| **部署路径** | `/opt/dci/deploy/` | `/home/dci/deploy/` | neMgr, taskInfo, sftpserver |
| **NAMESPACE** | `dciworld/clband` | `clband` | neMgr, taskInfo, sftpserver |
| **myIp** | `192.168.3.112` | `0.0.0.0` | neMgr, taskInfo, sftpserver |

## 🔧 **已修改的文件**

### **1. `scripts/manual/env-shanghai2.conf`**

```bash
# 修改前
DEPLOY_BASE="/opt/dci/deploy"
myIp="192.168.3.112"
NAMESPACE="dciworld/clband"

# 修改后
DEPLOY_BASE="/home/dci/deploy"  # 与其他微服务一致
myIp="0.0.0.0"                  # 自动检测IP
NAMESPACE="clband"              # 不带dciworld/前缀
```

## ✅ **验证方法**

1. **检查标准路径下的其他微服务配置：**
```bash
cat /home/dci/deploy/neManager-1.0.0-SNAPSHOT/config/zkclient_conf.properties | grep -E "NAMESPACE|myIp|port="
cat /home/dci/deploy/taskinfo-1.0.0-SNAPSHOT/config/zkclient_conf.properties | grep -E "NAMESPACE|myIp|port="
cat /home/dci/deploy/sftpserver-1.0.0-SNAPSHOT/config/zkclient_conf.properties | grep -E "NAMESPACE|myIp|port="
```

2. **预期输出：**
```
NAMESPACE=clband          # 不是 dciworld/clband
myIp=0.0.0.0             # 不是具体IP
port=18002/18200/18280   # 各自的端口
```

## 🚀 **下次部署**

使用修正后的配置部署时：

```bash
cd scripts/manual
bash shanghai2-manual-deploy.sh
```

服务将：
1. 部署到 `/home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/`
2. 配置文件中 `NAMESPACE=clband`
3. 配置文件中 `myIp=0.0.0.0`
4. 程序启动时自动检测本机IP并注册到 `/dciworld/clband/STATE/device-maintenance/`
5. 能够发现其他注册在 `/dciworld/clband/STATE/` 下的微服务

## 📝 **关键代码逻辑**

### **ConfLoader.getMyIp()** (自动IP检测)
```java
// 优先从环境变量获取K8S Pod IP
String podIp = System.getenv("POD_IP");
if (StringUtils.hasLength(podIp)) {
    return podIp;
}

// 遍历网络接口获取非回环地址
// 优先返回IPv4地址
// ...
```

### **DCIClientImpl.register()** (ZK路径构建)
```java
// namespace 从 ConfLoader.getValue("NAMESPACE") 读取
// 假设 NAMESPACE=clband
// ZK注册路径会被构建为：/dciworld/clband/STATE/device-maintenance/...
String zkPath = "/" + namespace + "/STATE/" + moduleName + "/" + instanceId;
```

## ⚠️ **注意事项**

1. **不要在 zkclient_conf.properties 中写完整路径**
   - ❌ `NAMESPACE=dciworld/clband`
   - ✅ `NAMESPACE=clband`

2. **不要硬编码 myIp**
   - ❌ `myIp=192.168.3.112`
   - ✅ `myIp=0.0.0.0`

3. **使用标准部署路径**
   - ❌ `/opt/dci/deploy/`
   - ✅ `/home/dci/deploy/`

---

**总结：** 通过对齐配置与其他微服务保持一致，device-maintenance 现在可以正确注册到 ZooKeeper 并发现其他服务。

