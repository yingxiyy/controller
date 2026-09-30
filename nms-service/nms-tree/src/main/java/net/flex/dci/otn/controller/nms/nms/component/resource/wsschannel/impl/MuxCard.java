package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.impl;

import static net.flex.dci.otn.topology.cache.utils.DciCacheConstants.BLANK;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.springframework.stereotype.Component;

/**
 * MUX PANEL  WSS CHANNEL
 *
 * @version 1.0
 * @date 2022/12/27 10:47
 */
@Component
@Slf4j
public class MuxCard extends AbstractWaveLengthSelectiveSwitchCard {

    @Override
    public List<Channel> getCardWssChannel(NeYangModel model,
            List<CrossConnections> crossConnections, List<OchLinkBriefInfo> ochLinks) {
        log.debug("start to get mux wss channel");
        if (model == NeYangModel.ByteDance) {
            return getByteDanceModelWssChannel(model, crossConnections, ochLinks);
        }
        return getDefaultModelWssChannel(model, crossConnections);
    }

    private List<Channel> getByteDanceModelWssChannel(NeYangModel model,
            List<CrossConnections> crossConnections,
            List<OchLinkBriefInfo> ochLinks) {
        log.debug("get byte dance model wss channels");
        List<CrossConnections> wssXcs = getWssConnections(crossConnections);
        List<WeightWssChannel> weightWssChannels = new LinkedList<>();
        Map<BigInteger, List<OchLinkBriefInfo>> centreFrequencyLinkMap = getOchLinkCrentreFqMap(
                ochLinks);
        wssXcs.stream().filter(Objects::nonNull).forEach(crossConnection -> {
            log.debug("get wss channel from cross connection:{}",
                    crossConnection.getCrossConnectionId().getValue());

            WssChannel wssChannel = crossConnection.getWssChannel();
            MuxWssChannel muxWssChannel = getWssChannelDetail(model, crossConnection);
            Available availableFrequency = CrossConnectionSlotNamingRule.getFrequencyScope(
                    crossConnection);
//            List<Property> properties =
//                    crossConnection.getProperties() == null ? new ArrayList<>() :
//                            crossConnection.getWssChannel().getProperties().getProperty();
//            WssChannelControl wssChannelControl = WssChannelUtils.parseWssChannelControl(
//                    properties);
//
//            PowerControl powerControl = getPowerControl(wssChannelControl);
            String description = crossConnection.getDescription();
            BigInteger upperFrequency = availableFrequency.getUpperFrequency().getValue();
            BigInteger lowerFrequency = availableFrequency.getLowerFrequency().getValue();
            BigInteger centreFrequency = upperFrequency.add(lowerFrequency).divide(
                    BigInteger.valueOf(2));
            List<OchLinkBriefInfo> refOchLinks = centreFrequencyLinkMap.get(centreFrequency);
            OchLinkBriefInfo carriedOchLink = getCarriedOchLink(refOchLinks, crossConnection);
            ImplementState implementState = carriedOchLink == null ? null
                    : carriedOchLink.getImplementState();
            String channelName = muxWssChannel.wssChannelName;
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

    private List<Channel> getDefaultModelWssChannel(NeYangModel model,
            List<CrossConnections> crossConnections) {
        List<CrossConnections> wssXc = getWssConnections(crossConnections);
        List<MuxWeightWssChannel> wssChannels = new ArrayList<>();
        wssXc.forEach(xc -> {
            MuxWssChannel muxWssChannel = getWssChannelDetail(model, xc);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel wssChannel = xc.getWssChannel();
            TerminationPointInfo srcTp = getCrossConnectionTpId(
                    xc.getSourceTp().get(0).getTpRef());
            TerminationPointInfo destTp = getCrossConnectionTpId(
                    xc.getDestinationTp().get(0).getTpRef());
            String description = xc.getDescription();
            Channel channel = new ChannelBuilder()
                    .setName(muxWssChannel.wssChannelName)
                    .setSourcePort(srcTp.getTpName())
                    .setSourcePortId(Uri.getDefaultInstance(srcTp.getTpId()))
                    .setDestPort(destTp.getTpName())
                    .setDestPortId(Uri.getDefaultInstance(destTp.getTpId()))
                    .setCrossConnectionId(xc.getCrossConnectionId())
                    .setDescription(description)
                    .setWssChannel(new WssChannelBuilder()
                            .setVoaUpdateModel(wssChannel.getVoaUpdateModel())
                            .setDestToSourceVoa(wssChannel.getDestToSourceVoa())
                            .setSourceToDestVoa(wssChannel.getSourceToDestVoa())
                            .setProperties(wssChannel.getProperties())
                            .build())
                    .build();
            wssChannels.add(
                    MuxWeightWssChannel.builder().wssChannel(channel)
                            .portNumber(muxWssChannel.portNum).build());
        });
        wssChannels.sort(new MuxWeightWssChannelComparator());
        return wssChannels.stream().map(MuxWeightWssChannel::getWssChannel)
                .collect(Collectors.toList());
    }

    private MuxWssChannel getWssChannelDetail(NeYangModel model, CrossConnections xc) {
        log.debug("start to get wss channel detail");
        //wss channel name for mux like m?d?
        String wssChannelName = generateWssChannelName(model, xc);
        Integer portNum = Integer.valueOf(wssChannelName.substring(wssChannelName.length() - 1));
        return MuxWssChannel.builder().wssChannelName(wssChannelName).portNum(portNum).build();
    }


    @Override
    public EquipType[] getCardTypes() {
        return new EquipType[]{EquipType.MUXPANEL};
    }

    @Override
    public String generateWssChannelName(NeYangModel model, CrossConnections xc) {
        String crossConnectionId = xc.getCrossConnectionId().toString();
//        MuxCardPortFormatting formatting = new MuxCardPortFormatting(model);
        log.debug("generate mux card wss channel name,crossConnections id is:{}",
                crossConnectionId);
        List<String> xcRefTpIds = CrossConnectionUtils.getCrossConnectionRefTpIds(xc);
        List<String> muxTpIds = xcRefTpIds.stream().filter(tpId -> {
            Pattern pattern = Pattern.compile(model.muxPortMatchingRegex());
            Matcher matcher = pattern.matcher(tpId);
            return matcher.find();
        }).collect(Collectors.toList());
        //assem the tp only have one
        if (!muxTpIds.isEmpty()) {
            String muxTpId = muxTpIds.get(0);
            Pattern pattern = Pattern.compile(model.muxPortMatchingRegex());
            Matcher matcher = pattern.matcher(muxTpId);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return BLANK;
    }

    @Data
    @Builder
    private static class MuxWssChannel implements Serializable {

        private String wssChannelName;

        private Integer portNum;
    }

    @Data
    @Builder
    private static class MuxWeightWssChannel implements Serializable {

        private Integer portNumber;

        private Channel wssChannel;

    }

    private static class MuxWeightWssChannelComparator implements Comparator<MuxWeightWssChannel> {

        @Override
        public int compare(MuxWeightWssChannel wss1, MuxWeightWssChannel wss2) {
            return wss1.getPortNumber() - wss2.getPortNumber();
        }
    }
}
