package net.flex.dci.otn.controller.implement.common.ase.ne.bytedance;

import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificParamNode;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificalParam;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class OdOtmSpecfic extends SpecificParamNode implements SpecificalParam {
    public OdOtmSpecfic(ChangedObject changedObject, Node node, RouteInfo rInfo) {
        super(changedObject, node, rInfo);
    }

    //change default value and stored in changedObject
    public void set() {
        //配置CHASSIS-1-1的子框类型为BONE_OPC
//        changeChassisType();
//        changeTpDefaultValue();
        changeWssDefaultValue();

        done();
    }

    private void changeTpDefaultValue() {
        List<String> tpIds = rInfo.getTpIdList().stream().filter(x->x.contains(node.getNodeId().getValue())).collect(Collectors.toList());

        TerminationPoint oaLine = null;
        for (String tpId : tpIds) {
            TerminationPoint tp = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(tpId)).findAny().orElse(null);
            Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
            if (tpAttr.getPortType().equals(PortType.OALine)) {
                oaLine = tp;
                break;
            }
        }
        if (oaLine == null) {
            return;
        }
        TerminationPoint finalOaLine = oaLine;
        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp-> {
                if (tp.getTpId().getValue().equals(finalOaLine.getTpId().getValue())) {
                    Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();

                    TerminationPoint newTp = new TerminationPointBuilder(finalOaLine)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(new PhysicalBuilder(tpAttr)
                                            .setProperties(PropertyTool.addProperty(tpAttr.getProperties(),
                                                    "channel-optical-power-adjustment.control-mode", "APC"))
                                            .build())
                                    .build())
                            .build();

                    return newTp;
                } else {
                    return tp;
                }
            }).collect(Collectors.toList());

        node = new NodeBuilder(node).setTerminationPoint(newTpList).build();
    }

    private void changeWssDefaultValue() {
        List<String> xcIds = rInfo.getXcIdList().stream().filter(x->x.contains(node.getNodeId().getValue())).collect(Collectors.toList());
        nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream().map(x->{
            if (x.getWssChannel() != null && xcIds.contains(x.getCrossConnectionId().getValue())) {
                Properties newProp = x.getWssChannel().getProperties();

                newProp = PropertyTool.addProperty(newProp, "ase-control-mode", "ASE_ENABLED");
                newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode", "MANUAL");
                newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode", "APC");
                newProp = PropertyTool.addProperty(newProp, "auto-control-range", "6");
                newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-dest", "0.5");
                newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-source", "0.5");

                CrossConnections newXc = new CrossConnectionsBuilder(x)
                        .setWssChannel(new WssChannelBuilder(x.getWssChannel())
                                .setSourceToDestVoa(new BigDecimal(3))
                                .setDestToSourceVoa(new BigDecimal(0))
                                .setProperties(newProp)
                                .build())
                        .build();
                return newXc;
            } else {
                return x;
            }
        }).collect(Collectors.toList());

        node = new NodeBuilder(node).addAugmentation(Node1.class,
                new Node1Builder().setPhysical(
                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(nodeAttr)
                        .setCrossConnections(newXcList)
                        .build())
                    .build())
                 .build();
    }
}
