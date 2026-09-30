package net.flex.dci.otn.controller.db.monitor.utils;

import java.util.Arrays;
import java.util.List;

/**
 * @version 1.0
 * @date 2021/11/4 13:27
 */
public class Constants {

    public final static String COLLECTION = "collection";

    public final static String DIFF_VERSION_KEY = "$v";

    public final static String DIFF_KEY = "diff";

    public final static String OP_SET = "$set";

    public final static String OP_UNSET = "$unset";

    public final static String DELETE_OP = "delete";

    public final static String DATA = "data";

    public final static String REMOVE_ATTRIBUTES = "removed-attributes";

    public final static String ID = "id";

    public final static String PROPERTY = "property";

    public final static String PROPERTY_VALUE = "property";

    public final static String VALUE = "value";

    public final static String NAME = "name";

    public final static String ADAPTER_ID = "adapterId";

    public final static String TELEMETRY_SERVER_ID = "telemetryId";

    public final static String NODE_ID = "node-id";

    public final static String LINK_ID = "link-id";

    public final static String TERMINATION_POINT_ID = "tp-id";

    public final static String EQUIPMENT_ID = "equipment-id";

    public final static String TUNNEL_ID = "tunnel-id";

    public final static String NE_ID = "neId";

    public final static String NONE = "--";

    public final static String ATTRIBUTE_IP = "ip";

    //chang body root key
    public final static String PHYSICAL = "physical";

    public final static String SITE_ROOT = "site";

    public final static String TUNNEL_ROOT = "";

    public final static String TUNNEL_CONTAINER = "tunnel";

    public final static String LINK_CONTAINER = "link";

    public final static String SITE_LINK_ATTRIBUTE_CONTAINER = "site-topology:site";

    public final static String PHY_LINK_ATTRIBUTE_CONTAINER = "otn-phy-topology:physical";

    public final static String TP_ATTRIBUTE_CONTAINER = "otn-phy-topology:physical";

    public final static String SUPPORTING_LINK = "supporting-link";

    public final static String SITE_LINK_CONTAINER = "site";

    public final static String PHY_LINK_CONTAINER = "physical";

    public final static String FRIENDLY_NAME = "friendly-name";

    public final static String PORT_IN_ID_SUFFIX = "#IN";

    public final static String PORT_OUT_ID_SUFFIX = "#OUT";


    public final static String OTU_LINE_KEY = "otu-line";

    public final static String MODEL_SPEC_KEY = "model-spec";

    public static final String SERVICE_TYPE_KEY = "service-type";

    public static final String REG_PORT_PREFIX = "REGEN_";

    public static class ImplementMonitor {

        public static final String ON_IMPLEMENT_STATE_DIRECT_PATH = "data.node.0.otn-phy-topology:physical.implement-state";

        public static final String ON_IMPLEMENT_STATE_LEVEL_SECONDARY_PATH = "data.node.0.otn-phy-topology:physical";

        public static final String ON_IMPLEMENT_STATE_LEVEL_TOP_NUMBER_PATH = "data.node.0";

        public static final String ON_IMPLEMENT_STATE_LEVEL_TOP_PATH = "data";

        public static final String IMPLEMENT_STATE = "implement-state";

        public static final String PHY_NODE_PHYSICAL = "otn-phy-topology:physical";

        public static final String NE_NOTIFY_TYPE = "NE_STATE_CHANGE";
    }

    public static class StatusEvents {

        public static final String OPERATIONAL_STATE = "operational-state";
        public static final String ADMIN_STATE = "admin-state";

        public static final String[] PHY_PHYSICAL_KEY = {"physical",
                "otn-phy-topology:physical"};

        public static final String ALIGNMENT_STATUS = "alignment-status";
    }

    public static class CrossConnectionConstants {

        public static final String APS_CONTAINER_KEY = "aps";

        public static final String CROSS_CONNECTION_ID = "cross-connection-id";

        public static final String APS_ACTIVE_PATH = "active-path";

        public static final String NODE_REF = "node-ref";

    }

    public static class RouteConstants {

        public static final String EXPLICIT_ROUTE = "explict-route";
    }

    public static final List<String> CACHE_RELEVANT_KEYS = Arrays.asList(
            FRIENDLY_NAME,     // "friendly-name"
            "properties",      //
            "node-type",       //
            "customed-type"    //
    );

    public static final String PROPERTIES = "properties";

    public static final String SOFTWARE_VERSION = "software-version";


    public static final String DB_CHANGE_TOPIC = "db-change";
}
