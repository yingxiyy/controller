package net.flex.dci.otn.controller.app.monitor.util;

import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.COLON;

/**
 *
 * 2025/12/7
 *
 * @author musa
 * @version 1.0
 **/
public class AppMonitorUtils {

    public static String diskMountKeyGenerate(String hostname, String mountPoint) {
        return hostname + COLON + mountPoint;
    }

    public static String generateAlarmIdForUsage(String hostname, String mountPoint) {
        String safeMountPoint = mountPoint.replace("/", "_")
                .replace(".", "_")
                .replace(" ", "_");

        // 格式：DISK_{模块简写}_{主机名}_{挂载点}_{时间戳}
        // 例如：DISK_SYS_svr01_data_1672501234567
        return String.format("DISK_%s_%s",//
                hostname.replace(".", "_"),
                safeMountPoint);
    }

}
