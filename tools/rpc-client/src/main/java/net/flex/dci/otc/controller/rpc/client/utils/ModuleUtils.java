/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.utils;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.balancer.Balancer;
import net.flex.dci.otc.controller.rpc.client.dto.ModuleCredential;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;

/**
 * @version 1.0
 * @date 2021/8/25 15:00
 */
@Slf4j
public class ModuleUtils {

    private static Balancer moduleBalancer;

    public static void setBalancer(Balancer balancer) {
        moduleBalancer = balancer;
    }

    public static ModuleCredential getCredential(String module) throws Exception {
        List<InstanceDetails> details = DciInstancesUtils.getStateInstancesByModuleName(module);
        if (details != null && !details.isEmpty()) {
            InstanceDetails detail = moduleBalancer.select(details);
            ModuleCredential credential = new ModuleCredential();
            credential.setIp(detail.getExternalIp() != null ? detail.getExternalIp()
                    : detail.getMyIp());
            credential.setPort(detail.getPort());
//            credential.setUserName("admin");
//            credential.setPassword("admin");
            return credential;
        } else {
            return null;
        }

    }


    public static ModuleCredential getCredential(InstanceDetails detail) throws Exception {
        log.debug("get credential details: {}", detail);
        if (detail != null) {
            ModuleCredential credential = new ModuleCredential();
            credential.setIp(detail.getExternalIp() != null ? detail.getExternalIp()
                    : detail.getMyIp());
            Integer httpPort =
                    detail.getHttpPort() == null ? detail.getPort() : detail.getHttpPort();
            credential.setPort(httpPort);
            credential.setUsername(detail.getUser() == null ? null : detail.getUser());
            credential.setPassword(detail.getPasswd() == null ? null : detail.getPasswd());
            return credential;
        } else {
            return null;
        }

    }
}
