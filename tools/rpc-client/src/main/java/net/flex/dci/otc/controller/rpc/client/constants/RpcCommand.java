package net.flex.dci.otc.controller.rpc.client.constants;

/**
 * @version 1.0
 * @date 2022/9/19 12:54
 */
public class RpcCommand {


    public static class AllocatorRpcCmd {

        // allocate rpc command
        public static final String CREATE_SITE_LINK = "site-topology:create-link";

        public static final String REMOVE_SITE_LINK = "site-topology:remove-link";

        public static final String REMOVE_TUNNEL = "tunnel:remove-tunnel";

        public static final String CREATE_TUNNEL = "tunnel:create-tunnel";
    }


    public static class AdapterRpcCmd {

        //adapter rpc
        public static final String GET_MANAGED_NES = "get-managed-nes";

        public static final String REMOVE_NE = "remove-ne";

        public static final String CONNECT_NE = "connect-ne";

        public static final String REFRESH_ALARM = "refresh-alarm";

        public static final String COMPARE_NE = "compare-ne";

        public static final String GET_NE_DATA = "get-ne-data";

        public static final String SYNC_NE_DATA = "sync-ne-data";

        public static final String CONFIG_NE = "config-ne";

        public static final String GET_NE = "nes/ne/";

        public static final String REMOVE_RESOURCE = "remove-resource";

        public static final String TEST_NE_CONNECTION = "test-ne-connection";

        public static final String REPORT_1524_TELEMETRY_DATA = "report-1524-telemetry-data";
    }


    public static class AdapterManagerRpcCmd {

        public static final String CREATE_ADAPTER = "create-adapter";

        public static final String DELETE_ADAPTER = "delete-adapter";
    }


    public static class TelemetryManagerRpcCmd {

        public final static String CREATE_TELEMETRY_SERVER = "telemetry-manager:create-telemetry-server";

        public final static String DELETE_TELEMETRY_SERVER = "telemetry-manager:delete-telemetry-server";
    }


    public static class ImplementRpcCmd {

        public static final String UPDATE_TUNNEL = "tunnel:update-tunnel";

        public static final String UPDATE_LINK = "site-topology:update-link";

        public static final String UPDATE_TUNNEL_SYNC = "tunnel:update-tunnel-sync";

        public static final String UPDATE_SITE_LINK_SYNC = "site-topology:update-sitelink-sync";
    }


    public static class AlarmRpcCmd {

        public static final String GENERATE_ALARM = "generate-alarm";

        public static final String CLEAR_ALARM = "clear-alarm";

        public static final String GET_CURRENT_ALARMS = "get-current-alarms";
    }


    public static class NeManagerRpcCmd {

        public static final String MERGE_DATA = "merge-data";
        public static final String MANAGE_NE = "manage-ne";

        public static final String REGISTER_NE = "registe-ne";

        public static final String CONFIG_NE = "config-ne";

        public static final String BATCH_CONFIG_NE = "batch-config-ne";

        public static final String UPLOAD_NE = "upload-ne";

        public static final String GET_NE_DATA = "eml:get-ne-data";

        public static final String UNREGISTER_NE = "unregiste-ne";

        public static final String REMOVE_RESOURCE = "remove-resource";

        public static final String REPORT_1524_TELEMETRY_DATA = "report-1524-telemetry-data";

        public static final String NE_DATABASE_OPERATE = "ne-database-operate";

        public static final String NE_SOFTWARE_OPERATE = "ne-software-operate";

        public static final String CLEAR_NE_SWITCH_LOGS = "clear-ne-aps-switch-logs";

        public static final String MANAGE_NE_APS_SWITCH = "manage-aps-switch";

        public static final String NE_OPERATION_LINK = "ne-operation-link";

        public static final String SWITCH_CU_ACTIVE_STANDBY = "switch-cu-active-standby";

        public static final String UPLOAD_HISTORY_PM = "upload-history-pm";

        public static final String CHANNEL_ASE_RESTORE = "channel-ase-restore";

        public static final String REASSIGN_NTP_SERVER = "reassign-ntp-server";

        public static final String CONFIG_NORTHBOUND_TELEMETRY = "configure-northbound-telemetry";
    }

    public static class EmlRpcCmd {

        public static final String GET_MANAGED_NES = "get-managed-nes";

        public static final String MERGE_DATA = "merge-data";

        public static final String REMOVE_NE = "remove-ne";

        public static final String CONNECT_NE = "connect-ne";

        public static final String REFRESH_ALARM = "refresh-alarm";

        public static final String COMPARE_NE = "compare-ne";

        public static final String GET_NE_DATA = "get-ne-data";

        public static final String SYNC_NE_DATA = "sync-ne-data";

        public static final String CONFIG_NE = "config-ne";
        public static final String BATCH_CONFIG_NE = "batch-config-ne";
        public static final String GET_NE = "nes/ne/";
        public static final String REMOVE_RESOURCE = "remove-resource";
        public static final String TEST_NE_CONNECTION = "test-ne-connection";
        public static final String REPORT_1524_TELEMETRY_DATA = "report-1524-telemetry-data";
        public static final String NE_DATABASE_OPERATE = "ne-database-operate";
        public static final String NE_SOFTWARE_OPERATE = "ne-software-operate";
        public static final String CLEAR_APS_SWITCH_LOG = "clear-aps-switch-log";

        public static final String APS_SWITCH = "aps-switch";

        public static final String NE_OPERATION_LINK = "ne-operation-link";

        public static final String SWITCH_CU_ACTIVE_STANDBY = "switch-cu-active-standby";
        public static final String UPLOAD_HISTORY_PM = "upload-history-pm";

        public static final String CHANNEL_ASE_RESTORE = "channel-ase-restore";

        public static final String EXECUTE_XML = "execute-xml";

        public static final String REFRESH_DEVICE_ALARM = "refresh-device-alarm";
    }

    public static class FTPRpcCmd {

        public static final String LIST = "list";
        public static final String MKDIR = "mkdir";
        public static final String PUT = "put";
        public static final String DOWNLOAD = "get";
        public static final String RM = "rm";
        public static final String RM_FOLDER = "rm-folder";
    }

}
