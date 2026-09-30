/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import static net.flex.dci.otc.common.data.DataConvertors.ODU2_SUPPORT_SIGNALS;
import static net.flex.dci.otc.common.data.DataConvertors.ODU4_SUPPORT_SIGNALS;
import static net.flex.dci.otc.common.data.DataConvertors.ODU4x2_SUPPORT_SIGNALS;
import static net.flex.dci.otc.common.data.DataConvertors.ODU4x4_SUPPORT_SIGNALS;
import static net.flex.dci.otc.common.data.DataConvertors.ODU4x6_SUPPORT_SIGNALS;

import java.util.*;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.Prot10GE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTU4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc6;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp;

import java.util.stream.Collectors;

/**
 * @author YYX
 * @date 12/6/2021
 */

public class TunnelUtil {

       /**
     * convert from tunnelignalRate
     *
     * @return
     */
    public static OduGranularity getOdujType(Class<? extends SignalProtocolType> tunnelSignalRate) throws CommonException {
        if (ODU4x6_SUPPORT_SIGNALS.contains(tunnelSignalRate.getSimpleName())) {
            return OduGranularity.Odu4x6;
        }
        if (ODU4x4_SUPPORT_SIGNALS.contains(tunnelSignalRate.getSimpleName())) {
            return OduGranularity.Odu4x4;
        }
        if (ODU4x2_SUPPORT_SIGNALS.contains(tunnelSignalRate.getSimpleName())) {
            return OduGranularity.Odu4x2;
        }
        if (ODU4_SUPPORT_SIGNALS.contains(tunnelSignalRate.getSimpleName())) {
            return OduGranularity.Odu4;
        }
        if (ODU2_SUPPORT_SIGNALS.contains(tunnelSignalRate.getSimpleName())) {
            return OduGranularity.Odu2;
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Unknown tunnel signal rate " + tunnelSignalRate.getSimpleName());
    }

    public static Class<? extends SignalProtocolType> getLineSignal(OduGranularity oduType) throws CommonException {
        switch (oduType) {
            case Odu2:
                return Prot10GE.class;
            case Odu4:
                return ProtOTU4.class;
            case Odu4x2:
                return ProtOTUc2.class;
            case Odu4x4:
                return ProtOTUc4.class;
            case Odu4x6:
                return ProtOTUc6.class;
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Unknown oduType " + oduType);
    }


    public static Set<String> getTunnelNode(List<Tunnel> tunnelList) {
        List<String> tpIdList = new ArrayList<>();

        for (Tunnel tunnel : tunnelList) {
            tpIdList.addAll(getTunnelTp(tunnel));
        }
        Set<String> nodeIdSet = tpIdList.stream().map(tpId -> PhysicalTpIdNamingRule.getNodeId(tpId)).collect(Collectors.toSet());
        return nodeIdSet;
    }

    public static List<String> getTunnelTp(Tunnel tunnel) {
        List<String> tpIdList = new ArrayList<>();
        for (SourceTp sTp : tunnel.getSourceTp()) {
            tpIdList.add(sTp.getTpRef().getValue());
        }
        for (DestinationTp dTp : tunnel.getDestinationTp()) {
            tpIdList.add(dTp.getTpRef().getValue());
        }

        return tpIdList;
    }

    /**
     * Deprecated, only for compile. Later can be removed together with takeover tunnels related.
     *
     * @param tpId
     * @return
     */
    public static boolean isLPort(String tpId) {
        String portName = PhysicalTpIdNamingRule.getPortNameByTpId(tpId);
        return portName.matches("L\\d");

    }

    /**
     * Deprecated, only for compile. Later can be removed together with takeover tunnels related.
     *
     * @param tpId
     * @return
     */
    public static boolean isSigPort(String tpId) {
        return tpId.contains("SIG");
    }

    public static String createVendorProductKey(String vendorName, String productType) {
        return String.format("%s%s", vendorName, productType);
    }
}
