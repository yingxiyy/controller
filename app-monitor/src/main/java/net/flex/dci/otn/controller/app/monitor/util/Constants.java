/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.util;

import java.util.Arrays;
import java.util.List;

/**
 * @version 1.0
 * @date 2021/12/20 14:05
 */
public class Constants {

    public static final String ADAPTER = "deviceMapper";

    public static final List<String> ADAPTER_TYPES = Arrays.asList("deviceMapper", "adapter");

    public static final String COLLECTOR = "telemetryServer";

    public static final String GATEWAY = "gateway";

    public static final String TELEMETRY_SERVER_PREFIX = "telemetryServer_";

    public static final String DISK_USAGE = "SYSTEM_DISK";


    public static final String SYSTEM_MONITOR_DISK_USAGE = "SYSTEM_MONITOR_DISK_USAGE";

    public static final double DISK_USAGE_THRESHOLD = 0.8 * 100;

    public static final String SERVER_PREFIX = "SERVER_";

    public static final String SERVER_NTP_OOS_GROUP = "SERVER_NTP_OOS";
}
