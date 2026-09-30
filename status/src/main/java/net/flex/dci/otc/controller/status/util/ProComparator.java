/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.util;

import java.util.Comparator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;

public class ProComparator implements Comparator<PathRouteObject> {

    @Override
    public int compare(PathRouteObject p1, PathRouteObject p2) {
        if (p1.getIndex().longValue() == p2.getIndex().longValue()) {
            return 0;
        } else if (p1.getIndex().longValue() < p2.getIndex().longValue()) {
            return -1;
        } else if (p1.getIndex().longValue() > p2.getIndex().longValue()) {
            return 1;
        }
        return 0;
    }

}
