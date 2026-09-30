#!/bin/bash

# 检查ZooKeeper结构和服务状态
source "$(dirname "$0")/config-loader.sh"

echo "=== 检查 ZK 根目录 ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "/opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh -server 127.0.0.1:2190 <<< 'ls /' 2>/dev/null | grep -E '^\[.*\]$' | head -1"

echo ""
echo "=== 检查 /dciworld ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "/opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh -server 127.0.0.1:2190 <<< 'ls /dciworld' 2>/dev/null | grep -E '^\[.*\]$' | head -1"

echo ""
echo "=== 检查 /dciworld/default ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "/opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh -server 127.0.0.1:2190 <<< 'ls /dciworld/default' 2>/dev/null | grep -E '^\[.*\]$' | head -1"

echo ""
echo "=== 检查 /dciworld/default/STATE ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "/opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh -server 127.0.0.1:2190 <<< 'ls /dciworld/default/STATE' 2>/dev/null | grep -E '^\[.*\]$' | head -1"

echo ""
echo "=== 检查 /STATE ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "/opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh -server 127.0.0.1:2190 <<< 'ls /STATE' 2>/dev/null | grep -E '^\[.*\]$' | head -1"

echo ""
echo "=== 检查服务进程 ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "ps aux | grep device-maintenance | grep -v grep"

echo ""
echo "=== 检查端口 18008 ==="
sshpass -p "$SIT_SSH_PASSWORD" ssh -o StrictHostKeyChecking=no root@$SIT_SERVER_HOST \
  "netstat -tlnp | grep :18008 || ss -tlnp | grep :18008"
