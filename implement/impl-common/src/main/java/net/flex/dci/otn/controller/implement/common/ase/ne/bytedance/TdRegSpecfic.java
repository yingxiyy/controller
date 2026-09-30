package net.flex.dci.otn.controller.implement.common.ase.ne.bytedance;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificParamNode;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificalParam;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.Client;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.ClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TdRegSpecfic extends SpecificParamNode implements SpecificalParam {
    private static final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final boolean bindingThirdLeg;

    public TdRegSpecfic(ChangedObject changedObject, Node node, RouteInfo rInfo) {
        this(changedObject, node, rInfo, false);
    }

    public TdRegSpecfic(ChangedObject changedObject, Node node, RouteInfo rInfo,
            boolean bindingThirdLeg) {
        super(changedObject, node, rInfo);
        this.bindingThirdLeg = bindingThirdLeg;
    }

    //change default value and stored in changedObject
    public void set() {
        log.debug("start to set TdRegSpecfic for node:{}", node.getNodeId().getValue());
        //配置CHASSIS-1-1的子框类型为BONE_EPC
        changeChassisType();
        changeLinePortLos();

        done();
    }

    private void changeLinePortLos() {
        String nodeId = node.getNodeId().getValue();
        List<String> tpIdList = rInfo.getTpIdList().stream().filter(tpId-> tpId.contains(nodeId)).collect(Collectors.toList());


        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp->{
            Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();

            if (tpAttr.getOtuLine() != null) {
                log.debug("set reg param of line port los param on tp {}", tp.getTpId().getValue());

                Properties newProps = tpAttr.getProperties();
                newProps = PropertyTool.addProperty(newProps, "regen-failure-propagated", "GEN_AIS");
                newProps = PropertyTool.addProperty(newProps, "ber-failure-threshold", "0.021");
                newProps = PropertyTool.delProperty(newProps, "tx_laser");
                newProps = PropertyTool.delProperty(newProps, "tx_laser_0");
                newProps = PropertyTool.delProperty(newProps, "tx_laser_1");
                newProps = PropertyTool.delProperty(newProps, "tx_laser_2");
                newProps = PropertyTool.delProperty(newProps, "tx_laser_3");
                if (tpIdList.contains(tp.getTpId().getValue())) {
                    PhysicalBuilder physicalBuilder = new PhysicalBuilder(tpAttr)
                            .setProperties(newProps);
                    // Third-leg traffic is not adjusted later, so its REG line power must be
                    // included in the normal implement payload.
                    if (bindingThirdLeg) {
                        physicalBuilder.setOtuLine(new OtuLineBuilder(tpAttr.getOtuLine())
                                .setTargetOutputPower(getMaxOutputPower(tp.getTpId().getValue()))
                                .build());
                    }
                    return new TerminationPointBuilder(tp)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(physicalBuilder.build())
                                    .build())
                            .build();
                } else {
                    return tp;
                }
            } else {
                return tp;
            }
        }).collect(Collectors.toList());

        node = new NodeBuilder(node).setTerminationPoint(newTpList).build();
    }

    private BigDecimal getMaxOutputPower(String tpId) {
        String nodeId = node.getNodeId().getValue();
        Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
        if (opNode == null || nodeAttr.getIp() == null) {
            log.error("The node hasn't been managed by Adapter or hasn't IP yet {}",
                    nodeAttr.getFriendlyName());
            return BigDecimal.valueOf(-1);
        }

        TerminationPoint opTp = opNode.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().equals(tpId))
                .findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find out tpId " + tpId));
        Physical opTpAttr = opTp.getAugmentation(TerminationPoint1.class).getPhysical();
        String value = PropertyTool.getValue(opTpAttr.getProperties(), "max-output-power");
        log.debug("find max-output-power on REG tp {} = {}", tpId, value);

        if (StringUtils.isEmpty(value)) {
            return BigDecimal.valueOf(-1);
        }

        try {
            double power = Math.min(Double.parseDouble(value), 5.0);
            if (nodeAttr.getVendorName().equalsIgnoreCase("HUAWEI")) {
                power = 1.0;
            }
            return BigDecimal.valueOf(power);
        } catch (NumberFormatException e) {
            log.error("invalid max-output-power on REG tp {}, return default value -1", tpId, e);
            return BigDecimal.valueOf(-1);
        }
    }

}
