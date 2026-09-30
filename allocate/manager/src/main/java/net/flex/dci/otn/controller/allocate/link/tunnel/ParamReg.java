/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reg.segment.info.RegSegments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reg.segment.info.reg.segments.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reg.segment.info.reg.segments.Source;

@Slf4j
public class ParamReg extends ParamCreaionBasic {

    String productType;
    String vendorName;
    List<RegSegments> primarySegments;
    List<RegSegments> secondarySegments;
    BigInteger centFreq;
    //    BigDecimal outputPower = new BigDecimal(-3);
    AllocateRegsInput input;
    NeInfo tpcNeInfo;

    public ParamReg(AllocateRegsInput input) {
        primarySegments = input.getPrimary().getRegSegments();
        secondarySegments =
                input.getSecondary() != null && input.getSecondary().getRegSegments() != null && !input.getSecondary().getRegSegments().isEmpty() ? input.getSecondary().getRegSegments()
                        : null;
        validateSegments();
        vendorName = input.getVendorOccupationRate().get(0).getVendorName();
        productType = input.getVendorOccupationRate().get(0).getProductType();
        centFreq = input.getCentralFrequency();

    }

    public void parser() throws CommonException {
        if (vendorName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "vendorName is mandatory");
        }
        if (productType == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "productType is mandatory");
        }
        if (centFreq == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "centFreq is mandatory");
        }
        Class<ProtectionBidir1To1> protectionType = secondarySegments != null ? ProtectionBidir1To1.class : null;
        input = new AllocateRegsInputBuilder(input).setProtectionType(protectionType).build();
        super.parser(input);

        if (bundleNumber > clientLineRate) {
            String msg = String.format("Invalid bundle number:%d, because can create %d bundle max.", bundleNumber, clientLineRate);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
    }

    private void validateSegments() {
        validatePrimary();
        validateSecondary();
    }

    private void validateSecondary() {
        if (secondarySegments == null) {
            return;
        }
        validateSegmentsBasic(secondarySegments);
    }

    private void validatePrimary() {
        if (secondarySegments == null) {//non-protected
            validateSegmentsBasic(primarySegments);
        } else {
            List<RegSegments> segmentExcludeSigLPort = excludeOpSigSegment(primarySegments);
            validateSegmentsBasic(segmentExcludeSigLPort);//remove  segment: e.g. L1-Sig
        }

    }

    private boolean includeOpSig(RegSegments segment) {
        if (TunnelUtil.isSigPort(segment.getSource().getTpFriendlyName()) || TunnelUtil.isSigPort(segment.getDestination().getTpFriendlyName())) {
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
    private List<RegSegments> excludeOpSigSegment(List<RegSegments> segmentsList) {
        return segmentsList.stream().filter(segment -> !includeOpSig(segment)).collect(Collectors.toCollection(ArrayList::new));
    }

    private void validateSegmentsBasic(List<RegSegments> segments) {

        RegSegments startSegment = segments.get(0);
        RegSegments endSegment = segments.get(segments.size() - 1);
        if (!startSegment.getSource().getIp().equals(endSegment.getDestination().getIp())) {
            String msg = String.format("The first segment source ip should be equals to the last segment dest ip");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
        if (!startSegment.getSource().getIp().equals(endSegment.getDestination().getIp())) {
            String msg = String.format("The first segment source tp should be equals to the last segment dest tp");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        int size = segments.size();
        for (int i = 0; i < size - 1; i++) {
            RegSegments s1 = segments.get(i);
            RegSegments s2 = segments.get(i + 1);
            compareSegPoint(s1.getDestination(), s2.getSource());
        }
    }

    private void compareSegPoint(Destination destination, Source source) {
        String previousIp = destination.getIp();
        String currentIp = source.getIp();
        if (!StringUtils.equals(previousIp, currentIp)) {
            String msg = String.format("The previous segment dest ip not equals to the current segment src ip. previous:%s, current:%s", destination, source);
            log.error("Invalid segment for IP: previous:{},current:{}.", destination, source, msg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        String previousTp = destination.getTpFriendlyName();
        String currentTp = source.getTpFriendlyName();
        if (!StringUtils.equals(previousTp, currentTp)) {
            String msg = String.format("The segment src tp should be equals to the previous segment dest tp. previous:%s, current:%s", destination, source);
            log.error("Invalid segment for TP: previous:{},current:{}.", destination, source, msg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
    }
}
