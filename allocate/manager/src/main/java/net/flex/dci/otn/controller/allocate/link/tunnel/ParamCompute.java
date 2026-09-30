/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class ParamCompute extends ParamCreaionBasic {

    private ExplictRoute ro;
    private List<VendorOccupationRate> vendorOccupationRateList;

    public void parser(ComputeTunnelsInput input) throws CommonException {
        super.parser(input);
        ro = input.getExplictRoute();

        checkVendorTunnelNumber(input.getVendorOccupationRate());
        vendorOccupationRateList = input.getVendorOccupationRate();

    }

    private void checkVendorTunnelNumber(List<VendorOccupationRate> vendorOccupationRates) throws CommonException {
        Map<String, Integer> vendorRate = new HashMap<>();
        Integer totalNum = 0;
        for (VendorOccupationRate vendorOccupationRate : vendorOccupationRates) {
            String vendorName = vendorOccupationRate.getVendorName();
            String vendorType = vendorOccupationRate.getProductType();
            Integer vendorNum = vendorOccupationRate.getNumber();
            String vendorKey = vendorName + "#" + vendorType;
            if (vendorRate.containsKey(vendorKey)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("Duplicate VendorName[%s] ProductType[%s]", vendorName, vendorType));
            } else {
                vendorRate.put(vendorKey, vendorNum);
            }
            totalNum = totalNum + vendorNum;
        }

        if (totalNum != super.getBundleNumber()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("total num[%s] based on vendor isn't match with total tunnel numbers [%s]", String.valueOf(super.getBundleNumber()), String.valueOf(totalNum)));
        }
    }


    public int getVendorTunnelNumber(String vendor) throws CommonException {
        for (VendorOccupationRate rate : vendorOccupationRateList) {
            if (rate.getVendorName().equals(vendor)) {
                return rate.getNumber();
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "required vendor hasn't find in input");
    }

    public VendorOccupationRate getVendorOccupationRate(String vendor) throws CommonException {
        for (VendorOccupationRate rate : vendorOccupationRateList) {
            if (rate.getVendorName().equals(vendor)) {
                return rate;
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "required vendor hasn't find in input");
    }

}
