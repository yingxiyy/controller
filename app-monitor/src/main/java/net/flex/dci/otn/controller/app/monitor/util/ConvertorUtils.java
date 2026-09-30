/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.util;

import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;
import net.flex.dci.otc.zk.common.entity.TimeInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.WorkingStatus;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.system.info.output.Server;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.system.info.output.ServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.system.info.output.ServerKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.version.output.Apps;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.version.output.AppsBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.version.output.AppsKey;

/**
 * @version 1.0
 * @date 2021/12/17 10:18
 */
public class ConvertorUtils {

    public static List<Apps> convertIns2ListApps(List<InstanceInfo> details) {
        return details.stream().map(ConvertorUtils::convertIns2Apps).collect(Collectors.toList());
    }

    public static Apps convertIns2Apps(InstanceInfo info) {
        AppsBuilder builder = new AppsBuilder();
        builder.setKey(new AppsKey(info.getId()));
        builder.setAppName(info.getId());
        InstanceDetails detail = info.getData();
        builder.setAppType(detail.getModule());
        builder.setRunningIp(detail.getMyIp());
        builder.setHostIp(detail.getHostIp());
        builder.setExternalIp(detail.getExternalIp());
        builder.setWorkingStatus(info.isAlive() ? WorkingStatus.Running : WorkingStatus.Dead);
        builder.setAppVersion(detail.getSwVersion());
        builder.setNbiVersion(detail.getNbiVersion());
        builder.setSupportedSbiVersion(detail.getSbiVersion());
        builder.setIsCompatible(info.getCompatible());
        builder.setRestart(info.getRecoverTimes());
        builder.setAges(info.isAlive() ? Math.toIntExact(duration(info.getDuration())) : 0);
        builder.setGitTag(detail.getTagInfo());
        return builder.build();
    }

    public static List<Server> convertIns2ServerList(List<InstanceDetails> details) {
        return details.stream().map(ConvertorUtils::convertIns2Server).collect(Collectors.toList());
    }

    private static Server convertIns2Server(InstanceDetails detail) {
        TimeInfo timeInfo = detail.getTimeInfo();
        ServerBuilder builder = new ServerBuilder();
        builder.setServerName(detail.getId())
                .setKey(new ServerKey(detail.getId()))
                .setIpAddress(detail.getMyIp())
                .setCurrentDataTime(timeInfo.getCurrentDataTime())
                .setCurrentNtpServer(timeInfo.getCurrentNtpServer())
                .setNtpSynchronized(timeInfo.getNtpSynchronized())
                .setTimezone(timeInfo.getTimezone());
        return builder.build();
    }


    private static Long duration(Long duration) {
        return duration / (3600000L);
    }

}
