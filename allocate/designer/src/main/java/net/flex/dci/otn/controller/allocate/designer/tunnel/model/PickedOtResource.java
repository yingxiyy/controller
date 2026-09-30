/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.model;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Builder
@Data
public class PickedOtResource {

    @NonNull
    private Node node;

    private PickedOtTps pickedOtTps;

    @Default
    private Boolean reusedLPort = false;//只有一种场景会为true，就是场景一，L口已经有和Mux的OSlink存在，但是L口仍然有时隙可用。

}
