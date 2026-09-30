package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.VoaUpdateModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DgeOchXcToCreate {
    private static final String CHANNEL_TARGET_DEST_PORT_OPTICAL_POWER = "target-dest-port-output-optical-power";
    private static final String CHANNEL_TARGET_SOURCE_PORT_OPTICAL_POWER = "target-source-port-output-optical-power";

    final String ochLinkId;
    final FrequencyType lowerFrequency;
    final FrequencyType upperFrequency;
    final FrequencyType centreFrequency;
    final boolean cBand;
    private BigDecimal targetPower;

    DgeOchXcToCreate(String ochLinkId, FrequencyType lowerFrequency, FrequencyType upperFrequency, boolean cBand) {
        this.ochLinkId = ochLinkId;
        this.lowerFrequency = lowerFrequency;
        this.upperFrequency = upperFrequency;
        this.centreFrequency = calculateCentreFrequency(lowerFrequency, upperFrequency);
        this.cBand = cBand;
    }

    void setTargetPower(double targetPower) {
        this.targetPower = new BigDecimal(targetPower).setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    CrossConnections buildCrossConnection(TerminationPoint sourceTp, TerminationPoint destinationTp,
            Equipments equipment) {
        SourceTp xcSourceTp = new SourceTpBuilder()
                .setTpRef(sourceTp.getTpId())
                .setKey(new SourceTpKey(sourceTp.getTpId()))
                .setSlot(frequencySlot())
                .build();
        List<SourceTp> sourceTps = new ArrayList<>();
        sourceTps.add(xcSourceTp);

        DestinationTp xcDestinationTp = new DestinationTpBuilder()
                .setTpRef(destinationTp.getTpId())
                .setKey(new DestinationTpKey(destinationTp.getTpId()))
                .setSlot(frequencySlot())
                .build();
        List<DestinationTp> destinationTps = new ArrayList<>();
        destinationTps.add(xcDestinationTp);

        Uri xcId = createXcId(sourceTp.getTpId().getValue(), destinationTp.getTpId().getValue());
        return new CrossConnectionsBuilder()
                .setCrossConnectionId(xcId)
                .setKey(new CrossConnectionsKey(xcId))
                .setDescription(createDescription(sourceTp.getTpId().getValue()))
                .setNodeRef(new NodeId(PhysicalEqpIdNamingRule.getNodeId(equipment.getEquipmentId())))
                .setAdminState(AdminStatus.Unknown)
                .setOperationalState(OperStatus.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setFixed(false)
                .setDirection(LinkDirection.Bidirection)
                .setSourceTp(sourceTps)
                .setDestinationTp(destinationTps)
                .setWssChannel(createWssChannel())
                .build();
    }

    private String frequencySlot() {
        return String.format("/frequency=%d,%d", lowerFrequency.getValue().longValue(),
                upperFrequency.getValue().longValue());
    }

    private WssChannel createWssChannel() {
        return new WssChannelBuilder()
                .setLowerFrequency(lowerFrequency)
                .setUpperFrequency(upperFrequency)
                .setCentreFrequency(centreFrequency)
                .setSourceToDestVoa(new BigDecimal(3))
                .setDestToSourceVoa(new BigDecimal(3))
                .setVoaUpdateModel(VoaUpdateModel.Manually)
                .setProperties(createWssChannelProperties())
                .build();
    }

    private Properties createWssChannelProperties() {
        // 插入 DGE 时新建的 WSS channel XC 需要直接带上自动功率控制默认值，避免后续 implement 阶段再补齐。
        Properties properties = PropertyTool.addProperty(null, CHANNEL_TARGET_DEST_PORT_OPTICAL_POWER,
                targetPower.toString());
        properties = PropertyTool.addProperty(properties, CHANNEL_TARGET_SOURCE_PORT_OPTICAL_POWER,
                targetPower.toString());
        properties = PropertyTool.addProperty(properties, "dest-to-source-power-control-mode", "APC");
        properties = PropertyTool.addProperty(properties, "auto-control-active-threshold-source", "0.5");
        properties = PropertyTool.addProperty(properties, "source-to-dest-voa", "3.00");
        properties = PropertyTool.addProperty(properties, "auto-control-active-threshold-dest", "0.5");
        properties = PropertyTool.addProperty(properties, "dest-to-source-voa", "3.00");
        properties = PropertyTool.addProperty(properties, "auto-control-range", "6");
        properties = PropertyTool.addProperty(properties, "source-to-dest-power-control-mode", "APC");
        properties = PropertyTool.addProperty(properties, "ase-control-mode", "ASE_DISABLED");
        return properties;
    }

    private Uri createXcId(String sourceTpId, String destinationTpId) {
        List<String> tpIds = new ArrayList<>();
        tpIds.add(String.format("%s/%d", sourceTpId, centreFrequency.getValue().longValue()));
        tpIds.add(String.format("%s/%d", destinationTpId, centreFrequency.getValue().longValue()));

        // 与现有 DummyOchLinkConstructor 的 WSS XC 命名一致：双向 XC 按 TP/中心频率排序后拼接。
        return new Uri(tpIds.stream().sorted().collect(Collectors.joining("-", "XC-", "")));
    }

    private String createDescription(String sourceTpId) {
        String[] segments = sourceTpId.split("#");
        String portName = segments[segments.length - 1];
        return String.format("%s/%d", portName.replace("PORT", "WSS"), centreFrequency.getValue().longValue());
    }

    private FrequencyType calculateCentreFrequency(FrequencyType lowerFrequency, FrequencyType upperFrequency) {
        BigInteger lower = lowerFrequency.getValue();
        BigInteger upper = upperFrequency.getValue();
        return new FrequencyType(lower.add(upper.subtract(lower).divide(BigInteger.valueOf(2))));
    }
}
