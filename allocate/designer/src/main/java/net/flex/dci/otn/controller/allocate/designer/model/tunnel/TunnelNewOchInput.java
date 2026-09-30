/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.ServiceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RegSiteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;

@Builder
@Data
@Slf4j
public class TunnelNewOchInput {

    public static final String UN_PROTECTED = "UN_PROTECTED";
    public static final String PROTECTED_1TO1 = "ProtectionBidir1To1";
    public static final String PROTECTED_1TO2 = "ProtectionBidir1To2";


    private String opMode;
    private WDM_Band wdmBand;

    // Requested protection and actual route count differ when a 1:2 OCH is initially created with two legs.
    private Class<? extends ProtectionType> requestedProtectionType;

    @NonNull
    private GridType grid;
    @NonNull
    private Integer tunnelNumber;
    @NonNull
    private Integer ochNumber;
    @NonNull
    private Integer tunnelNumberPerOch;//the bundle can create for one OCH

    private static final BigDecimal DEFAULT_OUTPUT_POWER = new BigDecimal(-1);

    @NonNull
    private SiteLinkRoute siteLinkRoute;
    @NonNull
    private String vendorName;
    @NonNull
    private String vendorType;
    @NonNull
    private String cardType;//e.g. 200G-C4L2

    private String op6CardType;

    @NonNull
    private Class<? extends SignalProtocolType> tunnelSignalRate;
    @NonNull
    private Class<? extends SignalProtocolType> lineSignalRate;
    @NonNull
    private Class<? extends ETHERNETCOMPLIANCECODE> clientMediumA;
    @NonNull
    private Class<? extends ETHERNETCOMPLIANCECODE> clientMediumZ;

    @Builder.Default
    private BigDecimal outputPower = DEFAULT_OUTPUT_POWER;
    @NonNull
    private String riskGroupName;
    @NonNull
    private SERVICETYPE serviceType;
    @NonNull
    private String plane;
    @NonNull
    private String planeId;
    @NonNull
    private Map<String, Node> totalInMemoryNode;
    @NonNull
    private String srcSite;
    @NonNull
    private String destSite;
    @NonNull
    private List<Node> reusedNodesInDb;//根据利旧策略，提供有空槽位可利旧的node池
    @NonNull
    private List<Node> reusedIncludeNodes;//利旧优先选取的nodes
    @NonNull
    private Set<String> excludeNodes;//不能用的node
    @NonNull
    private Boolean isReusedMixed;
    @Builder.Default
    private Map<String, List<String>> selectedCardIdsBySite = Collections.EMPTY_MAP;

    public String getProtectionType() {
        if ((siteLinkRoute.getSecondary() == null || siteLinkRoute.getSecondary().isEmpty()) && (siteLinkRoute
                .getThird() == null || siteLinkRoute
                .getThird().isEmpty())) {
            return UN_PROTECTED;
        }
        if (siteLinkRoute.getThird() == null || siteLinkRoute.getThird().isEmpty()) {
            return PROTECTED_1TO1;
        }
        return PROTECTED_1TO2;
    }

    public RouteData getPrimaryRoute() throws NeDesignerException {
        Map<String, RegSiteInfo> map;
        if (siteLinkRoute.getPrimary() == null) {
            throw new NeDesignerException("Invalid path,primary is missing");
        }
        if (siteLinkRoute.getPrimaryReg() == null) {
            map = Collections.EMPTY_MAP;
        } else {
            try {
                map = siteLinkRoute.getPrimaryReg().stream()
                        .collect(Collectors.toMap(
                                RegSiteInfo::getSiteId,
                                info -> info,
                                (existing, duplicate) -> {
                                    throw new IllegalArgumentException("Duplicate siteId: " + duplicate.getSiteId());
                                }
                        ));

            } catch (IllegalArgumentException e) {
                throw new NeDesignerException("Invalid path,duplicate site id", e);
            }
        }

        return RouteData.builder().
                linkList(siteLinkRoute.getPrimary()).
                siteMap(map).build();
    }

    public RouteData getSecondaryRoute() throws NeDesignerException {
        if (siteLinkRoute.getSecondary() == null || siteLinkRoute.getSecondary().isEmpty()) {
           return null;
        }
        Map<String, RegSiteInfo> map;
        if (siteLinkRoute.getSecondaryReg() == null || siteLinkRoute.getSecondaryReg().isEmpty()) {
            map = Collections.EMPTY_MAP;
        } else {
            try {
                map = siteLinkRoute.getSecondaryReg().stream()
                        .collect(Collectors.toMap(
                                RegSiteInfo::getSiteId,
                                info -> info,
                                (existing, duplicate) -> {
                                    throw new IllegalArgumentException("Duplicate siteId: " + duplicate.getSiteId());
                                }
                        ));

            } catch (IllegalArgumentException e) {
                throw new NeDesignerException("Invalid path,duplicate site id", e);
            }
        }

        return RouteData.builder().
                linkList(siteLinkRoute.getSecondary()).
                siteMap(map).build();
    }

    public RouteData getThirdRoute() throws NeDesignerException {
        if (siteLinkRoute.getThird() == null || siteLinkRoute.getThird().isEmpty()) {
            return null;
        }
        Map<String, RegSiteInfo> map;
        if (siteLinkRoute.getThirdReg() == null || siteLinkRoute.getThirdReg().isEmpty()) {
            map = Collections.EMPTY_MAP;
        } else {
            try {
                map = siteLinkRoute.getThirdReg().stream()
                        .collect(Collectors.toMap(
                                RegSiteInfo::getSiteId,
                                info -> info,
                                (existing, duplicate) -> {
                                    throw new IllegalArgumentException("Duplicate siteId: " + duplicate.getSiteId());
                                }
                        ));

            } catch (IllegalArgumentException e) {
                throw new NeDesignerException("Invalid path,duplicate site id", e);
            }
        }

        return RouteData.builder().
                linkList(siteLinkRoute.getThird()).
                siteMap(map).build();

    }
}
