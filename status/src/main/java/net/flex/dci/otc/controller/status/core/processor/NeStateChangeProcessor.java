package net.flex.dci.otc.controller.status.core.processor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.IStateProcessor;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.handler.StateChangeChainHandler;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/4 16:10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeStateChangeProcessor implements IStateProcessor {

    private final StateChangeChainHandler stateChainHandler;

    private final NodeCacheManager nodeCacheManager;

    private final ConnectionCacheManager connectionCacheManager;

    @Autowired
    @Qualifier("nodeStateExecutor")
    private ExecutorService eventExecutor;


    @Override
    public void process(String neId) {
        log.info("Processing state change for neId: {}", neId);
        long startTime = System.currentTimeMillis();
        Node phyNode = nodeCacheManager.getConfigNode(neId);
        if (phyNode == null) {
            log.error("Invalid phy node id:{}, discard it", neId);
            return;
        }
        log.info("Loaded phyNode for neId: {}", neId);
        stateChainHandler.executeAllStateChangeChainHandle(phyNode);
        long cost = System.currentTimeMillis() - startTime;
        log.info("Completed state change for neId: {}, cost={}ms", neId, cost);
    }

    @Override
    public void processBatch(List<String> neIds) {
        if (neIds == null || neIds.isEmpty()) {
            return;
        }
        log.info("[MONITOR] processBatch start: {} NEs", neIds.size());
        long startTime = System.currentTimeMillis();
        Map<String, Node> nodeMap = nodeCacheManager.getConfigNodes(neIds);

        if (nodeMap.isEmpty()) {
            log.warn("[MONITOR] processBatch: no valid nodes found");
            return;
        }
        log.info("[MONITOR] processBatch loaded {} nodes", nodeMap.size());
//        nodeMap.values().forEach(phyNode -> {
//            long t = System.currentTimeMillis();
//            try {
//                stateChainHandler.executeAllStateChangeChainHandle(phyNode);
//            } catch (Exception e) {
//                log.error("Failed to process neId:{}", phyNode.getNodeId().getValue(), e);
//            }
//            long cost = System.currentTimeMillis() - t;
//            if (cost > 1000) {
//                log.warn("[MONITOR] slow NE: {} cost={}ms",
//                        phyNode.getNodeId().getValue(), cost);
//            }
//        });
        List<CompletableFuture<Void>> futures = nodeMap.values().stream()
                .map(phyNode -> CompletableFuture.runAsync(() -> {
                    stateChainHandler.executeAllStateChangeChainHandle(phyNode);
                }, eventExecutor))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            log.warn("processBatch timeout: {} NE not finished within 30s", futures.size());
        }
        long cost = System.currentTimeMillis() - startTime;
        log.info("[MONITOR] processBatch end: {} NEs, total={}ms", nodeMap.size(), cost);
    }

    @Override
    public void processRemove(String neId) {
        log.info("Processing remove for neId: {}", neId);
        long startTime = System.currentTimeMillis();
        nodeCacheManager.invalidatePhyNode(neId);
        nodeCacheManager.invalidateAllTopologyCache(neId);
        log.info("Invalidated all caches for neId: {}", neId);
//        String siteId = PhysicalNodeIdNamingRule.getSiteId(neId);
        stateChainHandler.executeAllNodeRemoveStateChangeChainHandle(neId);
        long cost = System.currentTimeMillis() - startTime;

    }


    @Override
    public void processRemoveBatch(List<String> removeNeIds) {
        log.info("Processing remove batch: {} NEs", removeNeIds.size());
        long startTime = System.currentTimeMillis();
        for (String neId : removeNeIds) {
            try {
                nodeCacheManager.invalidatePhyNode(neId);
                nodeCacheManager.invalidateAllTopologyCache(neId);
                stateChainHandler.executeAllNodeRemoveStateChangeChainHandle(neId);
            } catch (Exception e) {
                log.error("Failed to process remove neId: {}", neId, e);
            }
        }
        long cost = System.currentTimeMillis() - startTime;

    }


}
