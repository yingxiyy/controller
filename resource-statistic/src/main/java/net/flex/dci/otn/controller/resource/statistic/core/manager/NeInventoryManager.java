package net.flex.dci.otn.controller.resource.statistic.core.manager;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.BATCH_TASK_TIMEOUT;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.FIRMWARE_VERSION;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.MFG_DATE;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.PART_NO;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.SOFTWARE_VERSION;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.YANG_VERSION;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.SiteDetails;
import net.flex.dci.otn.controller.resource.statistic.rest.NeDevice;
import net.flex.dci.otn.controller.resource.statistic.rest.NeResourceElement;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.SiteCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import net.flex.dci.otn.topology.cache.model.TerminationPointCache;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 10/30/2025 3:40 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeInventoryManager extends AbstractElementInventoryManager<NeDevice> {

    private final AdapterDao adapterDao;

    private final PhyNodeDao phyNodeDao;

    private final TopologyCacheManager topologyCacheManager;

    private final SiteLinkDao siteLinkDao;

    private final PhyLinkDao phyLinkDao;

    private static final ExecutorService parseExecutor = new ThreadPoolExecutor(
            8, 16, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactoryBuilder().setNameFormat("parse-ne-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    @Override
    public NeDevice getInventoryDetail(String neId) {
        log.debug("start to get ne:{} inventory details ", neId);
        Node node = phyNodeDao.getConfigPhyNodeById(neId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the ne:%s is not existed", neId));
        }
        PhyNodeCache phyNodeCache = topologyCacheManager.getValue(neId);
        String neName = phyNodeCache.getFriendlyName();
        Adapter registeredAdapter = adapterDao.getAdapterByNeId(neId);
        if (null == registeredAdapter) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne:%s is not supervised.", neName));
        }
        Map<String, List<String>> neToPhyLinks = batchLoadPhyLinks(Collections.singletonList(neId));
        Map<String, String> siteLinkMap = batchLoadSiteLinkMapping(neToPhyLinks);
        NeDevice neDevice = extractNeDevice(node, neToPhyLinks, siteLinkMap);
        return neDevice;
    }

    @Override
    public List<NeDevice> getInventoryDetails(List<String> neIds) {
        log.debug("start to list all ne:{} inventory details", neIds);
        if (CollectionUtils.isEmpty(neIds)) {
            return Collections.emptyList();
        }
        List<NeDevice> finalResult = new ArrayList<>();
        List<List<String>> partitions = Lists.partition(neIds, BATCH_SIZE);
        log.info("ne export total:{}, divided into {} partition, each partition is {}",
                neIds.size(), partitions.size(),
                BATCH_SIZE);
        for (int i = 0; i < partitions.size(); i++) {
            List<String> batchNeIds = partitions.get(i);
            log.debug("start process {} nes，size:{}", i + 1, batchNeIds.size());

            List<NeDevice> batchResult = processSingleBatch(batchNeIds);
            finalResult.addAll(batchResult);
        }
        return finalResult;
    }

    @Override
    public List<NeDevice> getInventoryDetails(List<String> ids, List<String> subnetIds) {
        return Collections.emptyList();
    }

    private List<NeDevice> processSingleBatch(List<String> batchNeIds) {
        log.debug("processing single ne batch:{}", batchNeIds);
        List<Node> batchNes = phyNodeDao.listConfigPhyNodeByIds(batchNeIds);
        if (CollectionUtils.isEmpty(batchNes)) {
            return Collections.emptyList();
        }
        Map<String, List<String>> neToPhyLinks = batchLoadPhyLinks(batchNeIds);
        Map<String, String> siteLinkMap = batchLoadSiteLinkMapping(neToPhyLinks);
//        List<CompletableFuture<NeDevice>> futures = batchNes.stream()
//                .map(node -> CompletableFuture.supplyAsync(() -> {
//                    try {
//                        return extractNeDevice(node);
//                    } catch (Exception e) {
//                        log.error("process node to NeDevice error", e);
//                        return null;
//                    }
//                }, asyncExecutor))
//                .collect(Collectors.toList());
        CompletableFuture<List<NeDevice>> batchFuture = CompletableFuture.supplyAsync(() -> {
//            List<NeDevice> batchResult = new ArrayList<>(batchNes.size());
//            for (Node node : batchNes) {
//                try {
//                    NeDevice device = extractNeDevice(node, neToPhyLinks, siteLinkMap);
//                    if (Objects.nonNull(device)) {
//                        batchResult.add(device);
//                    }
//                } catch (Exception e) {
//                    log.error("parse node to NeDevice failed, nodeId:{}",
//                            node.getNodeId().getValue(), e);
//                }
//            }
//            return batchResult;
            List<CompletableFuture<NeDevice>> tasks = batchNes.stream()
                    .map(node -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return extractNeDevice(node, neToPhyLinks, siteLinkMap);
                        } catch (Exception e) {
                            log.error("parse node error: {}", node.getNodeId().getValue(), e);
                            return null;
                        }
                    }, parseExecutor))
                    .collect(Collectors.toList());

            return tasks.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }, asyncExecutor);

        // 3. 等待所有并行任务完成，并过滤null
