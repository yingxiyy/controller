package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.OpNodeMerger;
import net.flex.dci.otn.controller.allocate.common.service.MyExecutor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RemovedTunnelQueue {

    @Autowired
    private TunnelDao tunnelDao;
    private final BlockingQueue<RemoveData> queue = new LinkedBlockingQueue<>();

    @Autowired
    private MyExecutor executor;

    /**
     * 添加 tunnel 到待删除队列（线程安全）
     */
    public void add(TaskInfoMessage taskInfoMessage, RemoveTunnelInput removeData) {
        try {
            queue.put(new RemoveData(taskInfoMessage, removeData));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while adding tunnel to removal queue", e);
        }
    }

    /**
     * 后台线程：持续消费删除任务
     */
    @PostConstruct
    public void launch() {
        executor.lazyDo(() -> {
            try {
                TimeUnit.SECONDS.sleep(30); // 延迟启动，等待系统稳定
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            OpNodeMerger opMerger = new OpNodeMerger();
            log.debug("start loop for fetch removing tunnel from queue");
            while (true) {
                try {
                    // 第一层：阻塞等待第一个数据
                    RemoveData firstData = queue.take();
                    Set<String> needMerged = new HashSet<>();
                    needMerged.addAll(processRemove(firstData));

                    // 第二层：非阻塞处理队列中剩余的所有数据
                    List<RemoveData> remaining = new ArrayList<>();
                    queue.drainTo(remaining);  // 取出所有剩余数据

                    for (RemoveData data : remaining) {
                        needMerged.addAll(processRemove(data));
                    }

                    // 处理完当前所有数据后再合并
                    needMerged.forEach(opMerger::merge);

                } catch (Exception e) {
                    log.error("Error while processing tunnel removal", e);
                }
            }
        });
    }

    private List<String> processRemove(RemoveData data) {
        ZkResourceLock resourceLocker = new ZkResourceLock();

        String tunnelId = data.getRemoveData().getTunnelId();
        try {
            Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
            lockResource(tunnel, resourceLocker);
            log.debug("fetch a tunnel from queue, start removeing {}", tunnelId);
            TunnelRemover remover = new TunnelRemover();
            remover.startRemove(data.getTaskInfoMessage(), tunnel, data.getRemoveData().isForce(), data.getRemoveData().isForceDb());
            return remover.getNeedMerged();
        } catch (Exception e) {
            log.error("Failed to remove tunnel: {}", tunnelId, e);
        } finally {
            resourceLocker.unlock();
        }
        return new ArrayList<>();
    }

    /**
     * 锁定 tunnel 相关资源（tunnelId + ochLinkId + siteLinkId）
     */
    private void lockResource(Tunnel tunnel, ZkResourceLock resourceLocker) {
        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

        resourceLocker.addResource(tunnel.getTunnelId().getValue());

        for (SupportingLink sl : tunnel.getSupportingLink()) {
            String ochLinkId = sl.getLinkRef().getValue();
            if (OchLinkIdNamingRule.isOchLink(ochLinkId)) {
                resourceLocker.addResource(ochLinkId);

                Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
                if (ochLink != null) {
                    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink ochSL : ochLink.getSupportingLink()) {
                        String siteLinkId = ochSL.getLinkRef().getValue();
                        if (SiteLinkIdNamingRule.isSiteLink(siteLinkId)) {
                            resourceLocker.addResource(siteLinkId);
                        }
                    }
                }
            }
        }

        resourceLocker.getLock(); // 阻塞直到获取锁
    }

    /**
     * 内部类：存储待删除 tunnel 任务
     */
    @Data
    private static class RemoveData {
        private final TaskInfoMessage taskInfoMessage;
        private final RemoveTunnelInput removeData;
    }
}
