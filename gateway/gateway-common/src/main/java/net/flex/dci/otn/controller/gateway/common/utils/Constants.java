/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.topology.type.ViewTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.topology.type.OchTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;

/**
 * CONSTANTS FOR THE GATEWAY
 *
 * @date: 2021/3/24
 */
public class Constants {

    public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36";

    public final static String RPC_URL_PREFIX = "/restconf/operations/";

    public final static String OPERATIONAL_URL_PREFIX = "/restconf/operational/";

    public final static String CONFIG_URL_PREFIX = "/restconf/config/";

    public final static String BLANK = "";

    public final static String CHARSET_UTF8 = "UTF-8";

    public final static String HTTP_PREFIX = "http://";

    public final static String NMS_PREFIX = "nms:";

    public final static String TENCENT_PREFIX = "tencent-idc:";

    public final static String JSON_CONTENT_TYPE = "application/json;charset=utf-8";

    public final static String SITE_TOPO_KEY = SiteTopology.QNAME.getLocalName();

    public final static String OCH_TOPO_KEY = OchTopology.QNAME.getLocalName();

    public final static String PHY_TOPO_KEY = OtnPhyTopology.QNAME.getLocalName();

    public final static String TUNNEL_TOPO_KEY = SiteTopology.QNAME.getLocalName() + "/TUNNEL";

    public final static String SITE_VIEW_TOPO_KEY = "site-" + ViewTopology.QNAME.getLocalName();

    public final static String API_VERSION = "api-version";

    public final static String REGION_ID = "regionId";

    public final static String CONTROLLER_VERSION = "controller-version";

    public final static String SUPPORT_ADAPTER_VERSION = "supported-adapter-api-version";

    public final static String EQUIP = "EQUIP";

    public final static String NODE = "NODE";

    public final static String SUCCESS_CODE = "success";


    public final static String SOURCE_TP_NAME = "source-tp-name";

    public final static String DEST_TP_NAME = "dest-tp-name";

    public final static String SOURCE_NODE_NAME = "source-node-name";

    public final static String DEST_NODE_NAME = "dest-node-name";

    public final static String SOURCE_SITE_NAME = "source-site-name";

    public final static String DEST_SITE_NAME = "dest-site-name";

    public final static String YANG_VERSION = "yang-version";

    public final static String DOMAIN_NAME = "domain-name";

    public final static String AZ_ACTIVE = "az-active";

    public final static String ZA_ACTIVE = "za-active";

    public final static String COLLECTOR_ID = "collector-id";

    public final static String ADAPTER_ID = "adapter-id";

    public final static String SITE_NAME = "site-name";

    public final static String SITE_LINK_NAME = "site-link-name";

    public final static String HTTPS = "https";

    public final static String HTTP = "http";

    public final static String WEBSOCKET = "WebSocket";

    public final static String TARGET_INSTANCE_HEADER = "target-instance";

}
