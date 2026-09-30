/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.common;

import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public class LinkUtil {

    public static boolean shouldLinkUpdate(Link link, String neId, Set<String> nmlKeySet) {
        String srcNeId = link.getSource().getSourceNode().getValue();
        String destNeId = link.getDestination().getDestNode().getValue();
        if (srcNeId.equals(neId) || destNeId.equals(neId)) {
            if (nmlKeySet != null) {
                return shouldLinkUpdate(link, nmlKeySet);
            } else {
                return true;
            }

        } else {
            return false;
        }
    }

    private static boolean shouldLinkUpdate(Link link, Set<String> keySet) {
        for (String nmlKey : keySet) {
            if (link.getSource().getSourceTp().getValue().startsWith(nmlKey)) {
                return true;
            }
            if (link.getDestination().getDestTp().getValue().startsWith(nmlKey)) {
                return true;
            }
        }
        return false;
    }
}
