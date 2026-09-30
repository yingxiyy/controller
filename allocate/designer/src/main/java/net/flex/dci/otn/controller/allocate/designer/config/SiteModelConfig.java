/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.config;


import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.OcmGridGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SiteModelConfig {

    @Autowired
    private SiteModelConfigC siteModelConfigC;
    @Autowired
    private SiteModelConfigCL siteModelConfigCL;


    private SiteModelConfigInterface getSiteModelConfig(WDM_Band wdmBand) {
        if (wdmBand.equals(WDM_Band.C)) return siteModelConfigC;
        return siteModelConfigCL;
    }

    /**
     * The card must be in route order
     *
     * @param nodeType
     * @param grid
     * @param isProtected
     * @param linkModel
     * @return
     */
    public List<String> getMainCardTypes(String nodeType, Integer grid, @NonNull Boolean isProtected, @NonNull String linkModel, WDM_Band wdmBand) throws NeDesignerException {

        return getSiteModelConfig(wdmBand).getMainCardTypes(nodeType, grid, isProtected, linkModel);
    }


    public List<String> getSlaveCardTypes(String nodeType, Integer grid, Boolean isProtected, @NonNull String linkModel, WDM_Band wdmBand) {
        return getSiteModelConfig(wdmBand).getSlaveCardTypes(nodeType, grid, isProtected, linkModel);

    }

    public OcmGridGroup getOcmGridGroup(Integer gridInput) throws NeDesignerException {
        return null;//目前ocm不放在allocate这里配置，但是接口先留着

    }
}
