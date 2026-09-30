package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.WaveLengthSelectiveSwitch;
import net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.WssChannel;
import net.flex.dci.otn.controller.nms.nms.dto.VoaThresholdDto;
import net.flex.dci.otn.controller.nms.nms.dto.WssChannelDto;
import net.flex.dci.otn.controller.nms.properties.equip.EquipmentTypeConfiguration;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.VoaConstants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.ChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.KvDefine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/7/5 17:14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WssChannelImpl implements WssChannel {

    private final NetconfTopology netconfTopology;

    private final WaveLengthSelectiveSwitch waveLengthSelectiveSwitch;

    private final EquipmentTypeConfiguration equipmentTypeConfiguration;

//    private NeYangModel model;

    @Override
    public WssChannelDto retrieveRefEquipAllChannels(String nodeId) {
        log.info("start to get the node :{} wss channel", nodeId);
        log.debug("start to get the node :{} wss channel", nodeId);
        Node ne = netconfTopology.getNeNode(nodeId);
        if (ne == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "required ne is not found");
        }
        Node1 node1 = ne.getAugmentation(Node1.class);
        if (node1 == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node data structure is invalid");
        }
        Physical nodePhysical = node1.getPhysical();
        if (nodePhysical == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node data structure is invalid");
        }
        List<Equipments> equipments = nodePhysical.getEquipments();

        if (null == equipments) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node data structure is invalid");
        }

        VoaThresholdDto voaThresholdDto = getVoaThreshold(nodePhysical);
        List<Channel> wssChannels = getWssChannels(nodePhysical);

        return WssChannelDto.builder().voaThresholdDto(voaThresholdDto).wssChannels(wssChannels)
                .build();
    }

    /**
     * retrieve all the equipment wss channel
     *
     * @param nodeId
     * @param equipmentId
     * @return
     */
    @Override
    public WssChannelDto retrieveRefEquipAllChannels(String nodeId, String equipmentId) {
        log.info("start to get the node wss channels,the nodeId is :{},equipmentId is :{}", nodeId,
                equipmentId);
        Node ne = netconfTopology.getConfigPhyNode(nodeId);
        if (ne == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "required ne is not found");
        }
        Node1 node1 = ne.getAugmentation(Node1.class);
        if (node1 == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node data structure is invalid");
        }
        Physical nodePhysical = node1.getPhysical();
        if (nodePhysical == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node data structure is invalid");
        }
        NeYangModel model = NeYangModel.getModel(nodePhysical);
        List<String> refPhyLinkIds = getEquipmentRefPhyLinkIds(nodeId, equipmentId);
        List<Equipments> equipments = nodePhysical.getEquipments();
        Equipments refEquipment = getRefWSSOrMuxEquipment(equipments, equipmentId);
        List<Channel> wssChannels = getRefWssChannel(nodePhysical.getCrossConnections(),
                refEquipment, model, refPhyLinkIds);
        VoaThresholdDto voaThresholdDto = getVoaThreshold(nodePhysical);
        return WssChannelDto.builder().voaThresholdDto(voaThresholdDto).wssChannels(wssChannels)
                .build();
    }

    private List<String> getEquipmentRefPhyLinkIds(String nodeId, String equipmentId) {
        log.debug("get equipment ref phyLink the nodeId:{} and equipmentId:{}", nodeId,
                equipmentId);
        List<String> phyLinkIds = netconfTopology.listPhyLinkIdsByNodeIds(
                Collections.singletonList(nodeId));
        log.debug("total phyLink size:{}", phyLinkIds.size());
        List<String> equipmentRefPhyLinkIds = phyLinkIds.stream()
                .filter(phyLinkId -> phyLinkId.contains(equipmentId)).collect(
                        Collectors.toList());
        log.debug("equipment ref phy link Ids:{}", equipmentRefPhyLinkIds.size());
        return equipmentRefPhyLinkIds;
    }

    private List<Channel> getRefWssChannel(List<CrossConnections> crossConnections,
            Equipments equipment, NeYangModel neYangModel, List<String> refPhyLinkIds) {
        log.debug("start to find the ref wss channel for the equipments, equipmentId is:{}",
                equipment.getEquipmentId());
        //todo:to find the equipment relative cross connections
        String equipmentId = equipment.getEquipmentId();
        List<CrossConnections> refCrossConnections = crossConnections.stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(equipmentId)).collect(
                        Collectors.toList());
        List<Channel> wssChannels = waveLengthSelectiveSwitch.getWssChannel(neYangModel,
                equipment, refCrossConnections, refPhyLinkIds);
        return wssChannels;
    }

    private Equipments getRefWSSOrMuxEquipment(List<Equipments> equipments, String equipmentId) {
        log.debug("get ref wss or mux equipments :{}", equipmentId);
        if (null == equipments) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node data structure is invalid");
        }
        Map<String, Equipments> equipmentsMap = equipments.stream().collect(HashMap::new,
                (map, equipment) -> map.put(equipment.getEquipmentId(), equipment),
                HashMap::putAll);
        if (!equipmentsMap.containsKey(equipmentId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the equipment id :" + equipmentId + " is invalid ");
        }
        Equipments equipment = equipmentsMap.get(equipmentId);
        EquipType equipType = equipment.getEquipType();
        if (equipmentTypeConfiguration.isWaveDivisionMultiplexing(equipType)) {
            return equipment;
        }
//        if (equipType.equals(EquipType.CMUX64) || equipment.getEquipType().equals(EquipType.WSS)
//                || equipment.getEquipType().equals(EquipType.MUX) || equipment.getEquipType()
//                .equals(EquipType.MUXPANEL)) {
//            return equipment;
        else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the required equipment type is invalid,the equipType should be "
                            + equipmentTypeConfiguration.getWaveDivisionMultiplexing()
                            .getEquipmentTypes());
        }
    }

    /**
     * get wss channel
     *
     * @param nodePhysical
     * @return
     */
    private List<Channel> getWssChannels(Physical nodePhysical) {
        List<CrossConnections> crossConnections = nodePhysical.getCrossConnections();
        if (crossConnections == null || crossConnections.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "invalid wss data");
        }
        List<CrossConnections> wssXc = crossConnections.stream()
                .filter(xc -> xc.getWssChannel() != null).collect(
                        Collectors.toList());
        List<Channel> result = new ArrayList<>();
        NeYangModel model = NeYangModel.getModel(nodePhysical);
        wssXc.forEach(xc -> {
            String channelName = CrossConnectionUtils.getRefWssChannelName(model, xc);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel wssChannel = xc.getWssChannel();
            Channel channel = new ChannelBuilder()
                    .setName(channelName)
                    .setCrossConnectionId(xc.getCrossConnectionId())
                    .setWssChannel(new WssChannelBuilder()
                            .setVoaUpdateModel(wssChannel.getVoaUpdateModel())
                            .setDestToSourceVoa(wssChannel.getDestToSourceVoa())
                            .setSourceToDestVoa(wssChannel.getSourceToDestVoa())
                            .build())
                    .build();
            result.add(channel);
        });
        return result;
    }

    /**
     * get the ne voa threshold
     *
     * @param nodePhysical
     * @return
     */
    private VoaThresholdDto getVoaThreshold(Physical nodePhysical) {
        List<Equipments> equipments = nodePhysical.getEquipments();
        log.debug("equipments is :{}", equipments);
        //todo:find the multiplexed architecture card,card type is CMUX64 OR WSS
        List<Equipments> supportedWssEqs = equipments.stream()
                .filter(equipment -> equipment.getEquipType() != null)
                .filter(equipment -> equipmentTypeConfiguration.isWaveDivisionMultiplexing(
                        equipment.getEquipType()))
                .collect(
                        Collectors.toList());
        //assemble only have one multiplexed architecture
        if (supportedWssEqs.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the node don't have any cross connection");
        }
        Properties properties = nodePhysical.getProperties();
        if (properties == null) {
            return VoaThresholdDto.builder().build();
        }
        Map<String, String> propertyMap = properties.getProperty().stream()
                .collect(Collectors.toMap(KvDefine::getName, KvDefine::getValue));
        BigDecimal maxSourceToDestVoa =
                propertyMap.get(VoaConstants.MAX_SOURCE_TO_DEST_VOA) == null ? new BigDecimal(0)
                        : new BigDecimal(propertyMap.get(VoaConstants.MAX_SOURCE_TO_DEST_VOA));
        BigDecimal minSourceToDestVoa =
                propertyMap.get(VoaConstants.MIN_SOURCE_TO_DEST_VOA) == null ? new BigDecimal(0)
                        : new BigDecimal(propertyMap.get(VoaConstants.MIN_SOURCE_TO_DEST_VOA));
        BigDecimal maxDestToSourceVoa =
                propertyMap.get(VoaConstants.MAX_DEST_TO_SOURCE_VOA) == null ? new BigDecimal(0)
                        : new BigDecimal(propertyMap.get(VoaConstants.MAX_DEST_TO_SOURCE_VOA));
        BigDecimal minDestToSourceVoa =
                propertyMap.get(VoaConstants.MIN_DEST_TO_SOURCE_VOA) == null ? new BigDecimal(0)
                        : new BigDecimal(propertyMap.get(VoaConstants.MIN_DEST_TO_SOURCE_VOA));
        if (maxSourceToDestVoa.equals(minSourceToDestVoa)) {
            log.warn("no wss voa threshold,return do nothing");
            return null;
        }
        return VoaThresholdDto.builder().maxDestToSourceVoa(maxDestToSourceVoa)
                .minDestToSourceVoa(minDestToSourceVoa)
                .maxSourceToDestVoa(maxSourceToDestVoa).minSourceToDestVoa(minSourceToDestVoa)
                .build();
    }
}
