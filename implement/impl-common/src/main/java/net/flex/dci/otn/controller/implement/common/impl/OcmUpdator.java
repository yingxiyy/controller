package net.flex.dci.otn.controller.implement.common.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

/**
 * 核心想法是基于工作的och 全部重算 ocmData
 */
@Slf4j
public class OcmUpdator {

    private final ChangedObject changedObject;

    private final List<Link> siteLinkList;

    private final Map<String, Node> updatedNodeMap;

    public List<Link> getRelatedSiteLinks() {
        return siteLinkList;
    }

    public OcmUpdator(ChangedObject changedObject, Link ochLink) {
        this.changedObject = changedObject;

        if (ochLink.getSupportingLink() != null) {
            List<String> siteLinkIdList = ochLink.getSupportingLink().stream()
                    .filter(sl -> SiteLinkIdNamingRule.isSiteLink(sl.getLinkRef().getValue()))
                    .map(sl -> sl.getLinkRef().getValue())
                    .collect(Collectors.toList());
            siteLinkList = siteLinkIdList.stream().map(changedObject::getChangedSiteLink)
                    .collect(Collectors.toList());
        } else {
            siteLinkList = new ArrayList<>();
        }
        updatedNodeMap = new HashMap<>();
    }

    /**
     * 仅针对指定的复用段重算 OCM。
     *
     * <p>保护 OCH 减腿时，OCH 本身仍保留，不能使用“整条 OCH 的所有 SiteLink”作为
     * OCM 更新范围；否则主腿和保留保护腿也会被重复下发。调用方传入的集合必须是待删腿
     * 实际经过的 SiteLink。</p>
     */
    public OcmUpdator(ChangedObject changedObject, Collection<Link> siteLinks) {
        this.changedObject = changedObject;
        this.siteLinkList = siteLinks == null ? new ArrayList<>() : new ArrayList<>(siteLinks);
        this.updatedNodeMap = new HashMap<>();
    }

    public Collection<Node> getUpdatedNode() {
        return updatedNodeMap.values();
    }

    public Map<String, Node> getOcmNodeMap() {
        return updatedNodeMap;
    }

    private String getFrequencyInfo(Link ochLink) {
        Och linkAttr = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        return String.format("[%d,%d]", linkAttr.getLowerFrequency().getValue().longValue(),
                linkAttr.getUpperFrequency().getValue().longValue());
    }

    public void remove(Link ochLink) {
        log.info("remove a och from ocm {}", getFrequencyInfo(ochLink));
        update(ochLink, null);
    }

    public void insert(Link ochLink) {
        log.info("insert a och in ocm {}", getFrequencyInfo(ochLink));
        update(null, ochLink);
    }

    //build NE OCM table based on OCH Link which is not in allocate state.
    private void update(Link removed, Link inserted) {
        //基于每一个复用段，获取处于 非 allocate 的 och, 根据这些och 计算 ocm 数据
        ChangedObject cache = new ChangedObject();
        //这个bug 隐藏的太深了， 所有假波操作结束才入库， 所以每次cache从数据库取到的值是一样的
        //但是对应的假波已经加入了，只是还没有入库
        // Dummy OCH actions may run per siteLink in parallel. Build the OCH
        // cache from a value snapshot, not key -> lookup, because another
        // siteLink may mark one key removed between those two operations.
        new ArrayList<>(changedObject.getChangedOchLinkList().values()).stream()
                .filter(ochLink -> ochLink != null)
                .forEach(cache::addChangedOchLink);

        siteLinkList.forEach(siteLink -> {
            List<Link> workingOchLinkList = new ArrayList<>();
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

            log.debug("the siteLink is {} ({})", siteLinkAttr.getFriendlyName(), siteLink.getLinkId().getValue());
            if (siteLinkAttr.getDummyLink() != null) {
                siteLinkAttr.getDummyLink().forEach(ochLinkId -> {
                    if (changedObject.getRemovedOchLinkIdList().contains(ochLinkId)) {
                        return;
                    }
                    Link ochLink = cache.getChangedOchLink(ochLinkId);
                    if (ochLink != null && isNotAllocate(ochLink)) {
                        workingOchLinkList.add(ochLink);
                    }
                });
            }
            if (siteLinkAttr.getSupportedLink() != null) {
                siteLinkAttr.getSupportedLink().forEach(sl -> {
                    String ochLinkId = sl.getLinkRef().getValue();
                    if (changedObject.getRemovedOchLinkIdList().contains(ochLinkId)) {
                        return;
                    }
                    Link ochLink = cache.getChangedOchLink(ochLinkId);
                    if (ochLink != null && isNotAllocate(ochLink)) {
                        log.debug("the business och link is: {}", getFrequencyInfo(ochLink));
                        workingOchLinkList.add(ochLink);
                    }
                });
            }

            if (removed != null) {
                workingOchLinkList.removeIf(
                        x -> x.getLinkId().getValue().equals(removed.getLinkId().getValue()));
            } else if (inserted != null) {
                workingOchLinkList.removeIf(ochLink -> ochLink.getLinkId().getValue()
                        .equals(inserted.getLinkId().getValue()));
                workingOchLinkList.add(inserted);
            }

            List<CrossConnectionAttributes> amplifierXcList = fetchSiteLinkXcIdList(
                    siteLinkAttr.getExplictRoute().getRoute());

            NeYangModel yangModel = CommonUtils.getYangModelInProperties(
                    siteLinkAttr.getProperties());
            WDM_Band band = CommonUtils.getWDMBand(siteLinkAttr);
            List<Available> bandScopes = FrequencyAvailable.getInitializedAvailableList(yangModel,
                    band, GridType._0);

            buildOcmGroup(bandScopes, amplifierXcList, workingOchLinkList);
        });
    }


