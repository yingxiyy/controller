package net.flex.dci.otn.controller.implement.physical.util;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.SuccessObj;

/**
 * 2025/9/5
 *
 * @author musa
 * @version 1.0
 **/
public class PhysicalUtils {

    public static String convertSuccessObj(SuccessObj successObj) {
        StringBuilder sb = new StringBuilder();
        if (successObj != null && successObj.getObject() != null) {
            successObj.getObject().forEach(obj -> {
                sb.append(obj.getObjectType().name()).append(": ");
                sb.append(obj.getObjectId()).append(", ");
                sb.append(obj.getMessageInfo()).append("\n");
            });
        }
        return sb.toString();
    }


    public static String convertFailObj(FailObj failObj) {
        StringBuilder sb = new StringBuilder();
        if (failObj != null && failObj.getObject() != null) {
            failObj.getObject().forEach(obj -> {
                sb.append(obj.getObjectType().name()).append(": ");
                sb.append(obj.getObjectId()).append(", ");
                sb.append(obj.getMessageInfo()).append("\n");
            });
        }
        return sb.toString();
    }

    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return isValidIpv4(ip) || isValidIpv6(ip);
    }

    private static boolean isValidIpv4(String ip) {
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipv4Pattern);
    }

    private static boolean isValidIpv6(String ip) {
        String ipv6Pattern = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,7}:$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}$|" +
                "^[0-9a-fA-F]{1,4}:(?::[0-9a-fA-F]{1,4}){1,6}$|" +
                "^:(?::[0-9a-fA-F]{1,4}){1,7}$|" +
                "^::$|" +
                "^::1$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,4}:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";
        return ip.matches(ipv6Pattern);
    }

}
