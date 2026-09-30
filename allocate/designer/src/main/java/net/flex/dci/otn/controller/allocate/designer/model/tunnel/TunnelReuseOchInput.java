/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Builder
@Data
@Slf4j
public class TunnelReuseOchInput {

    @NonNull
    private String ochLinkId;
    @NonNull
    private Integer number;
    @NonNull
    private OduGranularity tunnelOdu;
    @NonNull
    private String vendorName;
    @NonNull
    private String vendorType;
    @NonNull
    private String cardType;//e.g. 200G-C4L2
    @NonNull
    private Class<? extends SignalProtocolType> lineSignalRate;
    @NonNull
    private Map<String, Node> totalInMemoryNode;
    @NonNull
    private Class<? extends ETHERNETCOMPLIANCECODE> clientMediumA;
    @NonNull
    private Class<? extends ETHERNETCOMPLIANCECODE> clientMediumZ;
    @NonNull
    private Class<? extends SignalProtocolType> tunnelSignalRate;
    private SERVICETYPE servicetype;

}
