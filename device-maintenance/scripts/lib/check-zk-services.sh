#!/bin/bash

echo "=== 检查 ZooKeeper /STATE 目录下的实际注册服务 ==="

# 连接到远程服务器检查 ZK
sshpass -p 'pureg123!@#$' ssh -o StrictHostKeyChecking=no actsvt@116.128.204.13 '
cd /opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin

# 直接使用 zkCli.sh 交互模式
echo "ls /STATE" | ./zkCli.sh -server 127.0.0.1:2190 2>/dev/null | grep -E "^\[" | head -1

echo "---"
echo "检查其他可能的服务目录："

# 检查 /default 路径
echo "ls /default" | ./zkCli.sh -server 127.0.0.1:2190 2>/dev/null | grep -E "^\[" | head -1

# 也检查根目录下其他可能的服务注册路径
echo "ls /" | ./zkCli.sh -server 127.0.0.1:2190 2>/dev/null | grep -E "^\[" | head -1
'

echo "=== 检查完成 ==="
