/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import java.util.EnumSet;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.topology.type.ViewTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.topology.type.OchTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
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

    public final static String VERTICAL_LINE = "|";

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

    public final static String SUCCESS = "success";

    public final static Integer SUCCESS_CODE = 200;


    public final static String SOURCE_TP_NAME = "source-tp-name";

    public final static String DEST_TP_NAME = "dest-tp-name";

    public final static String SOURCE_TP_ID = "source-tp-id";

    public final static String DEST_TP_ID = "dest-tp-id";

    public final static String SOURCE_NE_ID = "source-node-id";

    public final static String DEST_NE_ID = "dest-node-id";

    public final static String SOURCE_SITE_ID = "source-site-id";

    public final static String DEST_SITE_ID = "dest-site-id";

    public final static String SOURCE_NODE_NAME = "source-node-name";

    public final static String DEST_NODE_NAME = "dest-node-name";

    public final static String SOURCE_SITE_NAME = "source-site-name";

    public final static String DEST_SITE_NAME = "dest-site-name";

    public final static String YANG_VERSION = "yang-version";

    public final static String YANG_MODEL = "yang-model";

    public final static String DEFAULT_BAND = "C";
    /**
     * site link properties
     */
    public final static String DOMAIN_NAME = "domain-name";

    public final static String AZ_ACTIVE = "az-active";

    public final static String ZA_ACTIVE = "za-active";

    public final static String ZA_DELAY = "za-delay";

    public final static String AZ_DELAY = "az-delay";

    public final static String NONE = "--";

    public final static String DEFAULT_ACTIVE_PATH = "PRIMARY";

    public final static String COLLECTOR_ID = "collector-id";

    public final static String ADAPTER_ID = "adapter-id";

    public final static String SITE_NAME = "site-name";

    public final static String SITE_ID = "site-id";

    public final static String CURRENT_SOFTWARE_VERSION = "software-version";

    public final static String SITE_LINK_NAME = "site-link-name";


    public static final String SITE_RACK = "SITE_RACK";

    public static final String SITE_NODE = "SITE_NODE";

    public static final String PHY_NODE = "PHY_NODE";

    public static final String PHY_TP = "PHY_TP";

    public static final String SITE_TP = "SITE_TP";

    public static final String PHY_LINK = "PHY_LINK";

    public static final String SITE_LINK = "SITE_LINK";

    public static final String OCH_LINK = "OCH_LINK";

    public static final String SITE_TUNNEL = "SITE_TUNNEL";

    public static final String PHY_EQUIPMENT = "PHY_EQUIPMENT";

    public static final String MPO_SUFFIX_REGEX = ".*MPO\\d*$";

    public static final String MPO_SUFFIX = "MPO";

    public static final String MPO_FIST_INDEX = "1";

    public static final int MPO_PORT_SIZE = 8;

    public static final String MPO = "MPO";

    public static final String SITE_LINK_PREFIX = "SiteLink";

    public static final String PORT = "PORT";

