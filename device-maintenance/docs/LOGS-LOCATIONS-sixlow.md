# 日志位置参考文档（sixlow 环境）

## 服务器信息
- **主机**: 10.192.230.22 (sixlow)
- **跳板机**: 116.128.204.12:13102 (actsvt / pureg@1n)
- **目标账号**: actsvt111 / pureg123!@#

## 主要服务日志位置

### 1. device-maintenance (设备维护服务)
```bash
目录: /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT
日志: /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log
```

**查看最新日志**:
```bash
tail -f /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log
```

**查看特定时间段**:
```bash
tail -1000 /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log | \
  sed -n '/2025-12-08 20:30:00/,/2025-12-08 20:31:00/p'
```

---

### 2. adapter (设备适配器)
```bash
目录: /home/dci/deploy/adapter-1.0.0-SNAPSHOT
日志目录: /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log
主日志: /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/adapter.log
错误日志: /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/error.log
```

**日志文件列表**:
- `adapter.log` - 主日志（当天）
- `adapter_YYYY-MM-DD.0.log` - 历史日志（按日期归档）
- `error.log` - 错误日志（当天）
- `error_YYYY-MM-DD.0.log` - 历史错误日志

**查看最新日志**:
```bash
tail -f /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/adapter.log
```

**查看错误日志**:
```bash
tail -200 /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/error.log
```

**查找特定设备的日志**:
```bash
tail -500 /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/adapter.log | \
  grep "Site-1997998703557873664"
```

---

### 3. neManager (网元管理器 / neMgr)
```bash
目录: /home/dci/deploy/neManager-1.0.0-SNAPSHOT
日志目录: /home/dci/deploy/neManager-1.0.0-SNAPSHOT/log
主日志: /home/dci/deploy/neManager-1.0.0-SNAPSHOT/log/ne-manager-YYYY-MM-DD.0.log
```

**日志文件命名规则**:
- `ne-manager-2025-12-08.0.log` - 按日期命名的日志文件

**查看当天日志**:
```bash
# 查找当天日志文件
TODAY=$(date +%Y-%m-%d)
tail -f /home/dci/deploy/neManager-1.0.0-SNAPSHOT/log/ne-manager-${TODAY}.0.log
```

**查看最新日志文件**:
```bash
ls -lt /home/dci/deploy/neManager-1.0.0-SNAPSHOT/log/*.log | head -1
```

---

## 其他重要服务

### 4. taskinfo (任务信息服务)
```bash
目录: /home/dci/deploy/taskinfo-1.0.0-SNAPSHOT
```

### 5. sftpserver (SFTP 服务器管理)
```bash
目录: /home/dci/deploy/sftpserver-1.0.0-SNAPSHOT
```

### 6. gateway-rest (网关服务)
```bash
目录: /home/dci/deploy/gateway-rest-1.0.0-SNAPSHOT
```

---

## 快速登录脚本

### 方法 1: 通过跳板机
```bash
sshpass -p 'pureg@1n' ssh -p 13102 -o StrictHostKeyChecking=no actsvt@116.128.204.12 bash << 'OUTER_EOF'
sshpass -p 'pureg123!@#' ssh -o StrictHostKeyChecking=no actsvt111@10.192.230.22 'bash -s' << 'INNER_EOF'

# 你的命令
tail -100 /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log

INNER_EOF
OUTER_EOF
```

---

## 常用日志查询命令

### 查找下载失败的日志
```bash
tail -500 /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/adapter.log | \
  grep -A 10 -B 5 "download.*fail\|FAIL"
```

### 查找 RPC 调用日志
```bash
tail -500 /home/dci/deploy/neManager-1.0.0-SNAPSHOT/log/ne-manager-$(date +%Y-%m-%d).0.log | \
  grep -A 5 "RPC\|software-operate"
```

### 查找特定设备的所有操作
```bash
DEVICE_ID="Site-1997998703557873664#Ne-1998002027359244288"
tail -1000 /home/dci/deploy/device-maintenance-1.0.0-SNAPSHOT/logs/device-maintenance.log | \
  grep "$DEVICE_ID"
```

