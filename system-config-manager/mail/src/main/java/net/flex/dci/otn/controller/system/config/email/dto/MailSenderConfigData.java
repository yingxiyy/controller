/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.Builder.Default;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailSenderConfigData {

    @NonNull
    private String smtpHost;
    @NonNull
    private Integer smtpPort;
    @NonNull
    private Boolean enableSSL;
    @NonNull
    private String user;
    @NonNull
    private String password;


    @Default
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private int timeOut = 5000;

    @Default
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean enableDebug = false;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String proxyHost;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String proxyPort;
}
