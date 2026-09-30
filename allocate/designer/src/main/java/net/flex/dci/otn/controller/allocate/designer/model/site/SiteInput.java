/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.site;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

@Builder
@Data
public class SiteInput {

    private Map<RoutingType, List<SiteNodeInput>> nodesMap;
//    @NonNull
//    private List<SiteNodeInput> nodes;
//
//    private List<SiteNodeInput> slaveNodes;

    @NonNull
    private String vendorName;
    @NonNull
    private String vendorType;
    // Initial siteLink channel capacity; only Bone2.0 CHASSIS2.0 uses it to select FMUX quantity.
    private Integer bandwidth;
    @NonNull
    private Boolean isProtected;
    @NonNull
    private Integer grid;
    @NonNull
    private String plane;

    @NonNull
    private String planeId;
    @NonNull
    private String riskGroupName;
    @NonNull
    private String linkModel;
    private WDM_Band wdmBand;


    @NonNull
    private Class<? extends ProtectionType> protectionType;
}
