/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.util;

public class LinkFriendlyName {

    public static String creaateSiteLinkFriendlyName(String siteLinkId, int sequence)
            throws Exception {
        //源园区名称-目的园区名称-平面-序号
        String friendlyName = "";
        return friendlyName;
    }

    public static String creaateSiteLinkFriendlyName(String srcCampus, String desCampus,
            String plane, int sequence) {
        //源园区名称-目的园区名称-平面-序号
        String friendlyName = srcCampus + "-" + desCampus + "-" + plane + "-" + sequence;
        return friendlyName;
    }

    public static String creaatePhyLinkFriendlyName(String phyLinkId) throws Exception {
        //源园区名称-目的园区名称-平面-序号-主用/备用-光纤序号
        String friendlyName = "";
        return friendlyName;
    }

    public static String creaatePhyLinkFriendlyName(String siteLinkFriendlyName, String routeName,
            int sequence) {
        //源园区名称-目的园区名称-平面-序号-主用/备用-光纤序号
        String friendlyName = siteLinkFriendlyName + "-" + routeName + "-" + sequence;
        return friendlyName;
    }

    public static String creaateTunnelFriendlyName(String tunnelId) throws Exception {
        //厂商-源园区-宿园区-DWDM-序号-H-A平面
        String friendlyName = "";
        return friendlyName;
    }

    public static String creaateTunnelFriendlyName(String vendorName, String srcCampus,
            String desCampus, String tunnelGe, String plane, int sequence) {
        //厂商-源园区-宿园区-DWDM-序号-H-A平面
        String friendlyName = vendorName + "-"
                + srcCampus + "-"
                + desCampus + "-"
                + Constant.Tunnel.DWDM + "-"
                + sequence + "-"
                + tunnelGe + "-"
                + Constant.Tunnel.ROUTE + plane;
        return friendlyName;
    }

    public static String creaateNeFriendlyName(String neId) throws Exception {
        //机房编码-机架编号-设备简称-角色-子架序号
        String friendlyName = "";
        return friendlyName;
    }

    public static String creaateNeFriendlyName(String unitCode, String vendorMinName, String neRole,
            int sequence) {
        //机房编码-机架编号-设备简称-角色-子架序号
        long dt = System.currentTimeMillis();
        String seq = "";
        if (sequence < 10) {
            seq = "0" + String.valueOf(sequence);
        } else {
            seq = String.valueOf(sequence);
        }
        String friendlyName =
                unitCode + "-" + Constant.RACK_NUMBER + "-" + vendorMinName + "-" + neRole + "-"
                        + seq + "-" + String.valueOf(dt);
        return friendlyName;
    }
}
