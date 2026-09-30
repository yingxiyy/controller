package net.flex.dci.otn.controller.implement.common.ase.ne;

import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SummaryAseSiteLinkState implements FutureCallback<List<StepRecord>> {

    private ChangedObject changedObject;
    private String siteLinkId;
    private ImplActionType actionType;
    private List<Node> write2NeList;


    private final BiConsumer<Boolean, Throwable> onFinish; // <-- NEW FIELD

    //在清理ASE 数据 （deImple SiteLink）的时候，locker=null
    public SummaryAseSiteLinkState(ChangedObject changedObject, List<Node> write2NeList, String siteLinkId, ImplActionType actionType,
                                   BiConsumer<Boolean, Throwable> onFinish) {
        this.changedObject = changedObject;

        this.siteLinkId = siteLinkId;
        this.actionType = actionType;

        this.write2NeList = write2NeList;
        this.onFinish = onFinish;
    }

    @Override
    public void onSuccess(@Nullable List<StepRecord> stepRecords) {
        log.info("all ASE related done in onSuccess");
        Exception exception = null;
        long fail = 0;
        try {
            Gson gson = new Gson();
            List<StepRecord.Property> allResults = new ArrayList<>();
            for (StepRecord stepRecord : stepRecords) {
                log.debug("action has done {} ({}) \n {} ", stepRecord.getNodeId(), stepRecord.getFriendlyName(), gson.toJson(allResults));
                allResults.addAll(stepRecord.getProperties().getPropertyList());
            }

            fail = allResults.stream().filter(e -> e.getValue().equals(ConfigNeSequence.STATUS_FAILURE)).count();
            if (fail > 0) {
                log.error("error happen when writeing to ne");
            }

            if (actionType.equals(ImplActionType.Implement)) {
                updateImplementStatus(stepRecords);
            } else {
                removeResource(stepRecords);
            }
            log.info("ASE inject has done successfully {}. ", siteLinkId);
        } catch (Exception e) {
            log.error("ASE inject hasn't completed {}. ", e);
            exception = e;
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "ASE inject hasn't completed " + e.getMessage());
        } finally {
            if (onFinish != null) {
                if (fail > 0) {
                    onFinish.accept(false, new Throwable("下发网元出错"));
                } else {
                    if (exception != null) {
                        onFinish.accept(false, exception);
                    } else {
                        onFinish.accept(true, null);
                    }
                }
            }
            log.debug("SummaryAseSiteLinkState will call back");
        }
    }

    private void removeResource(List<StepRecord> stepRecords) {
        Map<String, List<CrossConnections>> xcMap = getSortedXcList();
        List<Link> dummyOchList = getSortedDummyOchList();

        Map<String, List<CrossConnections>> newMap = new HashMap<>();
        for (Node node : write2NeList) {
            String nodeId = node.getNodeId().getValue();
            Node dbNode = changedObject.getChangedPhyNode(nodeId);
            Physical dbNodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();
            List<CrossConnections> newXcList = new ArrayList<>(dbNodeAttr.getCrossConnections());

            for (CrossConnections xc : xcMap.get(nodeId)) {
                String xcId = xc.getCrossConnectionId().getValue();
                if (isSuccess(stepRecords, nodeId, xcId)) {
                    newXcList.removeIf(x->x.getCrossConnectionId().getValue().equals(xcId));
                    updateOchImplementState(dummyOchList, CrossConnectionSlotNamingRule.getFrequencyScope(xc), ImplementState.Allocate);
                } else {
                    log.warn("the XC remove fail. {}", xcId);
                    updateOchImplementState(dummyOchList, CrossConnectionSlotNamingRule.getFrequencyScope(xc), ImplementState.PartialImplement);
                }
            }

            newMap.put(nodeId, newXcList);
        }

        Map<String, Boolean> ocmActionSuccessful = new HashMap<>();
        for (Node node : write2NeList) {
            String nodeId = node.getNodeId().getValue();
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            String ocmStepName = String.format(ConfigNeSequence.FORMAT_OCM, nodeId, nodeAttr.getFriendlyName());

            if (isSuccess(stepRecords, nodeId, ocmStepName)) {
                ocmActionSuccessful.put(nodeId, true);
            } else {
                ocmActionSuccessful.put(nodeId, false);
            }
        }

        for (Node node : write2NeList) {
            String nodeId = node.getNodeId().getValue();
            Node dbNode = changedObject.getChangedPhyNode(nodeId);
            Physical dbNodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();
            Physical node2NeAttr = node.getAugmentation(Node1.class).getPhysical();

            Node newNode = new NodeBuilder(dbNode).addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(dbNodeAttr)
                        .setOCMGripGroups(ocmActionSuccessful.get(nodeId) ? node2NeAttr.getOCMGripGroups() : dbNodeAttr.getOCMGripGroups())
                        .setCrossConnections(newMap.get(nodeId))
                        .build())
                    .build())
                .build();
            changedObject.addChangedPhyNode(newNode);
        }

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        List<String> dummyIdList = siteLinkAttr.getDummyLink();
        dummyOchList.forEach(link->{
            if (link.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch().getImplementState().equals(ImplementState.Allocate)) {
                changedObject.addRemovedOchLink(link.getLinkId().getValue());
                dummyIdList.removeIf(id->link.getLinkId().getValue().equals(id));
            }
        });

        Link newSiteLink = new LinkBuilder(siteLink).addAugmentation(Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                .setSite(new SiteBuilder(siteLinkAttr)
                    .setDummyLink(dummyIdList.isEmpty() ? null : dummyIdList)
                    .build())
                .build())
            .build();
        changedObject.addChangedSiteLink(newSiteLink);
    }

    private void updateImplementStatus(List<StepRecord> stepRecords) {
        Map<String, List<CrossConnections>> xcMap = getSortedXcList();
        List<Link> dummyOchList = getSortedDummyOchList();

        Map<String, List<CrossConnections>> newMap = new HashMap<>();
        for (Node node : write2NeList) {
            String nodeId = node.getNodeId().getValue();
            List<CrossConnections> newXcList = new ArrayList<>();

            for (CrossConnections xc : xcMap.get(nodeId)) {
                String xcId = xc.getCrossConnectionId().getValue();
                if (isSuccess(stepRecords, nodeId, xcId)) {
                    CrossConnections newXc = new CrossConnectionsBuilder(xc)
                        .setAdminState(AdminStatus.Up)
                        .setImplementState(ImplementState.Implement)
                        .build();
                    newXcList.add(newXc);
                    updateOchImplementState(dummyOchList, CrossConnectionSlotNamingRule.getFrequencyScope(xc), ImplementState.Implement);
                } else {
                    log.warn("the XC process fail. {}", xcId);
                    newXcList.add(xc); //keep unImplement
                    //related och should be partial
                    updateOchImplementState(dummyOchList, CrossConnectionSlotNamingRule.getFrequencyScope(xc), ImplementState.PartialImplement);
                }
            }
            newMap.put(nodeId, newXcList);
        }

        Map<String, Boolean> ocmActionSuccessful = new HashMap<>();
        for (Node node : write2NeList) {
            String nodeId = node.getNodeId().getValue();
            Physical node2NeAttr = node.getAugmentation(Node1.class).getPhysical();
            String ocmStepName = String.format(ConfigNeSequence.FORMAT_OCM, nodeId, node2NeAttr.getFriendlyName());

            if (isSuccess(stepRecords, nodeId, ocmStepName)) {
                ocmActionSuccessful.put(nodeId, true);
            } else {
                ocmActionSuccessful.put(nodeId, false);
                dummyOchList = updateOchImplementState(dummyOchList, ImplementState.PartialImplement);
                log.error("all dummy och is partial-implement because ocm write fail on node {}", node.getNodeId());
            }

            Node dbNode = changedObject.getChangedPhyNode(nodeId);
            List<CrossConnections> dbXcList = new ArrayList<>(dbNode.getAugmentation(Node1.class).getPhysical().getCrossConnections());
            newMap.get(nodeId).forEach(dbXc->{
                String xcId = dbXc.getCrossConnectionId().getValue();
                dbXcList.removeIf(xc->xc.getCrossConnectionId().getValue().equals(xcId));
            });
            dbXcList.addAll(newMap.get(nodeId));
            Physical dbNodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();
            Node newNode = new NodeBuilder(dbNode).addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(dbNodeAttr)
                                    .setOCMGripGroups(ocmActionSuccessful.get(nodeId) ? node2NeAttr.getOCMGripGroups() : dbNodeAttr.getOCMGripGroups())
                                    .setCrossConnections(dbXcList)
                                    .build())
                            .build())
                    .build();

            changedObject.addChangedPhyNode(newNode);
        }
    }

    private List<Link>  updateOchImplementState(List<Link> dummyOchList, ImplementState implementState) {
        return dummyOchList.stream().map(ochLink-> {
            Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

            Link newOchLink = new LinkBuilder(ochLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder().setOch(
                                            new OchBuilder(ochLinkAttr).setImplementState(implementState).setAdminState(AdminStatus.Up).build())
                                    .build())
                    .build();
            changedObject.addChangedOchLink(newOchLink);
            return newOchLink;
        }).collect(Collectors.toList());
    }

    private void updateOchImplementState(List<Link> dummyOchList, Available pos, ImplementState implementState) {
        String frequencyScope = pos.getLowerFrequency().getValue().longValue() + "-" + pos.getUpperFrequency().getValue().longValue();

        boolean found = false;
        for (int i = 0; i < dummyOchList.size(); i++) {
            Link ochLink = dummyOchList.get(i);
            if (ochLink.getLinkId().getValue().contains(frequencyScope)) {
                Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

                Link newOchLink = new LinkBuilder(ochLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder().setOch(
                                                new OchBuilder(ochLinkAttr).setImplementState(implementState).setAdminState(AdminStatus.Up).build())
                                        .build())
                        .build();
                dummyOchList.set(i, newOchLink);
                found = true;
                changedObject.addChangedOchLink(newOchLink);
                break;
            }
        }
        if (!found) {
            log.error("Cannot find dummy och with lower frequency {}", frequencyScope);
            return;  //容错
        }
    }

    private List<Link> getSortedDummyOchList() {
        List<Link> dummyOchList = new ArrayList<>();

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        List<String> dummyOchIdList = siteLink.getAugmentation(Link1.class).getSite().getDummyLink();
        dummyOchIdList.sort(Comparator.comparingLong(this::extractFirstNumberOnDummyOchId));

        for (String ochId : dummyOchIdList) {
            Link ochLink = changedObject.getChangedOchLink(ochId);
            if (ochLink != null) {
                dummyOchList.add(ochLink);
            }
        }

        return dummyOchList;
    }

    private long extractFirstNumberOnDummyOchId(String id) {
        Pattern OCHLINK_PATTERN = Pattern.compile("OchLink-#(\\d+)");
        Matcher matcher = OCHLINK_PATTERN.matcher(id);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        } else {
            return Long.MAX_VALUE; // 无法匹配的放到最后
        }
    }

    private Map<String, List<CrossConnections>> getSortedXcList() {
        //because at WssXcIdGenerator for hiding one issue, the xcID for duplicated is xcID_1
        Map<String, List<CrossConnections>> xcMap = new HashMap<>();

        write2NeList.forEach(node -> {
            String nodeId = node.getNodeId().getValue();
            List<CrossConnections> xcList = node.getAugmentation(Node1.class)
                    .getPhysical()
                    .getCrossConnections();

            xcList.sort(Comparator.comparingLong(xc -> {
                String xcId = xc.getCrossConnectionId().getValue();

                // 1. 取最后一段（兼容无 "/" 的情况）
                String last = xcId.contains("/") ?
                        xcId.substring(xcId.lastIndexOf("/") + 1) :
                        xcId;

                // 2. 如果存在 "_"，取 "_" 前的数字部分
                if (last.contains("_")) {
                    last = last.substring(0, last.indexOf("_"));
                }

                // 3. 安全解析数字，不是数字则当作 0
                try {
                    return Long.parseLong(last);
                } catch (NumberFormatException e) {
                    return 0L;  // 或者你想返回的默认值
                }
            }));

            xcMap.put(nodeId, xcList);
        });

        return xcMap;
    }


    private boolean isSuccess(List<StepRecord> stepRecords, String nodeId, String xcId) {
        StepRecord record = stepRecords.stream().filter(x -> x.getNodeId().equals(nodeId)).findAny()
            .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "impossible, the node must be here " + nodeId));

        StepRecord.Property prop = record.getProperties().getPropertyList().stream().filter(x -> {
            if (x.getName().contains(xcId) && x.getValue().equals(ConfigNeSequence.STATUS_SUCCESS)) {
                return true;
            }
            return false;
        }).findAny().orElse(null);
        // Pending means the NE-level action failed before this object was confirmed by the adapter.
        return prop != null;
    }

    @Override
    public void onFailure(Throwable throwable) {
        log.error("error happen in ase action {}", throwable.getMessage(), throwable);
        if (onFinish != null) {
            onFinish.accept(false, throwable);
        }
    }
}
