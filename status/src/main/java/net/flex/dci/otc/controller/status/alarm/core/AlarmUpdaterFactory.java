/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core;

import net.flex.dci.otc.common.model.type.YangObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AlarmUpdaterFactory {

    @Autowired
    private PhyLinkUpdater phyLinkUpdater;

    @Autowired
    private SiteLinkUpdater siteLinkUpdater;

    @Autowired
    private OchLinkUpdater ochLinkUpdater;

    @Autowired
    private TunnelUpdater tunnelUpdater;

    @Autowired
    private ViewLinkUpdater viewLinkUpdater;

    @Autowired
    private SiteNodeUpdater siteNodeUpdater;

    @Autowired
    private ViewNodeUpdater viewNodeUpdater;

    public AlarmUpdater createAlarmUpdater(YangObjectType yangObjectType) {
        if (yangObjectType == YangObjectType.PhyLink) {
            return phyLinkUpdater;
        } else if (yangObjectType == YangObjectType.SiteLink) {
            return siteLinkUpdater;
        } else if (yangObjectType == YangObjectType.OchLink) {
            return ochLinkUpdater;
        } else if (yangObjectType == YangObjectType.Tunnel) {
            return tunnelUpdater;
        } else if (yangObjectType == YangObjectType.ViewLink) {
            return viewLinkUpdater;
        } else if (yangObjectType == YangObjectType.SiteNode) {
            return siteNodeUpdater;
        } else if (yangObjectType == YangObjectType.ViewNode) {
            return viewNodeUpdater;
        }
        return null;
    }
}
