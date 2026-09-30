/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network.template;

import static net.flex.dci.otn.controller.allocate.network.template.SiteLinkSheetService.getTpName;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.WssLinks;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RoadmSheetService {

    public SiteLinkRelationSheetData createSheetData(WssLinks wssLink, final Map<String, String> wssEquipMap) {
        String aNeId = wssLink.getSource().getSourceNode().getValue();
        String aTpId = wssLink.getSource().getSourceTp().getValue();
        String aEquipId = PhysicalTpIdNamingRule.getEquipId(aTpId);
        String aEquipSpec = wssEquipMap.get(aEquipId);
        if (aEquipSpec == null) {
            log.error("Invalid compute input for wssLink:{}, because failed to get source equipTypeVendorSpecific for equip:{}", wssLink, aEquipId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Invalid input, failed to get source equipTypeVendorSpecific :%s for wssLink:%s", aEquipId, wssLink.getLinkId().getValue()));
        }
        String aTpName = getTpName(aEquipSpec, aTpId);
        String linkA = wssLink.getSupportingLink().get(0).getLinkRef().getValue();//note: 这里写死第0个，是为了方便处理

        String zNeId = wssLink.getDestination().getDestNode().getValue();
        String zTpId = wssLink.getDestination().getDestTp().getValue();
        String zEquipId = PhysicalTpIdNamingRule.getEquipId(zTpId);
        String zEquipSpec = wssEquipMap.get(zEquipId);
        if (zEquipSpec == null) {
            log.error("Invalid compute input for wssLink:{}, because failed to get dest equipTypeVendorSpecific for equip:{}", wssLink, zEquipId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Invalid input, failed to get dest equipTypeVendorSpecific :%s for wssLink:%s", zEquipId, wssLink.getLinkId().getValue()));
        }
        String zTpName = getTpName(zEquipSpec, zTpId);
        String linkZ = wssLink.getSupportingLink().get(1).getLinkRef().getValue();//note: 这里写死第1个，是为了方便处理

        return SiteLinkRelationSheetData.builder()
                .aNeId(aNeId)
                .aTpName(aTpName)
                .linkA(linkA)
                .zNeId(zNeId)
                .zTpName(zTpName)
                .linkZ(linkZ)
                .build();
    }
}
