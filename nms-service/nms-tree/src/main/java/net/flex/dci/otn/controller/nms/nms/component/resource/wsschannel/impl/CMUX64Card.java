package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.mongo.dto.OchLinkBriefInfo;
import net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.AbstractWaveLengthSelectiveSwitchCard;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.ChannelBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.OchLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.springframework.stereotype.Component;

/**
 * 2025/12/6
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class CMUX64Card extends AbstractWaveLengthSelectiveSwitchCard {

    @Override
    public List<Channel> getCardWssChannel(NeYangModel model,
            List<CrossConnections> crossConnections, List<OchLinkBriefInfo> ochLinks) {
        log.debug("get CMUX64  card wss channel ");
        List<CrossConnections> wssXcs = getWssConnections(crossConnections);
        List<WeightWssChannel> weightWssChannels = new LinkedList<>();
        Map<BigInteger, List<OchLinkBriefInfo>> centreFrequencyLinkMap = getOchLinkCrentreFqMap(
                ochLinks);
        wssXcs.stream().filter(Objects::nonNull).forEach(crossConnection -> {
            log.debug("get wss channel from cross connection:{}",
                    crossConnection.getCrossConnectionId().getValue());

            WssChannel wssChannel = crossConnection.getWssChannel();
            Available availableFrequency = CrossConnectionSlotNamingRule.getFrequencyScope(
                    crossConnection);
            String description = crossConnection.getDescription();
            BigInteger upperFrequency = availableFrequency.getUpperFrequency().getValue();
            BigInteger lowerFrequency = availableFrequency.getLowerFrequency().getValue();
            BigInteger centreFrequency = upperFrequency.add(lowerFrequency).divide(
                    BigInteger.valueOf(2));
            List<OchLinkBriefInfo> refOchLinks = centreFrequencyLinkMap.get(centreFrequency);
            OchLinkBriefInfo carriedOchLink = getCarriedOchLink(refOchLinks, crossConnection);
            ImplementState implementState = carriedOchLink == null ? null
                    : carriedOchLink.getImplementState();
            String channelName = generateWssChannelName(crossConnection, centreFrequency);
            TerminationPointInfo srcTp = getCrossConnectionTpId(
                    crossConnection.getSourceTp().get(0).getTpRef());
            TerminationPointInfo destTp = getCrossConnectionTpId(
                    crossConnection.getDestinationTp().get(0).getTpRef());
            BigDecimal sourceToDestVoa = wssChannel.getSourceToDestVoa() == null ? new BigDecimal(0)
                    : wssChannel.getSourceToDestVoa();
            BigDecimal destToSourceVoa = wssChannel.getDestToSourceVoa() == null ? new BigDecimal(0)
                    : wssChannel.getDestToSourceVoa();
            Channel channel = new ChannelBuilder()
                    .setName(channelName)
                    .setCrossConnectionId(crossConnection.getCrossConnectionId())
                    .setSourcePort(srcTp.getTpName())
                    .setSourcePortId(Uri.getDefaultInstance(srcTp.getTpId()))
                    .setDestPort(destTp.getTpName())
                    .setDestPortId(Uri.getDefaultInstance(destTp.getTpId()))
                    .setDescription(description)
                    .setWssChannel(new WssChannelBuilder()
                            .setVoaUpdateModel(wssChannel.getVoaUpdateModel())
                            .setDestToSourceVoa(destToSourceVoa)
                            .setSourceToDestVoa(sourceToDestVoa)
                            .setUpperFrequency(new FrequencyType(upperFrequency))
                            .setLowerFrequency(new FrequencyType(lowerFrequency))
                            .setCentreFrequency(new FrequencyType(centreFrequency))
                            .setProperties(wssChannel.getProperties())
                            .build())
                    .setOchLink(carriedOchLink == null ? null
                            : new OchLinkBuilder().setLinkId(
                                            LinkId.getDefaultInstance(carriedOchLink.getOchLinkId()))
                                    .setTopologyId(
                                            TopologyId.getDefaultInstance(Constants.OCH_TOPO_KEY))
                                    .setImplementState(implementState).build())
                    .setSpectrum(
                            getSpectrum(centreFrequency.longValue(), lowerFrequency.longValue(),
                                    upperFrequency.longValue()))
                    .build();
            WeightWssChannel weightWssChannel = WeightWssChannel.builder().weight(
                            BigInteger.valueOf(0))
                    .channel(channel).build();
            weightWssChannels.add(weightWssChannel);
        });
        if (weightWssChannels.isEmpty()) {
            return new ArrayList<>();
        }
        //sort with the weight
        weightWssChannels.sort(new WeightWssChannelComparator());
        return weightWssChannels.stream().map(WeightWssChannel::getChannel)
                .collect(Collectors.toList());
    }

    @Override
    public EquipType[] getCardTypes() {
        return new EquipType[]{EquipType.CMUX64};
    }
}
