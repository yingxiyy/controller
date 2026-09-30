/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.core.enrich;

import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.nes.top.nes.Ne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NeEnrichment {

    @Autowired
    private FriendlyNameEnrich friendlyNameEnrich;

    /**
     * eml ne the real ne from adapter
     *
     * neId,
     *
     * @param neId
     * @param emlNe
     * @return
     */
    public Ne enrichNe(String neId, Ne emlNe) {
        Ne ne = friendlyNameEnrich.enrich(neId, emlNe);
        return ne;
    }

}
