package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.AbstractResourceLock;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.common.OpNodeMerger;
import net.flex.dci.otn.controller.allocate.common.util.CommonUtils;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelBindOutputData;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelNewOchAllocate;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchBindTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchBindTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchBindTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchGetBindingListInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchGetBindingListOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchGetBindingListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.binding.list.output.BindingRoutes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.binding.list.output.BindingRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TunnelBinder {

    private static final AtomicLong TASK_GROUP_ID_GENERATOR =
            new AtomicLong(System.currentTimeMillis());

    public static final String LEG_REQUIRED = "leg-required";
    public static final String BINDING_3_RD_LEG = "binding3rdLeg";
    public static final String BINDING_3_RD_LEG_RESTORE_TUNNEL = "binding3rdLegRestoreTunnel";

    @Autowired
    private TunnelDao tunnelDao;
    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private SiteLinkDao siteLinkDao;

    @Autowired
    private TunnelNewOchAllocate tunnelNewOchAllocate;
    @Autowired
    private XCRepo xcRepo;

    @Autowired
    private TunnelComputer2 tunnelComputer2;

    @Autowired
    private JsonOutputer jsonOutputer;

    @Autowired
    private PhyLinkFriendlyName phyLinkFriendlyName;

    @Autowired
    private PhyNodeFriendlyName phyNodeFriendlyName;

    @Autowired
    private PhyLinkDao phyLinkDao;

    private final MultipleTransaction multipleTransaction;

    private Supplier<AbstractResourceLock> resourceLockFactory = ZkResourceLock::new;

    @Autowired
    public TunnelBinder(MultipleTransaction multipleTransaction) {
        this.multipleTransaction = multipleTransaction;
    }


    public BindTunnelOutput doIt(BindTunnelInput input, TaskInfoMessage rootTaskInfo) {
        Tunnel tunnel = getRequiredTunnel(input);
        return doIt(input, tunnel, rootTaskInfo);
    }

    private BindTunnelOutput doIt(BindTunnelInput input, Tunnel tunnel, TaskInfoMessage rootTaskInfo) {
        List<TaskInfoMessage> taskInfos = new ArrayList<>();

        // rootTaskInfo 只是template
        rootTaskInfo.setResourceName(tunnel.getFriendlyName());
        rootTaskInfo.setResourceId(tunnel.getTunnelId().getValue());
        taskInfos.add(rootTaskInfo);
        try {
            validateTunnelProtectionType(tunnel);
            validateTunnel(tunnel);
            validateBindingRoute(input, tunnel);
            //step1: allocate
            ImmutablePair<TunnelBindOutputData, TaskInfoMessage> allocatePair = step1_allocate(input, rootTaskInfo);
            TunnelBindOutputData allocateResult = allocatePair.left;
            taskInfos.add(allocatePair.right);

            //step2 update db
            TaskInfoMessage createTaskInfo = step2_updateDB(input, tunnel, allocateResult, rootTaskInfo);
            taskInfos.add(createTaskInfo);

            //update taskinfo
            rootTaskInfo.setSuccessfully(true);
            rootTaskInfo.setEndTime(System.currentTimeMillis());
            taskInfos.addAll(buildSameOchTunnelRootTaskInfos(tunnel, taskInfos, true, null));

            String msg = String.format("Bind tunnel %s successfully.", tunnel.getFriendlyName());
            sendBindSuccessNotification(tunnel);

            return new BindTunnelOutputBuilder().setReturnCode(RpcResultType.Success).setReturnMessage(msg).build();
        } catch (BindExistedException e) {
            rootTaskInfo.setSuccessfully(true);
            rootTaskInfo.setEndTime(System.currentTimeMillis());
            taskInfos.addAll(buildSameOchTunnelRootTaskInfos(tunnel, taskInfos, false,
                    "OCH already has third leg"));
            log.info("No need  to bind for :{}, 3 legs already.", input);
            String msg = String.format("Bind tunnel %s successfully.", tunnel.getFriendlyName());
            sendBindSuccessNotification(tunnel);
            return new BindTunnelOutputBuilder().setReturnCode(RpcResultType.Success).setReturnMessage(msg).build();
        } catch (Exception e) {
            rootTaskInfo.setSuccessfully(false);
            rootTaskInfo.setErrorReason(e.getMessage());
            log.error("Failed to bind for :{}", input, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "bind tunnel error" + e.toString(), e);
        } finally {
            //step3 send taskInfo
            for (TaskInfoMessage taskInfoMessage : taskInfos) {
                TaskInfoMessager.sendMessage(taskInfoMessage);
            }
        }
    }

    private Tunnel getRequiredTunnel(BindTunnelInput input) {
        if (input == null || isBlank(input.getTunnelId())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Tunnel id is empty");
        }
        Tunnel tunnel = tunnelDao.getTunnelById(input.getTunnelId());
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Tunnel does not exist: " + input.getTunnelId());
        }
        return tunnel;
    }

    private List<TaskInfoMessage> buildSameOchTunnelRootTaskInfos(Tunnel representativeTunnel,
                                                                  List<TaskInfoMessage> sourceTaskInfos,
                                                                  boolean successfully,
                                                                  String errorReason) {
        try {
            String representativeTunnelId = representativeTunnel.getTunnelId().getValue();
            String ochLinkId = getOchLinkIdFromTunnel(representativeTunnel);
            return tunnelDao.getTunnelNameAndIdUnderOchLink(Arrays.asList(ochLinkId)).stream()
                    .filter(tunnel -> tunnel != null && tunnel.getId() != null)
                    .filter(tunnel -> !tunnel.getId().equals(representativeTunnelId))
                    .flatMap(tunnel -> {
                        long groupId = nextTaskGroupId();
                        // Binding is an OCH-level operation; show every affected tunnel as an independent root task.
                        return sourceTaskInfos.stream()
                                .map(taskInfo -> copyBindTaskInfo(taskInfo, tunnel.getId(),
                                        tunnel.getFriendlyName(), groupId, successfully,
                                        errorReason));
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to append same-OCH tunnel bind task infos for {}",
                    representativeTunnel.getTunnelId().getValue(), e);
            return Collections.emptyList();
        }
    }

    private TaskInfoMessage copyBindTaskInfo(TaskInfoMessage source, String tunnelId,
                                             String tunnelName, long groupId,
                                             boolean successfully, String errorReason) {
        TaskInfoMessage taskInfo = new TaskInfoMessage(source);
        taskInfo.setId(null);
        taskInfo.setGroupId(groupId);
        taskInfo.setRoot(source.getRoot());
        taskInfo.setObjectId(source.getObjectId());
        taskInfo.setObjectType(source.getObjectType());
        taskInfo.setScanResultId(source.getScanResultId());
        taskInfo.setResourceId(source.getResourceId());
        taskInfo.setResourceName(source.getResourceName());
        if (source.getRoot() == null || source.getRoot()) {
            taskInfo.setResourceId(tunnelId);
            taskInfo.setResourceName(tunnelName);
        }
        taskInfo.setSuccessfully(successfully);
        taskInfo.setErrorReason(errorReason);
        return taskInfo;
    }

    private long nextTaskGroupId() {
        return TASK_GROUP_ID_GENERATOR.incrementAndGet();
    }

    private TaskInfoMessage step2_updateDB(BindTunnelInput input, Tunnel tunnel,
                                           TunnelBindOutputData allocateResult,
                                           TaskInfoMessage rootTaskInfo) {
        AbstractResourceLock locker = resourceLockFactory.get();
        lockerResource(locker, input, tunnel, allocateResult);

        //construct taskInfo
        TaskInfoMessage createTaskInfo = new TaskInfoMessage(rootTaskInfo.getWho(), rootTaskInfo.getResourceType(), TaskInfoMessage.ActionType.bind, "");
        createTaskInfo.setGroupId(rootTaskInfo.getGroupId());
        createTaskInfo.setRoot(false);
        createTaskInfo.setResourceName("update to db for binding");
        createTaskInfo.setResourceId("updateDB" + System.currentTimeMillis());

        createTaskInfo.setActionTime(System.currentTimeMillis());

        log.debug("binding result: {}", allocateResult );
        try {
            // Allocation is computed before the distributed lock is acquired. Re-read the
            // tunnel/OCH view after waiting for the lock so a concurrent protection change
            // cannot make this DB update apply a stale leg decision.
            String tunnelId = tunnel.getTunnelId().getValue();
            Tunnel lockedTunnel = tunnelDao.getTunnelById(tunnelId);
            validateTunnel(lockedTunnel);
            validateBindingRoute(input, lockedTunnel);
            removeReplacedPhyLinks(allocateResult.getRemovedResourceIds()); //实际上不会有需要删除的内容
            ChangedObject changedObject = new ChangedObject();

            log.debug("The obj to be updated to db is: {}", tunnelId);

            //update ochLink
            Link updatedOchLink = constructOchUpdated(tunnelId, allocateResult);
            changedObject.addChangedOchLink(updatedOchLink);
//            log.debug("updatedOchLink: {}", jsonOutputer.fromOchLinkToJson(updatedOchLink));
            log.debug("updatedOchLink: {}", jsonOutputer.fromOchLinkToJson(updatedOchLink));

            // The later phyLink friendly-name update may reference nodes created only in this bind allocation.
            addAllocatedPhyNodes(allocateResult, changedObject);
            updateSiteNodeRackForAllocatedTdNodes(allocateResult, changedObject);

            //add new PhyLink
            List<Link> newPhyLinks = constructNewPhyLinks(allocateResult, changedObject);
            List<Link> newOmsLinks = new ArrayList<>();//e.g., the exp-mpo link for A/Z external
            for (Link newPhyLink : newPhyLinks) {
                changedObject.addChangedPhyLink(newPhyLink);
                log.debug("new PhyLink: {}", newPhyLink);
                if (PhysicalLinkIdNamingRule.isOmsLink(newPhyLink.getLinkId().getValue())) {
                    newOmsLinks.add(newPhyLink);
                }
            }

            //update siteLinks
            List<Link> updatedSiteLinks = constructSiteLinkUpdated(allocateResult, newOmsLinks);
            for (Link updatedSiteLink : updatedSiteLinks) {
                changedObject.addChangedSiteLink(updatedSiteLink);
                log.debug("updatedSiteLink: {}", updatedSiteLink);
            }

            for (CrossConnections xc : allocateResult.getXcs()) {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue());
                Node node = changedObject.getChangedPhyNode(nodeId);
                List<CrossConnections> nodeXcList = new ArrayList<>(node.getAugmentation(Node1.class)
                        .getPhysical().getCrossConnections());
                // RoadmService/OTAllocate already place some generated XCs in the returned nodes.
                // Add only segment XCs that are not present to avoid duplicate XC ids in config-phy-node.
                boolean existed = nodeXcList.stream().anyMatch(item -> item.getCrossConnectionId()
                        .equals(xc.getCrossConnectionId()));
                if (existed) {
                    continue;
                }
                nodeXcList.add(xc);
                Node updatedNode = new NodeBuilder(node)
                        .addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(
                                        new PhysicalBuilder(
                                                node.getAugmentation(Node1.class).getPhysical())
                                                .setCrossConnections(nodeXcList)
                                                .build())
                                .build())
                        .build();
                changedObject.addChangedPhyNode(updatedNode);
                log.debug("updatedPhyNode: {}", jsonOutputer.formatNode(updatedNode));
            }

            // Binding is applied to the OCH, so update all tunnels under it, not only the input tunnel.
            List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(Arrays.asList(updatedOchLink.getLinkId().getValue()));
            validateSameOchTunnelStateForBind(sameOchTunnels);
            int updatedLegCount = getProtectionLegCount(updatedOchLink);
            for (Tunnel sameOchTunnel : sameOchTunnels) {
                boolean shouldRestore = sameOchTunnel.getImplementState().equals(ImplementState.Implement);
                // Allocate tunnels are still unimplemented, so bind keeps them Allocate and marker-free.
                ImplementState targetTunnelImplementState = shouldRestore ? ImplementState.PartialImplement : ImplementState.Allocate;
                Properties newProp = sameOchTunnel.getProperties();
                if (shouldRestore) {
                    // Only tunnels changed by this bind operation should be restored to Implement later.
                    newProp = PropertyTool.addProperty(newProp, BINDING_3_RD_LEG, "true");
                    newProp = PropertyTool.addProperty(newProp, BINDING_3_RD_LEG_RESTORE_TUNNEL, "true");
                }
                newProp = updateLegRequiredProperty(newProp, updatedLegCount);
                Tunnel newTunnel = new TunnelBuilder(sameOchTunnel)
                        .setProperties(newProp)
                        .setImplementState(targetTunnelImplementState)
                        .build();
                changedObject.addChangedTunnel(newTunnel);
                log.debug("update tunnel:{}", newTunnel.getTunnelId().getValue());
            }

            //Save to db
            store2DB(changedObject);

            log.debug("Finished update db for binding tunnel:{}", tunnelId);

            //update taskInfo
            createTaskInfo.setSuccessfully(true);

        } catch (Exception e) {
            log.error("Failed to update binding to db ", e);
            createTaskInfo.setSuccessfully(false);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "error in construct binding " + e.getMessage(), e);
        } finally {
            locker.unlock();
        }
        createTaskInfo.setEndTime(System.currentTimeMillis());
        return createTaskInfo;
    }

    private void addAllocatedPhyNodes(TunnelBindOutputData allocateResult, ChangedObject changedObject) {
        for (Node node : allocateResult.getNodes()) {
            changedObject.addChangedPhyNode(node);
            log.debug("updatedPhyNode: {}", jsonOutputer.formatNode(node));
        }
    }

    private void updateSiteNodeRackForAllocatedTdNodes(TunnelBindOutputData allocateResult, ChangedObject changedObject) {
        List<String> siteLinkIds = allocateResult.getSiteLinks().stream()
                .map(siteLink -> siteLink.getLinkId().getValue())
                .collect(Collectors.toList());
        Set<String> newOsLinkNodeIds = getNewOsLinkNodeIds(allocateResult);
        for (Node node : allocateResult.getNodes()) {
            if (!newOsLinkNodeIds.contains(node.getNodeId().getValue()) || !isTdNode(node)) {
                continue;
            }
            String siteLinkId = getSiteLinkIdForTdNode(node.getNodeId().getValue(), siteLinkIds, changedObject);
            updateSiteNodeRack(node, siteLinkId, changedObject);
        }
    }

    private Set<String> getNewOsLinkNodeIds(TunnelBindOutputData allocateResult) {
        Set<String> nodeIds = new HashSet<>();
        for (Link link : allocateResult.getLinks()) {
            String linkId = link.getLinkId().getValue();
            if (!PhysicalLinkIdNamingRule.isOsLink(linkId) || phyLinkDao.isExistedPhyLinkId(linkId)) {
                continue;
            }
            nodeIds.add(link.getSource().getSourceNode().getValue());
            nodeIds.add(link.getDestination().getDestNode().getValue());
        }
        return nodeIds;
    }

    private boolean isTdNode(Node node) {
        return node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null
                && NodeType.TD.equals(node.getAugmentation(Node1.class).getPhysical().getNodeType());
    }

    private String getSiteLinkIdForTdNode(String nodeId, List<String> siteLinkIds, ChangedObject changedObject) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
        List<String> relatedSiteLinks = siteLinkIds.stream()
                .filter(siteLinkId -> siteLinkId.contains(siteNodeId))
                .collect(Collectors.toList());

        if (relatedSiteLinks.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the TD node cannot find related siteLink should binding");
        }

        if (relatedSiteLinks.size() == 1) {
            return relatedSiteLinks.get(0);
        }

        Node siteNode = changedObject.getChangedSiteNode(siteNodeId);
        for (String relatedSiteLink : relatedSiteLinks) {
            if (SiteNodeCorrelateResource.hasSpaceInRack(siteNode, relatedSiteLink)) {
                return relatedSiteLink;
            }
        }
        return relatedSiteLinks.get(0);
    }

    private void updateSiteNodeRack(Node tpcNode, String siteLinkId, ChangedObject changedObject) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(tpcNode.getNodeId().getValue());
        Node siteNode = changedObject.getChangedSiteNode(siteNodeId);

        // insertRack is idempotent for reused REG/TPC nodes that are already recorded in the site rack.
        SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);
        correlateResource.insertRack(siteLinkId, tpcNode);

        Node updatedSiteNode = correlateResource.getSiteNode();
        changedObject.addChangedSiteNode(updatedSiteNode);

        // Keep bind-tunnel aligned with normal tunnel creation: friendlyName is derived after rack placement.
        Node updatedPhyNode = phyNodeFriendlyName.updateFriendlyName(tpcNode, updatedSiteNode);
        changedObject.addChangedPhyNode(updatedPhyNode);
    }

    private List<Link> constructNewPhyLinks(TunnelBindOutputData allocateResult, ChangedObject changedObject) {
        List<Link> newPhyLinks = new ArrayList<>();
        List<Link> siteLinks = allocateResult.getSiteLinks();
        Och ochAddr = allocateResult.getOchLink().getAugmentation(Link1.class).getOch();
        String planeName = ochAddr.getPlaneName();
        String planeId = ochAddr.getPlaneId();
        List<String> orderId = ochAddr.getOrderId();
        String riskGroup = ochAddr.getRiskGroupName();
        for (Link phyLink : allocateResult.getLinks()) {
            String phyLinkId = phyLink.getLinkId().getValue();
            if (phyLinkDao.isExistedPhyLinkId(phyLinkId)) {
                continue;
            }
            Node srcNode = changedObject.getChangedPhyNode(phyLink.getSource().getSourceNode().getValue());
            Node dstNode = changedObject.getChangedPhyNode(phyLink.getDestination().getDestNode().getValue());
            phyLink = phyLinkFriendlyName.updateFriendlyName(phyLink, srcNode, dstNode);

            List<SupportedLink> supportedLinks = null;
            if (PhysicalLinkIdNamingRule.isOmsLink(phyLinkId)) {
                // A newly bound 3rd leg may cross multiple siteLinks in ROADM networks.
                // Attach each new OMS/MPO link to the siteLink whose endpoint NE appears in the link id.
                supportedLinks = getMatchedSiteLinks(phyLink, siteLinks).stream()
                        .map(siteLink -> buildPhySupportedLink(siteLink.getLinkId()))
                        .collect(Collectors.toList());
                if (supportedLinks.isEmpty()) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot identify siteLink for new phyLink " + phyLinkId);
                }
            }
            phyLink = new LinkBuilder(phyLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(
                                            phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical())
                                            .setSupportedLink(supportedLinks)
                                            .setPlaneName(planeName)
                                            .setPlaneId(planeId)
                                            .setOrderId(orderId)
                                            .setRiskGroupName(riskGroup)
                                            .build())
                                    .build())
                    .build();
            newPhyLinks.add(phyLink);
        }
        return newPhyLinks;
    }

    private List<Link> constructSiteLinkUpdated(TunnelBindOutputData allocateResult, List<Link> newOmsLinks) {
        List<Link> updatedSiteLinks = new ArrayList<>();
        for (Link siteLink : allocateResult.getSiteLinks()) {
            log.debug("constructSiteLinkUpdated");  //实际上这个是不需要的

            validateImplementStatus(siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite().getImplementState());

            // Update frequency availability, bandwidth and business-OCH supported-link for every
            // siteLink crossed by the newly bound 3rd leg.
            SiteLinkOchUpdater updater = new SiteLinkOchUpdater(siteLink);
            updater.addNewOch(allocateResult.getOchLink());
            Link siteLinkUpdated = updater.getSiteLink();
            Site siteAddr = siteLinkUpdated.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
            String siteLinkId = siteLinkUpdated.getLinkId().getValue();

            List<AddDropLink> aExtneralAddDrops = new ArrayList<>(siteAddr.getAExternal().getAddDropLink());
            List<AddDropLink> zExtneralAddDrops = new ArrayList<>(siteAddr.getZExternal().getAddDropLink());
            for (Link link : newOmsLinks) {
                String linkId = link.getLinkId().getValue();
                if (!isPhyLinkRelatedToSiteLink(link, siteLinkUpdated)) {
                    continue;
                }
                AddDropLink addDroplink = new AddDropLinkBuilder().setLinkRef(linkId)
                        .setKey(new AddDropLinkKey(linkId))
                        .setConnnectorType(EquipType.MUXPANEL)
                        .build();
                if (siteLinkUpdated.getSource().getSourceNode().getValue().equals(PhysicalLinkIdNamingRule.getSiteAId(linkId))) {
                    log.debug("Add new aExtneral addDrop:{}, for siteLink:{}", linkId, siteLinkId);
                    aExtneralAddDrops.add(addDroplink);
                } else {
                    log.debug("Add new zExtneral addDrop:{}, for siteLink:{}", linkId, siteLinkId);
                    zExtneralAddDrops.add(addDroplink);
                }
            }

            siteLinkUpdated = new LinkBuilder(siteLinkUpdated)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                    .setSite(new SiteBuilder(siteAddr)
                                            .setImplementState(siteAddr.getImplementState())
                                            .setZExternal(new ZExternalBuilder(siteAddr.getZExternal()).setAddDropLink(zExtneralAddDrops).build())
                                            .setAExternal(new AExternalBuilder(siteAddr.getAExternal()).setAddDropLink(aExtneralAddDrops).build())
                                            .build()
                                    ).build()
                    ).build();
            updatedSiteLinks.add(siteLinkUpdated);
        }
        return updatedSiteLinks;
    }

    private void lockerResource(AbstractResourceLock locker, BindTunnelInput input, Tunnel tunnel,
                                TunnelBindOutputData allocateResult) {
        locker.addResource(tunnel.getTunnelId().getValue());
        tunnelDao.getAllTunnelsUnderOchLink(Collections.singletonList(
                allocateResult.getOchLink().getLinkId().getValue()))
                .forEach(item -> locker.addResource(item.getTunnelId().getValue()));
        for (Node node : allocateResult.getNodes()) {
            locker.addResource(node.getNodeId().getValue());
            locker.addResource(PhysicalNodeIdNamingRule.getSiteId(node.getNodeId().getValue()));
        }

        locker.addResource(allocateResult.getOchLink().getLinkId().getValue());

        allocateResult.getSiteLinks().forEach(link -> locker.addResource(link.getLinkId().getValue()));
        allocateResult.getLinks().forEach(link -> {
            locker.addResource(link.getLinkId().getValue());
            addTpResourceLocks(locker, link.getSource().getSourceTp().getValue());
            addTpResourceLocks(locker, link.getDestination().getDestTp().getValue());
        });
        allocateResult.getXcs().forEach(xc -> {
            locker.addResource(xc.getCrossConnectionId().getValue());
            if (xc.getNodeRef() != null) {
                locker.addResource(xc.getNodeRef().getValue());
                locker.addResource(PhysicalNodeIdNamingRule.getSiteId(xc.getNodeRef().getValue()));
            }
            if (xc.getSourceTp() != null) {
                xc.getSourceTp().stream().filter(tp -> tp.getTpRef() != null)
                        .forEach(tp -> addTpResourceLocks(locker, tp.getTpRef().getValue()));
            }
            if (xc.getDestinationTp() != null) {
                xc.getDestinationTp().stream().filter(tp -> tp.getTpRef() != null)
                        .forEach(tp -> addTpResourceLocks(locker, tp.getTpRef().getValue()));
            }
        });

        locker.getLock();
    }

    private void addTpResourceLocks(AbstractResourceLock locker, String tpId) {
        locker.addResource(tpId);
        locker.addResource(PhysicalTpIdNamingRule.getNodeId(tpId));
        locker.addResource(PhysicalTpIdNamingRule.getEquipId(tpId));
        locker.addResource(PhysicalNodeIdNamingRule.getSiteId(PhysicalTpIdNamingRule.getNodeId(tpId)));
    }

    private void removeReplacedPhyLinks(List<String> removedResourceIds) {
        if (removedResourceIds == null || removedResourceIds.isEmpty()) {
            return;
        }
        log.debug("removeReplacedPhyLinks wehn binding {}", removedResourceIds);
        // Match TunnelCreator3: REG allocation may replace an obsolete physical link before
        // the newly allocated route is stored.
        ChangedObject removedResources = new ChangedObject();
        PhyLinkUtil phyLinkUtil = new PhyLinkUtil(removedResources);
        removedResourceIds.forEach(id -> phyLinkUtil.removePhyLink(id, null));
        multipleTransaction.save(removedResources);
    }


    private ImmutablePair<TunnelBindOutputData, TaskInfoMessage> step1_allocate(BindTunnelInput input, TaskInfoMessage rootTaskInfo) throws NeDesignerException {

        try {

            //allocate
            TunnelBindOutputData allocateResult = tunnelNewOchAllocate.allocateBinding(
                    input.getTunnelId(), input.getSiteLinkRoute());

            //construct taskInfo
            String jsonResult = constructJsonMsg(allocateResult);
            TaskInfoMessage allocateTaskInfo = new TaskInfoMessage(rootTaskInfo.getWho(), rootTaskInfo.getResourceType(), TaskInfoMessage.ActionType.bind, "");
            allocateTaskInfo.setGroupId(rootTaskInfo.getGroupId());
            allocateTaskInfo.setRoot(false);
            allocateTaskInfo.setResourceName("allocate bind");
            allocateTaskInfo.setResourceId("allocate bind" + System.currentTimeMillis());
            allocateTaskInfo.setActionTime(System.currentTimeMillis());
            allocateTaskInfo.setEndTime(System.currentTimeMillis());

            allocateTaskInfo.setSuccessfully(true);

            log.debug("The allocate result for input:{}\n is:{}", input, jsonResult);

//            TaskInfoMessager.sendMessage(rootTaskInfo);
            return ImmutablePair.of(allocateResult, allocateTaskInfo);
        } catch (Exception e) {
            log.error("Failed to allocateBinding ", e);
            throw e;
        }

    }

    private String constructJsonMsg(TunnelBindOutputData allocateResult) {
        // Task detail is internal data and must not depend on a public RPC schema.
        return jsonOutputer.format(allocateResult);
    }

    private Link constructOchUpdated(String tunnelId, TunnelBindOutputData allocateResult) {

        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
        Och ochAddr = ochLink.getAugmentation(Link1.class).getOch();
        validateImplementStatus(ochAddr.getImplementState());

        //add supporting link
        ArrayList<SupportingLink> supportingLinkList = new ArrayList<>(ochLink.getSupportingLink());
        for (Link link : allocateResult.getLinks()) {
            supportingLinkList.add(new SupportingLinkBuilder()
                    .setLinkRef(link.getLinkId())
                    .setKey(new SupportingLinkKey(link.getLinkId()))
                    .build());
        }
        for (Link siteLink : allocateResult.getSiteLinks()) {
            addSupportingLinkIfAbsent(supportingLinkList, siteLink.getLinkId());
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route ochRoute =
                ochAddr.getExplictRoute().getRoute().get(0);
        RouteBuilder routeBuilder = new RouteBuilder(ochRoute);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route routeUpdated;
        if (hasSecondaryLeg(ochRoute)) {
            routeBuilder.setThird(new ArrayList<>(Collections.singletonList(constructOchThird(allocateResult))));
            // Only 2->3 enables the existing primary APS C member.
            routeUpdated = enablePrimaryApsThirdMember(routeBuilder.build());
        } else {
            routeBuilder.setSecondary(constructOchSecondary(allocateResult));
            routeUpdated = routeBuilder.build();
        }

        //should add a new temporary properties to indicate this is a binding (implement has special)
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        Properties newProp = ochLinkAttr.getProperties();
        if (!ochAddr.getImplementState().equals(ImplementState.Allocate)) {
            // Only implemented OCHs need a later 3rd-leg cleanup during toImplement.
            newProp = PropertyTool.addProperty(newProp, BINDING_3_RD_LEG, "true");
        }
        newProp = updateLegRequiredProperty(newProp, getProtectionLegCount(routeUpdated));

        Och ochAddrUpdate = new OchBuilder(ochLinkAttr)
                .setExplictRoute(new ExplictRouteBuilder().setRoute(new ArrayList<>(Collections.singletonList(routeUpdated))).build())
                .setImplementState(ochAddr.getImplementState().equals(ImplementState.Allocate) ? ImplementState.Allocate : ImplementState.PartialImplement)
                .setProtectionType(ProtectionBidir1To2.class)  //in fact original should be this
                .setProperties(newProp)
                .build();

        return new LinkBuilder(ochLink).addAugmentation(Link1.class,
                        new Link1Builder()
                                .setOch(ochAddrUpdate)
                                .build())
                .setSupportingLink(supportingLinkList)
                .build();
    }

    private boolean hasSecondaryLeg(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route) {
        return route.getSecondary() != null
                && route.getSecondary().getExplicitRouteObjects() != null
                && !route.getSecondary().getExplicitRouteObjects().isEmpty();
    }

    private Properties updateLegRequiredProperty(Properties properties, int legCount) {
        if (legCount < 3) {
            return PropertyTool.addProperty(properties, LEG_REQUIRED, "true");
        }
        return PropertyTool.delProperty(properties, LEG_REQUIRED);
    }

    private int getProtectionLegCount(Link ochLink) {
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        return getProtectionLegCount(och.getExplictRoute().getRoute().get(0));
    }

    private int getProtectionLegCount(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route) {
        int legCount = 0;
        if (route.getPrimary() != null && route.getPrimary().getExplicitRouteObjects() != null
                && !route.getPrimary().getExplicitRouteObjects().isEmpty()) {
            legCount = 1;
        }
        if (hasSecondaryLeg(route)) {
            legCount = 2;
        }
        if (route.getThird() != null && !route.getThird().isEmpty()) {
            legCount = 3;
        }
        return legCount;
    }

    private void addSupportingLinkIfAbsent(List<SupportingLink> supportingLinkList, LinkId linkId) {
        boolean existed = supportingLinkList.stream()
                .anyMatch(sl -> sl.getLinkRef() != null && sl.getLinkRef().equals(linkId));
        if (existed) {
            return;
        }
        supportingLinkList.add(new SupportingLinkBuilder()
                .setLinkRef(linkId)
                .setKey(new SupportingLinkKey(linkId))
                .build());
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route enablePrimaryApsThirdMember(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route) {
        RouteBuilder builder = new RouteBuilder(route);
        if (route.getPrimary() != null && route.getPrimary().getCrossConnections() != null) {
            builder.setPrimary(new PrimaryBuilder(route.getPrimary())
                    .setCrossConnections(route.getPrimary().getCrossConnections().stream()
                            .map(xc -> xcRepo.updateApsMemberEnable(xc, true))
                            .collect(Collectors.toList()))
                    .build());
        }
        // Bind adds the third leg to an existing OCH; only primary contains its APS XC.
        return builder.build();
    }

    private SupportedLink buildPhySupportedLink(LinkId siteLinkId) {
        TopologyId siteTopology = new TopologyId(Constant.SITE_TOPOID);
        return new SupportedLinkBuilder()
                .setLinkRef(siteLinkId)
                .setTopologyRef(siteTopology)
                .setKey(new SupportedLinkKey(siteLinkId, siteTopology))
                .build();
    }

    private List<Link> getMatchedSiteLinks(Link phyLink, List<Link> siteLinks) {
        return siteLinks.stream()
                .filter(siteLink -> isPhyLinkRelatedToSiteLink(phyLink, siteLink))
                .collect(Collectors.toList());
    }

    private boolean isPhyLinkRelatedToSiteLink(Link phyLink, Link siteLink) {
        String phyLinkId = phyLink.getLinkId().getValue();
        String siteLinkId = siteLink.getLinkId().getValue();
        return siteLinkId.contains(PhysicalLinkIdNamingRule.getNodeAId(phyLinkId))
                || siteLinkId.contains(PhysicalLinkIdNamingRule.getNodeZId(phyLinkId));
    }

    private void validateImplementStatus(ImplementState implementState) {
        if (implementState.equals(ImplementState.Implement) || implementState.equals(ImplementState.Allocate) ) {
            return;
        }

        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                String.format("Can't do bind for implementState: %s", implementState.name()));
    }

    private void validateSameOchTunnelStateForBind(List<Tunnel> sameOchTunnels) {
        // Binding changes the OCH, so every tunnel under it must be in a stable state.
        List<String> invalidTunnelNames = sameOchTunnels.stream()
                .filter(tunnel -> !tunnel.getImplementState().equals(ImplementState.Allocate)
                        && !tunnel.getImplementState().equals(ImplementState.Implement))
                .map(Tunnel::getFriendlyName)
                .collect(Collectors.toList());
        if (!invalidTunnelNames.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Can't bind 3rd leg when tunnels under the same OCH are not Allocate or Implement: "
                            + String.join(", ", invalidTunnelNames));
        }

    }

    private Third constructOchThird(TunnelBindOutputData allocateResult) {
        RouteInfo routeInfo = RouteInfo.builder()
                .main(net.flex.dci.otn.controller.allocate.designer.model.Route.builder()
                        .nodes(allocateResult.getNodes())
                        .links(allocateResult.getRouteLinks())
                        .xcs(allocateResult.getXcs())
                        .build())
                .build();
        List<ExplicitRouteObjects> eroList = new Route(routeInfo, Route.RouteType.OchLink)
                .getExplictRoute(allocateResult.getThirdSourceTp(), allocateResult.getThirdDestTp())
                .getRoute().get(0).getPrimary().getExplicitRouteObjects();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third third = new ThirdBuilder()
                .setIndex((short) 0)
                .setExplicitRouteObjects(eroList)
                .setCrossConnections(getRouteXC(allocateResult.getXcs()))
                .build();

        return third;
    }

    private Secondary constructOchSecondary(TunnelBindOutputData allocateResult) {
        RouteInfo routeInfo = RouteInfo.builder()
                .main(net.flex.dci.otn.controller.allocate.designer.model.Route.builder()
                        .nodes(allocateResult.getNodes())
                        .links(allocateResult.getRouteLinks())
                        .xcs(allocateResult.getXcs())
                        .build())
                .build();
        List<ExplicitRouteObjects> eroList = new Route(routeInfo, Route.RouteType.OchLink)
                .getExplictRoute(allocateResult.getThirdSourceTp(), allocateResult.getThirdDestTp())
                .getRoute().get(0).getPrimary().getExplicitRouteObjects();
        return new SecondaryBuilder()
                .setExplicitRouteObjects(eroList)
                .setCrossConnections(getRouteXC(allocateResult.getXcs()))
                .build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> getRouteXC(@NonNull List<CrossConnections> xcs) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> result = new ArrayList<>(xcs
                .size());
        long index = 1;
        for (CrossConnections xc : xcs) {
            result.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(xc)
                    .setSequence(index++)
                    .build());
        }
        return result;
    }

    private void validateTunnel(Tunnel tunnel) throws BindExistedException {
        validateTunnelBase(tunnel);
        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnel.getTunnelId().getValue());
        validateOchLinkForBind(ochLink);
    }

    private void validateTunnelBase(Tunnel tunnel) {
        validateTunnelProtectionType(tunnel);
        validateImplementStatus(tunnel.getImplementState());
    }

    private void validateTunnelProtectionType(Tunnel tunnel) {
        if (!tunnel.getProtectionType().equals(ProtectionBidir1To2.class)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "invalid tunnel, only support 1:3 binding, but is: " + tunnel.getProtectionType()
                    .getSimpleName());
        }
    }

    private void validateOchLinkForBind(Link ochLink) throws BindExistedException {
        Och ochAddr = ochLink.getAugmentation(Link1.class).getOch();

        if (!ochAddr.getProtectionType().equals(ProtectionBidir1To2.class)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "OCH protection type invalid as :" + ochAddr.getProtectionType());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route ochRoute =
                ochAddr.getExplictRoute().getRoute().get(0);
        if (ochRoute.getPrimary() == null || ochRoute.getPrimary().getExplicitRouteObjects() == null
                || ochRoute.getPrimary().getExplicitRouteObjects().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "binding protection leg requires an OCH with primary leg");
        }
        if (ochRoute.getThird() != null && !ochRoute.getThird().isEmpty()) {
            throw new BindExistedException();
        }
    }

    private void validateBindingRoute(BindTunnelInput input, Tunnel tunnel) {
        if (input.getSiteLinkRoute() == null || input.getSiteLinkRoute().getPrimary() == null
                || input.getSiteLinkRoute().getPrimary().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "binding route must contain at least one siteLink");
        }

        // The UI selects only siteLinks. REG/ROADM allocation expands all physical links later.
        List<String> selectedSiteLinks = input.getSiteLinkRoute().getPrimary();
        if (selectedSiteLinks.stream().anyMatch(linkId -> !SiteLinkIdNamingRule.isSiteLink(linkId))) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "binding route contains a non-siteLink resource");
        }
        if (new HashSet<>(selectedSiteLinks).size() != selectedSiteLinks.size()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "binding route contains duplicate siteLinks");
        }

        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnel.getTunnelId().getValue());
        Set<String> existingSiteLinks = ochLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toSet());
        if (selectedSiteLinks.stream().anyMatch(existingSiteLinks::contains)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "binding route overlaps an existing OCH leg");
        }
    }

    public GetBindingListOutput getBindingList(GetBindingListInput input) {
        //validate
        String tunnelId = input.getTunnelId();
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        try {
            validateTunnel(tunnel);
        } catch (BindExistedException e) {
            return new GetBindingListOutputBuilder().setBindingRoutes(Collections.emptyList()).build();
        }

        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
        try {
            List<BindingRoutes> routes = new ArrayList<>();
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute> computed =
                    tunnelComputer2.getBindingRoutes(ochLink, tunnel);
            for (int i = 0; i < computed.size(); i++) {
                routes.add(new BindingRoutesBuilder()
                        .setIndex(i)
                        .setSiteLinkRoute(computed.get(i))
                        .build());
            }
            return new GetBindingListOutputBuilder().setBindingRoutes(routes).build();
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to compute binding routes: " + e.getMessage(), e);
        }
    }

    public BatchGetBindingListOutput batchGetBindingList(BatchGetBindingListInput input) {
        BatchBindingContext context = buildBatchBindingContext(input.getTunnelId());
        try {
            List<SiteLinkRoute> computed = tunnelComputer2.getBindingRoutes(context.getRepresentativeOchLink(),
                    context.getRepresentativeTunnel());
            List<SiteLinkRoute> filteredRoutes = filterBatchBindingRoutes(computed, context);
            if (filteredRoutes.isEmpty()) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "No common binding route for selected tunnels");
            }

            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.batch.get.binding.list.output.BindingRoutes> routes =
                    new ArrayList<>();
            for (int i = 0; i < filteredRoutes.size(); i++) {
                routes.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.batch.get.binding.list.output.BindingRoutesBuilder()
                        .setIndex(i)
                        .setSiteLinkRoute(filteredRoutes.get(i))
                        .build());
            }
            return new BatchGetBindingListOutputBuilder().setBindingRoutes(routes).build();
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to compute batch binding routes: " + e.getMessage(), e);
        }
    }

    public BatchBindTunnelOutput batchDoIt(BatchBindTunnelInput input, TaskInfoMessage rootTaskInfo) {
        BatchBindingContext context = buildBatchBindingContext(input.getTunnelId());
        validateBatchBindingRoute(input.getSiteLinkRoute(), context);

        // Batch bind reports progress as one root task per affected tunnel; the incoming taskInfo is only a template.
        try {
            for (BatchOchBindingGroup group : context.ochGroups.values()) {
                Tunnel bindTunnel = group.getBindRepresentativeTunnel();
                BindTunnelInput bindInput = new BindTunnelInputBuilder()
                        .setTunnelId(bindTunnel.getTunnelId().getValue())
                        .setSiteLinkRoute(input.getSiteLinkRoute())
                        .build();
                TaskInfoMessage subTaskInfo = new TaskInfoMessage(rootTaskInfo.getWho(),
                        rootTaskInfo.getResourceType(), TaskInfoMessage.ActionType.bind, "");
                subTaskInfo.setGroupId(nextTaskGroupId());
                subTaskInfo.setRoot(true);
                subTaskInfo.setResourceId(bindTunnel.getTunnelId().getValue());
                doIt(bindInput, bindTunnel, subTaskInfo);
            }
            return new BatchBindTunnelOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .setReturnMessage("Batch bind tunnel successfully.")
                    .build();
        } catch (Exception e) {
            log.error("Failed to batch bind for :{}", input, e);
            if (e instanceof CommonException) {
                throw (CommonException) e;
            }
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "batch bind tunnel error" + e.toString(), e);
        }
    }

    private BatchBindingContext buildBatchBindingContext(List<String> tunnelIds) {
        if (tunnelIds == null || tunnelIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "batch binding tunnel-id list is empty");
        }
        LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
        LinkedHashSet<String> uniqueTunnelIds = new LinkedHashSet<>(tunnelIds);
        if (uniqueTunnelIds.size() != tunnelIds.size()) {
            Set<String> duplicateTunnelIds = findDuplicates(tunnelIds);
            validationErrors.add("batch binding tunnel-id list contains duplicate tunnel: "
                    + duplicateTunnelIds.stream()
                    .map(this::getTunnelDisplayName)
                    .collect(Collectors.joining(", ")));
        }

        BatchBindingContext context = new BatchBindingContext();
        LinkedHashSet<String> pendingTunnelIds = new LinkedHashSet<>(uniqueTunnelIds);
        while (!pendingTunnelIds.isEmpty()) {
            String tunnelId = pendingTunnelIds.iterator().next();
            pendingTunnelIds.remove(tunnelId);
            if (isBlank(tunnelId)) {
                validationErrors.add("Tunnel id is empty");
                continue;
            }
            Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
            if (tunnel == null) {
                validationErrors.add("Tunnel does not exist: " + tunnelId);
                continue;
            }
            if (!collectBatchValidationError(validationErrors, tunnel.getFriendlyName(), () -> validateTunnelBase(tunnel))) {
                continue;
            }
            String ochLinkId;
            try {
                ochLinkId = getOchLinkIdFromTunnel(tunnel);
            } catch (CommonException e) {
                validationErrors.add(batchValidationMessage(tunnel.getFriendlyName(), e));
                continue;
            }
            BatchOchBindingGroup group = context.ochGroups.get(ochLinkId);
            if (group == null) {
                Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
                try {
                    validateOchLinkForBind(ochLink);
                } catch (BindExistedException e) {
                    validationErrors.add("OCH already has third leg for tunnel: " + tunnel.getFriendlyName());
                    continue;
                } catch (CommonException e) {
                    validationErrors.add(batchValidationMessage(tunnel.getFriendlyName(), e));
                    continue;
                }

                List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(Arrays.asList(ochLinkId));
                if (!collectBatchValidationError(validationErrors, tunnel.getFriendlyName(),
                        () -> validateSameOchTunnelStateForBind(sameOchTunnels))) {
                    continue;
                }
                Set<String> sameOchTunnelIds = sameOchTunnels.stream()
                        .map(sameOchTunnel -> sameOchTunnel.getTunnelId().getValue())
                        .collect(Collectors.toSet());
                // Same-OCH input tunnels share one OCH operation, so remove them from the outer scan.
                sameOchTunnels.stream()
                        .filter(sameOchTunnel -> pendingTunnelIds.contains(sameOchTunnel.getTunnelId().getValue()))
                        .forEach(sameOchTunnel -> collectBatchValidationError(validationErrors,
                                sameOchTunnel.getFriendlyName(), () -> validateTunnelBase(sameOchTunnel)));
                pendingTunnelIds.removeAll(sameOchTunnelIds);
                group = new BatchOchBindingGroup(ochLink, tunnel,
                        sameOchTunnels,
                        getEndpointSiteIds(ochLink),
                        getPrimarySiteLinks(ochLink), getSecondarySiteLinks(ochLink));
                context.ochGroups.put(ochLinkId, group);
            }
        }

        throwBatchValidationErrors(validationErrors);
        validateBatchOchCompatibility(context, validationErrors);
        throwBatchValidationErrors(validationErrors);
        return context;
    }

    private boolean collectBatchValidationError(LinkedHashSet<String> validationErrors,
                                                String tunnelFriendlyName, Runnable validation) {
        try {
            validation.run();
            return true;
        } catch (CommonException e) {
            validationErrors.add(batchValidationMessage(tunnelFriendlyName, e));
            return false;
        }
    }

    private String batchValidationMessage(String tunnelFriendlyName, CommonException e) {
        return tunnelFriendlyName + ": " + e.getMessage();
    }

    private void throwBatchValidationErrors(LinkedHashSet<String> validationErrors) {
        if (validationErrors.isEmpty()) {
            return;
        }
        // Batch validation reports every invalid input instead of failing fast on the first tunnel.
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                String.join("; ", validationErrors));
    }

    private void validateBatchOchCompatibility(BatchBindingContext context, LinkedHashSet<String> validationErrors) {
        BatchOchBindingGroup reference = context.getRepresentativeGroup();
        for (BatchOchBindingGroup group : context.ochGroups.values()) {
            if (!reference.endpointSiteIds.equals(group.endpointSiteIds)) {
                validationErrors.add(group.representativeTunnel.getFriendlyName()
                        + ": selected tunnel has different A/Z sites from "
                        + reference.representativeTunnel.getFriendlyName());
            }
            if (!reference.primarySiteLinks.equals(group.primarySiteLinks)) {
                validationErrors.add(group.representativeTunnel.getFriendlyName()
                        + ": selected tunnel has different primary siteLink routes from "
                        + reference.representativeTunnel.getFriendlyName());
            }
            if (!reference.secondarySiteLinks.equals(group.secondarySiteLinks)) {
                validationErrors.add(group.representativeTunnel.getFriendlyName()
                        + ": selected tunnel has different secondary siteLink routes from "
                        + reference.representativeTunnel.getFriendlyName());
            }
        }
    }

    private List<SiteLinkRoute> filterBatchBindingRoutes(List<SiteLinkRoute> routes, BatchBindingContext context) {
        List<Long> batchCenterFrequencies = context.getCenterFrequencies();
        return routes.stream()
                // The representative OCH computes paths once; every candidate must still carry all OCH frequencies.
                .filter(route -> isBatchRouteAvailable(route, context))
                .map(route -> new SiteLinkRouteBuilder(route)
                        .setCentralFrequencies(batchCenterFrequencies)
                        .build())
                .collect(Collectors.toList());
    }

    private void validateBatchBindingRoute(SiteLinkRoute route, BatchBindingContext context) {
        LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
        if (route == null || route.getPrimary() == null || route.getPrimary().isEmpty()) {
            validationErrors.add("batch binding route must contain at least one siteLink");
            throwBatchValidationErrors(validationErrors);
        }
        List<String> selectedSiteLinks = route.getPrimary();
        List<String> nonSiteLinks = selectedSiteLinks.stream()
                .filter(linkId -> !SiteLinkIdNamingRule.isSiteLink(linkId))
                .collect(Collectors.toList());
        List<String> validSiteLinks = selectedSiteLinks.stream()
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toList());
        if (!nonSiteLinks.isEmpty()) {
            validationErrors.add("batch binding route contains non-siteLink resources: "
                    + String.join(", ", nonSiteLinks));
        }
        Set<String> duplicateSiteLinks = findDuplicates(selectedSiteLinks);
        if (!duplicateSiteLinks.isEmpty()) {
            validationErrors.add("batch binding route contains duplicate siteLinks: "
                    + duplicateSiteLinks.stream()
                    .map(this::getSiteLinkDisplayName)
                    .collect(Collectors.joining(", ")));
        }
        for (BatchOchBindingGroup group : context.ochGroups.values()) {
            Set<String> existingRouteSiteLinks = new HashSet<>();
            existingRouteSiteLinks.addAll(group.primarySiteLinks);
            existingRouteSiteLinks.addAll(group.secondarySiteLinks);
            List<String> overlappedSiteLinks = validSiteLinks.stream()
                    .filter(existingRouteSiteLinks::contains)
                    .collect(Collectors.toList());
            if (!overlappedSiteLinks.isEmpty()) {
                validationErrors.add(group.representativeTunnel.getFriendlyName()
                        + ": batch binding route overlaps existing leg siteLinks: "
                        + overlappedSiteLinks.stream()
                        .map(this::getSiteLinkDisplayName)
                        .collect(Collectors.joining(", ")));
            }
        }
        validationErrors.addAll(getBatchRouteUnavailableMessages(validSiteLinks, context));
        throwBatchValidationErrors(validationErrors);
    }

    private Set<String> findDuplicates(List<String> values) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String value : values) {
            if (!seen.add(value)) {
                duplicates.add(value);
            }
        }
        return duplicates;
    }

    private boolean isBatchRouteAvailable(SiteLinkRoute route, BatchBindingContext context) {
        if (route == null || route.getPrimary() == null || route.getPrimary().isEmpty()) {
            return false;
        }
        return getBatchRouteUnavailableMessages(route.getPrimary(), context).isEmpty();
    }

    private List<String> getBatchRouteUnavailableMessages(List<String> siteLinkIds, BatchBindingContext context) {
        List<String> validationErrors = new ArrayList<>();
        for (BatchOchBindingGroup group : context.ochGroups.values()) {
            List<String> unavailableSiteLinks = getUnavailableSiteLinksForOch(siteLinkIds, group.och);
            if (!unavailableSiteLinks.isEmpty()) {
                validationErrors.add(group.representativeTunnel.getFriendlyName()
                        + ": batch binding route is not available for siteLinks: "
                        + unavailableSiteLinks.stream()
                        .map(this::getSiteLinkDisplayName)
                        .collect(Collectors.joining(", ")));
            }
        }
        return validationErrors;
    }

    private List<String> getUnavailableSiteLinksForOch(List<String> siteLinkIds, Och och) {
        List<String> unavailableSiteLinks = new ArrayList<>();
        long lowerFrequency = och.getLowerFrequency().getValue().longValue();
        long upperFrequency = och.getUpperFrequency().getValue().longValue();
        for (String siteLinkId : siteLinkIds) {
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            // Batch bind must match get-binding-list: every selected siteLink must have free spectrum.
            boolean available = siteLink != null
                    && CommonUtils.isSiteLinkAvailableForOch(siteLink, lowerFrequency, upperFrequency);
            if (!available) {
                unavailableSiteLinks.add(siteLinkId);
            }
        }
        return unavailableSiteLinks;
    }

    private Set<String> getEndpointSiteIds(Link ochLink) {
        Set<String> endpointSiteIds = new HashSet<>();
        // OCH is bidirectional for this validation, so compare A/Z as an unordered site pair.
        endpointSiteIds.add(normalizeSiteNodeId(ochLink.getSource().getSourceTp().getValue()));
        endpointSiteIds.add(normalizeSiteNodeId(ochLink.getDestination().getDestTp().getValue()));
        return endpointSiteIds;
    }

    private String normalizeSiteNodeId(String id) {
        if (id == null || id.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid empty node id in OCH endpoint");
        }
        int index = id.indexOf("#");
        return index > 0 ? id.substring(0, index) : id;
    }

    private List<String> getPrimarySiteLinks(Link ochLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route =
                ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute().get(0);
        return extractSiteLinks(route.getPrimary().getExplicitRouteObjects());
    }

    private List<String> getSecondarySiteLinks(Link ochLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route =
                ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute().get(0);
        return extractSiteLinks(route.getSecondary().getExplicitRouteObjects());
    }

    private List<String> extractSiteLinks(List<ExplicitRouteObjects> explicitRouteObjects) {
        List<String> siteLinkIds = new ArrayList<>();
        if (explicitRouteObjects == null) {
            return siteLinkIds;
        }
        // Different OCHs have different OP/OT to optical resources, so compare only ordered siteLinks.
        for (ExplicitRouteObjects explicitRouteObject : explicitRouteObjects) {
            if (explicitRouteObject.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject pathRouteObject : explicitRouteObject.getPathRouteObject()) {
                if (!(pathRouteObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link)) {
                    continue;
                }
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link link =
                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject.getResourceType();
                String linkId = link.getLinkHop().getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    siteLinkIds.add(linkId);
                }
            }
        }
        return siteLinkIds;
    }

    private String getOchLinkIdFromTunnel(Tunnel tunnel) {
        if (tunnel.getSupportingLink() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Tunnel has no supporting OCH link");
        }
        return tunnel.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .filter(linkId -> linkId != null && linkId.startsWith("OchLink-"))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Tunnel has no supporting OCH link"));
    }

    private String getTunnelDisplayName(String tunnelId) {
        if (isBlank(tunnelId)) {
            return "<empty>";
        }
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        return tunnel == null ? tunnelId : tunnel.getFriendlyName();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String getSiteLinkDisplayName(String siteLinkId) {
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        if (siteLink == null || siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class) == null) {
            return siteLinkId;
        }
        Site site = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        return site == null || site.getFriendlyName() == null ? siteLinkId : site.getFriendlyName();
    }

    private void sendBindSuccessNotification(Tunnel tunnel) {
        String msg = String.format("Bind tunnel %s successfully.", tunnel.getFriendlyName());
        // Notify only after the binding DB update has succeeded, so UI messages match committed data.
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("bind tunnel")
                        .message(msg)
                        .error(false)
                        .build());
    }

    private class BatchBindingContext {
        private final LinkedHashMap<String, BatchOchBindingGroup> ochGroups = new LinkedHashMap<>();

        BatchOchBindingGroup getRepresentativeGroup() {
            return ochGroups.values().iterator().next();
        }

        Link getRepresentativeOchLink() {
            return getRepresentativeGroup().ochLink;
        }

        Tunnel getRepresentativeTunnel() {
            return getRepresentativeGroup().representativeTunnel;
        }

        List<Long> getCenterFrequencies() {
            return ochGroups.values().stream()
                    .map(group -> getCenterFrequency(group.och))
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    private class BatchOchBindingGroup {
        private final Link ochLink;
        private final Och och;
        private final Tunnel representativeTunnel;
        private final List<Tunnel> sameOchTunnels;
        private final Set<String> endpointSiteIds;
        private final List<String> primarySiteLinks;
        private final List<String> secondarySiteLinks;

        BatchOchBindingGroup(Link ochLink, Tunnel representativeTunnel, List<Tunnel> sameOchTunnels,
                             Set<String> endpointSiteIds,
                             List<String> primarySiteLinks, List<String> secondarySiteLinks) {
            this.ochLink = ochLink;
            this.och = ochLink.getAugmentation(Link1.class).getOch();
            this.representativeTunnel = representativeTunnel;
            this.sameOchTunnels = sameOchTunnels;
            this.endpointSiteIds = endpointSiteIds;
            this.primarySiteLinks = primarySiteLinks;
            this.secondarySiteLinks = secondarySiteLinks;
        }

        Tunnel getBindRepresentativeTunnel() {
            // Binding affects the whole OCH; prefer an implemented tunnel as the operation representative.
            return sameOchTunnels.stream()
                    .filter(tunnel -> tunnel.getImplementState().equals(ImplementState.Implement))
                    .findFirst()
                    .orElse(representativeTunnel);
        }
    }

    private long getCenterFrequency(Och och) {
        return (och.getLowerFrequency().getValue().longValue()
                + och.getUpperFrequency().getValue().longValue()) / 2;
    }

    private class BindExistedException extends Exception {

    }


    protected void store2DB(ChangedObject changedObject) {
        log.debug("start save to mongo");
        changedObject.unsetAllPhyOpNode();
        multipleTransaction.save(changedObject);


        log.debug("start merge to OP\"");

        OpNodeMerger opMerger = new OpNodeMerger();

        List<String> nodes = new ArrayList<>(changedObject.getChangedPhyNodeList().keySet());
        int batchSize = 8;
        for (int i = 0; i < nodes.size(); i += batchSize) {
            int end = Math.min(i + batchSize, nodes.size());
            List<String> batch = nodes.subList(i, end);

            batch.parallelStream().forEach(opMerger::merge);
        }
        log.debug("merge to OP done");
    }
}
