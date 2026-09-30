/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.service;

import java.util.List;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;

/**
 * @version 1.0
 * @date 2021/12/17 10:06
 */
public interface IAppMonitorService {

    String getSystemInfo();

    String getModuleVersion();

    void removeModuleInstance(List<InstanceInfo> instanceInfos);

    void checkStartupModuleStatus();
}
