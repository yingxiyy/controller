/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.lib;

import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.constants.DciClientConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;

@Slf4j
public class ZkClient {

    public static final int DEFAULT_CONN_TIMEOUT = 8000;// ms
    public static final int DEFAULT_MAX_RETRY_NUM = 5;
    public static final int DEFAULT_RETRY_INTERVAL = 2000;
    private static final ZkClient INSTANCE = new ZkClient();
    private CuratorFramework client;
    private volatile ConnectionState state;
    private ScheduledExecutorService executor;
    private long sessionId;

    private ZkClient() {
        try {
            init();
        } catch (Exception e) {
            log.error("init zookeeper client error", e);
        }
    }

    public static ZkClient getInstance() {
        return INSTANCE;
    }

    public CuratorFramework getClient() {
        return client;
    }

    private void init() {
        log.info("connecting zookeeper...");
        // 1 重试策略：默认不重试，连接超时后就LOST
        RetryPolicy retryPolicy = new RetryNTimes(DEFAULT_MAX_RETRY_NUM, DEFAULT_RETRY_INTERVAL);

        // 2 通过工厂创建连接
        String cluster = ZooKeeperConfig.instance().getZookeeperServer();
        if (StringUtils.isEmpty(cluster)) {
            log.error("can't find zookeeper info, batchlock will not work");
            return;
        }
        client = CuratorFrameworkFactory.builder()
                .namespace(DciClientConstants.NAMESPACE)
                .connectString(cluster)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(DEFAULT_CONN_TIMEOUT)
                .sessionTimeoutMs(5000)
                .build();

        // 添加对client连接状态的监听
        addClientListener();

        start();
    }

    private void start() {
        // 3 开启连接
        if (CuratorFrameworkState.STARTED == client.getState()) {
            return;
        }
        client.start();
        log.info("zookeeper client started");
        try {
            client.blockUntilConnected();
            setSessionId();
        } catch (Exception e) {
            log.error("connect to zk error", e);
            client = null;
            return;
        }

        // 创建根节点
//        try {
//            if (null == client.checkExists().forPath(ROOT)) {
//                client.create().forPath(ROOT);
//            }
//        } catch (Exception e) {
//            log.error("create root node error", e);
//        }
    }

    private void addClientListener() {
        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            log.info("ZkClient listener work, connect state:" + connectionState);
            String zooKeeperInfo = null;
            try {
                // 获取每个client所连接的zk信息
                // State:CONNECTED Timeout:40000 sessionid:0x1000064d24e0003
                // local:/127.0.0.1:63795 remoteserver:127.0.0.1/127.0.0.1:2181 lastZxid:2586
                // xid:19 sent:25 recv:25 queuedpkts:0 pendingresp:0 queuedevents:0
                if (curatorFramework.getZookeeperClient().getZooKeeper().getSessionId()
                        != sessionId) {
                    //not my session change, discard.
                    return;
                }
                zooKeeperInfo = curatorFramework.getZookeeperClient().getZooKeeper().toString();
                log.info("zooKeeperInfo = {}", zooKeeperInfo);
            } catch (Exception e) {
                log.error("failed to get zooKeeperInfo", e);
            }

            // 获取client连接zk的ip
            String zkIp = getRemoteServer(zooKeeperInfo);
            log.info(connectionState + " to zk server:" + zkIp);
            switch (connectionState) {
                case CONNECTED:
                    handleConnected();
                    setSessionId();
                    break;
                case SUSPENDED:
                    handleSuspended();
                    break;
                case LOST:
                    handleLost();
                    break;
                case RECONNECTED:
                    handleReconnected(executor, curatorFramework);
                    setSessionId();
                    break;
                default:
                    break;
            }
        });
    }

    private void setSessionId() {
        try {
            sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConnected() {
        // 服务注册完后client状态为connected,state初始值为connected
        state = ConnectionState.CONNECTED;
    }

    private void handleSuspended() {
        log.warn("disconnect! try to reconnect......");
        state = ConnectionState.SUSPENDED;
    }

    private void handleReconnected(ScheduledExecutorService executor,
            CuratorFramework curatorFramework) {
        log.info("handleReconnected. state: " + state);
        // 修改state为reconnected
        state = ConnectionState.RECONNECTED;
    }

    private void handleLost() {
        // 超过sessionTimeOut后client状态为lost,会话失效
        state = ConnectionState.LOST;
    }

    private String getRemoteServer(String zooKeeperInfo) {
        if (StringUtils.isEmpty(zooKeeperInfo)) {
            return "";
        }
        String regex = "(\\d+\\.\\d+\\.\\d+\\.\\d+\\:(\\d+))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(zooKeeperInfo);
        while (matcher.find(zooKeeperInfo.indexOf("remoteserver"))) {
            return matcher.group().trim();
        }
        return "";
    }
}