/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RiskGroupInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RiskGroupInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnle.segment.info.TunnelSegments;

@Slf4j
public class ParamTakeover extends ParamCreaionBasic {

    public static final int BASIC_SEGMENT_SIZE = 2;
    public String cardType;//tpc card type. e.g.T2X4C8
    Boolean isBaseOnNe;
    List<TunnelSegments> primarySegments;
    List<TunnelSegments> secondarySegments;
    BigInteger centFreq;
    BigDecimal outputPower;
    Class<? extends ProtectionType> protectionType;
    List<RiskGroupInfo> riskGroupInfos;
    String productType;
    String vendorName;
    NeInfo tpcNeInfo;
    TunnelSegments ochSrcSegment;
    TunnelSegments ochEndSegment;//set this value during createOchRoute in TunnelTakeOver
    String ochSrcTp;
    String ochDestTp;

    public ParamTakeover() {

    }



    protected void validateSegments() {
        validatePrimary();
        validateSecondary();
    }

    protected void validateSecondary() {
        if (secondarySegments == null) {
            return;
        }
        validateSegmentsBasic(secondarySegments);
    }

    protected void validatePrimary() {
        if (!TunnelUtil.isLPort(ochSrcTp)) {
            String msg = String.format("The first segment source tp should be %s port, but is :%s", PortType.OTULine.name(), ochSrcTp);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
        if (!TunnelUtil.isLPort(ochDestTp)) {
            String msg = String.format("The last segment destination tp should be %s port, but is :%s", PortType.OTULine.name(), ochDestTp);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        if (secondarySegments == null) {//non-protected
            validateSegmentsBasic(primarySegments);
        } else {
            String sigStartTp = ochSrcSegment.getDestinationTp();
            if (!TunnelUtil.isSigPort(sigStartTp)) {
                String msg = String.format("The first segment destination tp should be %s port, but is :%s", PortType.OPSig.name(), sigStartTp);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
            }
            String sigEndTp = ochSrcSegment.getSourceTp();
            if (!TunnelUtil.isSigPort(sigEndTp)) {
                String msg = String.format("The first segment destination tp should be %s port, but is :%s", PortType.OPSig.name(), sigEndTp);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
            }

            List<TunnelSegments> segmentExcludeSigLPort = primarySegments.subList(1, primarySegments.size() - 1);//去除首尾的L-SIG, SIG-L
            validateSegmentsBasic(segmentExcludeSigLPort);
        }

    }


    protected void validateSegmentsBasic(List<TunnelSegments> segments) {
        int size = segments.size();
        if (size != BASIC_SEGMENT_SIZE) {
            //non-protected: L-MD, MD->L; protected: A-MD,MD->
            String msg = String.format("The basic segments size  should be %d , but is :%d", BASIC_SEGMENT_SIZE, size);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
    }


    /**
     * 当以NE为准时，以下信息需要根据NE重新更新
     */
    public void parseUpdate() {
        RiskGroupInfo riskGroupInfo = new RiskGroupInfoBuilder().setRiskGroupName(getRiskGroupName()).setPlaneName(getPlaneName()).build();
        riskGroupInfos = Arrays.asList(riskGroupInfo);
        lineRateNumber = getNumByLineSignalRate(linePortSignalRate);
        clientRateNumber = getNumByTunnelSignalRate(tunnelSignalRate);
        clientLineRate = lineRateNumber / clientRateNumber;
        tunnelOdu = parseTunnelOdu();
    }

//    /**
//     * todo: 这部分目前先写死，后续支持电信模型时，需要重构，根据NE的json来获取相关的type
//     *
//     * @param tpFriendlyName
//     * @return
//     */
//    public EquipType getEquipTypeByTpFriendlyName(String tpFriendlyName) throws NeDesignerException {
//        if (tpFriendlyName.matches(".*-L\\d")) {
//            return EquipType.OT;
//        }
//        if (tpFriendlyName.matches(".*-SIG\\d")) {
//            return EquipType.OP;
//        }
//        throw new NeDesignerException("Failed to get equip type by tp friendly name:" + tpFriendlyName);
//    }

    public String getCardTypeByTpFriendlyName(String tpFriendlyName) {
        return tpFriendlyName.substring(0, tpFriendlyName.indexOf("-"));
    }

    public Integer getEquipSlotBypFriendlyName(String tpFriendlyName) {
        return Integer.parseInt(tpFriendlyName.split("-")[BASIC_SEGMENT_SIZE]);
    }

    public void parserTakeOver(TakeoverTunnelsInput input) {
        //takeover specific param
        productType = input.getProductType();
        vendorName = input.getVendorName();
        isBaseOnNe = input.isBaseOnNe();
        primarySegments = input.getPrimary().getTunnelSegments();
        secondarySegments =
                input.getSecondary() != null && input.getSecondary().getTunnelSegments() != null && !input.getSecondary().getTunnelSegments().isEmpty() ? input.getSecondary().getTunnelSegments()
                        : null;
        outputPower = new BigDecimal(-1);
        if (primarySegments.size() < BASIC_SEGMENT_SIZE) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "primarySegments can not less than 2.");
        }

        validateSegments();
        ochSrcSegment = primarySegments.get(0);
        ochEndSegment = primarySegments.get(primarySegments.size() - 1);
//        ochSrcTp = ochSrcSegment.getSourceTp();
//        ochDestTp = ochEndSegment.getDestinationTp();
        protectionType = secondarySegments != null ? ProtectionBidir1To1.class : null;
        RiskGroupInfo riskGroupInfo = new RiskGroupInfoBuilder().setRiskGroupName(getRiskGroupName()).setPlaneName(getPlaneName()).build();
        riskGroupInfos = Arrays.asList(riskGroupInfo);

        super.parser(input);

    }
}
