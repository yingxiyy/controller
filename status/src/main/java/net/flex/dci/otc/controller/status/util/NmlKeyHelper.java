package net.flex.dci.otc.controller.status.util;

import static net.flex.dci.otc.controller.status.util.Constants.LINE_PORT_REGEX;
import static net.flex.dci.otc.controller.status.util.Constants.LINE_PORT_SUFFIX;
import static net.flex.dci.otc.controller.status.util.Constants.SIG_PORT_REGEX;
import static net.flex.dci.otc.controller.status.util.Constants.SIG_PORT_SUFFIX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.status.dto.nmlkey.NmlKeyDto;
import net.flex.dci.otc.mongo.dto.LinkStateDto;

/**
 * helper method for the nml key get detail info from nml key
 *
 * @version 1.0
 * @date 2022/4/9 17:51
 */
@Slf4j
public class NmlKeyHelper {

    public static NmlKeyDto getDetailInfoFromNmlKey(String nmlKey) {
        NmlKeyDto nmlKeyDto = null;
        String siteNodeId = null;
        String phyNodeId = null;
        String equipId = null;
        String tpId = null;
        String transceiverId = null;
        if (PhysicalEqpIdNamingRule.isTransceiverId(nmlKey)) {
            transceiverId = nmlKey;
            tpId = PhysicalTpIdNamingRule.getPortIdFromTransceiverId(nmlKey);
            phyNodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
            siteNodeId = PhysicalTpIdNamingRule.getSiteId(tpId);
        } else if (PhysicalEqpIdNamingRule.isEquipId(nmlKey)) {
            siteNodeId = PhysicalEqpIdNamingRule.getSiteId(nmlKey);
            phyNodeId = PhysicalEqpIdNamingRule.getNodeId(nmlKey);
            equipId = nmlKey;
        } else if (PhysicalTpIdNamingRule.isTpId(nmlKey)) {
            siteNodeId = PhysicalTpIdNamingRule.getSiteId(nmlKey);
            phyNodeId = PhysicalTpIdNamingRule.getNodeId(nmlKey);
            equipId = PhysicalTpIdNamingRule.getEquipId(nmlKey);
            tpId = nmlKey;
        } else if (PhysicalNodeIdNamingRule.isPhyNodeId(nmlKey)) {
            siteNodeId = PhysicalNodeIdNamingRule.getNodeId(nmlKey);
            phyNodeId = nmlKey;
        }

        nmlKeyDto = NmlKeyDto.builder().siteNodeId(siteNodeId)
                .phyNodeId(phyNodeId)
                .equipId(equipId)
                .tpId(tpId)
                .transceiverId(transceiverId)
                .build();

        return nmlKeyDto;
    }

    public static List<String> parseNmlKeysForSiteLink(LinkStateDto siteLinkDto) {
        List<String> supportingLinkIds = siteLinkDto.getSupportingLink();
        List<String> nmlKeys = new ArrayList<>();
        for (String phyLink : supportingLinkIds) {
            List<String> phyLinkNmlKeys = getPhyLinkRefNmlKeys(phyLink);
            nmlKeys.addAll(phyLinkNmlKeys);
        }
        return nmlKeys;
    }


    public static List<String> getPhyLinkRefNmlKeys(String linkId) {
        List<String> nmlKeys = new ArrayList<>();
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);

        String sourceNodeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
        String destNodeId = PhysicalTpIdNamingRule.getNodeId(destTpId);
        nmlKeys.add(sourceTpId);
        nmlKeys.add(destTpId);
        nmlKeys.addAll(getNmlKeysForTp(sourceTpId));
        nmlKeys.add(destNodeId);
        nmlKeys.addAll(getNmlKeysForTp(destTpId));
        nmlKeys.add(sourceNodeId);
        return nmlKeys;
    }

    public static Set<String> getNmlKeysForTp(String tpId) {
//        String arr[] = tpId.split("#");
//        String neId = arr[0] + "#" + arr[1];
//        String equipId = neId + "#" + arr[2];
        log.debug("get tp relative nml key :{}", tpId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String transceiverId = PhysicalTpIdNamingRule.getTransceiverIdWithNamingRule(tpId);
        Set<String> set = new HashSet<String>();
        set.add(neId);
        set.add(transceiverId);
        set.add(equipId);
        set.add(tpId);
        if (isLinePortNeedsSigMapping(tpId)) {
            String sigTpId = convertLineToSig(tpId);
            set.add(sigTpId);
            log.debug("Mapped LINE port {} to SIG logical port {}", tpId, sigTpId);
        }
        return set;
    }

    private static String convertLineToSig(String tpId) {
        return tpId.replaceFirst(LINE_PORT_REGEX, SIG_PORT_SUFFIX);
    }

    private static boolean isLinePortNeedsSigMapping(String tpId) {
        return tpId.endsWith(LINE_PORT_SUFFIX);
    }


    public static boolean isSigPort(String tpId) {
        return tpId.endsWith(SIG_PORT_SUFFIX);
    }


    public static String convertSigToLine(String tpId) {
        return tpId.replaceFirst(SIG_PORT_REGEX, LINE_PORT_SUFFIX);
    }
}
