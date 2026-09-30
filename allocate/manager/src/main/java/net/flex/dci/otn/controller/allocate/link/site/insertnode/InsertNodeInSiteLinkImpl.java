package net.flex.dci.otn.controller.allocate.link.site.insertnode;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.allocate.common.util.CommonUtils;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkNodeOperationState;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 在 siteLink 中间的一条 OTS phyLink 上插入 ILA/DGE。
 *
 * <p>输入中的 phy-link-id 指向被拆分的旧 OTS: A--B，site-node-id 指向新网元 N 所在站点。
 * 插入后旧路由变为 A--N--B。这里需要同时维护 siteLink、phyLink、phyNode、siteNode rack、
 * internal-link、已有 OCH link route 这些模型，否则 UI、BOM、后续业务创建和实现阶段会看到不一致的数据。</p>
 *
 * <p>删除/替换资源：</p>
 * <ul>
 *     <li>删除旧 OTS phyLink: A--B，并同步删除/扣减它对应的 OtsLink viewLink 映射。</li>
 *     <li>删除 A、B 两端与旧 A--B 关联的 internal-link。</li>
 *     <li>从 siteLink.supportingLink 和 siteLink.explictRoute 中移除旧 A--B。</li>
 *     <li>OCH link 仍只承载 siteLink hop，不删除或插入 OTS/DGE TP path-route-object。</li>
 * </ul>
 *
 * <p>创建/更新资源：</p>
 * <ul>
 *     <li>新建光层 phyNode: N，厂家信息来自 siteLink properties: product-type / vendor-name。</li>
 *     <li>根据 ILA/DGE 板卡类型创建 N 的 equipment、TP、BOM 信息。</li>
 *     <li>新建两条 OTS phyLink: A--N、N--B，implement-state 都是 Allocate。</li>
 *     <li>为 A--N、N--B 创建/增加 OtsLink viewLink 映射；OTS link 和 siteLink 一样会被投影到 view topo 入库。</li>
 *     <li>给 A/N/B 增加新的 internal-link，implement-state 都是 Allocate。</li>
 *     <li>把 N 通过 SiteNodeCorrelateResource.updateRack(...) 加入 site-node-id 对应 rack。</li>
 *     <li>更新 siteLink.supportingLink / explictRoute，把旧 A--B 替换为 A--N、N--B。</li>
 *     <li>siteLink.implement-state 置为 PartialImplement，因为插入节点后只有新增资源处于 allocate 状态。</li>
 *     <li>DGE 场景更新受影响 OCH link 的 route crossConnections，按实际路由合并新增 WSS channel XC。</li>
 * </ul>
 *
 * <p>ILA 和 DGE 的大部分资源处理一致，最大差异是 DGE 需要 WSS channel XC：</p>
 * <ul>
 *     <li>ILA: 只更新 siteLink/phyLink/internal-link/OCH path，不额外创建 WSS channel XC。</li>
 *     <li>DGE: 需要基于已有 implement/partial OCH link，包括 dummyOchLink，在新 DGE 节点上创建 WSS channel XC；
 *     创建出的 XC 先缓存起来，后续 OCH route 修改时再加入对应 route crossConnections。</li>
 * </ul>
 *
 * <p>因此代码结构按公共基础流程 + feature hook 设计：
 * BaseInsertNodeInSiteLinkOperation 完成公共资源拆分和模型更新，
 * DgeInsertNodeInSiteLinkOperation 只实现 DGE 新节点上的 WSS channel XC 准备逻辑。</p>
 */
@Slf4j
@Service
public class InsertNodeInSiteLinkImpl {
    @Autowired
    private MultipleTransaction mongoTransaction;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private ViewLinkDao viewLinkDao;

    @Autowired
    private BomGenerator bomGenerator;

