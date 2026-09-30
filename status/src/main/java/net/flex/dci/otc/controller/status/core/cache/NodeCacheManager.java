package net.flex.dci.otc.controller.status.core.cache;

import static net.flex.dci.otc.controller.status.util.Constants.OD_PREFIX;
import static net.flex.dci.otc.controller.status.util.Constants.TD_PREFIX;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otc.mongo.dto.SupportingRackDto;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * 2026/4/2
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
public class NodeCacheManager {

    private final PhyNodeDao phyNodeDao;

    private final SiteNodeDao siteNodeDao;

    private final LoadingCache<String, Node> configNodeCache;

    private final LoadingCache<String, Node> opNodeCache;

    private final LoadingCache<String, Node> siteNodeCache;

    private final LoadingCache<String, Node> siteByPhyNodeCache;

    private final LoadingCache<String, List<LinkStateDto>> tunnelCache;

    private final LoadingCache<String, List<LinkStateDto>> siteLinkCache;

    private final LoadingCache<String, List<LinkStateDto>> ochLinkCache;

    private final LoadingCache<String, SupportingRackDto> rackInfoByNeIdCache;


    private final OchLinkDao ochLinkDao;

    private final SiteLinkDao siteLinkDao;

    private final TunnelDao tunnelDao;

    private final RackDao rackDao;

    private final ConnectionCacheManager connectionCacheManager;

    private final Semaphore dbLoadSemaphore = new Semaphore(100);

    public NodeCacheManager(PhyNodeDao phyNodeDao, SiteNodeDao siteNodeDao,
            OchLinkDao ochLinkDao, SiteLinkDao siteLinkDao, TunnelDao tunnelDao, RackDao rackDao,
            ConnectionCacheManager connectionCacheManager) {
        this.phyNodeDao = phyNodeDao;
        this.siteNodeDao = siteNodeDao;
        this.ochLinkDao = ochLinkDao;
        this.siteLinkDao = siteLinkDao;
        this.tunnelDao = tunnelDao;
        this.rackDao = rackDao;
        this.connectionCacheManager = connectionCacheManager;
        this.configNodeCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.SECONDS) // 配置缓存10秒
                .recordStats()
                .build(phyNodeDao::getConfigPhyNodeById);

