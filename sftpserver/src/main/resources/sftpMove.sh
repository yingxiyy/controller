#!/bin/bash
#
# ============================================================
# Script Name: moveSFTP.sh
# Description:
#   将已有 SFTP 用户从旧目录迁移到新目录
#
# 功能：
#   - 创建新目录结构
#   - 复制原有数据
#   - 更新 sshd_config 中的 ChrootDirectory
#   - 重启 SSH 服务
#
# 使用方式：
#   chmod +x moveSFTP.sh
#   ./moveSFTP.sh
#
# 输入参数（交互）：
#   用户名（例如：dciUser）
#   旧目录（例如：/sftp）
#   新目录（例如：/home/sftp）
#
# 示例迁移：
#   /sftp/dciUser  →  /home/sftp/dciUser
#
# ⚠️ 重要限制：
#
#   1. 仅支持单用户迁移（脚本未做批量处理）
#
#   2. 要求 sshd_config 中使用：
#      ChrootDirectory OLD_BASE/%u
#
#   3. chroot 权限规则必须满足：
#      - 新目录 root:root
#      - 子目录用户可写
#
#   4. 脚本使用 cp -a：
#      ✔ 保留权限/时间
#      ✘ 不适合超大数据（建议用 rsync）
#
#   5. 不会删除旧目录（需手动确认后删除）
#
#   6. 执行期间若有用户上传：
#      ❗ 可能出现数据不同步（建议低峰执行）
#
# ============================================================

set -e

# ===== 用户输入 =====
read -p "请输入用户名: " USER_NAME
read -p "请输入旧目录 (例如 /sftp): " OLD_BASE
read -p "请输入新目录 (例如 /home/sftp): " NEW_BASE

GROUP_NAME="sftpusers"

OLD_UPLOAD="${OLD_BASE}/${USER_NAME}/upload"
NEW_UPLOAD="${NEW_BASE}/${USER_NAME}/upload"

echo "== 创建新目录 =="
sudo mkdir -p ${NEW_UPLOAD}

echo "== 设置权限 =="

# chroot 根目录
sudo chown root:root ${NEW_BASE}/${USER_NAME}
sudo chmod 755 ${NEW_BASE}/${USER_NAME}

# 用户目录
sudo chown ${USER_NAME}:${GROUP_NAME} ${NEW_UPLOAD}
sudo chmod 755 ${NEW_UPLOAD}

echo "== 迁移数据 =="

if [ -d "${OLD_UPLOAD}" ]; then
    sudo cp -a ${OLD_UPLOAD}/. ${NEW_UPLOAD}/
else
    echo "⚠️ 未找到旧数据目录: ${OLD_UPLOAD}"
fi

echo "== 备份 sshd_config =="

SSHD_CONFIG="/etc/ssh/sshd_config"
sudo cp ${SSHD_CONFIG} ${SSHD_CONFIG}.bak_$(date +%F_%H-%M-%S)

echo "== 修改 ChrootDirectory =="

sudo sed -i "s|ChrootDirectory ${OLD_BASE}/%u|ChrootDirectory ${NEW_BASE}/%u|g" ${SSHD_CONFIG}

echo "== 检查 SSH 配置 =="

sudo sshd -t

echo "== 重启 SSH =="

sudo systemctl restart sshd || sudo systemctl restart ssh

echo "== 完成 =="

echo "测试: sftp ${USER_NAME}@yourserver"
echo "确认正常后可删除旧目录:"
echo "sudo rm -rf ${OLD_BASE}/${USER_NAME}"