    public InsertNodeInSiteLinkResult doIt(TaskInfoMessage taskInfo, String siteLinkId, String phyLinkId, String siteNodeId,
                                           LinkTerminationNodeType nodeType) {
        ZkResourceLock locker = new ZkResourceLock();
        try {
            checkingRequiredInput(siteLinkId, phyLinkId, siteNodeId, nodeType);

            lockResources(locker, siteLinkId, phyLinkId, siteNodeId);

            // 这是 Spring 单例 Service，请求级对象必须保持在局部变量里，避免不同 siteLink 的并发请求互相覆盖。
            ChangedObject changedObject = new ChangedObject();
            InsertNodeInSiteLinkContext context = checking(changedObject, siteLinkId, phyLinkId, siteNodeId, nodeType);
            boolean applyRequired = requiresApply(context.siteLink.getAugmentation(Link1.class)
                    .getSite().getImplementState());
            insert(context, changedObject);
            applyInsertedNodeAmplifierProfile(context, changedObject);
            synchronizeInsertedAmplifierRoute(context, changedObject);
            BomInfo bomInfo = constructBomInfo(context);
            taskInfo.setResourceId(siteLinkId);
            taskInfo.setResourceName(context.siteLink.getAugmentation(Link1.class).getSite().getFriendlyName()
                    + ": insert " + context.insertedPhyNode.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                            .getPhysical().getFriendlyName());
            if (applyRequired) {
                markWaitingInsertApply(siteLinkId,
                        context.insertedPhyNode.getNodeId().getValue(), changedObject);
            }
            changedObject.siteLinkRouteChange(siteLinkId);
            mongoTransaction.save(changedObject);
            return new InsertNodeInSiteLinkResult(context.insertedPhyNode.getNodeId().getValue(),
                    siteLinkId, bomInfo, applyRequired);
        } catch (CommonException ce) {
            taskInfo.setErrorReason(ce.getMessage());
            TaskInfoMessager.sendMessage(taskInfo);
            throw ce;
        } catch (Exception e) {
            taskInfo.setErrorReason(e.getMessage());
            TaskInfoMessager.sendMessage(taskInfo);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ExceptionUtils.getRootCauseMessage(e));
        } finally {
            locker.unlock();
        }
    }

    static boolean requiresApply(ImplementState originalSiteLinkState) {
        return !ImplementState.Allocate.equals(originalSiteLinkState);
    }

    private void lockResources(ZkResourceLock locker, String siteLinkId, String phyLinkId,
            String siteNodeId) {
        String aNodeId = PhysicalLinkIdNamingRule.getNodeAId(phyLinkId);
        String zNodeId = PhysicalLinkIdNamingRule.getNodeZId(phyLinkId);
        Set<String> resources = new TreeSet<>();
        resources.add(siteLinkId);
        resources.add(phyLinkId);
        resources.add(aNodeId);
        resources.add(zNodeId);
        resources.add(siteNodeId);

        List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(
                Collections.singletonList(siteLinkId));
        if (ochLinks != null) {
            ochLinks.stream()
                    .filter(link -> link != null && link.getLinkId() != null)
                    .map(link -> link.getLinkId().getValue())
                    .forEach(resources::add);
        }

        List<Link> oldViewLinks = viewLinkDao.getViewLinkBySupportingLinks(
                Collections.singletonList(phyLinkId));
        if (oldViewLinks != null) {
            oldViewLinks.stream()
                    .filter(link -> link != null && link.getLinkId() != null)
                    .map(link -> link.getLinkId().getValue())
                    .forEach(resources::add);
        }

        Link siteLink = new ChangedObject().getChangedSiteLink(siteLinkId);
        Link1 siteLinkAugmentation = siteLink == null ? null : siteLink.getAugmentation(Link1.class);
        Site site = siteLinkAugmentation == null ? null : siteLinkAugmentation.getSite();
        if (site != null && site.getPlaneId() != null) {
            String aSiteId = PhysicalNodeIdNamingRule.getSiteId(aNodeId);
            String zSiteId = PhysicalNodeIdNamingRule.getSiteId(zNodeId);
            Collections.addAll(resources, ViewLink.getViewLinkId(
                    aSiteId, siteNodeId, ViewLinkType.OtsLink, site.getPlaneId()));
            Collections.addAll(resources, ViewLink.getViewLinkId(
                    siteNodeId, zSiteId, ViewLinkType.OtsLink, site.getPlaneId()));
        }

        resources.forEach(locker::addResource);
        locker.getLock();
    }

    private void insert(InsertNodeInSiteLinkContext context, ChangedObject changedObject) {
        BaseInsertNodeInSiteLinkOperation operation = context.nodeType.equals(LinkTerminationNodeType.DGE)
                ? new DgeInsertNodeInSiteLinkOperation(context, changedObject)
                : new IlaInsertNodeInSiteLinkOperation(context, changedObject);
        operation.execute();
    }

    private void applyInsertedNodeAmplifierProfile(InsertNodeInSiteLinkContext context,
            ChangedObject changedObject) {
        Link updatedSiteLink = changedObject.getChangedSiteLink(context.siteLinkId);
        Site site = updatedSiteLink.getAugmentation(Link1.class).getSite();
        NeYangModel yangModel = CommonUtils.getYangModelInProperties(site.getProperties());
        if (!NeYangModel.ByteDance.equals(yangModel)
                && !NeYangModel.Chassis20.equals(yangModel)) {
            log.warn("skip inserted node amplifier profile because yang-model is "
                            + "unsupported: siteLink={}, node={}, yangModel={}",
                    context.siteLinkId, context.insertedPhyNode.getNodeId().getValue(), yangModel);
            return;
        }

        String insertedNodeId = context.insertedPhyNode.getNodeId().getValue();
        Node insertedNode = changedObject.getChangedPhyNode(insertedNodeId);
        Node updatedNode = InsertedAmplifierProfileApplier.apply(updatedSiteLink, insertedNode);
        changedObject.addChangedPhyNode(updatedNode);
        context.setInsertedPhyNode(updatedNode);
        log.info("prepared inserted node amplifier template defaults without optical calculation: "
                        + "siteLink={}, node={}", context.siteLinkId, insertedNodeId);
    }

    private void synchronizeInsertedAmplifierRoute(InsertNodeInSiteLinkContext context,
            ChangedObject changedObject) {
        Link siteLink = changedObject.getChangedSiteLink(context.siteLinkId);
        Link updatedSiteLink = InsertedAmplifierRouteSynchronizer.synchronize(
                siteLink, context.insertedPhyNode);
        changedObject.addChangedSiteLink(updatedSiteLink);
    }

    private void markWaitingInsertApply(String siteLinkId, String insertedNodeId,
            ChangedObject changedObject) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        changedObject.addChangedSiteLink(SiteLinkNodeOperationState.set(
                siteLink, SiteLinkNodeOperationState.WAITING_INSERT_APPLY, insertedNodeId));
    }

    private BomInfo constructBomInfo(InsertNodeInSiteLinkContext context) {
        try {
            // insert-node-in-site-link 只新增一个光层设备，BOM 只应统计这个 inserted phyNode 的新增板卡。
            // 这里不传 reused snapshot，避免把原 siteLink 两端已有设备再次计入 BOM。
            return bomGenerator.constructBomInfo(Collections.singletonMap(
                    context.insertedPhyNode.getNodeId().getValue(), context.insertedPhyNode), Collections.<Node>emptyList());
        } catch (NeDesignerException e) {
            log.error("Failed to generate BOM for inserted node {}", context.insertedPhyNode.getNodeId().getValue(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to generate BOM." + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    private void checkingRequiredInput(String siteLinkId, String phyLinkId, String siteNodeId,
                                       LinkTerminationNodeType nodeType) {
        if (siteLinkId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "site-link-id is required");
        }
        if (phyLinkId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "phy-link-id is required");
        }
        if (siteNodeId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "site-node-id is required");
        }
        if (nodeType == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "node-type is required");
        }
        if (!nodeType.equals(LinkTerminationNodeType.DGE) && !nodeType.equals(LinkTerminationNodeType.ILA)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "only support ILA and DGE, current is " + nodeType);
        }
    }

    private InsertNodeInSiteLinkContext checking(ChangedObject changedObject, String siteLinkId, String phyLinkId, String siteNodeId,
                                                 LinkTerminationNodeType nodeType) throws CommonException {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required sitelink");
        }
        Site site = siteLink.getAugmentation(Link1.class) == null
                ? null : siteLink.getAugmentation(Link1.class).getSite();
        if (!supportsNodeInsertion(site)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "insert-node-in-site-link only supports C+L siteLink");
        }
        String operationState = SiteLinkNodeOperationState.get(siteLink);
        if (operationState != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink is processing a node operation: " + operationState);
        }
        Link oldPhyLink = changedObject.getChangedPhyLink(phyLinkId);
        if (oldPhyLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required OTS phylink");
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 oldPhyLinkAug =
                oldPhyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical oldPhyAttr =
                oldPhyLinkAug == null ? null : oldPhyLinkAug.getPhysical();
        if (oldPhyAttr == null || !org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType.OtsLink.equals(oldPhyAttr.getLinkType())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "phy-link-id must be an OTS phylink");
        }
        if (changedObject.getChangedSiteNode(siteNodeId) == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required site node");
        }

        List<String> siteLinkIds = new ArrayList<>();
        siteLinkIds.add(siteLinkId);
        List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(siteLinkIds);

        //additional check does some och working route still on this siteLink
        //TODO for later

        String planeId = site.getPlaneId();
        return new InsertNodeInSiteLinkContext(siteLinkId, phyLinkId, siteNodeId, planeId, nodeType, siteLink, oldPhyLink, ochLinks);
    }

    static boolean supportsNodeInsertion(Site site) {
        return site != null && WDM_Band.C_L.equals(CommonUtils.getWDMBand(site));
    }
}
