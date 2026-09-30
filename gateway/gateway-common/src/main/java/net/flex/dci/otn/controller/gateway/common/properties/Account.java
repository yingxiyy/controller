/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: xinyzhao
 * @date: 2021/3/23
 */
@Component
@Data
@ConfigurationProperties(prefix = "author")
public class Account {

    private String account;

    private String password;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("account:").append(account);
        return sb.toString();
    }
}
