# SFTP 下载失败问题排查记录

## 问题描述
**日期**: 2025-12-08  
**环境**: sixlow (10.192.230.22)  
**现象**: 设备软件下载任务创建成功，RPC 调用成功，但设备报告下载失败（download-state=FAIL）

## 症状

### 表现
1. **device-maintenance** 成功发起 RPC 调用，返回 `success`
2. **adapter** 日志显示 RPC 命令成功下发到设备
3. 设备开始下载，状态变为 `DOWNLOADING`（约 1-2 秒后）
4. 设备在约 8-10 秒后报告 `download-state=FAIL`
5. **没有详细的错误信息**

### 日志时间线
```
20:40:08 - device-maintenance 发起下载请求
20:40:09 - adapter RPC 调用成功返回
20:40:10 - 设备状态: DOWNLOADING
20:40:18 - 设备状态: FAIL
```

### Kafka 通知内容
```json
{
  "download.software-version":"POS_3.5.0.43_20251207",
  "download.file-name":"Release_POS_3.5.0.43_20251207.tar",
  "download.download-state":"FAIL",
  "download.download-time":"2025-12-08T20:40:09+08:00",
  "neId":"Site-1997998703557873664#Ne-1998002027359244288"
}
```

## 排查过程

### 第一步：检查基础条件

#### 1. 文件存在性
```bash
ls -lh /sftp/sftpuser/upload/Release_POS_3.5.0.43_20251207.tar
# 结果: -rw-rw-r-- 1 actsvt111 actsvt111 736M Dec 8 18:49
```
✅ 文件存在

#### 2. 网络连通性
```bash
ping -c 3 10.192.230.118  # 设备 IP
# 结果: 3 packets transmitted, 3 received, 0% packet loss
```
✅ 网络正常

#### 3. SFTP 服务运行状态
```bash
ss -tlnp | grep :22
# 结果: LISTEN 0 4096 0.0.0.0:22 0.0.0.0:*
```
✅ SFTP 服务运行中

### 第二步：检查认证日志

#### ⚠️ 关键发现 #1
查看 `/var/log/auth.log`，发现设备**确实尝试连接**了 SFTP 服务器：

```log
2025-12-08T20:40:11.183278 Accepted password for sftpuser from 10.192.230.118 port 57472 ssh2
2025-12-08T20:40:11.187147 pam_unix(sshd:session): session opened for user sftpuser(uid=1002)
2025-12-08T20:40:11.385955 pam_unix(sshd:session): session closed for user sftpuser
```

**问题点**：
- ✅ 设备成功认证
- ✅ SFTP 会话建立
- ❌ 会话**立即关闭**（仅持续 0.2 秒）

这说明问题不是网络或认证，而是 **SFTP 会话本身**。

### 第三步：检查 SFTP Chroot 配置

#### 当前配置
```bash
# /etc/ssh/sshd_config
Match User sftpuser
    ChrootDirectory /sftp
    ForceCommand internal-sftp
    AllowTcpForwarding no
    X11Forwarding no
    PermitTTY no
```

#### 目录结构
```
/sftp/                           # root:root (755)
├── sftpuser/                    # sftpuser:sftpuser (755)  ⚠️ 问题！
    ├── upload/                  # sftpuser:sftpuser (777)
    │   └── Release_POS_3.5.0.43_20251207.tar
    ├── download/
    └── backup/
```

#### ⚠️ 关键发现 #2：路径映射错误

当 `ChrootDirectory /sftp` 时：
- **系统实际路径**: `/sftp/sftpuser/upload/file.tar`
- **用户 chroot 后看到**: `/sftpuser/upload/file.tar`
- **设备尝试访问**: `/upload/file.tar` ❌ **路径不匹配！**

设备尝试访问 `/upload/file.tar`，但实际上这映射到 `/sftp/upload/file.tar`（不存在）。

#### ⚠️ 关键发现 #3：Chroot 目录权限错误

SFTP chroot 要求：
1. ✅ ChrootDirectory (`/sftp`) 必须由 **root** 拥有
2. ❌ ChrootDirectory 的所有父目录和本身都不能被组或其他用户写入
3. ❌ 用户主目录 (`/sftp/sftpuser`) **也必须由 root 拥有**（这是 chroot 的严格要求）

当前 `/sftp/sftpuser` 所有者是 `sftpuser`，**违反了 chroot 安全要求**，导致会话立即被拒绝。

## 解决方案

### 修复 1: 修改 ChrootDirectory 配置

