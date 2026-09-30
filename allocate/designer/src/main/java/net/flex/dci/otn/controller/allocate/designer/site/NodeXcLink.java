/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;

@Builder
@Data
class NodeXcLink {

    @NonNull
    private List<CrossConnections> xcs;
    @NonNull
    private List<Link> links;


    private List<CrossConnections> slaveXs;

    private List<Link> slaveLinks;

    private List<CrossConnections> thirdXs;

    private List<Link> thirdLinks;

    private Set<String> busyIds;


    @NonNull
    private List<InternalLinks> internalLinks;

    public List<CrossConnections> getNodeXcs() {
        if (slaveXs == null || slaveXs.isEmpty()) {
            return this.xcs;
        }

        if (thirdXs == null || thirdXs.isEmpty()) {
            return new ArrayList() {{
                addAll(xcs);
                addAll(slaveXs);
            }};
        }
        return new ArrayList() {{
            addAll(xcs);
            addAll(slaveXs);
            addAll(thirdXs);
        }};
    }
}
