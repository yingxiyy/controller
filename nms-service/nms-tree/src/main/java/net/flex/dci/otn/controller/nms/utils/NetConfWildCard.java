/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

/**
 * netconf api wild card
 *
 * @date: 2021/3/26
 */
public class NetConfWildCard {

    public final static String RPC_REQUEST_WILDCARD = "/restconf/operations/**";

    public final static String NETWORK_TOPOLOGY_OPERATIONAL_WILDCARD = "/restconf/operational/network-topology:network-topology/**";

    public final static String NETWORK_TOPOLOGY_CONFIG_WILDCARD = "/restconf/config/network-topology:network-topology/**";

    public final static String NMS_RPC_REQUEST_WILDCARD = "/restconf/operations/nms:**";

    public final static String DRUID_WILDCARD = "/druid/**";

}
