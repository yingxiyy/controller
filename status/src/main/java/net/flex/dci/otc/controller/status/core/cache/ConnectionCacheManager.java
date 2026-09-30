package net.flex.dci.otc.controller.status.core.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

/**
 * 2026/4/2
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Component
@Slf4j
public class ConnectionCacheManager {

    private final PhyLinkDao phyLinkDao;

    private LoadingCache<String, List<LinkStateDto>> linksByNodeCache;
    private LoadingCache<String, LinkStateDto> linkByIdCache;
    private LoadingCache<String, LinkStateDto> ochLinkByIdCache;
    private LoadingCache<String, LinkStateDto> siteLinkByIdCache;
    private LoadingCache<String, LinkStateDto> tunnelByIdCache;
    private LoadingCache<String, List<LinkStateDto>> linksByTpCache;
    private LoadingCache<String, List<LinkStateDto>> linksByEquipCache;

    private LoadingCache<String, List<LinkStateDto>> tunnelsByTpCache;

    private LoadingCache<String, List<LinkStateDto>> phyLinkToSiteLinkCache;

    private LoadingCache<String, List<LinkStateDto>> ochLinkToTunnelCache;

    private LoadingCache<String, List<LinkStateDto>> siteLinkToOchLinkCache;

    private LoadingCache<String, List<LinkStateDto>> phyLinkToOchLinkCache;

    private final SiteLinkDao siteLinkDao;

    private final OchLinkDao ochLinkDao;

    private final TunnelDao tunnelDao;

    private final Semaphore dbLoadSemaphore = new Semaphore(500);

    public ConnectionCacheManager(PhyLinkDao phyLinkDao, SiteLinkDao siteLinkDao,
            OchLinkDao ochLinkDao, TunnelDao tunnelDao) {
        this.phyLinkDao = phyLinkDao;

        linksByNodeCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key, phyLinkDao::getPhyLinksStateByNodeId,
                        "PhyLinksByNode"));

        linkByIdCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(
                        new CacheLoader<String, LinkStateDto>() {
                            @Override
                            public @Nullable LinkStateDto load(@NonNull String key)
                                    throws Exception {
                                return loadWithPermit(key,
                                        phyLinkDao::getPhyLinkStateByLinkId, "PhyLinkById");
                            }

                            @Override
                            public Map<String, LinkStateDto> loadAll(
                                    Iterable<? extends String> keys) throws Exception {
                                List<String> keyList = new ArrayList<>();
                                for (String key : keys) {
                                    keyList.add(key);
                                }
                                return loadAllWithPermit(
                                        keyList,
                                        phyLinkDao::getAllPhyLinkStateByLinkIds,
                                        LinkStateDto::getId,
                                        "PhyLinkById-batch");
                            }
                        }
                );

        ochLinkByIdCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(new CacheLoader<String, LinkStateDto>() {
                           @Override
                           public @Nullable LinkStateDto load(@NonNull String key) throws Exception {
                               return loadWithPermit(key, ochLinkDao::getOchLinkLinkStateDto,
                                       "OchLinkById");
                           }

                           @Override
                           public Map<String, LinkStateDto> loadAll(
                                   Iterable<? extends String> keys) throws Exception {
                               List<String> keyList = new ArrayList<>();
                               for (String key : keys) {
                                   keyList.add(key);
                               }
                               return loadAllWithPermit(
                                       keyList,
                                       ochLinkDao::getOchLinkLinksStateDto,
                                       LinkStateDto::getId,
                                       "OchLinkById-batch");
                           }

                       }
                );

        siteLinkByIdCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(new CacheLoader<String, LinkStateDto>() {
                    @Override
                    public LinkStateDto load(String key) {

                        return loadWithPermit(key, siteLinkDao::getSiteLinkStateById,
                                "SiteLinkById");
                    }


                    @Override
                    public Map<String, LinkStateDto> loadAll(
                            Iterable<? extends String> keys) throws Exception {
                        List<String> keyList = new ArrayList<>();
                        for (String key : keys) {
                            keyList.add(key);
                        }
                        return loadAllWithPermit(
                                keyList,
                                siteLinkDao::getSiteLinkStateByIds,
                                LinkStateDto::getId,
                                "SiteLinkById-batch");
                    }
                });

        linksByTpCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key, phyLinkDao::getAllPhyLinkStateUnderTp,
                        "PhyLinksByTp"));

        linksByEquipCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key, phyLinkDao::getAllPhyLinkStateUnderEquip,
                        "PhyLinksByEquip"));
        this.siteLinkDao = siteLinkDao;
        this.ochLinkDao = ochLinkDao;

        this.ochLinkToTunnelCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key,
                        ochLinkIds -> tunnelDao.getAllTunnelsStateUnderOchLink(
                                parseOchLinkIds(ochLinkIds)),
                        "TunnelsByOchLink"));

        this.siteLinkToOchLinkCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key,
                        siteLinkIds -> ochLinkDao.getAllBusinessOchLinkStatesUnderSiteLinkIds(
                                parseSiteLinkIds(siteLinkIds)),
                        "OchLinksBySiteLink"));
        this.phyLinkToSiteLinkCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key,
                        phyLinkIds -> siteLinkDao.listAllSiteLinkStateBasedOnPhyLinks(
                                parsePhyLinkIds(phyLinkIds)),
                        "SiteLinksByPhyLink"));
        this.phyLinkToOchLinkCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key,
                        phyLinkIds -> ochLinkDao.getAllOchLinkStateUnderPhyLinkIds(
                                parsePhyLinkIds(phyLinkIds)),
                        "OchLinksByPhyLink"));
        this.tunnelsByTpCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key, tunnelDao::getAllTunnelsStateUnderTp,
                        "TunnelsByTp"));
        tunnelByIdCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .recordStats()
                .build(key -> loadWithPermit(key, tunnelDao::getTunnelStateByTunnelId,
                        "TunnelById"));
        this.tunnelDao = tunnelDao;
    }

    private <T> Map<String, T> loadAllWithPermit(
            List<String> keys,
            java.util.function.Function<List<String>, List<T>> batchLoader,
            java.util.function.Function<T, String> keyExtractor,
            String cacheName) {
        long startWait = System.currentTimeMillis();

        int permitsNeeded = 1;

        try {
            if (!dbLoadSemaphore.tryAcquire(permitsNeeded, 30, TimeUnit.SECONDS)) {
                log.warn(
                        "Couldn't get batch load permit after 30s, cache={}, keyCount={}",
                        cacheName, keys.size());
                throw new RuntimeException("Batch DB load timeout for cache: " + cacheName);
            }

            try {
                long waitTime = System.currentTimeMillis() - startWait;
                if (waitTime > 1000) {
                    log.info("Waited {}ms for batch DB load permit, cache={}, keyCount={}",
                            waitTime, cacheName, keys.size());
                }

                List<T> batchResult = batchLoader.apply(keys);

                Map<String, T> resultMap = new java.util.HashMap<>();
                for (T item : batchResult) {
                    resultMap.put(keyExtractor.apply(item), item);
                }
                return resultMap;
            } finally {
                dbLoadSemaphore.release(permitsNeeded);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DB load interrupted for cache: " + cacheName);
        }
    }

    /**
     * 带限流的缓存加载
     */
    private <T> T loadWithPermit(String key, java.util.function.Function<String, T> loader,
            String cacheName) {
        long startWait = System.currentTimeMillis();
        try {
            if (!dbLoadSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                log.warn("Timeout waiting for DB load permit, cache={}, key={}, waitTime={}ms",
                        cacheName, key, System.currentTimeMillis() - startWait);
                throw new RuntimeException("DB load timeout for cache: " + cacheName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for DB load permit, cache={}, key={}", cacheName,
                    key);
            throw new RuntimeException("DB load interrupted for cache: " + cacheName);
        }
        try {
            long waitTime = System.currentTimeMillis() - startWait;
            if (waitTime > 1000) {
                log.info("Waited {}ms for DB load permit, cache={}, key={}", waitTime, cacheName,
                        key);
            }
            return loader.apply(key);
        } finally {
            dbLoadSemaphore.release();
        }
    }


    public List<LinkStateDto> getByNodeId(String nodeId) {
        return linksByNodeCache.get(nodeId);
    }

    public LinkStateDto getById(String linkId) {
        return linkByIdCache.get(linkId);
    }

    public List<LinkStateDto> getPhyLinksByIds(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, LinkStateDto> result = linkByIdCache.getAll(linkIds);
        return new ArrayList<>(result.values());
    }

    public LinkStateDto getOchLinkById(String linkId) {
        return ochLinkByIdCache.get(linkId);
    }

    public Map<String, LinkStateDto> batchGetOchLinksByIds(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return ochLinkByIdCache.getAll(linkIds);
    }

    public List<LinkStateDto> getOchLinksByIds(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, LinkStateDto> result = ochLinkByIdCache.getAll(linkIds);
        return new ArrayList<>(result.values());
    }

    public Map<String, LinkStateDto> batchGetSiteLinksByIds(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return siteLinkByIdCache.getAll(linkIds);
    }

    public Map<String, LinkStateDto> batchGetPhyLinksByIds(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return linkByIdCache.getAll(linkIds);
    }

    public Map<String, LinkStateDto> batchGetTunnelsByIds(List<String> tunnelIds) {
        if (tunnelIds == null || tunnelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return tunnelByIdCache.getAll(tunnelIds);
    }

    public List<LinkStateDto> batchGetAllTunnelsByIds(List<String> tunnelIds) {
        if (tunnelIds == null || tunnelIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, LinkStateDto> result = tunnelByIdCache.getAll(tunnelIds);
        return new ArrayList<>(result.values());
    }

    public List<LinkStateDto> getByTpId(String tpId) {
        return linksByTpCache.get(tpId);
    }


    public List<LinkStateDto> getByEquipId(String equipId) {
        return linksByEquipCache.get(equipId);
    }

    public Map<String, List<LinkStateDto>> batchGetByNodeIds(Set<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return linksByNodeCache.getAll(nodeIds);
    }

    public Map<String, List<LinkStateDto>> batchGetByTpIds(Set<String> tpIds) {
        if (tpIds == null || tpIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return linksByTpCache.getAll(tpIds);
    }

    public Map<String, List<LinkStateDto>> batchGetByEquipIds(Set<String> equipIds) {
        if (equipIds == null || equipIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return linksByEquipCache.getAll(equipIds);
    }

    public Map<String, List<LinkStateDto>> batchGetTunnelByTpIds(Set<String> tpIds) {
        if (tpIds == null || tpIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return tunnelsByTpCache.getAll(tpIds);
    }

    public void invalidate(String key) {
        linksByNodeCache.invalidate(key);
        linkByIdCache.invalidate(key);
        linksByTpCache.invalidate(key);
        linksByEquipCache.invalidate(key);
        log.debug("Invalidated link caches for key: {}", key);
    }

    public void invalidateByNodeId(String nodeId) {
        linksByNodeCache.invalidate(nodeId);
        log.info("Invalidated linksByNodeCache for NE: {}", nodeId);
    }

    public void invalidateByLinkId(String linkId) {
        linkByIdCache.invalidate(linkId);
        siteLinkByIdCache.invalidate(linkId);
        ochLinkByIdCache.invalidate(linkId);
        siteLinkToOchLinkCache.invalidate(linkId);
        ochLinkToTunnelCache.invalidate(linkId);
        phyLinkToSiteLinkCache.invalidate(linkId);
        phyLinkToOchLinkCache.invalidate(linkId);
        log.debug("Invalidated all link caches for linkId: {}", linkId);
    }

    public void invalidateLinksCache(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return;
        }
        linkByIdCache.invalidateAll(linkIds);
        siteLinkByIdCache.invalidateAll(linkIds);
        ochLinkByIdCache.invalidateAll(linkIds);
        siteLinkToOchLinkCache.invalidateAll(linkIds);
        ochLinkToTunnelCache.invalidateAll(linkIds);
        phyLinkToSiteLinkCache.invalidateAll(linkIds);
        phyLinkToOchLinkCache.invalidateAll(linkIds);
        log.debug("Invalidated all link caches for {} links", linkIds.size());
    }

    private String buildSortedCacheKey(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream().sorted().collect(java.util.stream.Collectors.joining(","));
    }

    public void invalidateBySiteLinkIds(List<String> siteLinkIds) {
        if (siteLinkIds != null && !siteLinkIds.isEmpty()) {
            String cacheKey = buildSortedCacheKey(siteLinkIds);
            siteLinkToOchLinkCache.invalidate(cacheKey);
            log.debug("Invalidated siteLinkToOchLinkCache for key: {}", cacheKey);
        }
    }

    public void invalidateByOchLinkIds(List<String> ochLinkIds) {
        if (ochLinkIds != null && !ochLinkIds.isEmpty()) {
            String cacheKey = buildSortedCacheKey(ochLinkIds);
            ochLinkToTunnelCache.invalidate(cacheKey);
            log.debug("Invalidated ochLinkToTunnelCache for key: {}", cacheKey);
        }
    }

    public void invalidateByPhyLinkIds(List<String> phyLinkIds) {
        if (phyLinkIds != null && !phyLinkIds.isEmpty()) {
            String cacheKey = buildSortedCacheKey(phyLinkIds);
            phyLinkToSiteLinkCache.invalidate(cacheKey);
            phyLinkToOchLinkCache.invalidate(cacheKey);
            log.debug("Invalidated phyLink caches for key: {}", cacheKey);
        }
    }

    private volatile boolean statsMonitoringEnabled = true;
    private Thread statsThread;

    @PostConstruct
    public void startStatsMonitoring() {
        statsThread = new Thread(() -> {
            while (statsMonitoringEnabled) {
                try {
                    Thread.sleep(60000); // 每分钟打印一次
                    logCacheStats();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "cache-stats-monitor");
        statsThread.setDaemon(true);
        statsThread.start();
        log.info("ConnectionCacheManager stats monitoring started");
    }

    @PreDestroy
    public void stopStatsMonitoring() {
        statsMonitoringEnabled = false;
        if (statsThread != null) {
            statsThread.interrupt();
        }
        log.info("ConnectionCacheManager stats monitoring stopped");
    }

    private void logCacheStats() {
        Map<String, CacheStats> stats = getCacheStats();
        StringBuilder sb = new StringBuilder("\n========== ConnectionCache Stats ==========\n");
        stats.forEach((name, stat) -> {
            long hits = stat.hitCount();
            long misses = stat.missCount();
            long total = hits + misses;
            double hitRate = total > 0 ? (double) hits / total * 100 : 0;
            sb.append(String.format("%s: hits=%d, misses=%d, hitRate=%.1f%%, evictions=%d\n",
                    name, hits, misses, hitRate, stat.evictionCount()));
        });
        sb.append("==========================================");
        log.info(sb.toString());
    }

    public Map<String, CacheStats> getCacheStats() {
        Map<String, CacheStats> stats = new java.util.HashMap<>();
        stats.put("linksByNodeCache", linksByNodeCache.stats());
        stats.put("linkByIdCache", linkByIdCache.stats());
        stats.put("linksByTpCache", linksByTpCache.stats());
        stats.put("linksByEquipCache", linksByEquipCache.stats());
        stats.put("siteLinkToOchLinkCache", siteLinkToOchLinkCache.stats());
        stats.put("ochLinkToTunnelCache", ochLinkToTunnelCache.stats());
        stats.put("phyLinkToSiteLinkCache", phyLinkToSiteLinkCache.stats());
        stats.put("phyLinkToOchLinkCache", phyLinkToOchLinkCache.stats());
        return stats;
    }

    public List<LinkStateDto> getOchLinksBySiteLinkIds(List<String> siteLinkIds) {
        if (siteLinkIds == null || siteLinkIds.isEmpty()) {
            return Collections.emptyList();
        }
        String cacheKey = buildSortedCacheKey(siteLinkIds);
        List<LinkStateDto> ochLinks = siteLinkToOchLinkCache.get(cacheKey);
        log.debug("getOchLinkIdsBySiteLinkIds: siteLinkCount={}, ochLinkCount={}",
                siteLinkIds.size(), ochLinks.size());
        return ochLinks;
    }

    public List<LinkStateDto> getTunnelsByOchLinkIds(List<String> ochLinkIds) {
        if (ochLinkIds == null || ochLinkIds.isEmpty()) {
            return Collections.emptyList();
        }
        String cacheKey = buildSortedCacheKey(ochLinkIds);
        List<LinkStateDto> tunnels = ochLinkToTunnelCache.get(cacheKey);
        log.debug("getTunnelIdsByOchLinkIds: ochLinkCount={}, tunnelCount={}",
                ochLinkIds.size(), tunnels.size());
        return tunnels;
    }


    private List<String> parseSiteLinkIds(String key) {
        if (key == null || key.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(key.split(","));
    }

    private List<String> parseOchLinkIds(String key) {
        if (key == null || key.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(key.split(","));
    }

    private List<String> parsePhyLinkIds(String key) {
        if (key == null || key.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(key.split(","));
    }

    public List<LinkStateDto> getSiteLinkByPhyLinkId(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyList();
        }
        String cacheKey = buildSortedCacheKey(linkIds);
        List<LinkStateDto> siteLinks = phyLinkToSiteLinkCache.get(cacheKey);
        log.debug("getSiteIdsByPhyLinkIds: phyLinkCount={}, siteCount={}",
                linkIds.size(), siteLinks.size());
        return siteLinks;
    }

    public List<LinkStateDto> getOchLinkByPhyLinkId(List<String> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return Collections.emptyList();
        }
        String cacheKey = buildSortedCacheKey(linkIds);
        List<LinkStateDto> ochLinks = phyLinkToOchLinkCache.get(cacheKey);
        log.debug("getOchLinksByPhyLinkIds: phyLinkCount={}, ochLinkCount={}",
                linkIds.size(), ochLinks.size());
        return ochLinks;
    }


    public List<LinkStateDto> getTunnelByTpId(String tpId) {
        return tunnelsByTpCache.get(tpId);
    }
}
