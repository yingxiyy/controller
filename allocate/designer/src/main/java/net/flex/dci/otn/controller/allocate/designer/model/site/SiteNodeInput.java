/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.site;


import java.util.Collections;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otc.mongo.enums.NeSubType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;


@Data
@Builder
public class SiteNodeInput {

    @NonNull
    private String siteId;
    @NonNull
    private String nodeType;
    @NonNull
    private NeSubType neSubType;
    @Default
    private Set<String> cardTypeVendors = Collections.EMPTY_SET;
    @Default
    private boolean ramanOnLeft = false;
    @Default
    private boolean ramanOnRight = false;
    private Node ipNode;//for NE which is specified ip
    // Non-null only for the hard-coded Bone2.0 1+2 peer NE at an endpoint site.
    private RoutingType protectionPeerRole;
}
