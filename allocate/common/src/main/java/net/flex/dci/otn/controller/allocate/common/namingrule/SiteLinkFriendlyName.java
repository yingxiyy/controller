/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.namingrule;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.common.util.Constant;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.springframework.stereotype.Component;

@Slf4j
@Component
/**
 * @author YYX
 * @version 1.0
 */
public class SiteLinkFriendlyName {
    //源site名称-目的site名称-平面-序号

//    public String buildFriendlyName(CreateSiteLinkParam param) {
//        log.debug("start update siteLink friendlyName");
//        String friendlyName = "";
//        friendlyName = generateFriendlyName(param.getSrcSiteNode(), param.getDstSiteNode(), param.getRiskGroupName(), param.getPlaneName());
//
//        return friendlyName;
//    }

    public String buildFriendlyName(Node srcNode, Node dstNode, String riskGroup, String plane) {
        log.debug("start update siteLink friendlyName");
        String idA = SiteNodeFriendlyName.getRackGlbalID(srcNode);
        String idZ = SiteNodeFriendlyName.getRackGlbalID(dstNode);

        String srcName = srcNode.getAugmentation(Node1.class).getSite().getFriendlyName();
        String dstName = dstNode.getAugmentation(Node1.class).getSite().getFriendlyName();

        String suffix = generateLinkIndex(idA, idZ);
        return generateFriendlyName(srcName, dstName, plane, suffix);
    }

    private String generateFriendlyName(String srcName, String dstName, String planeName, String suffix) {
        //源site名称-目的site名称-平面-序号
        String friendlyName = getFixPart(srcName, dstName, planeName) + "-" + suffix;
        return friendlyName;
    }

    private String getFixPart(String srcName, String dstName, String planeName) {
        return srcName + "-" + dstName + "-" + planeName;
    }

    private String generateLinkIndex(String idA, String idZ) {
        return String.format("(%s--%s)", idA, idZ);
    }

    private static String creaatePhyLinkFriendlyName(String phyLinkId) {
        //源园区名称-目的园区名称-平面-序号-主用/备用-光纤序号
        String friendlyName = "";
        return friendlyName;
    }

    private static String creaatePhyLinkFriendlyName(String siteLinkFriendlyName, String routeName,
            int sequence) {
        //源园区名称-目的园区名称-平面-序号-主用/备用-光纤序号
        String friendlyName = siteLinkFriendlyName + "-" + routeName + "-" + sequence;
        return friendlyName;
    }

    private static String creaateTunnelFriendlyName(String tunnelId) {
        //厂商-源园区-宿园区-DWDM-序号-H-A平面
        String friendlyName = "";
        return friendlyName;
    }

    private static String creaateTunnelFriendlyName(String vendorName, String srcCampus,
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

    private static String creaateNeFriendlyName(String neId) {
        //机房编码-机架编号-设备简称-角色-子架序号
        String friendlyName = "";
        return friendlyName;
    }

    private static String creaateNeFriendlyName(String unitCode, String vendorMinName, String neRole,
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
