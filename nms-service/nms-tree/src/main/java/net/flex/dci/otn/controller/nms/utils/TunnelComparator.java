/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import java.util.Comparator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * @author: xinyzhao
 * @date: 2021/4/9
 */
public class TunnelComparator implements Comparator<Tunnel> {

    @Override
    public int compare(Tunnel t1, Tunnel t2) {
        String freq1 = this.getFrequency(t1);
        String freq2 = this.getFrequency(t2);
        if (freq1.compareTo(freq2) == 0) {
            return t1.getTunnelId().getValue().compareTo(t2.getTunnelId().getValue());
        } else {
            return freq1.compareTo(freq2);
        }
    }

    private String getFrequency(Tunnel tunnel) {
        Properties properties = tunnel.getProperties();
        if (properties != null && properties.getProperty() != null) {
            for (Property property : properties.getProperty()) {
                if (property.getName().equals("frequency")) {
                    return property.getValue();
                }
            }
        }
        return "";
    }


}