```bash
# 备份配置
sudo cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup

# 修改配置
sudo sed -i 's|ChrootDirectory /sftp|ChrootDirectory /sftp/%u|' /etc/ssh/sshd_config

# 验证配置
sudo grep -A 6 "Match User sftpuser" /etc/ssh/sshd_config
```

**修改后配置**:
```
Match User sftpuser
    ChrootDirectory /sftp/%u    # ← 改为 /sftp/%u
    ForceCommand internal-sftp
    AllowTcpForwarding no
    X11Forwarding no
    PermitTTY no
```

**效果**: 
- 用户 chroot 到 `/sftp/sftpuser`
- 用户看到的 `/upload` 映射到 `/sftp/sftpuser/upload` ✅

### 修复 2: 修改目录权限

```bash
# 1. 修改 /sftp/sftpuser 所有者为 root（chroot 要求）
sudo chown root:root /sftp/sftpuser
sudo chmod 755 /sftp/sftpuser

# 2. 保持子目录 sftpuser 可写
sudo chown sftpuser:sftpuser /sftp/sftpuser/upload
sudo chmod 755 /sftp/sftpuser/upload

# 3. 修改文件所有者
sudo chown sftpuser:sftpuser /sftp/sftpuser/upload/Release_POS_3.5.0.43_20251207.tar
```

**修复后目录结构**:
```
/sftp/                           # root:root (755) ✅
├── sftpuser/                    # root:root (755) ✅ 修复！
    ├── upload/                  # sftpuser:sftpuser (755) ✅
    │   └── Release_POS_3.5.0.43_20251207.tar  # sftpuser:sftpuser (664)
    ├── download/                # sftpuser:sftpuser (755)
    └── backup/                  # sftpuser:sftpuser (755)
```

### 修复 3: 重启 SSH 服务

```bash
# 测试配置语法
sudo sshd -t

# 重新加载配置（不断开现有连接）
sudo systemctl reload ssh
# 或
sudo service ssh reload
```

## 验证

### 1. 手动 SFTP 测试

```bash
sshpass -p 'dci123456' sftp sftpuser@localhost << 'EOF'
pwd
ls
cd /upload
ls -la
bye
EOF
```

**预期输出**:
```
sftp> pwd
Remote working directory: /
sftp> ls
backup    download  snap      upload    
sftp> cd /upload
sftp> ls -la
-rw-rw-r-- 1 1002 1003 771430400 Dec  8 18:49 Release_POS_3.5.0.43_20251207.tar
```

### 2. 设备下载测试

重新触发设备下载任务：
- ✅ RPC 调用成功
- ✅ 设备连接 SFTP
- ✅ 设备状态: DOWNLOADING
- ✅ 设备状态: **COMPLETE** 🎉

### 3. 查看成功日志

```bash
tail -100 /var/log/auth.log | grep sftpuser
```

**成功的会话日志**:
```log
2025-12-08T20:45:11.183278 Accepted password for sftpuser from 10.192.230.118
2025-12-08T20:45:11.187147 pam_unix(sshd:session): session opened for user sftpuser
# ... 文件传输过程 ...
2025-12-08T20:46:25.385955 pam_unix(sshd:session): session closed for user sftpuser
```

会话持续约 1 分钟（文件 736MB），正常关闭。

## 知识点总结

### SFTP Chroot 安全要求

1. **ChrootDirectory 权限规则**:
   ```
   ChrootDirectory 及其所有父目录：
   - 必须由 root 拥有
   - 权限必须是 755 或更严格（不能 777）
   - 不能被组或其他用户写入
   ```

2. **用户主目录规则**:
   ```
   如果 ChrootDirectory 是用户主目录的父目录：
   - 用户主目录必须由 root 拥有
   - 用户需要写入权限的目录应该是 ChrootDirectory 的子目录
   ```

3. **推荐配置**:
   ```
   ChrootDirectory /sftp/%u  （使用 %u 占位符，代表用户名）
   
   目录结构:
   /sftp/%u/           ← root 拥有（chroot 点）
   ├── upload/         ← 用户拥有（可写）
   ├── download/       ← 用户拥有（可写）
   └── backup/         ← 用户拥有（可写）
   ```

### 常见错误

#### ❌ 错误 1: ChrootDirectory 权限过于宽松
```bash
drwxrwxrwx 4 root root 4096 /sftp  # ← 777 权限，会被拒绝
```
**解决**: `chmod 755 /sftp`

#### ❌ 错误 2: ChrootDirectory 不是 root 拥有
```bash
drwxr-xr-x 4 sftpuser sftpuser 4096 /sftp/sftpuser  # ← 所有者错误
```
**解决**: `chown root:root /sftp/sftpuser`

