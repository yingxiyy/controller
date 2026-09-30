#!/bin/bash

echo "=== 详细检查 ZooKeeper 中的所有注册服务 ==="

sshpass -p 'pureg123!@#$' ssh -o StrictHostKeyChecking=no actsvt@116.128.204.13 '
ZK_CLI="/opt/dci/software/zookeeper/apache-zookeeper-3.6.3-bin/bin/zkCli.sh"
ZK_SERVER="127.0.0.1:2190"

echo "=== 1. 检查 /STATE 目录 ==="
echo "ls /STATE" | timeout 5 $ZK_CLI -server $ZK_SERVER 2>/dev/null | grep -E "^\[.*\]" | tr "," "\n" | sed "s/\[//g" | sed "s/\]//g" | sed "s/ //g" | grep -v "^$" | sort

echo ""
echo "=== 2. 检查 /default 目录 ==="
echo "ls /default" | timeout 5 $ZK_CLI -server $ZK_SERVER 2>/dev/null | grep -E "^\[.*\]"

echo ""
echo "=== 3. 检查 /dciworld 目录 ==="
echo "ls /dciworld" | timeout 5 $ZK_CLI -server $ZK_SERVER 2>/dev/null | grep -E "^\[.*\]"

echo ""
echo "=== 4. 检查根目录下所有条目 ==="
echo "ls /" | timeout 5 $ZK_CLI -server $ZK_SERVER 2>/dev/null | grep -E "^\[.*\]"

echo ""
echo "=== 检查完成 ==="
'
