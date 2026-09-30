#!/bin/bash
#
# ============================================================
# Script Name: createSFTP.sh
# Description:
#   创建一个基于 chroot 的 SFTP 用户环境（单用户模式）
#
# 功能：
#   - 创建 sftp 用户组（sftpusers）
#   - 创建用户（禁止 SSH 登录，仅允许 SFTP）
#   - 创建 SFTP 目录结构
#   - 设置正确的 chroot 权限
#   - 写入 sshd_config（如不存在）
#
# 使用方式：
#   chmod +x createSFTP.sh
#   ./createSFTP.sh
#
# 输入参数（交互）：
#   用户名（例如：dciUser）
#   SFTP根目录（例如：/home/sftp 或 /data/sftp）
#
# 目录结构（最终）：
#   BASE_DIR/username          -> chroot 根目录（root:root, 755）
#   BASE_DIR/username/upload   -> 用户可写目录
#
# 示例：
#   /home/sftp/dciUser
#   └── upload
#
# ⚠️ 重要限制（必须理解）：
#   1. chroot 根目录必须是 root:root 且不可写
#      否则登录报错：
#      "bad ownership or modes for chroot directory"
#
#   2. 用户不能直接写根目录，只能写子目录（如 upload）
#
#   3. 所有 sftpusers 组用户共享同一规则：
#      ChrootDirectory BASE_DIR/%u
#
#   4. 此脚本不会覆盖已有 sshd_config 中的 Match Group 配置
#
#   5. shell 设置为 /usr/sbin/nologin → 禁止 SSH，只允许 SFTP
#
#   6. 仅适用于：
#      ✔ 单机
#      ✔ 少量用户
#      ✘ 不适用于复杂多租户隔离（需额外设计）
#
# ============================================================

set -e

# ===== 用户输入 =====
read -p "请输入用户名: " USER_NAME
read -p "请输入SFTP根目录 (例如 /home/sftp): " BASE_DIR

GROUP_NAME="sftpusers"
UPLOAD_DIR="${BASE_DIR}/${USER_NAME}/upload"

echo "== 创建组（如果不存在） =="
if ! getent group ${GROUP_NAME} >/dev/null; then
    sudo groupadd ${GROUP_NAME}
fi

echo "== 创建用户（如果不存在） =="
if ! id ${USER_NAME} >/dev/null 2>&1; then
    sudo useradd -m -g ${GROUP_NAME} -s /usr/sbin/nologin ${USER_NAME}
    echo "请为用户设置密码:"
    sudo passwd ${USER_NAME}
fi

echo "== 创建目录结构 =="
sudo mkdir -p ${UPLOAD_DIR}

echo "== 设置权限（关键） =="

# chroot 根目录必须 root 拥有
sudo chown root:root ${BASE_DIR}/${USER_NAME}
sudo chmod 755 ${BASE_DIR}/${USER_NAME}

# 用户可写目录
sudo chown ${USER_NAME}:${GROUP_NAME} ${UPLOAD_DIR}
sudo chmod 755 ${UPLOAD_DIR}

echo "== 配置 sshd =="

SSHD_CONFIG="/etc/ssh/sshd_config"

# 备份配置
sudo cp ${SSHD_CONFIG} ${SSHD_CONFIG}.bak_$(date +%F_%H-%M-%S)

# 写入 Match Group（仅在不存在时）
if ! grep -q "Match Group ${GROUP_NAME}" ${SSHD_CONFIG}; then
    {
        echo ""
        echo "Match Group ${GROUP_NAME}"
        echo "    ChrootDirectory ${BASE_DIR}/%u"
        echo "    ForceCommand internal-sftp"
        echo "    X11Forwarding no"
        echo "    AllowTcpForwarding no"
    } | sudo tee -a ${SSHD_CONFIG} >/dev/null
else
    echo "⚠️ 已存在 Match Group ${GROUP_NAME}，未修改配置"
    echo "请确认 ChrootDirectory 是否为 ${BASE_DIR}/%u"
fi

echo "== 检查 SSH 配置 =="
sudo sshd -t

echo "== 重启 SSH 服务 =="
sudo systemctl restart sshd || sudo systemctl restart ssh

echo "== 完成 =="
echo "测试: sftp ${USER_NAME}@yourserver"
