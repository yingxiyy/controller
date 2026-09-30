package net.flex.dci.otn.controller.allocate.link.phy;

import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType.CMUX64;
import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType.FMUX32;
import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType.WSS;

public class AddDropLinkConstructor {

    public List<AddDropLink> getLinkBetweenMuxPanelCmux(Node node, List<Link> links) {
        List<Equipments> cmuxList = node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(
                equipment -> equipment.getEquipType().equals(CMUX64)
                        || equipment.getEquipType().equals(FMUX32)).collect(Collectors.toList());
        if (cmuxList == null || cmuxList.isEmpty()) {
            return new ArrayList<>();
        }

        List<AddDropLink> rst = new ArrayList<>();
        for (Equipments cmux : cmuxList) {
            List<Link> cmuxLinks = links.stream().filter(
                    link -> link.getLinkId().getValue().contains(cmux.getEquipmentId())).collect(Collectors.toList());
            for (Link link : cmuxLinks) {
                //这里需去掉 CMUX64-A/B 口相关的link
                if (isCMUXABTP(node, link)) {
                    continue;
                }
                EquipType peerType = getPeerType(node, cmux.getEquipmentId(), link);
                if (cmux.getEquipType().equals(FMUX32) && !EquipType.MUXPANEL.equals(peerType)) {
                    continue;
                }
                AddDropLink addDroplink = new AddDropLinkBuilder().setLinkRef(link.getLinkId().getValue())
                        .setKey(new AddDropLinkKey(link.getLinkId().getValue()))
                        .setConnnectorType(peerType)
                        .build();
                rst.add(addDroplink);
            }
        }
        return rst;
    }

    /**
     * 复用段得起止点是WSS/IRA 的Line 口
     *
     * @param node
     * @param links
     * @return
     */
    public List<AddDropLink> getLink2WSS(Node node, List<Link> links) {
        List<Equipments> wssCardList = node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(
                equipment -> equipment.getEquipType().equals(WSS)|| equipment.getEquipType().equals(EquipType.IRA)).collect(Collectors.toList());
        if (wssCardList == null || wssCardList.isEmpty()) {
            return new ArrayList<>();
        }

        List<AddDropLink> rst = new ArrayList<>();
        List<Link> wssLinks = new ArrayList<>();
        for (Equipments wssCard : wssCardList) {
            wssLinks.addAll(links.stream().filter(
                    link -> link.getLinkId().getValue().contains(wssCard.getEquipmentId())).collect(Collectors.toList()));


            for (Link link : wssLinks) {
                //这里需去掉 WSS-Sig 口相关的link
                if (isWssOutputTP(node, link)) {
                    continue;
                }
                EquipType peerType = getPeerType(node, wssCard.getEquipmentId(), link);
                AddDropLink addDroplink = new AddDropLinkBuilder().setLinkRef(link.getLinkId().getValue())
                        .setKey(new AddDropLinkKey(link.getLinkId().getValue()))
                        .setConnnectorType(peerType)
                        .build();
                rst.add(addDroplink);
            }
        }
        return rst;
    }

    //找到wssCard 的peer
    private EquipType getPeerType(Node node, String wssCardId, Link link) {
        String checkingSrcTp = link.getSource().getSourceTp().getValue();
        if (checkingSrcTp.contains(wssCardId)) {
            checkingSrcTp = link.getDestination().getDestTp().getValue();
        }
        String finalCheckingSrcTp = checkingSrcTp;
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(finalCheckingSrcTp)).findFirst();
        if (tpOp.isPresent()) {
            TerminationPoint tp = tpOp.get();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
            if (!tpAttr.getPortType().equals(PortType.WSSSig)) {
                String equipId = PhysicalTpIdNamingRule.getEquipId(tp.getTpId().getValue());
                Optional<Equipments> eqOp = node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(x -> x.getEquipmentId().equals(equipId)).findFirst();
                if (eqOp.isPresent()) {
                    return eqOp.get().getEquipType();
                }
            }
        }
        return null;
    }

    //the link is the wss related TP now.
    private boolean isWssOutputTP(Node node, Link link) {
        final String srcTp = link.getSource().getSourceTp().getValue();
        final String dstTp = link.getDestination().getDestTp().getValue();
        if (isWssOutputTP(node, srcTp)) {
            return true;
        } else if (isWssOutputTP(node, dstTp)) {
            return true;
        }
        return false;
    }

    private boolean isWssOutputTP(Node node, String checkingTp) {
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(checkingTp)).findFirst();
        if (tpOp.isPresent()) {
            TerminationPoint tp = tpOp.get();
            if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OALine))
                return true;
        }
        return false;
    }


    //the link is the wss related TP now.
    private boolean isCMUXABTP(Node node, Link link) {
        final String srcTp = link.getSource().getSourceTp().getValue();
        final String dstTp = link.getDestination().getDestTp().getValue();
        if (isCMUXABTP(node, srcTp)) {
            return true;
        } else if (isCMUXABTP(node, dstTp)) {
            return true;
        }
        return false;
    }

    private boolean isCMUXABTP(Node node, String checkingTp) {
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(checkingTp)).findFirst();
        if (tpOp.isPresent()) {
            TerminationPoint tp = tpOp.get();
            if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPA)
                    || tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPB))
                return true;
        }
        return false;
    }

}
