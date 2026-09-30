/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.flex.dci.otc.zk.common.entity.ServerInfo;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;

public class MainCache {

    public static final Map<String, ServerInfo> deadServerMap = new ConcurrentHashMap<>();
  
    public static final Map<String, AlarmRecord> alarmMap = new ConcurrentHashMap<>();

    private MainCache() {
        super();
    }
}