//    public static final String MD_SUFFIX = "D";


    public static final String NMS_RPC_PREFIX = "";

    public static final String COMMA = ",";

    public static final String VIRTUAL_PLANE = "virtual-site-Link";

    public static final String REF_BLANK = "--";


    public final static String FREQUENCY_FREE = "free";

    public final static String FREQUENCY_BUSY = "busy";

    public final static String LOGIC_DESCRIPTION_XC = "logical cross connection";

    /**
     * card type
     */

    public final static String MUX = "MUX";

    public final static String MUX_SPECTRUM_96 = "96";
    public final static String MUX_SPECTRUM_48 = "48";

    public final static String MUX_SPECTRUM_64 = "64";

    public final static String MUX_SPECTRUM_FLEX = "PANEL";
    public final static String MUX_SPECTRUM_FLEX_32CL = "MUX32CL";
    public final static String MUX_SPECTRUM_FLEX_32C32L = "32C32L";
    public final static String MUX_SPECTRUM_FLEX_PB64 = "PB64";
    public final static String MUX_SPECTRUM_PMUX64 = "PMUX64";
    public final static String MUX_SPECTRUM_PMUXC64 = "PMUXC64";

    public final static String WSS = "WSS";

    public final static String XC_PREFIX = "XC-";

    public final static String SITE_LINK_MODE = "model";

    public final static String PROPERTY_SOURCE_SITE_TYPE = "sourceSiteNodeType";

    public final static String PROPERTY_DEST_SITE_TYPE = "destinationSiteNodeType";

    public final static String WSS_SIG_PORT = "SIG";

    public final static String SIG_PORT_SUFFIX = "SIG";

    public final static String LINE_PORT_SUFFIX = "LINE";

    public final static String Parentheses_RIGHT = ")";

    public final static String Parentheses_LEFT = "(";

    public final static String LINK_DESTINATION_NODE_PATH = "destination#dest-node";

    public final static String LINK_SOURCE_NODE_PATH = "source#source-node";

    public final static String TUNNEL_DESTINATION_TP_PATH = "destination-tp#tp-ref";

    public final static String TUNNEL_SOURCE_TP_PATH = "source-tp#tp-ref";

    public final static String LINK_DESTINATION_TP_PATH = "destination#dest-tp";

    public final static String LINK_SOURCE_TP_PATH = "source#source-tp";

    public final static String UNKNOWN = "unknown";

    public final static String DEFAULT = "DEFAULT";

    public static final Set<EquipType> MUX_TYPES =
            EnumSet.of(EquipType.MUXPANEL, EquipType.MUX32CL, EquipType.MUX);

    public static final String DGE_TYPE_PREFIX = "DGE_";

    public static final String IRA_TYPE_PREFIX = "IRA_";

    public static final String MUX32CL_TYPE_PREFIX = "MUX32CL_";

    public static final String WSS_TYPE_PREFIX = "WSS_";
    public static final String RESOURCE_REMOVE = "DELETED.";

    public static final String PORT_INFIX = "PORT-";
    public static final String CHASSIS_INFIX = "CHASSIS-";
    public static final String TRANSCEIVER_INFIX = "TRANSCEIVER-";

    public static final String WEST_SUFFIX = "_WEST";

    public static final String EAST_SUFFIX = "_EAST";

    public static final String PORT_IN_SUFFIX = "-IN";

    public static final String PORT_OUT_SUFFIX = "-OUT";

    public static final String PORT_IN_ID_SUFFIX = "#IN";

    public static final String PORT_OUT_ID_SUFFIX = "#OUT";

    public static final String ASE_PREFIX = "ASE:";

    public static final String ASE_CROSS_CONNECTION_PREFIX = "ASEXC";

    public static final String MUXPANEL32C32L = "MUXPANEL32C32L";

    public static String MD_PORT_PATTERN = "D(\\d+)$";

    public static String EXP = "EXP";


    public static final class WSS_PROPERTIES {

        public static final String CHANNEL_INDEX = "channel-index";
        public static final String ASE_CONTROL_MODE = "ase-control-mode";
        public static final String ASE_INJECTION_THRESHOLD = "ase-injection-threshold";
        public static final String SOURCE_TO_DEST_POWER_CONTROL_MODE = "source-to-dest-power-control-mode";
        public static final String DEST_TO_SOURCE_POWER_CONTROL_MODE = "dest-to-source-power-control-mode";
        public static final String ASE_INJECTION_HYSTERESIS = "media-channel-injection-hysteresis";
        public static final String TARGET_DEST_PORT_OUTPUT_OPTICAL_POWER = "target-dest-port-output-optical-power";
        public static final String TARGET_SOURCE_PORT_OUTPUT_OPTICAL_POWER = "target-source-port-output-optical-power";
        public static final String AUTO_CONTROL_ACTIVE_THRESHOLD_DEST = "auto-control-active-threshold-dest";
        public static final String AUTO_CONTROL_ACTIVE_THRESHOLD_SOURCE = "auto-control-active-threshold-source";
        public static final String DEST_CALIBRATION_OPTICAL_POWER = "calibrated-dest-port-output-optical-power";

        public static final String SOURCE_CALIBRATION_OPTICAL_POWER = "calibrated-source-port-output-optical-power";
    }


    public static final class Plane {

        public static final String PHY_NODE_FILTER_PLANE_PATH = "physical#plane-id";

        public static final String TUNNEL_FILTER_PLANE_PATH = "plane-id";

        public static final String SITE_LINK_FILTER_PLANE_PATH = "site#plane-id";

        public static final String PHY_LINK_FILTER_PLANE_PATH = "physical#plane-id";

        public static final String OCH_LINK_FILTER_PLANE_PATH = "och#plane-id";
    }
}
