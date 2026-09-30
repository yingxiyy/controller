/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.properties;

import net.flex.dci.otn.controller.gateway.common.properties.settings.LoginInfo;
import net.flex.dci.otn.controller.gateway.common.properties.settings.Ntp;
import net.flex.dci.otn.controller.gateway.common.properties.settings.Radius;
import net.flex.dci.otn.controller.gateway.common.properties.settings.Telemetry;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Syslog;

/**
 * @author: xinyzhao
 * @date: 2021/4/12
 */
@Data
public class OpenNeDefaultSetting implements Serializable {

    private List<Ntp> ntp;

    private java.util.List<Syslog> syslog;

    private List<Radius> radius;

    private List<Telemetry> telemetry;

    private String timezone;

    private List<LoginInfo> loginInfo;
    
}