#### ❌ 错误 3: 路径映射错误
```
ChrootDirectory /sftp
用户尝试访问: /upload
实际映射到: /sftp/upload  ← 不存在！
```
**解决**: 使用 `ChrootDirectory /sftp/%u`

### 调试技巧

1. **查看 SSH 详细日志**:
   ```bash
   # 临时启用 SSH 调试模式
   sudo /usr/sbin/sshd -d -p 2222
   
   # 客户端连接测试
   sftp -P 2222 -vvv sftpuser@localhost
   ```

2. **查看认证日志**:
   ```bash
   # Ubuntu/Debian
   sudo tail -f /var/log/auth.log | grep sftpuser
   
   # CentOS/RHEL
   sudo tail -f /var/log/secure | grep sftpuser
   ```

3. **测试 chroot 配置**:
   ```bash
   # 测试配置语法
   sudo sshd -t
   
   # 查看用户会话
   sudo lsof -u sftpuser
   ```

## 预防措施

### 1. SFTP 服务器配置检查清单

- [ ] ChrootDirectory 由 root 拥有
- [ ] ChrootDirectory 权限为 755 或更严格
- [ ] 用户可写目录是 ChrootDirectory 的子目录
- [ ] 文件所有者与 SFTP 用户匹配
- [ ] SSH 配置语法正确 (`sshd -t`)
- [ ] 防火墙允许 22 端口
- [ ] 路径映射测试通过

### 2. 部署脚本模板

创建 SFTP 用户的标准脚本：

```bash
#!/bin/bash
# setup-sftp-user.sh

USERNAME="sftpuser"
PASSWORD="your_password"
SFTP_ROOT="/sftp"

# 1. 创建 SFTP 根目录
sudo mkdir -p $SFTP_ROOT
sudo chown root:root $SFTP_ROOT
sudo chmod 755 $SFTP_ROOT

# 2. 创建用户
sudo useradd -m -d $SFTP_ROOT/$USERNAME -s /usr/sbin/nologin $USERNAME
echo "$USERNAME:$PASSWORD" | sudo chpasswd

# 3. 设置用户主目录权限（root 拥有）
sudo chown root:root $SFTP_ROOT/$USERNAME
sudo chmod 755 $SFTP_ROOT/$USERNAME

# 4. 创建用户可写的子目录
sudo mkdir -p $SFTP_ROOT/$USERNAME/{upload,download,backup}
sudo chown -R $USERNAME:$USERNAME $SFTP_ROOT/$USERNAME/{upload,download,backup}
sudo chmod 755 $SFTP_ROOT/$USERNAME/{upload,download,backup}

# 5. 配置 SSHD
sudo tee -a /etc/ssh/sshd_config > /dev/null << EOF

# SFTP Chroot Configuration for $USERNAME
Match User $USERNAME
    ChrootDirectory $SFTP_ROOT/%u
    ForceCommand internal-sftp
    AllowTcpForwarding no
    X11Forwarding no
    PermitTTY no
EOF

# 6. 测试配置
sudo sshd -t && echo "✅ 配置语法正确" || echo "❌ 配置语法错误"

# 7. 重新加载 SSH
sudo systemctl reload ssh || sudo service ssh reload

echo "✅ SFTP 用户 $USERNAME 设置完成"
echo "   主目录: $SFTP_ROOT/$USERNAME"
echo "   可写目录: upload, download, backup"
```

### 3. 监控和告警

设置 SFTP 连接失败的告警：

```bash
# 监控脚本
#!/bin/bash
# monitor-sftp-failures.sh

LOG_FILE="/var/log/auth.log"
ALERT_THRESHOLD=5

# 统计最近 5 分钟内的失败连接
FAILURES=$(sudo grep "$(date -d '5 minutes ago' '+%Y-%m-%dT%H:%M')" $LOG_FILE | \
           grep "sftpuser" | \
           grep -c "session closed" | \
           awk '{if ($1 > 0) print $1; else print 0}')

if [ "$FAILURES" -gt "$ALERT_THRESHOLD" ]; then
    echo "⚠️  SFTP 连接异常：最近 5 分钟内有 $FAILURES 次快速断开"
    # 发送告警
fi
```

## 相关文档

- [LOGS-LOCATIONS-sixlow.md](./LOGS-LOCATIONS-sixlow.md) - 日志位置参考
- [DEPLOYMENT-sixlow.md](./DEPLOYMENT-sixlow.md) - 部署配置文档

## 更新历史

- **2025-12-08**: 初始创建，记录 SFTP 下载失败问题的完整排查和解决过程