    private boolean isNotAllocate(Link ochLink) {
        Och ochLinkAttr = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        return !ochLinkAttr.getImplementState().equals(ImplementState.Allocate);
    }

    private List<CrossConnectionAttributes> fetchSiteLinkXcIdList(List<Route> route) {
        List<CrossConnectionAttributes> amplifierXcList = new ArrayList<>();

        route.stream().forEach(x -> {
            amplifierXcList.addAll(x.getPrimary().getCrossConnections()
                    .stream().filter(xc->xc.getAmplifier() != null)
                    .collect(Collectors.toList()));
            if (x.getSecondary() != null) {
                amplifierXcList.addAll(x.getSecondary().getCrossConnections()
                        .stream().filter(xc->xc.getAmplifier() != null)
                        .collect(Collectors.toList()));
            }
            if (x.getThird() != null) {
                x.getThird().forEach(third -> amplifierXcList.addAll(third.getCrossConnections()
                        .stream().filter(xc->xc.getAmplifier() != null)
                        .collect(Collectors.toList())));
            }
        });

        return amplifierXcList;
    }

    private void buildOcmGroup(List<Available> bandScopes,
            List<CrossConnectionAttributes> amplifierXcList, List<Link> workingOchLinkList) {

        OcmDataBuilder ocmDataBuilder = new OcmDataBuilder();
        ocmDataBuilder.buildOcmGroup(bandScopes, amplifierXcList, workingOchLinkList);
        Map<String, List<OCMGripGroups>> ocmData = ocmDataBuilder.getUpdatedOcmGroupMap();

        for (String nodeId : ocmData.keySet()) {
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(phyNodeAttr)
                            .setOCMGripGroups(ocmData.get(nodeId)).build())
                    .build()).build();

            log.debug("update OCM of node {} {}", phyNodeAttr.getFriendlyName(), phyNodeAttr.getIp());
            updatedNodeMap.put(node.getNodeId().getValue(), newNode);
        }
    }


    public OcmUpdator() {
        this.changedObject = new ChangedObject();
        siteLinkList = new ArrayList<>();
        updatedNodeMap = new HashMap<>();
    }

    //refresh ocm on this siteLink for rebuild, currently is useless
    public void buildOcmGroup(Link siteLink, List<Link> workingOchLinkList) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        List<CrossConnectionAttributes> amplifierXcList = fetchSiteLinkXcIdList(
                siteLinkAttr.getExplictRoute().getRoute());

        NeYangModel yangModel = CommonUtils.getYangModelInProperties(
                siteLinkAttr.getProperties());
        WDM_Band band = CommonUtils.getWDMBand(siteLinkAttr);
        List<Available> bandScopes = FrequencyAvailable.getInitializedAvailableList(yangModel,
                band, GridType._0);

        buildOcmGroup(bandScopes, amplifierXcList, workingOchLinkList);
    }
}
