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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RiskGroupInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RiskGroupInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnle.segment.info.TunnelSegments;

@Slf4j
public class RegParamTakeover extends ParamTakeover {

    public static final BigInteger DEFAULT_CEN_FREQUENCY = BigInteger.valueOf(191362500);

    public RegParamTakeover() {

    }

    public void parser(TakeoverTunnelsInput input) throws CommonException {
        super.parser(input);
        ochEndSegment = null;//因为REG是单向的，需要RegTunnelGenerator在handleLinksInOchRoute方法中，根据输入的dest site id得到的
        ochDestTp = null;//同上
        centFreq = input.getCentralFrequency() == null ? DEFAULT_CEN_FREQUENCY : input.getCentralFrequency();

        if (bundleNumber > clientLineRate) {
            String msg = String.format("Invalid bundle number:%d, because can create %d bundle max.", input.getBundleNumber(), clientLineRate);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

    }


    @Override
    protected void validateSecondary() {
        if (secondarySegments == null) {
            return;
        }
        validateSegmentsBasic(secondarySegments);
    }

    @Override
    protected void validatePrimary() {
        if (secondarySegments == null) {//non-protected
            validateSegmentsBasic(primarySegments);
        } else {
            List<TunnelSegments> segmentExcludeSigLPort = excludeOpSigSegment(primarySegments);
            validateSegmentsBasic(segmentExcludeSigLPort);//remove  segment: e.g. L1-Sig
        }

    }

    private boolean includeOpSig(TunnelSegments segment) {
        if (TunnelUtil.isSigPort(segment.getSourceTp()) || TunnelUtil.isSigPort(segment.getDestinationTp())) {
            return true;
        }
        return false;
    }


    /**
     * Remove the bi-direction segment: L->SIG, SIG->L
     *
     * @param segmentsList
     * @return
     */
    private List<TunnelSegments> excludeOpSigSegment(List<TunnelSegments> segmentsList) {
        return segmentsList.stream().filter(segment -> !includeOpSig(segment)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    protected void validateSegmentsBasic(List<TunnelSegments> segments) {

        TunnelSegments startSegment = segments.get(0);
        TunnelSegments endSegment = segments.get(segments.size() - 1);
        if (!startSegment.getSourceIp().equals(endSegment.getDestinationIp())) {
            String msg = String.format("The first segment source ip should be equals to the last segment dest ip");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
        if (!startSegment.getSourceTp().equals(endSegment.getDestinationTp())) {
            String msg = String.format("The first segment source tp should be equals to the last segment dest tp");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        int size = segments.size();
        for (int i = 0; i < size - 1; i++) {
            TunnelSegments s1 = segments.get(i);
            TunnelSegments s2 = segments.get(i + 1);
            if (!s1.getDestinationIp().equals(s2.getSourceIp())) {
                String msg = String.format("The segment src ip should be equals to the previous segment dest ip");
                log.error("Invalid segment:{},{}. {}", s1, s2, msg);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
            }

            if (!s1.getDestinationTp().equals(s2.getSourceTp())) {
                String msg = String.format("The segment src tp should be equals to the previous segment dest tp");
                log.error("Invalid segment:{},{}. {}", s1, s2, msg);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
            }

        }
    }

    @Override
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

        this.parser(input);

    }

}