### 查找 Kafka 通知
```bash
tail -500 /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/adapter.log | \
  grep "Kafka send msg"
```

### 查找特定时间段的日志
```bash
tail -2000 /home/dci/deploy/adapter-1.0.0-SNAPSHOT/log/adapter.log | \
  sed -n '/2025-12-08 20:30:00/,/2025-12-08 20:35:00/p'
```

---

## SFTP 相关信息

### SFTP 用户配置
- **用户名**: `sftpuser`
- **密码**: `dci123456`
- **根目录**: `/sftp` (系统路径)
- **用户主目录**: `/sftp/sftpuser` (系统路径)
- **上传目录**: `/sftp/sftpuser/upload` (系统路径)
- **从 SFTP 客户端看到的上传目录**: `/upload` (chroot 后的相对路径)

### 文件权限修复
```bash
# 如果上传的文件需要给 sftpuser 访问权限
echo 'pureg123!@#' | sudo -S chown sftpuser:sftpuser /sftp/sftpuser/upload/文件名.tar
```

### 测试 SFTP 连接
```bash
# 从服务器本地测试
sftp sftpuser@10.192.230.22
# 输入密码: dci123456
# 然后运行: ls /upload
```

---

## 故障排查流程

### 1. 软件下载失败
1. 检查 device-maintenance 日志：RPC 调用是否成功
2. 检查 adapter 日志：设备是否返回 FAIL 状态
3. 检查 neManager 日志：RPC 是否正确转发到设备
4. 检查 SFTP 文件权限：`ls -lh /sftp/sftpuser/upload/文件名`
5. 检查设备网络连接：设备能否访问 SFTP 服务器

### 2. 升级/回滚超时
1. 检查 device-maintenance 日志：轮询服务是否在检查
2. 查询 MongoDB 状态：`device-maintenance:get-device-operations-status`
3. 检查 Kafka 通知：adapter 日志中的 `system-change` 消息

### 3. TaskInfo 通知问题
1. 检查 device-maintenance 日志：是否发送了 TaskInfo 通知
2. 检查 taskinfo 服务日志：是否收到通知
3. 检查批次状态：`device-maintenance:get-batch-detail`

---

## 日志大小和清理

### 查看日志大小
```bash
du -sh /home/dci/deploy/*/log /home/dci/deploy/*/logs
```

### 查找大日志文件
```bash
find /home/dci/deploy -name "*.log" -type f -size +100M -exec ls -lh {} \;
```

---

## 故障排查参考

### SFTP 下载失败
如果遇到设备下载失败问题，参考详细排查文档：
- 📖 [SFTP 下载失败问题排查记录](./TROUBLESHOOTING-SFTP-DOWNLOAD-FAILURE.md)

**常见原因快速检查**:
```bash
# 1. 检查 SFTP chroot 配置
sudo grep -A 6 "Match User sftpuser" /etc/ssh/sshd_config

# 2. 检查目录权限
ls -ld /sftp /sftp/sftpuser /sftp/sftpuser/upload

# 3. 检查认证日志（设备是否连接）
sudo tail -50 /var/log/auth.log | grep "10.192.230.118"

# 4. 测试 SFTP 连接
sshpass -p 'dci123456' sftp sftpuser@localhost << 'EOF'
ls /upload
bye
EOF
```

**关键配置要求**:
- ✅ `ChrootDirectory /sftp/%u` (使用 %u 占位符)
- ✅ `/sftp/sftpuser` 必须由 root 拥有，权限 755
- ✅ `/sftp/sftpuser/upload` 由 sftpuser 拥有，权限 755

---

## 更新历史
- **2025-12-08**: 初始创建，记录 sixlow 环境的日志位置
- **2025-12-08**: 添加 SFTP 权限问题修复方法
- **2025-12-08**: 完成 SFTP 下载失败问题完整排查，添加故障排查参考链接

