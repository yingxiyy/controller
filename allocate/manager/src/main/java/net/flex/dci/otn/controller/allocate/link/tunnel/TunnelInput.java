/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.List;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

@Data
public class TunnelInput {

    @NonNull
    private List<Link> siteLink;
    @NonNull
    private String vendorName;
    @NonNull
    private String vendorType;
    @NonNull
    String cardType;//e.g. 200G-C4L2
    @NonNull
    Class<? extends SignalProtocolType> signalRate;
    @NonNull
    Class<? extends SignalProtocolType> lineSignalRate;
    @NonNull
    Class<? extends ETHERNETCOMPLIANCECODE> clientMedium;

    public TunnelInput() {
    }
}