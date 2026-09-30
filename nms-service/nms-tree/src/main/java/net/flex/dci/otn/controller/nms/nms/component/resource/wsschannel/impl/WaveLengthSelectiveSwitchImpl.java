package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.mongo.dto.OchLinkBriefInfo;
import net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.WaveLengthSelectiveSwitch;
import net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.WaveLengthSelectiveSwitchCard;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/27 11:05
 */
@Component
@Slf4j
public class WaveLengthSelectiveSwitchImpl implements WaveLengthSelectiveSwitch {

    private final Map<EquipType, WaveLengthSelectiveSwitchCard> waveLengthSelectiveSwitchCardMap = new HashMap<>();

    private final NetconfTopology netconfTopology;

    public WaveLengthSelectiveSwitchImpl(
            List<WaveLengthSelectiveSwitchCard> waveLengthSelectiveSwitchCards,
            NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
        for (WaveLengthSelectiveSwitchCard waveLengthSelectiveSwitchCard : waveLengthSelectiveSwitchCards) {
            EquipType[] supportCardTypes = waveLengthSelectiveSwitchCard.getCardTypes();
            Arrays.stream(supportCardTypes).forEach(supportCardType -> {
                waveLengthSelectiveSwitchCardMap.put(supportCardType,
                        waveLengthSelectiveSwitchCard);
            });
        }
    }

    @Override
    public List<Channel> getWssChannel(NeYangModel model, Equipments equipment,
            List<CrossConnections> crossConnections, List<String> phyLinkIds) {
        EquipType equipType = equipment.getEquipType();
        log.debug("get wss channel for the wss card:{},the card type is :{}",
                equipment.getEquipmentId(), equipType);
        //not support wss channel card like mux
        if (equipType.equals(EquipType.MUX)) {
            return new ArrayList<>();
        }
        if (!waveLengthSelectiveSwitchCardMap.containsKey(equipType)) {
            log.error("the have no handler for the card type:{} wss channel ", equipType);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get the wss channel");

        }

//        if (crossConnections == null || crossConnections.isEmpty()) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "invalid wss data");
//        }
        List<OchLinkBriefInfo> ochLinks = getWssRefOchLinks(phyLinkIds);
        WaveLengthSelectiveSwitchCard waveLengthSelectiveSwitchCard = waveLengthSelectiveSwitchCardMap.get(
                equipType);

        return waveLengthSelectiveSwitchCard.getCardWssChannel(model, crossConnections, ochLinks);
    }

    private List<OchLinkBriefInfo> getWssRefOchLinks(List<String> refPhyLinkIds) {
        log.debug("get phyLink:{} ref och links", refPhyLinkIds.size());
//        List<Link> phyLinks = netconfTopology.getEquipmentRefPhyLinks(equipmentId);
//        List<String> phyLinkIds = phyLinks.stream()
//                .map(phyLink -> phyLink.getLinkId().getValue())
//                .collect(
//                        Collectors.toList());
        List<OchLinkBriefInfo> ochLinks = netconfTopology.getBriefOchLinksBasedOnPhyLinks(
                refPhyLinkIds);
        return ochLinks;
    }
}
