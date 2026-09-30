package net.flex.dci.otn.controller.implement.site.nbi.impl.bytedance;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.ase.ne.bytedance.*;
import net.flex.dci.otn.controller.implement.common.utils.SiteTypeUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ByteDanceSpec {
    private ChangedObject changedObject;
    private RouteInfo rInfo;
    private Link siteLink;


    public ByteDanceSpec(ChangedObject changedObject, RouteInfo rInfo, Link siteLink) {
        this.changedObject = changedObject;
        this.rInfo = rInfo;
        this.siteLink = siteLink;
    }

    public RouteInfo start() {
        //通告添加a/z External的连接到rInfo.physicalLinkList(), 后续相关的板卡，tp的就可以处理
        insertEquipmentForOTM();

        insertPortForIRA(rInfo);

        defaultParam();  //these value has been moved in ASE inject
        return rInfo;
    }

    private void defaultParam() {
        rInfo.getNodeIdList().forEach(nodeId -> {
            Node node = changedObject.getChangedPhyNode(nodeId);

            String srcNode = PhysicalTpIdNamingRule.getSiteId(siteLink.getSource().getSourceTp().getValue());
            String dstNode = PhysicalTpIdNamingRule.getSiteId(siteLink.getDestination().getDestTp().getValue());
            List<String> linkAZNodes = new ArrayList<>();
            linkAZNodes.add(srcNode);
            linkAZNodes.add(dstNode);
                SiteType type = SiteTypeUtils.getSiteType(node, rInfo, linkAZNodes); //base OA card type find out OTM/DGE/ILA/ROADM
                switch (type) {
//                    case OTM:
//                        new OdOtmSpecfic(changedObject, node, rInfo).set();
//                        break;
//                    case DGE:
//                        new OdDgeSpecfic(changedObject, node, rInfo).set();
//                        break;
//                    case ILA:
//                        new OdIlaSpecfic(changedObject, node, rInfo).set();
//                        break;
                    case ROADM:
                        new OdRoadmSpecfic(changedObject, node, rInfo).set();
                        break;
                }
        });
    }

    /**
     * 因为复用段的起点，时IRA口，如果时OTM站点，对应的Mux 板卡的连接关系放在a/z External里面，
     * 这个板卡也要提前enable, 涉及的线也是提前处理
     */
    private void insertEquipmentForOTM() {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        siteLinkExternalChecking(siteLinkAttr.getAExternal().getAddDropLink());
        siteLinkExternalChecking(siteLinkAttr.getZExternal().getAddDropLink());
    }


    private void insertPortForIRA(RouteInfo rInfo) {
        ChangedObject changedObject = new ChangedObject();
        rInfo.getEqIdList().forEach(eqId->{
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            List<Equipments> eqList = node.getAugmentation(Node1.class).getPhysical().getEquipments();
            Optional<Equipments> eqOp = eqList.stream().filter(eq -> eq.getEquipmentId().equals(eqId)).findAny();
            if (!eqOp.isPresent()) {
                log.error("This is impossilbe, hasn't related eq in node {}", eqId);
                return;
            }
            if (eqOp.get().getEquipType().equals(EquipType.IRA)) {
                //need append tp in tpList of rInfo
                Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains(eqId) && tp.getTpId().getValue().contains("SIG")).findAny();
                if (!tpOp.isPresent()) {
                    log.error("This is impossilbe, IRA EQ must has SIG port {}", eqId);
                    return;
                }
                rInfo.getTpIdList().add(tpOp.get().getTpId().getValue());

                tpOp = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains(eqId) && tp.getTpId().getValue().contains("EXP33")).findAny();
                if (!tpOp.isPresent()) {
                    log.error("This is impossilbe, IRA EQ must has EXP33 port {}", eqId);
                    return;
                }
                rInfo.getTpIdList().add(tpOp.get().getTpId().getValue());
            }
        });
    }


    private void siteLinkExternalChecking(List<AddDropLink> addDropLink) {
        if (addDropLink == null || addDropLink.isEmpty()) {
            return;
        }

        if (addDropLink.get(0).getConnnectorType().name().contains(EquipType.MUX.name())) {
            String aTpId = PhysicalLinkIdNamingRule.getTpAId(addDropLink.get(0).getLinkRef());
            String zTpId = PhysicalLinkIdNamingRule.getTpZId(addDropLink.get(0).getLinkRef());

            String muxTpId;
            if (aTpId.contains(EquipType.MUX.name())) {
                muxTpId = aTpId;
            } else {
                muxTpId = zTpId;
            }
            String eqId = PhysicalTpIdNamingRule.getEquipId(muxTpId);;
            rInfo.getEqIdList().add(eqId);

            //只有对端是MUX的情况下，才把对端的MUX 板卡，和相关的物理连接放到路由中
            addDropLink.forEach(link->{
                rInfo.getPhyLinkIdList().add(link.getLinkRef());
                rInfo.getTpIdList().remove(PhysicalLinkIdNamingRule.getTpAId(link.getLinkRef()));
                rInfo.getTpIdList().remove(PhysicalLinkIdNamingRule.getTpZId(link.getLinkRef()));
                rInfo.getTpIdList().add(PhysicalLinkIdNamingRule.getTpAId(link.getLinkRef()));
                rInfo.getTpIdList().add(PhysicalLinkIdNamingRule.getTpZId(link.getLinkRef()));

                rInfo.getNodeIdList().remove(PhysicalLinkIdNamingRule.getNodeAId(link.getLinkRef()));
                rInfo.getNodeIdList().remove(PhysicalLinkIdNamingRule.getNodeZId(link.getLinkRef()));
                rInfo.getNodeIdList().add(PhysicalLinkIdNamingRule.getNodeAId(link.getLinkRef()));
                rInfo.getNodeIdList().add(PhysicalLinkIdNamingRule.getNodeZId(link.getLinkRef()));
            });
        }
    }
}
