/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.network.template.NetworkData;
import net.flex.dci.otn.controller.allocate.network.template.TemplateRequest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkOutput;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefinedNetworkCreator {

    public CreateNetworkOutput doIt(TemplateRequest templateRequest) {

        NetworkData networkData = getNetworkInputData(templateRequest);

        return null;
    }

    private NetworkData getNetworkInputData(TemplateRequest templateRequest) {
        return null;
    }
}
