package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.impl;

import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_TYPE_PREFIX;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/27 10:47
 */
@Component
@Slf4j
public class WssCard extends AbstractWaveLengthSelectiveSwitchCard {


    @Override
    public List<Channel> getCardWssChannel(NeYangModel model,
            List<CrossConnections> crossConnections, List<OchLinkBriefInfo> ochLinks) {
        log.debug("start to get wss card wss channel");
        List<CrossConnections> wssXcs = getWssConnections(crossConnections);
        Map<BigInteger, List<OchLinkBriefInfo>> centreFrequencyLinkMap = getOchLinkCrentreFqMap(
                ochLinks);
        List<WeightWssChannel> weightWssChannels = new LinkedList<>();
        wssXcs.stream().filter(Objects::nonNull).forEach(xc -> {
            log.debug("get wss xc from the xc id:{}", xc.getCrossConnectionId().getValue());
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel wssChannel = xc.getWssChannel();
            Available availiableFrequence = CrossConnectionSlotNamingRule.getFrequencyScope(
                    xc);
            String description = xc.getDescription();
            BigInteger upper = availiableFrequence.getUpperFrequency().getValue();
            BigInteger lower = availiableFrequence.getLowerFrequency().getValue();
            BigInteger centre = upper.add(lower).divide(new BigInteger("2"));
            String channelName = generateWssChannelName(xc, centre);
            TerminationPointInfo srcTp = getCrossConnectionTpId(xc.getSourceTp().get(0).getTpRef());
            List<OchLinkBriefInfo> refOchLinks = centreFrequencyLinkMap.get(centre);
            OchLinkBriefInfo carriedOchLink = getCarriedOchLink(refOchLinks, xc);
            ImplementState implementState = carriedOchLink.getImplementState();
            TerminationPointInfo destTp = getCrossConnectionTpId(
                    xc.getDestinationTp().get(0).getTpRef());
            BigDecimal sourceToDestVoa = wssChannel.getSourceToDestVoa() == null ? new BigDecimal(0)
                    : wssChannel.getSourceToDestVoa();
            BigDecimal destToSourceVoa = wssChannel.getDestToSourceVoa() == null ? new BigDecimal(0)
                    : wssChannel.getDestToSourceVoa();
            Channel channel = new ChannelBuilder()
                    .setName(channelName)
                    .setCrossConnectionId(xc.getCrossConnectionId())
                    .setSourcePort(srcTp.getTpName())
                    .setSourcePortId(Uri.getDefaultInstance(srcTp.getTpId()))
                    .setDestPort(destTp.getTpName())
                    .setDestPortId(Uri.getDefaultInstance(destTp.getTpId()))
                    .setDescription(description)
                    .setWssChannel(new WssChannelBuilder()
                            .setVoaUpdateModel(wssChannel.getVoaUpdateModel())
                            .setDestToSourceVoa(destToSourceVoa)
                            .setSourceToDestVoa(sourceToDestVoa)
                            .setUpperFrequency(new FrequencyType(upper))
                            .setLowerFrequency(new FrequencyType(lower))
                            .setCentreFrequency(new FrequencyType(centre))
                            .setProperties(wssChannel.getProperties())
                            .build())
                    .setOchLink(carriedOchLink == null ? null
                            : new OchLinkBuilder().setLinkId(
                                            LinkId.getDefaultInstance(carriedOchLink.getOchLinkId()))
                                    .setTopologyId(
                                            TopologyId.getDefaultInstance(Constants.OCH_TOPO_KEY))
                                    .setImplementState(implementState).build())
                    .setSpectrum(
                            getSpectrum(centre.longValue(), lower.longValue(),
                                    upper.longValue()))
                    .build();
            WeightWssChannel weightWssChannel = WeightWssChannel.builder().weight(
                            BigInteger.valueOf(0))
                    .channel(channel).build();
            weightWssChannels.add(weightWssChannel);
        });
        //sort the wss Channels with lower
        if (weightWssChannels.isEmpty()) {
            return new LinkedList<>();
        }
        //sort with the weight
        weightWssChannels.sort(new WeightWssChannelComparator());
        return weightWssChannels.stream().map(WeightWssChannel::getChannel)
                .collect(Collectors.toList());
    }


    @Override
    public EquipType[] getCardTypes() {
        return new EquipType[]{EquipType.WSS};
    }

    @Override
    public String generateWssChannelName(CrossConnections xc, BigInteger center) {
        String xcId = xc.getCrossConnectionId().getValue();
        log.debug("generate wss channel name for the cross connection id is:{}", xcId);
        //assume src and dest tp is only one tp
        return WSS_TYPE_PREFIX + CrossConnectionUtils.generateWssCrossName(xc, center.longValue());
    }


}
