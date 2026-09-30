/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelRouteBundleInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info.RouteBundleInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class ParamCreate extends ParamCreaionBasic {

    private List<RouteBundleInfo> routeBundleInfo;
    public void parser(CreateTunnelInput input) throws CommonException {
        super.parser(input);
        checkTunnelNumber(input);
    }

    public void parser(AllocateTunnelsInput input) throws CommonException {
        super.parser(input);
        checkTunnelNumber(input);
    }

    public void parser(CreateTunnel2Input input) throws CommonException {
        super.parser(input);
        checkTunnelNumber(input);
    }

    private void checkTunnelNumber(TunnelRouteBundleInfo input) {
        log.debug("checking tunnel number");

        routeBundleInfo = input.getRouteBundleInfo();

        int total = 0;
        for (RouteBundleInfo bundleInfo : routeBundleInfo) {
            if (bundleInfo.getBundleNumber() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "missing mandatory vendor's tunnel nubmer");
            }
            total += bundleInfo.getBundleNumber();
        }
        if (total != super.getBundleNumber()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "total of vendor's tunnel nubmer is not match with required tunnel number");
        }
    }

}
