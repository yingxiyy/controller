/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RegSiteInfo;

@Builder
@Data
@Slf4j
public class RouteData {

    @NonNull
    private List<String> linkList;
    @NonNull
    private Map<String, RegSiteInfo> siteMap;

    public RegSiteInfo getRegSiteInfo(String siteId) throws NeDesignerException {
        if (!siteMap.containsKey(siteId)) {
            throw new NeDesignerException("Invalid route date, no RegSiteInfo found for :" + siteId);
        }
        return siteMap.get(siteId);
    }
}
