/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.namingrule;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.allocate.common.util.Constant;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
public class SiteNodeFriendlyName {

    @Autowired
    private SiteNodeDao siteNodeDao;

    /**
     * @param friendlyName
     * @throws CommonException
     */
    public void checkFridendlyName(String friendlyName) throws CommonException {
        if (friendlyName == null || "".equals(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "site friendly-name cannot be null");
        }
        if (friendlyName.length() > Constant.friendlyNameLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "site friendly-name should be smaller than 64 characters");
        }
        if (CommonUtil.checkSpace(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "friendlyName can't contain space");
        }

        if (siteNodeDao.existsNodeFriendlyName(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("site friendly-name %s already exists",
                            friendlyName));
        }
    }


    /**
     * 整形字符串，生成最大值+1 （第一个rack 就是0+1 = 1）
     * 用这个值生成唯一的siteLinkID
     * @return
     */
    public static synchronized String getRackGlbalID(Node siteNode) {
        Site siteAttr = siteNode.getAugmentation(Node1.class).getSite();
        if (siteAttr.getSupportingRack() == null || siteAttr.getSupportingRack().isEmpty()) {
            return "1";
        } else {
            int max = 0;
            for (SupportingRack rack : siteAttr.getSupportingRack()) {
                try {
                    int id = Integer.valueOf(rack.getGlobalIdentify());
                    if (id > max) {
                        max = id;
                    }
                } catch (NumberFormatException e) {
                    max=1;
                }
            }
            return "" + (max + 1);
        }
    }
}
