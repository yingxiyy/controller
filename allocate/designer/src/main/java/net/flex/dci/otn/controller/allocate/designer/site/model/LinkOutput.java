/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site.model;


import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;

@Builder
@Data
public class LinkOutput {
    @NonNull
    private List<Link> links;
    @NonNull
    private List<InternalLinks> internalLinks;
    @NonNull
    private Set<String> busyTpIds;
}
