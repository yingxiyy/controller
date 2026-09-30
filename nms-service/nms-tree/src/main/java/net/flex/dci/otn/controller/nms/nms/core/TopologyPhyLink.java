/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.DirectionAmplifierInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.DirectionMetrics;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OmsLinkOtsLinkInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OmsLinkPAInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkAmplifierInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkRamanInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsTerminalInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.RouteContractInfoDto;
import net.flex.dci.otn.controller.nms.nms.handler.LinkHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOtsLinksUnderOmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOtsLinksUnderOmsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.OtsLinks;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.OtsLinksBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.PowerAmplifiers;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.PowerAmplifiersBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.OtsLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.OtsLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.AzBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.Destination;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.Source;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.ZaBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.az.Amplifier;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.az.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.az.RamanBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.AzPa;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.AzPaBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.ZaPa;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.ZaPaBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.PaBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.Terminal;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.TerminalBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.contract.info.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.contract.info.ZToABuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * get-phy-link get-phy-link-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyPhyLink extends BaseNms {

    private final String GET_PHY_LINK = "nms:get-phy-link";

    private final String GET_PHY_LINK_PAGED = "nms:get-phy-link-paged";

    private final String GET_OTS_LINKS_UNDER_SITE_LINK = "nms:get-ots-links-under-oms";

    @Autowired
    private LinkHandler linkHandler;

    public TopologyPhyLink(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        if (cmd.equals(GET_PHY_LINK)) {
            returnValue = getPhyLink(cmd, requestBody);
        } else if (cmd.equals(GET_PHY_LINK_PAGED)) {
            returnValue = getPhyLinkPaged(cmd, requestBody);
        } else if (cmd.equals(GET_OTS_LINKS_UNDER_SITE_LINK)) {
            returnValue = getOTSLinksUnderOMS(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unsupported phy link nms operations ");
        }
        return returnValue;
    }

    private String getOTSLinksUnderOMS(String cmd, String requestBody) {

        try {
            log.info("get ots links from requestBody :{}", requestBody);
            GetOtsLinksUnderOmsInput input = parseInput(cmd, requestBody,
                    GetOtsLinksUnderOmsInput.class);
            OmsLinkOtsLinkInfoDto omsLinkOtsLinkInfoDto = this.linkHandler.getOtsLinkByOMSLink(
                    input);
            RouteContractInfoDto routeContractInfoDto = omsLinkOtsLinkInfoDto.getRouteContractInfoDto();
            //a to z
            DirectionMetrics aToz = routeContractInfoDto.getAToz();
            //z to a
            DirectionMetrics zToa = routeContractInfoDto.getZToa();
            GetOtsLinksUnderOmsOutputBuilder outputBuilder = new GetOtsLinksUnderOmsOutputBuilder();
            outputBuilder.setOtsLinks(buildOtsLinks(omsLinkOtsLinkInfoDto.getOtsLinks()));
            outputBuilder.setOpenDate(routeContractInfoDto.getOpenDate());
            outputBuilder.setOmsLinkId(
                    LinkId.getDefaultInstance(omsLinkOtsLinkInfoDto.getOmsLinkId()));
            outputBuilder.setOmsLinkName(omsLinkOtsLinkInfoDto.getOmsName());
            outputBuilder.setOpenDate(routeContractInfoDto.getOpenDate());
            outputBuilder.setProvider(routeContractInfoDto.getProvider());
//            outputBuilder.setServiceCount(Math.toIntExact(routeContractInfoDto.getServiceCount()));
            outputBuilder.setAToZ(new AToZBuilder().setContractLengthKm(aToz.getContractLength())
                    .setRouteLengthKm(aToz.getRouteLength()).setRouteDelayMs(
                            aToz.getRouteDelay())
                    .setServiceCount(Math.toIntExact(aToz.getServiceCount())).build());
            outputBuilder.setZToA(new ZToABuilder().setContractLengthKm(zToa.getContractLength())
                    .setRouteDelayMs(zToa.getRouteDelay()).setRouteLengthKm(zToa.getRouteLength())
                    .setServiceCount(Math.toIntExact(zToa.getServiceCount()))
                    .build());
            outputBuilder.setPowerAmplifiers(
                    buildPowerAmplifiers(omsLinkOtsLinkInfoDto));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get ots links under oms link,reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    /**
     * build pa info source pa and destination pa
     *
     * @param omsLinkOtsLinkInfoDto
     * @return
     */
    private PowerAmplifiers buildPowerAmplifiers(OmsLinkOtsLinkInfoDto omsLinkOtsLinkInfoDto) {
        log.debug("build power amplifiers for oms");
        PowerAmplifiersBuilder powerAmplifiersBuilder = new PowerAmplifiersBuilder();
        OmsLinkPAInfo omsLinkPAInfo = omsLinkOtsLinkInfoDto.getPaInfo();
        log.debug("build az power amplifier");
        AzPa azPa = buildAZPaAmplifiers(omsLinkPAInfo);
        log.debug("build za amplifier");
        ZaPa zaPa = buildZAPaAmplifiers(omsLinkPAInfo);
        powerAmplifiersBuilder.setAzPa(azPa);
        powerAmplifiersBuilder.setZaPa(zaPa);
        return powerAmplifiersBuilder.build();
    }

    private ZaPa buildZAPaAmplifiers(OmsLinkPAInfo omsLinkOtsLinkInfoDto) {

        OtsLinkAmplifierInfo zaPowerAmplifier = omsLinkOtsLinkInfoDto.getZaPowerAmplifier();
        DirectionAmplifierInfo zaPAC = zaPowerAmplifier.getZaPaC();
        DirectionAmplifierInfo zaPAL = zaPowerAmplifier.getZaPaL();

        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.pa.Amplifier> amplifiers = new ArrayList<>();

        log.debug("build za pac");
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.pa.AmplifierBuilder zaPACAmplifierBuilder
                = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.pa.AmplifierBuilder();
        zaPACAmplifierBuilder.setName(zaPAC.getName());
        zaPACAmplifierBuilder.setCrossConnections(
                Collections.singletonList(zaPAC.getAmplifierXc()));
        amplifiers.add(zaPACAmplifierBuilder.build());
        log.debug("build za pal");
        if (zaPAL != null) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.pa.AmplifierBuilder zaPALAmplifierBuilder =
                    new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.pa.AmplifierBuilder();
            zaPALAmplifierBuilder.setName(zaPAL.getName());
            zaPALAmplifierBuilder.setCrossConnections(
                    Collections.singletonList(zaPAL.getAmplifierXc()));
            amplifiers.add(zaPALAmplifierBuilder.build());
        }
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.PaBuilder zaBuilder =
                new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.PaBuilder();
        zaBuilder.setAmplifier(amplifiers);
        ZaPaBuilder zaPaBuilder = new ZaPaBuilder();
        zaPaBuilder.setPa(zaBuilder.build());
        zaPaBuilder.setOtsLinkId(LinkId.getDefaultInstance(zaPowerAmplifier.getOtsLinkId()));
        zaPaBuilder.setOtsLinkName(zaPowerAmplifier.getOtsLinkName());
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.TerminalBuilder terminalBuilder =
                new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.za.pa.TerminalBuilder();
        OtsTerminalInfo source = zaPowerAmplifier.getSource();
        terminalBuilder.setNodeId(NodeId.getDefaultInstance(source.getNodeId()))
                .setNodeIp(source.getNodeIp())
                .setTpId(TpId.getDefaultInstance(source.getTpId()))
                .setTpName(source.getTpName());
        zaPaBuilder.setTerminal(terminalBuilder.build());

        return zaPaBuilder.build();
    }

    private AzPa buildAZPaAmplifiers(OmsLinkPAInfo omsLinkOtsLinkInfoDto) {
        OtsLinkAmplifierInfo azPowerAmplifier = omsLinkOtsLinkInfoDto.getAzPowerAmplifier();
        DirectionAmplifierInfo azPAC = azPowerAmplifier.getAzPaC();
        DirectionAmplifierInfo azPAL = azPowerAmplifier.getAzPaL();
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.pa.Amplifier> amplifiers = new ArrayList<>();

        log.debug("build az pac");
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.pa.AmplifierBuilder azPACAmplifierBuilder
                = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.pa.AmplifierBuilder();
        azPACAmplifierBuilder.setName(azPAC.getName());
        azPACAmplifierBuilder.setCrossConnections(
                Collections.singletonList(azPAC.getAmplifierXc()));
        amplifiers.add(azPACAmplifierBuilder.build());
        if (azPAL != null) {
            log.debug("build az paL");
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.pa.AmplifierBuilder azPALAmplifierBuilder
                    = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.power.amplifiers.az.pa.pa.AmplifierBuilder();
            azPALAmplifierBuilder.setName(azPAL.getName());
            azPALAmplifierBuilder.setCrossConnections(
                    Collections.singletonList(azPAL.getAmplifierXc()));
            amplifiers.add(azPALAmplifierBuilder.build());
        }

        PaBuilder azBuilder = new PaBuilder();
        azBuilder.setAmplifier(amplifiers);
        AzPaBuilder azPaBuilder = new AzPaBuilder();
        azPaBuilder.setPa(azBuilder.build());
        azPaBuilder.setOtsLinkName(azPowerAmplifier.getOtsLinkName());
        azPaBuilder.setOtsLinkId(LinkId.getDefaultInstance(azPowerAmplifier.getOtsLinkId()));

        OtsTerminalInfo paAZTerminal = azPowerAmplifier.getDestination();

        Terminal terminal = new TerminalBuilder()
                .setNodeId(NodeId.getDefaultInstance(paAZTerminal.getNodeId()))
                .setTpId(TpId.getDefaultInstance(paAZTerminal.getTpId()))
                .setNodeIp(paAZTerminal.getNodeIp())
                .setNodeName(paAZTerminal.getNodeName())
                .setTpName(paAZTerminal.getTpName())
                .setSiteId(NodeId.getDefaultInstance(paAZTerminal.getSiteId()))
                .setSiteName(paAZTerminal.getSiteName())
                .build();

        azPaBuilder.setTerminal(terminal);
        return azPaBuilder.build();
    }


    private OtsLinks buildOtsLinks(List<OtsLinkAmplifierInfo> otsLinkAmplifierInfos) {
        log.debug("build ots links amplifierXc info");
        List<OtsLink> otsLinks = otsLinkAmplifierInfos.stream().map(otsLinkAmplifierInfo -> {
            OtsLinkBuilder otsLinkBuilder = new OtsLinkBuilder();
            otsLinkBuilder.setOtsLinkId(
                    LinkId.getDefaultInstance(otsLinkAmplifierInfo.getOtsLinkId()));
            otsLinkBuilder.setOtsLinkName(otsLinkAmplifierInfo.getOtsLinkName());
            List<Amplifier> azAmplifiers = new ArrayList<>();
            log.debug("build a z C amplifierXc");
            AmplifierBuilder azCAmplifier = new AmplifierBuilder();
            DirectionAmplifierInfo azC = otsLinkAmplifierInfo.getAzC();
            azCAmplifier.setName(azC.getName());
            azCAmplifier.setCrossConnections(Collections.singletonList(azC.getAmplifierXc()));
            azAmplifiers.add(azCAmplifier.build());

            log.debug("build a z L amplifierXc");
            DirectionAmplifierInfo azL = otsLinkAmplifierInfo.getAzL();
            if (azL != null) {
                AmplifierBuilder azLAmplifier = new AmplifierBuilder();
                azLAmplifier.setName(azL.getName());
//            azLAmplifier.setNodeRef(NodeId.getDefaultInstance(azL.getNeId()));
                azLAmplifier.setCrossConnections(Collections.singletonList(azL.getAmplifierXc()));
                azAmplifiers.add(azLAmplifier.build());
            }
            AzBuilder azBuilder = new AzBuilder();
            azBuilder.setAmplifier(azAmplifiers);
            //az raman info
            OtsLinkRamanInfo azRaman = otsLinkAmplifierInfo.getAzRaman();
            if (azRaman != null) {
                RamanBuilder azRamanBuilder = new RamanBuilder();
                azRamanBuilder.setName(azRaman.getName());
                azRamanBuilder.setCrossConnections(
                        Collections.singletonList(azRaman.getAmplifierXc()));
                azBuilder.setRaman(azRamanBuilder.build());
            }

            ZaBuilder zaBuilder = new ZaBuilder();
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.Amplifier> zaAmplifiers = new ArrayList<>();
            log.debug("build z a C amplifierXc");
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.AmplifierBuilder zaCAmplifier = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.AmplifierBuilder();
            DirectionAmplifierInfo zaC = otsLinkAmplifierInfo.getZaC();
//            zaCAmplifier.setAmplifier(zaC.getAmplifierXc());
            zaCAmplifier.setName(zaC.getName());
            zaCAmplifier.setCrossConnections(Collections.singletonList(zaC.getAmplifierXc()));
//            zaCAmplifier.setNodeRef(NodeId.getDefaultInstance(zaC.getNeId()));
            zaAmplifiers.add(zaCAmplifier.build());

            DirectionAmplifierInfo zaL = otsLinkAmplifierInfo.getZaL();
            if (zaL != null) {
                log.debug("build z a L amplifierXc");
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.AmplifierBuilder zaLAmplifier = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.AmplifierBuilder();
//            zaLAmplifier.setAmplifier(zaL.getAmplifierXc());
//            zaLAmplifier.setNodeRef(NodeId.getDefaultInstance(zaL.getNeId()));
                zaLAmplifier.setCrossConnections(Collections.singletonList(zaL.getAmplifierXc()));
                zaLAmplifier.setName(zaL.getName());
                zaAmplifiers.add(zaLAmplifier.build());
            }
            zaBuilder.setAmplifier(zaAmplifiers);
            OtsLinkRamanInfo zaRaman = otsLinkAmplifierInfo.getZaRaman();
            if (zaRaman != null) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.RamanBuilder zaRamanBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ots.links.under.oms.output.ots.links.ots.link.za.RamanBuilder();
                zaRamanBuilder.setName(zaRaman.getName());
                zaRamanBuilder.setCrossConnections(
                        Collections.singletonList(zaRaman.getAmplifierXc()));
                zaBuilder.setRaman(zaRamanBuilder.build());
            }

            otsLinkBuilder.setAz(azBuilder.build());
            otsLinkBuilder.setZa(zaBuilder.build());
            otsLinkBuilder.setAlign(otsLinkAmplifierInfo.isAlign());
            log.debug("build terminal source info");
            OtsTerminalInfo terminalSource = otsLinkAmplifierInfo.getSource();
            OtsTerminalInfo terminalDestination = otsLinkAmplifierInfo.getDestination();
            //source
            Source source = new SourceBuilder()
                    .setNodeId(NodeId.getDefaultInstance(terminalSource.getNodeId()))
                    .setNodeName(terminalSource.getNodeName())
                    .setTpName(terminalSource.getTpName())
                    .setTpId(TpId.getDefaultInstance(terminalSource.getTpId()))
                    .setNodeIp(terminalSource.getNodeIp())
                    .setSiteId(NodeId.getDefaultInstance(terminalSource.getSiteId()))
                    .setSiteName(terminalSource.getSiteName())
                    .build();
            //destination
            Destination destination = new DestinationBuilder()
                    .setNodeId(NodeId.getDefaultInstance(terminalDestination.getNodeId()))
                    .setNodeName(terminalDestination.getNodeName())
                    .setTpId(TpId.getDefaultInstance(terminalDestination.getTpId()))
                    .setTpName(terminalDestination.getTpName())
                    .setNodeIp(terminalDestination.getNodeIp())
                    .setSiteId(NodeId.getDefaultInstance(terminalDestination.getSiteId()))
                    .setSiteName(terminalDestination.getSiteName())
                    .build();

            otsLinkBuilder.setSource(source);
            otsLinkBuilder.setDestination(destination);
            return otsLinkBuilder.build();
        }).collect(Collectors.toList());
        OtsLinksBuilder otsLinksBuilder = new OtsLinksBuilder();
        otsLinksBuilder.setOtsLink(otsLinks);
        return otsLinksBuilder.build();
    }

//    private String getPhyLinkPaged(String cmd, String requestBody) throws CommonException {
//        try {
//            GetPhyLinkPagedInput input = (GetPhyLinkPagedInput) parseInput(cmd, requestBody);
//            PagedList pagedList = this.linkHandler.getPhyLinkPaged(input);
//            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
//            GetPhyLinkPagedOutputBuilder outputBuilder = new GetPhyLinkPagedOutputBuilder();
//            outputBuilder.setLink((List<Link>) pagedList.getPage(startPos, input.getHowMany()));
//            outputBuilder.setStartPos(startPos);
//            outputBuilder.setTotalRecords(pagedList.getRecordsNumber());
//            return serializeDataObject(cmd, outputBuilder.build());
//        } catch (Exception ex) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
//        }
//    }

    private String getPhyLinkPaged(String cmd, String requestBody) throws CommonException {
        try {
            GetPhyLinkPagedInput input = parseInput(cmd, requestBody, GetPhyLinkPagedInput.class);
            PageResult<Link> pagedList = this.linkHandler.getPhyLinkPaged(input);
            GetPhyLinkPagedOutputBuilder outputBuilder = new GetPhyLinkPagedOutputBuilder();
            outputBuilder.setLink(pagedList.getList());
            outputBuilder.setStartPos(input.getStartPos());
            outputBuilder.setTotalRecords(Math.toIntExact(pagedList.getTotal()));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    private String getPhyLink(String cmd, String requestBody) throws CommonException {
        try {
            GetPhyLinkInput input = parseInput(cmd, requestBody, GetPhyLinkInput.class);
            List<Link> links = this.linkHandler.getPhyLink(input);
            GetPhyLinkOutputBuilder outputBuilder = new GetPhyLinkOutputBuilder();
            outputBuilder.setLink(links);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }
}
