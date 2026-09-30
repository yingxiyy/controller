package net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel;

import static net.flex.dci.otn.controller.nms.utils.Constants.ASE_CROSS_CONNECTION_PREFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.ASE_PREFIX;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dto.OchLinkBriefInfo;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssASEControl;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssChannelControl;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssPowerControl;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.AseControl;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.AseControlBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.PowerControl;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.PowerControlBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.Spectrum;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.SpectrumBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.power.control.DestToSource;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.power.control.DestToSourceBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.power.control.SourceToDest;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.channel.power.control.SourceToDestBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 2022/12/27 11:51k
 */
@Slf4j
public abstract class AbstractWaveLengthSelectiveSwitchCard implements
        WaveLengthSelectiveSwitchCard {

    @Autowired
    protected CrossConnectionsDao crossConnectionsDao;

    protected List<CrossConnections> getWssConnections(List<CrossConnections> crossConnections) {
        List<CrossConnections> wssChannelXcs = crossConnections.stream()
                .filter(xc -> xc.getWssChannel() != null).collect(
                        Collectors.toList());
        List<CrossConnections> allocateWssChannelXcs = wssChannelXcs.stream()
                .filter(xc -> xc.getImplementState() == ImplementState.Allocate).collect(
                        Collectors.toList());
        List<CrossConnections> implementWssChannelXcs = wssChannelXcs.stream()
                .filter(xc -> xc.getImplementState() == ImplementState.Implement).collect(
                        Collectors.toList());
        implementWssChannelXcs = getRealWssChannelXcsFromNe(implementWssChannelXcs);
        allocateWssChannelXcs = getAllocatedRealWssChannelXcsFromNe(allocateWssChannelXcs);
        List<CrossConnections> wssChannelXcList = Stream.concat(allocateWssChannelXcs.stream(),
                        implementWssChannelXcs.stream())
                .collect(Collectors.toList());
        return wssChannelXcList;
    }

    /**
     * @param allocateWssChannelXcs
     * @return
     */
    private List<CrossConnections> getAllocatedRealWssChannelXcsFromNe(
            List<CrossConnections> allocateWssChannelXcs) {
        log.debug("get the allocated wss channel properties from the ne the wss size is:{}",
                allocateWssChannelXcs.size());
        List<String> crossWssChannelXcIds = allocateWssChannelXcs.stream().map(
                CrossConnectionAttributes::getCrossConnectionId).map(Uri::getValue).collect(
                Collectors.toList());
        List<CrossConnections> realAllocateInNes = crossConnectionsDao.listAllRealXcByXcIds(
                crossWssChannelXcIds);
        Map<String, CrossConnections> realAllocateXcMap = realAllocateInNes.stream()
                .collect(Collectors.toMap(xc -> xc.getCrossConnectionId().getValue(), xc -> xc));
        List<CrossConnections> realAllocatedXcs = allocateWssChannelXcs.stream()
                .map(crossConnection -> {
                    log.debug("get the real cross connection:{} from neId:{}",
                            crossConnection.getCrossConnectionId(), crossConnection.getNodeRef());
                    String crossConnectionId = crossConnection.getCrossConnectionId().getValue();
                    CrossConnections realXc = realAllocateXcMap.get(crossConnectionId);
                    CrossConnectionsBuilder crossConnectionsBuilder = new CrossConnectionsBuilder(
                            crossConnection);
                    if (realXc != null) {
                        crossConnectionsBuilder.setProperties(realXc.getProperties());
                    }
                    return crossConnectionsBuilder.build();
                }).collect(Collectors.toList());
        return realAllocatedXcs;
    }

    /**
     * get real wss cahnnel
     *
     * @param implementWssChannelXcs
     * @return
     */
    private List<CrossConnections> getRealWssChannelXcsFromNe(
            List<CrossConnections> implementWssChannelXcs) {
        log.debug("get the implement wss channel from the ne implement wss channel xcs is:{}",
                implementWssChannelXcs.size());
        List<String> implementWssChannelXcIds = implementWssChannelXcs.stream()
                .map(crossConnections -> crossConnections.getCrossConnectionId().getValue())
                .collect(Collectors.toList());
//        List<CrossConnections> realWssChannelXcs = implementWssChannelXcs.stream()
//                .map(crossConnection -> {
//                    log.debug("get the cross connection :{} from neId:{}",
//                            crossConnection.getCrossConnectionId(), crossConnection.getNodeRef());
//                    String crossConnectionId = crossConnection.getCrossConnectionId().getValue();
//                    String nodeId = crossConnection.getNodeRef().getValue();
//                    CrossConnections realXcs = crossConnectionsDao.getOpXC(nodeId,
//                            crossConnectionId);
//                    return realXcs;
//                }).collect(
//                        Collectors.toList());
        List<CrossConnections> realWssChannelXcs = crossConnectionsDao.listAllRealXcByXcIds(
                implementWssChannelXcIds);
        return realWssChannelXcs;
    }

    /**
     * generate wss channel name
     *
     * @param xc
     * @return
     */
    public String generateWssChannelName(NeYangModel model, CrossConnections xc) {
        return null;
    }

    public String generateWssChannelName(CrossConnections xc, BigInteger centre) {
        String xcId = xc.getCrossConnectionId().getValue();
        log.debug("generate wss channel name for the cross connection id is:{}", xcId);
        //assume src and dest tp is only one tp
        String generalWssCrossName = CrossConnectionUtils.generateWssCrossName(xc,
                centre.longValue());
        return generalWssCrossName;
    }

    protected TerminationPointInfo getCrossConnectionTpId(TpId tpId) {
        log.debug("get cross connection reference tp id:{}", tpId);
        String srcSimpleId = PhysicalTpIdNamingRule.getShortTpByTpId(tpId.getValue());
        return TerminationPointInfo.builder().tpName(srcSimpleId).tpId(tpId.getValue()).build();
    }

    protected PowerControl getPowerControl(WssChannelControl wssChannelControl) {

        log.debug("get power Control mode wssChannelControl:{}", wssChannelControl);
        if (wssChannelControl == null) {
            return null;
        }
        WssPowerControl azPowerControl = wssChannelControl.getAzPowerControl();
        WssPowerControl zaPowerControl = wssChannelControl.getZaPowerControl();

        PowerControlBuilder powerControlBuilder = new PowerControlBuilder();
        SourceToDest sourceToDest = getSourceToDestPowerControlMode(azPowerControl);
        DestToSource destToSource = getDestToSourcePowerControlMode(zaPowerControl);

        powerControlBuilder.setDestToSource(destToSource);
        powerControlBuilder.setSourceToDest(sourceToDest);
        return powerControlBuilder.build();
    }

    protected AseControl getAseControl(WssChannelControl wssChannelControl) {
        if (wssChannelControl == null) {
            return null;
        }
        AseControlBuilder aseControlBuilder = new AseControlBuilder();
        WssASEControl wssASEControl = wssChannelControl.getASEControl();
        if (wssASEControl.getAseControlMode() == null) {
            return null;
        }
        aseControlBuilder.setAseControlMode(wssASEControl.getAseControlMode()
                .getAseControlMode());
        aseControlBuilder.setAseInjectionHysteresis(wssASEControl.getInjectionHysteresis());
        aseControlBuilder.setAseInjectionThreshold(wssASEControl.getInjectionThreshold());
        return aseControlBuilder.build();
    }

    protected Map<BigInteger, List<OchLinkBriefInfo>> getOchLinkCrentreFqMap(
            List<OchLinkBriefInfo> ochLinks) {
        Map<BigInteger, List<OchLinkBriefInfo>> ochLinkFrequenceMap = new HashMap<>();
        for (OchLinkBriefInfo ochLink : ochLinks) {

            BigInteger upperFrequency = ochLink.getUpperFrequency();
            BigInteger lowerFrequency = ochLink.getLowerFrequency();
            BigInteger centreFrequency = upperFrequency.add(lowerFrequency)
                    .divide(BigInteger.valueOf(2));
            ochLinkFrequenceMap.computeIfAbsent(centreFrequency, k -> new ArrayList<>())
                    .add(ochLink);
        }
        return ochLinkFrequenceMap;
    }

    protected OchLinkBriefInfo getCarriedOchLink(List<OchLinkBriefInfo> refOchLinks,
            CrossConnections crossConnection) {
        if (refOchLinks == null || refOchLinks.isEmpty()) {
            return null;
        }
        int size = refOchLinks.size();
        if (size == 1) {
            return refOchLinks.get(0);
        }
        //assemble only have one
        String crossConnectionId = crossConnection.getCrossConnectionId().getValue();
        boolean isAseXc = crossConnectionId.startsWith(ASE_CROSS_CONNECTION_PREFIX);
        OchLinkBriefInfo aseLink = null;
        OchLinkBriefInfo realLink = null;

        for (OchLinkBriefInfo och : refOchLinks) {

            boolean isAseLink = och.getFriendlyName().startsWith(ASE_PREFIX);

            if (isAseLink) {
                aseLink = och;
            } else {
                realLink = och;
            }
        }
        if (isAseXc && aseLink != null) {
            return aseLink;
        } else if (!isAseXc && realLink != null) {
            return realLink;
        }
        return null;
    }

    protected Spectrum getSpectrum(long centFreq, long lowFreq, long upFreq) {
        SpectrumBuilder sb = new SpectrumBuilder();
        sb.setCentreFrequency(new BigDecimal((double) centFreq / 1000000)
                .setScale(5, RoundingMode.HALF_UP).toString());
        sb.setLowerFrequency(
                new BigDecimal((double) lowFreq / 1000000).setScale(5, RoundingMode.HALF_UP)
                        .toString());
        sb.setUpperFrequency(
                new BigDecimal((double) upFreq / 1000000).setScale(5, RoundingMode.HALF_UP)
                        .toString());

        return sb.build();
    }


    private SourceToDest getSourceToDestPowerControlMode(WssPowerControl azPowerControl) {
        log.debug("build source to dest power control :{}", azPowerControl);
        if (azPowerControl.getMode() == null) {
            return null;
        }
        SourceToDestBuilder sourceToDestBuilder = new SourceToDestBuilder();
        sourceToDestBuilder.setEnabled(true);
        sourceToDestBuilder.setMode(azPowerControl.getMode().getPowerControlMode());
        sourceToDestBuilder.setActivationThreshold(azPowerControl.getActivationThreshold());
        sourceToDestBuilder.setTargetPower(azPowerControl.getTargetPower());
        sourceToDestBuilder.setCalibrationPower(azPowerControl.getCalibrationPower());
        return sourceToDestBuilder.build();
    }

    private DestToSource getDestToSourcePowerControlMode(WssPowerControl zaPowerControl) {
        log.debug("build dest to source power control :{}", zaPowerControl);
        if (zaPowerControl.getMode() == null) {
            return null;
        }
        DestToSourceBuilder destToSourceBuilder = new DestToSourceBuilder();
        destToSourceBuilder.setActivationThreshold(zaPowerControl.getActivationThreshold());
        destToSourceBuilder.setEnabled(true);
        destToSourceBuilder.setMode(zaPowerControl.getMode().getPowerControlMode());
        destToSourceBuilder.setTargetPower(zaPowerControl.getTargetPower());
        destToSourceBuilder.setCalibrationPower(zaPowerControl.getCalibrationPower());
        return destToSourceBuilder.build();
    }

    @Data
    @Builder
    protected static class TerminationPointInfo implements Serializable {

        private String tpId;
        private String tpName;
    }


    @Data
    @Builder
    protected static class WeightWssChannel implements Serializable {

        private BigInteger weight;

        private Channel channel;
    }

    public static class WeightWssChannelComparator implements Comparator<WeightWssChannel> {

        @Override
        public int compare(WeightWssChannel wssChannel1, WeightWssChannel wssChannel2) {
            long weight1 = wssChannel1.getWeight().longValue();
            long weight2 = wssChannel2.getWeight().longValue();
            return (int) (weight1 - weight2);
        }
    }
}
