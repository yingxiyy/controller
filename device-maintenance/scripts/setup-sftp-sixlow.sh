#!/bin/bash
##############################################################################
# SFTP 服务配置脚本 - sixlow 环境
# 服务器: 10.192.230.22
# 
# 使用方法:
#   1. 登录到跳板机: ssh -p 13102 actsvt@116.128.204.12
#   2. 从跳板机登录目标服务器: ssh pureg111@10.192.230.22
#   3. 复制本脚本内容并执行: sudo bash setup-sftp-sixlow.sh
##############################################################################

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 SFTP 服务配置 - sixlow 环境"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then 
    echo "❌ 请使用 sudo 运行此脚本"
    exit 1
fi

# 1. 创建 SFTP 根目录
echo ""
echo "📁 [1/5] 创建 SFTP 根目录"
if [ ! -d "/sftp" ]; then
    mkdir -p /sftp
    echo "  ✅ 目录已创建: /sftp"
else
    echo "  ℹ️  目录已存在: /sftp"
fi

# 2. 创建 sftpuser 用户
echo ""
echo "👤 [2/5] 创建 SFTP 用户"
if id "sftpuser" &>/dev/null; then
    echo "  ℹ️  用户已存在: sftpuser"
else
    useradd -m -d /sftp/sftpuser -s /bin/bash sftpuser
    echo "  ✅ 用户已创建: sftpuser"
fi

# 3. 设置用户密码
echo ""
echo "🔑 [3/5] 设置用户密码"
echo "sftpuser:dci123456" | chpasswd
echo "  ✅ 密码已设置: dci123456"

# 4. 设置目录权限
echo ""
echo "🔒 [4/5] 设置目录权限"

# SFTP chroot 要求根目录必须是 root 拥有，且不能被组或其他用户写入
chown root:root /sftp
chmod 755 /sftp

# 创建用户的上传目录
if [ ! -d "/sftp/sftpuser" ]; then
    mkdir -p /sftp/sftpuser
fi
chown sftpuser:sftpuser /sftp/sftpuser
chmod 755 /sftp/sftpuser

# 创建常用子目录
mkdir -p /sftp/sftpuser/upload /sftp/sftpuser/download /sftp/sftpuser/backup
chown -R sftpuser:sftpuser /sftp/sftpuser
chmod -R 755 /sftp/sftpuser

echo "  ✅ 权限已设置"
echo "    /sftp              -> root:root (755)"
echo "    /sftp/sftpuser     -> sftpuser:sftpuser (755)"
echo "    /sftp/sftpuser/*   -> sftpuser:sftpuser (755)"

# 5. 配置 SSHD
echo ""
echo "⚙️  [5/5] 配置 SSH 服务"

SSHD_CONFIG="/etc/ssh/sshd_config"

# 备份原配置
if [ ! -f "${SSHD_CONFIG}.bak" ]; then
    cp "${SSHD_CONFIG}" "${SSHD_CONFIG}.bak"
    echo "  ✅ 已备份配置: ${SSHD_CONFIG}.bak"
fi

# 检查是否已有 SFTP 配置
if grep -q "Match User sftpuser" "${SSHD_CONFIG}"; then
    echo "  ℹ️  SFTP 配置已存在，跳过"
else
    # 添加 SFTP 配置
    cat >> "${SSHD_CONFIG}" << 'SSHD_CONF'

# ============================================================================
# SFTP Configuration for sftpuser
# ============================================================================
Match User sftpuser
    # 限制用户只能访问 /sftp 目录
    ChrootDirectory /sftp
    # 强制使用内部 SFTP 服务（不允许 shell 访问）
    ForceCommand internal-sftp
    # 禁用 TCP 转发
    AllowTcpForwarding no
    # 禁用 X11 转发
    X11Forwarding no
    # 禁用 PTY 分配
    PermitTTY no
SSHD_CONF
    echo "  ✅ SFTP 配置已添加"
fi

# 测试 SSH 配置
echo ""
echo "🧪 测试 SSH 配置"
if sshd -t; then
    echo "  ✅ SSH 配置语法正确"
else
    echo "  ❌ SSH 配置有错误，请检查"
    exit 1
fi

# 重启 SSH 服务
echo ""
echo "🔄 重启 SSH 服务"
systemctl restart sshd
if systemctl is-active --quiet sshd; then
    echo "  ✅ SSH 服务已重启并运行"
else
    echo "  ❌ SSH 服务启动失败"
    exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ SFTP 服务配置完成！"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 连接信息:"
echo "  服务器: 10.192.230.22"
echo "  端口: 22"
echo "  用户: sftpuser"
echo "  密码: dci123456"
echo "  根目录: /sftp"
echo "  用户目录: /sftp/sftpuser"
echo ""
echo "📁 可用目录:"
echo "  /sftp/sftpuser/upload   - 上传文件"
echo "  /sftp/sftpuser/download - 下载文件"
echo "  /sftp/sftpuser/backup   - 备份文件"
echo ""
echo "🧪 测试命令:"
echo "  # 从本地测试"
echo "  sftp sftpuser@10.192.230.22"
echo ""
echo "  # 从跳板机测试"
echo "  sftp sftpuser@10.192.230.22"
echo ""
echo "  # 使用 lftp 测试"
echo "  lftp -u sftpuser,dci123456 sftp://10.192.230.22"
echo ""
echo "🔐 安全说明:"
echo "  - sftpuser 被限制在 /sftp 目录内（chroot）"
echo "  - 无法使用 shell 登录"
echo "  - 无法进行端口转发"
echo "  - 只能使用 SFTP 协议传输文件"
echo ""