        this.opNodeCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.SECONDS) // 运维缓存10秒
                .recordStats()
                .build(phyNodeDao::getOpPhyNodeById);

        this.siteNodeCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(10, TimeUnit.SECONDS) // 站点缓存10秒
                .recordStats()
                .build(siteNodeDao::getSiteNodeById);

        this.siteByPhyNodeCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.SECONDS) // 拓扑关联缓存10秒
                .recordStats()
                .build(siteNodeDao::getSiteByPhyNodeId);

        this.tunnelCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(5, TimeUnit.SECONDS) // 拓扑缓存5秒
                .recordStats()
                .build(this::loadFromDatabase);

        this.siteLinkCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(5, TimeUnit.SECONDS) // 拓扑缓存5秒
                .recordStats()
                .build(siteLinkDao::queryLinkStateDtoWithNode);

        this.ochLinkCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(5, TimeUnit.SECONDS) // 拓扑缓存5秒
                .recordStats()
                .build(this::loadOchFromDatabase);

        this.rackInfoByNeIdCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .recordStats()
                .build(rackDao::getNeRefRackByNeId);


    }

    private List<LinkStateDto> loadOchFromDatabase(@NonNull String key) {
        if (!acquireDbLoadPermit(key)) {
            return Collections.emptyList();
        }
        try {
            log.info("Cache miss for ochLinks, loading from DB for key:{}", key);
            if (key.startsWith(OD_PREFIX)) {
                return loadODOchLinks(key.replace(OD_PREFIX, ""));
            } else if (key.startsWith(TD_PREFIX)) {
                return loadTDOchLinks(key.replace(TD_PREFIX, ""));
            }
            log.warn("Invalid ochLink cache key format: {}", key);
            return Collections.emptyList();
        } finally {
            dbLoadSemaphore.release();
        }
    }

    private List<LinkStateDto> loadTDOchLinks(String neId) {
        return ochLinkDao.queryLinkStateWithNode(neId);
    }

    private List<LinkStateDto> loadODOchLinks(String neId) {
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsByPhyNodeId(
                Collections.singletonList(neId));
        List<LinkStateDto> ochLinks = ochLinkDao.getAllBusinessOchLinkStatesUnderSiteLinkIds(
                siteLinkIds);
        return ochLinks;
    }

    private List<LinkStateDto> loadFromDatabase(@NonNull String key) {
        if (!acquireDbLoadPermit(key)) {
            return Collections.emptyList();
        }
        try {
            log.info("Cache miss for tunnels, loading from DB for key: {}", key);

            if (key.startsWith(OD_PREFIX)) {
                return loadODTunnels(key.replace(OD_PREFIX, ""));
            } else if (key.startsWith(TD_PREFIX)) {
                return loadTDTunnels(key.replace(TD_PREFIX, ""));
            }
            log.warn("Invalid tunnel cache key format: {}", key);
            return Collections.emptyList();
        } finally {
            dbLoadSemaphore.release();
        }
    }

    /**
     * 获取数据库加载许可，带超时
     */
    private boolean acquireDbLoadPermit(String key) {
        long startWait = System.currentTimeMillis();
        try {
            if (!dbLoadSemaphore.tryAcquire(10, TimeUnit.SECONDS)) {
                log.warn(
                        "Timeout waiting for DB load permit, key: {}, waitTime={}ms, availablePermits={}",
                        key, System.currentTimeMillis() - startWait,
                        dbLoadSemaphore.availablePermits());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for DB load permit, key: {}", key);
            return false;
        }
        long waitTime = System.currentTimeMillis() - startWait;
        if (waitTime > 1000) {
            log.info("Waited {}ms for DB load permit, key: {}, availablePermits={}",
                    waitTime, key, dbLoadSemaphore.availablePermits() + 1);
        }
        return true;
    }

    private List<LinkStateDto> loadODTunnels(String neId) {
        log.debug("Loading ODTunnels from DB for neId: {}", neId);
        List<LinkStateDto> siteLinks = siteLinkCache.get(neId);
        if (siteLinks.isEmpty()) {
            log.debug("No siteLinks found for neId: {}, return empty tunnels", neId);
            return Collections.emptyList();
        }

        List<String> siteLinkIds = siteLinks.stream()
                .map(LinkStateDto::getId)
                .collect(Collectors.toList());

        List<LinkStateDto> ochLinks = connectionCacheManager.getOchLinksBySiteLinkIds(siteLinkIds);
        if (ochLinks.isEmpty()) {
            log.debug("No ochLinks found for siteLinkIds: {}, return empty tunnels", siteLinkIds);
            return Collections.emptyList();
        }

        List<String> ochLinkIds = ochLinks.stream()
                .map(LinkStateDto::getId)
                .collect(Collectors.toList());

        List<LinkStateDto> tunnels = tunnelDao.getAllTunnelsStateUnderOchLink(ochLinkIds);
        log.debug("Loaded ODTunnels from DB: neId={}, tunnelCount={}", neId, tunnels.size());
        return tunnels;
    }

    private List<LinkStateDto> loadTDTunnels(String neId) {
        log.debug("Loading TDTunnels from DB for neId: {}", neId);
        List<LinkStateDto> tunnels = tunnelDao.queryTunnelsStateWithNode(neId);
        log.debug("Loaded TDTunnels from DB: neId={}, tunnelCount={}", neId, tunnels.size());
        return tunnels;
    }


    public List<LinkStateDto> getTunnelsByNeId(String neId, Supplier<List<LinkStateDto>> loader) {
        return tunnelCache.get(neId, key -> loader.get());
    }

    public void invalidateTunnelCache(String neId) {
        tunnelCache.invalidate(neId);
        log.debug("Invalidated tunnel cache for NE: {}", neId);
    }

    public List<LinkStateDto> getOchLinksByNeId(NodeType nodeType, String neId) {
        String key = (nodeType == NodeType.TD || nodeType == NodeType.TPC4) ? TD_PREFIX : OD_PREFIX;
        return ochLinkCache.get(key + neId);
    }

    public List<LinkStateDto> getSiteLinksByNeId(String neId) {
        return siteLinkCache.get(neId);
    }


    public void invalidateAllTopologyCache(String neId) {
        tunnelCache.invalidate(neId);
        siteLinkCache.invalidate(neId);
        ochLinkCache.invalidate(neId);
        log.info("Invalidated all topology cache for NE: {}", neId);
    }

    public Map<String, CacheStats> getTopologyCacheStats() {
        Map<String, CacheStats> stats = new HashMap<>();
        stats.put("tunnelCache", tunnelCache.stats());
        stats.put("siteLinkCache", siteLinkCache.stats());
        stats.put("ochLinkCache", ochLinkCache.stats());
        return stats;
    }

    public Node getConfigNode(String neId) {
        Node node = configNodeCache.get(neId);
        log.debug("getConfigNode: neId={}, cacheHit={}", neId, node != null);
        return node;
    }

    public Node getOpNode(String neId) {
        Node node = opNodeCache.get(neId);
        log.debug("getOpNode: neId={}, cacheHit={}", neId, node != null);
        return node;
    }

    public Node getSiteNode(String siteId) {
        Node node = siteNodeCache.get(siteId);
        log.debug("getSiteNode: siteId={}, cacheHit={}", siteId, node != null);
        return node;
    }

    public Node getSiteByPhyNode(String phyNodeId) {
        return siteByPhyNodeCache.get(phyNodeId);
    }

    public Map<String, Node> getConfigNodes(List<String> neIds) {
        return getBatch(neIds, configNodeCache, phyNodeDao, false);
    }

    public Map<String, Node> getOpNodes(List<String> neIds) {
        return getBatch(neIds, opNodeCache, phyNodeDao, true);
    }

    public CacheStats getTunnelCacheStats() {
        return tunnelCache.stats();
    }

    public void invalidateConfigNode(String neId) {
        configNodeCache.invalidate(neId);
        log.debug("Invalidated configNode cache for NE: {}", neId);
    }

    public void invalidateOpNode(String neId) {
        opNodeCache.invalidate(neId);
        log.debug("Invalidated opNode cache for NE: {}", neId);
    }

    public void invalidateSiteNode(String siteId) {
        siteNodeCache.invalidate(siteId);
        log.debug("Invalidated siteNode cache for siteId: {}", siteId);
    }

    public void invalidateSiteByPhyNode(String phyNodeId) {
        siteByPhyNodeCache.invalidate(phyNodeId);
        log.debug("Invalidated siteByPhyNode cache for phyNodeId: {}", phyNodeId);
    }

    public void invalidateAll() {
        configNodeCache.invalidateAll();
        opNodeCache.invalidateAll();
        siteNodeCache.invalidateAll();
        siteByPhyNodeCache.invalidateAll();
        rackInfoByNeIdCache.invalidateAll();
        log.info("Invalidated all node caches");
    }

    public void invalidateRackCache() {
        rackInfoByNeIdCache.invalidateAll();
        log.info("Invalidated rack cache");
    }

    public void invalidateRackByNeId(String neId) {
        rackInfoByNeIdCache.invalidate(neId);
        log.debug("Invalidated rack cache for NE: {}", neId);
    }

    private Map<String, Node> getBatch(List<String> neIds,
            LoadingCache<String, Node> cache,
            PhyNodeDao dao, boolean isOperational) {
        if (neIds == null || neIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Node> result = new HashMap<>();
        List<String> missingIds = new ArrayList<>();

        neIds.stream()
                .distinct()
                .forEach(id -> {
                    Node node = cache.getIfPresent(id);
                    if (node != null) {
                        result.put(id, node);
                    } else {
                        missingIds.add(id);
                    }
                });

        if (!missingIds.isEmpty()) {
            log.debug("Batch loading {} nodes from DB, type={}", missingIds.size(),
                    isOperational ? "operational" : "config");
            List<Node> nodes = isOperational ? dao.listOperPhyNodeByIds(missingIds)
                    : dao.listConfigPhyNodeByIds(missingIds);
            for (Node node : nodes) {
                String id = node.getNodeId().getValue();
                cache.put(id, node);
                result.put(id, node);
            }
        }

        return result.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void invalidatePhyNode(String neId) {
        invalidateConfigNode(neId);
        invalidateOpNode(neId);
        invalidateRackByNeId(neId);
        log.debug("Invalidated phyNode caches for NE: {}", neId);
    }

    public List<LinkStateDto> getTunnelsByNe(NodeType nodeType, String phyNodeId) {
        String keyPrefix;
        if (nodeType == NodeType.OD || nodeType == NodeType.OPC4) {
            keyPrefix = OD_PREFIX;
        } else if (nodeType == NodeType.TD || nodeType == NodeType.TPC4) {
            keyPrefix = TD_PREFIX;
        } else {
            log.warn("Unsupported node type for tunnel cache: {}", nodeType);
            return Collections.emptyList();
        }
        return tunnelCache.get(keyPrefix + phyNodeId);
    }


    public List<LinkStateDto> getOchLinksByNe(NodeType nodeType, String phyNodeId) {
        String keyPrefix;
        if (nodeType == NodeType.OD || nodeType == NodeType.OPC4) {
            keyPrefix = OD_PREFIX;
        } else if (nodeType == NodeType.TD || nodeType == NodeType.TPC4) {
            keyPrefix = TD_PREFIX;
        } else {
            log.warn("Unsupported node type for tunnel cache: {}", nodeType);
            return Collections.emptyList();
        }
        return ochLinkCache.get(keyPrefix + phyNodeId);
    }


    public SupportingRackDto getNeRefRackByNeId(String neId) {
        return rackInfoByNeIdCache.get(neId);
    }


    public void invalidateRackCacheByNeIds(List<String> neIds) {
        if (neIds == null || neIds.isEmpty()) {
            log.warn("No neIds provided for rack cache invalidation");
            return;
        }
        for (String neId : neIds) {
            invalidateRackByNeId(neId);
        }
        log.info("invalidated rack cache for {} NEs", neIds.size());
    }
}
