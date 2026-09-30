/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;


@Builder
@Data
@Slf4j
public class RegInput {

    @NonNull
    private List<RegSegment> primarySegments;
    private List<RegSegment> secondarySegments;
    @NonNull
    private Integer tunnelNumber;

    private static final BigDecimal DEFAULT_OUTPUT_POWER = new BigDecimal(-1);
    @NonNull
    private String vendorName;
    @NonNull
    private String vendorType;

    @NonNull
    private Class<? extends SignalProtocolType> tunnelSignalRate;
    @NonNull
    private Class<? extends SignalProtocolType> lineSignalRate;
    @NonNull
    private Class<? extends ETHERNETCOMPLIANCECODE> clientMedium;

    @Builder.Default
    private BigDecimal outputPower = DEFAULT_OUTPUT_POWER;
    @NonNull
    private String riskGroupName;
    @NonNull
    private String plane;
    @NonNull
    private String srcSite;
    @NonNull
    private String destSite;
    private SERVICETYPE servicetype;
    @NonNull
    private Collection<Node> totalNodes;

    @NonNull
    NeInfo tpcNeInfo;
    @NonNull
    BigInteger centFreq;

    public boolean isProtected() {
        return secondarySegments != null && !secondarySegments.isEmpty();
    }
}
