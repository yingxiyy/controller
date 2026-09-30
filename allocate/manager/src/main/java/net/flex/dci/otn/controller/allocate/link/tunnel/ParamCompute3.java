/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.MandatorySiteLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc6;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;

/**
 * For DFS
 */
@Slf4j
@Data
public class ParamCompute3 extends ParamCreaionBasic {

    public static final List<String> GRID_75_GROUP = Arrays.asList(TunnelUtils.GRID_0, TunnelUtils.GRID_75, TunnelUtils.GRID_100);
    public static final List<String> GRID_FULL_GROUP = Arrays.asList(TunnelUtils.GRID_0, TunnelUtils.GRID_75, TunnelUtils.GRID_50, TunnelUtils.GRID_100);

    private List<VendorOccupationRate> vendorOccupationRateList;
    private List<String> vendorNames;
    private Map<String, Integer> vendorRequireOduMap;

    private Boolean isReusedOch;
    private List<String> primaryMandatorySiteLinkIds;
    private List<String> secondaryMandatorySiteLinkIds;


    public void parser(ComputeTunnels2Input input) throws CommonException {
        super.parser(input);
        checkVendorTunnelNumber(input.getVendorOccupationRate());
        vendorOccupationRateList = input.getVendorOccupationRate();
        vendorNames = input.getVendorOccupationRate().stream().map(item -> item.getVendorName()).collect(Collectors.toList());
        parseVendorRequireOduMap();
        isReusedOch = input.isIsReusedMixed() || input.isIsReusedTpc();

        primaryMandatorySiteLinkIds = input.getRouteRestriction().get(0).getMandatorySiteLink() != null && !input.getRouteRestriction().get(0).getMandatorySiteLink().isEmpty() ? input.getRouteRestriction().get(0).getMandatorySiteLink().stream().map(MandatorySiteLink::getLinkId).collect(
                Collectors.toList()) : Collections.EMPTY_LIST;

        secondaryMandatorySiteLinkIds = Collections.EMPTY_LIST;

    }

    private String getMandatoryNodeId(ComputeTunnels2Input input) {
        if (input.getRouteRestriction().get(0).getMandatoryNode() != null && !input.getRouteRestriction().get(0).getMandatoryNode().isEmpty()) {
            String nodeId = input.getRouteRestriction().get(0).getMandatoryNode().get(0).getNodeId();
            if (!nodeId.contains(input.getSrcSite().getValue()) && !nodeId.contains(input.getDstSite().getValue())) {
                return nodeId;
            }
        }
        return null;
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

    public int getVendorRequiredOdu(String vendor) {
        //todo, 将来业务类型扩展了，这个得根据tunnelSignalRate和linePortSignalRate计算
        try {
            return getVendorTunnelNumber(vendor);
        } catch (CommonException e) {
            return 0;
        }
    }

    private Map<String, Integer> parseVendorRequireOduMap() {
        vendorRequireOduMap = new HashMap<>();
        for (VendorOccupationRate rate : vendorOccupationRateList) {
            Integer tunnelNum = rate.getNumber();
            Integer oduNumber = getOduNumByTunnelNum(tunnelNum);
            vendorRequireOduMap.put(rate.getVendorName(), oduNumber);
        }
        return vendorRequireOduMap;
    }

    private Integer getOduNumByTunnelNum(Integer tunnelNum) {
        return tunnelNum * getClientLineRate();
    }

    public List<String> getMatchedGrids() {
        if (getCardType().equals(Constant.EquipmentClass.T2X4C8) || getLinePortSignalRate().equals(ProtOTUc4.class) || getLinePortSignalRate().equals(ProtOTUc6.class)) {
            return GRID_75_GROUP;
        }
        return GRID_FULL_GROUP;
    }
}
