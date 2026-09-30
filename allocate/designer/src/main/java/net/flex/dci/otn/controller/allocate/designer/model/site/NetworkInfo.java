/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.site;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.Roadms;

@Builder
@Data
public class NetworkInfo {

    @NonNull
    private Map<String, RouteInfo> createLinks;//新创建的复用段，关联的复用段（只输出改变了的地方); key是UI给的input中的sitelink的friendly-name
    @NonNull
    private List<Link> wssLinks;
    @NonNull
    List<Roadms> roadms;

}
