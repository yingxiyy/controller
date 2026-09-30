package net.flex.dci.otn.controller.nms.nms.component.amplifier;

import net.flex.dci.otn.controller.nms.nms.dto.link.OtsLinkAmplifierRefCache;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkAmplifierCLInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkRamanInfo;

/**
 * 2025/8/11
 *
 * @author musa
 * @version 1.0
 **/
public interface AmplifierHandler {

    OtsLinkAmplifierCLInfo getOtsLinkSourceBACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache);

    OtsLinkAmplifierCLInfo getOtsLinkDestBACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache);

    OtsLinkAmplifierCLInfo getOtsLinkSourcePACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache);

    OtsLinkAmplifierCLInfo getOtsLinkDestPACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache);

    OtsLinkRamanInfo getOtsLinkRamanInfoByTp(String ramanTpId,
            OtsLinkAmplifierRefCache refCache);


}