//        return futures.stream()
//                .map(future -> {
//                    try {
//                        return future.get(10, TimeUnit.SECONDS);
//                    } catch (Exception e) {
//                        log.error("batch process timeout/error", e);
//                        return null;
//                    }
//                }).filter(Objects::nonNull)
//                .collect(Collectors.toList());
        try {
            return batchFuture.get(BATCH_TASK_TIMEOUT, TimeUnit.MINUTES);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("batch process timeout, batch ne ids:{}", batchNeIds);
            throw new RuntimeException("NE batch processing timeout: " + batchNeIds, e);
        } catch (Exception e) {
            log.error("batch process ne inventory error", e);
            throw new RuntimeException("NE batch processing failed", e);
        }
    }

    private Map<String, String> batchLoadSiteLinkMapping(Map<String, List<String>> neToPhyLinks) {
        if (neToPhyLinks.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> allPhyLinks = neToPhyLinks.values().stream()
                .flatMap(List::stream).distinct().collect(Collectors.toList());

        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                allPhyLinks);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return Collections.emptyMap();
        }

        Map<String, SiteLinkCache> caches = topologyCacheManager.batchGetValues(siteLinkIds,
                SiteLinkCache.class);
        Map<String, String> map = new HashMap<>();
        for (Entry<String, SiteLinkCache> cache : caches.entrySet()) {
            if (cache == null) {
                continue;
            }
            String siteLinkId = cache.getKey();
            String sourceNodeId = SiteLinkIdNamingRule.getNodeA(siteLinkId);
            String destNodeId = SiteLinkIdNamingRule.getNodeZ(siteLinkId);
            SiteLinkCache siteLinkCache = cache.getValue();
            map.put(sourceNodeId, siteLinkCache.getFriendlyName());
            map.put(destNodeId, siteLinkCache.getFriendlyName());
        }
        return map;
    }

    private Map<String, List<String>> batchLoadPhyLinks(List<String> neIds) {
        if (CollectionUtils.isEmpty(neIds)) {
            return Collections.emptyMap();
        }
        List<String> allPhyLinks = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(neIds);
        Map<String, List<String>> map = new HashMap<>();
        for (String link : allPhyLinks) {
            String nodeA = PhysicalTpIdNamingRule.getNodeId(
                    PhysicalLinkIdNamingRule.getTpAId(link));
            String nodeZ = PhysicalTpIdNamingRule.getNodeId(
                    PhysicalLinkIdNamingRule.getTpZId(link));
            map.computeIfAbsent(nodeA, k -> new ArrayList<>()).add(link);
            map.computeIfAbsent(nodeZ, k -> new ArrayList<>()).add(link);
        }
        return map;
    }

    private NeDevice extractNeDevice(Node realNode, Map<String, List<String>> neToPhyLinks,
            Map<String, String> siteLinkMap) {
        String neId = realNode.getNodeId().getValue();
        log.debug("extract ne device:{} inventory info", neId);
        String refSiteId = PhysicalNodeIdNamingRule.getSiteId(neId);
        SiteDetails siteDetails = getRefSiteInfo(refSiteId);
        Physical nePhysical = realNode.getAugmentation(Node1.class).getPhysical();
        String vendorName = nePhysical.getVendorName();
        String friendlyName = nePhysical.getFriendlyName();
        String ipAddress = nePhysical.getIp();
        Integer port = nePhysical.getPort() == null ? null : nePhysical.getPort().getValue();
        String account = nePhysical.getLoginName();
        String password = nePhysical.getLoginPasswd();
        String neSubType = nePhysical.getCustomedType();
        String createTime = nePhysical.getCreationTime().getValue();
        String vendorType = nePhysical.getVendorType();
        String swVersion = PropertyTool.getValue(nePhysical.getProperties(), SOFTWARE_VERSION);
        String plane = nePhysical.getPlaneName();
        String yangVersion = PropertyTool.getValue(nePhysical.getProperties(), YANG_VERSION);
//        List<NeResourceElement> neElements = getNeResourceElement(nePhysical);
        String nodeType = nePhysical.getNodeType() == null ? null : nePhysical.getNodeType().name();
        String refSiteLink =
                nePhysical.getNodeType() == NodeType.OD ? siteLinkMap.get(neId)
                        : null;
        String refServiceInfo =
                nePhysical.getNodeType() == NodeType.TD ? getServiceInfo(neId, neToPhyLinks) : null;

        return NeDevice.builder().deviceType(nodeType)
                .neId(neId)
                .createTime(createTime)
                .siteId(siteDetails.getSiteId())
                .siteName(siteDetails.getSiteName())
//                .country(siteDetails.getCountry())
                .campus(siteDetails.getCampus())
                .city(siteDetails.getCity())
//                .location(siteDetails.getLocation())
                .name(friendlyName)
                .ipAddress(ipAddress)
                .userAccount(account)
                .password(password)
                .port(port)
                .vendor(vendorName)
                .vendorType(vendorType)
                .neSubType(neSubType)
                .swVersion(swVersion)
                .network(plane)
                .northApiVersion(yangVersion)
                .refSiteLink(refSiteLink)
                .serviceConnect(refServiceInfo)
//                .elements(neElements)
                .build();
    }

    private String getServiceInfo(String neId, Map<String, List<String>> neToPhyLinks) {
        log.debug
                ("get ne relative service info,the neId:{}", neId);
        List<String> refPhyLinkIds = neToPhyLinks.get(neId);
        List<String> serviceInfos = new ArrayList<>();
        for (String phyLinkId : refPhyLinkIds) {
            String localTpId = PhysicalLinkIdNamingRule.getTpAId(phyLinkId);
            String remoteTpId = PhysicalLinkIdNamingRule.getTpZId(phyLinkId);
            if (localTpId.contains(neId) && remoteTpId.contains(neId)) {
                log.debug("inner connection for ne,discard it continue");
                continue;
            }
            boolean isLocalSideA = localTpId.contains(neId);
            String currentPortId = isLocalSideA ? localTpId : remoteTpId;
            String oppositePortId = isLocalSideA ? remoteTpId : localTpId;

            String oppositeNeId = PhysicalTpIdNamingRule.getNodeId(oppositePortId);
            String siteLink = getNodeRefSiteLink(oppositeNeId);

            TerminationPointCache sourceTp = topologyCacheManager.getValue(currentPortId,
                    TerminationPointCache.class);
            TerminationPointCache destTp = topologyCacheManager.getValue(oppositePortId,
                    TerminationPointCache.class);
            String serviceInfo = buildServiceInfo(sourceTp, destTp, siteLink);
            serviceInfos.add(serviceInfo);

        }
        return String.join(";", serviceInfos);
    }

    private String buildServiceInfo(TerminationPointCache sourceTp, TerminationPointCache destTp,
            String siteLink) {
        String serviceInfo = String.format("%s--（%s）#%s", sourceTp.getSimpleName(), siteLink,
                destTp.getSimpleName());
        return serviceInfo;
    }

    private String getNodeRefSiteLink(String neId) {
        log.debug("get ne relative site link,the neId:{}", neId);
        List<String> refPhyLinkIds = phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(
                Collections.singletonList(neId));
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                refPhyLinkIds);
        String siteLinkId = siteLinkIds.get(0);
        SiteLinkCache siteLinkCache = topologyCacheManager.getValue(siteLinkId,
                SiteLinkCache.class);

        return siteLinkCache.getFriendlyName();
    }

    private SiteDetails getRefSiteInfo(String refSiteId) {
        log.debug("retrieve the site info by siteId:{}", refSiteId);
        SiteCache siteCache = topologyCacheManager.getValue(refSiteId, SiteCache.class);
        String friendlyName = siteCache.getFriendlyName();
        String campusName = siteCache.getCampusName();
        String city = siteCache.getCity();
//        String siteId = PropertyTool.getValue(siteAttribute.getProperties(), SITE_INFO_ID);
//        Long siteInfoId = Long.parseLong(siteId);
//        SiteInfo siteInfo = siteInfoDao.findById(siteInfoId);
//        String country = siteInfo.getCountry();
//        String city = siteInfo.getCity();
//        String campus = siteInfo.getCampus();
//        String location = siteInfo.getSiteLatitude() + DOT + siteInfo.getSiteLongitude();
        return SiteDetails.builder().siteId(refSiteId).siteName(friendlyName).campus(campusName)
                .city(city).build();
    }

    private List<NeResourceElement> getNeResourceElement(Physical nePhysical) {
        log.debug("get ne resource element by ne :{}", nePhysical.getFriendlyName());
        List<Equipments> equipments = nePhysical.getEquipments();
        List<Equipments> mountedEquips = equipments.stream()
                .filter(equip -> equip.isEmpty() != null && !equip.isEmpty())
                .collect(
                        Collectors.toList());
        List<NeResourceElement> neResourceElements = mountedEquips.stream()
                .map(this::extractResourceElementInfo)
                .collect(Collectors.toList());
        return neResourceElements;
    }

    private NeResourceElement extractResourceElementInfo(Equipments equipment) {
        String elementName = equipment.getFriendlyName();
        String slotNumber = equipment.getSlot();
        String serialNum = equipment.getSerialNo();
        String swVersion = equipment.getSoftwareVersion();
        String hdVersion = equipment.getHardwareVersion();
        String vendorName = equipment.getVendorName();
        String type = equipment.getEquipTypeInstalled();
        String partNo = PropertyTool.getValue(equipment.getProperties(), PART_NO);
        String firmVersion = PropertyTool.getValue(equipment.getProperties(), FIRMWARE_VERSION);
        String mfgDate = PropertyTool.getValue(equipment.getProperties(), MFG_DATE);
        NeResourceElement neResourceElement = NeResourceElement.builder()
                .sn(serialNum)
                .pn(partNo)
                .name(elementName)
                .slotNumber(slotNumber)
                .hwVersion(hdVersion)
                .swVersion(swVersion)
                .fwVersion(firmVersion)
                .vendor(vendorName)
                .mfgDate(mfgDate)
                .type(type)
                .build();
        return neResourceElement;
    }
}
