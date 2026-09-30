/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

@Builder
@Data
class CardXcs {

    /*    @NonNull
        private Card card;*/
    @NonNull
    private List<CrossConnections> xcs;
    @NonNull
    private List<CrossConnections> slaveXcs;
}
