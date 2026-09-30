package net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.IAlarmStateCalculator;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.enums.ProtectionActivePathRole;
import net.flex.dci.otc.controller.status.util.LinkHelper;
import net.flex.dci.otc.controller.status.util.NmlKeyHelper;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 8/22/2023 2:08 PM
 */
@Slf4j
public abstract class AbstractLinkAlarmStateCalculator<T, V> implements
        IAlarmStateCalculator<T, V> {

    @Autowired
    protected LinkHelper linkHelper;

    @Autowired
    protected SiteLinkDao siteLinkDao;

    @Autowired
    protected PhyLinkDao phyLinkDao;

    @Autowired
    protected ConnectionCacheManager connectionCacheManager;


    @Autowired
    private AlarmDaoService alarmDaoService;


    /**
     * get ref links severity
     *
     * @param linkIds
     * @return
     */
    protected AlarmSeverity getRefLinksSeverity(List<String> linkIds) {
        Set<String> siteLinkIds = linkIds.stream().filter(StatusUtil::isSiteLinkId).collect(
                Collectors.toSet());
        Set<String> phyLinkIds = CommonUtil.getDifferenceSetByGuava(new HashSet<>(linkIds),
                siteLinkIds);

        Map<String, LinkStateDto> phyLinkMap = connectionCacheManager.batchGetPhyLinksByIds(
                new ArrayList<>(phyLinkIds));
        Map<String, LinkStateDto> siteLinkMap = connectionCacheManager.batchGetSiteLinksByIds(
                new ArrayList<>(siteLinkIds));
        AlarmSeverity refSiteLinkSeverity = getRefSiteLinksSeverity(new ArrayList<>(siteLinkIds),
                siteLinkMap);
        AlarmSeverity refPhyLinksSeverity = getRefPhyLinksSeverity(new ArrayList<>(phyLinkIds),
                phyLinkMap);
        return StatusUtil.calculateAlarmSeverity(refPhyLinksSeverity, refSiteLinkSeverity);
    }

    protected AlarmSeverity getRefSiteLinksSeverity(List<String> siteLinkIds,
            Map<String, LinkStateDto> siteLinkMap) {

        Set<String> allNmlKeys = new HashSet<>();
        for (String siteLinkId : siteLinkIds) {
            LinkStateDto siteLink = siteLinkMap.get(siteLinkId);
            if (siteLink != null) {
                List<String> nmlKeys = NmlKeyHelper.parseNmlKeysForSiteLink(siteLink);
                allNmlKeys.addAll(nmlKeys);
            }
        }

        Map<String, List<AlarmSeverity>> alarmSeverityMap = alarmDaoService.getAlarmSeverityByNmlKeysWithMap(
                new ArrayList<>(allNmlKeys));
        List<AlarmSeverity> alarmSeverities = new ArrayList<>();
        for (List<AlarmSeverity> keyAlarms : alarmSeverityMap.values()) {
            alarmSeverities.add(StatusUtil.calculateAlarmStateByList(keyAlarms));
        }
        
        return StatusUtil.calculateAlarmStateByList(alarmSeverities);
    }

    protected AlarmSeverity getRefPhyLinksSeverity(List<String> phyLinkIds,
            Map<String, LinkStateDto> phyLinkMap) {
        List<AlarmSeverity> alarmSeverities = phyLinkIds.stream()
                .map(phyLinkMap::get)
                .filter(Objects::nonNull)
                .map(LinkStateDto::getAlarmSeverity)
                .collect(Collectors.toList());
        return StatusUtil.calculateAlarmStateByList(alarmSeverities);
    }


    protected AlarmSeverity calculateProtectAlarmSeverity(ProtectedLinkDto protectedSiteLinkDto) {
        log.debug("start to calculate protect alarm Severity");
        Set<ProtectionActivePathRole> protectionRoles = protectedSiteLinkDto.getActivePathRoles();
        if (CollectionUtils.isEmpty(protectionRoles)) {
            return AlarmSeverity.Unknown;
        }
        AlarmSeverity alarmSeverity = AlarmSeverity.Unknown;
        int activePathRoleSize = protectionRoles.size();
        if (activePathRoleSize == 1) {
            ProtectionActivePathRole activePathRole = protectionRoles.iterator().next();
            switch (activePathRole) {
                case Tertiary:
                    alarmSeverity = getRefLinksSeverity(protectedSiteLinkDto.getTertiaryLinkIds());
                    break;
                case Primary:
                    alarmSeverity = getRefLinksSeverity(protectedSiteLinkDto.getPrimaryLinkIds());
                    break;
                case Secondary:
                    alarmSeverity = getRefLinksSeverity(protectedSiteLinkDto.getSecondaryLinkIds());
                    break;
            }
        } else {
            alarmSeverity = calculateMixTypeSeverity(protectedSiteLinkDto);
        }
//        if (protectedSiteLinkDto.isPrimary() && !protectedSiteLinkDto.isSecondary()) {
//            alarmSeverity = getRefLinksSeverity(protectedSiteLinkDto.getPrimaryLinkIds());
//        } else if (!protectedSiteLinkDto.isPrimary() && protectedSiteLinkDto.isSecondary()) {
//            alarmSeverity = getRefLinksSeverity(protectedSiteLinkDto.getSecondaryLinkIds());
//        } else {
//            List<String> refLinkIds = Stream.concat(
//                            protectedSiteLinkDto.getPrimaryLinkIds().stream(),
//                            protectedSiteLinkDto.getSecondaryLinkIds().stream())
//                    .collect(Collectors.toList());
//            alarmSeverity = getRefLinksSeverity(refLinkIds);
//        }
        return alarmSeverity;
    }


    private AlarmSeverity calculateMixTypeSeverity(ProtectedLinkDto protectedSiteLinkDto) {
        log.debug("start to calculate the mix type active path route alarm type severity ");
        Set<ProtectionActivePathRole> protectionActivePathRoles = protectedSiteLinkDto.getActivePathRoles();
        AlarmSeverity alarmSeverity = AlarmSeverity.Unknown;
        for (ProtectionActivePathRole protectionActivePathRole : protectionActivePathRoles) {
            if (protectionActivePathRole == ProtectionActivePathRole.Primary) {
                AlarmSeverity primaryAlarmSeverity = getRefLinksSeverity(
                        protectedSiteLinkDto.getPrimaryLinkIds());
                alarmSeverity = StatusUtil.calculateAlarmSeverity(alarmSeverity,
                        primaryAlarmSeverity);
            } else if (protectionActivePathRole == ProtectionActivePathRole.Secondary) {
                AlarmSeverity secondaryAlarmSeverity = getRefLinksSeverity(
                        protectedSiteLinkDto.getSecondaryLinkIds());
                alarmSeverity = StatusUtil.calculateAlarmSeverity(alarmSeverity,
                        secondaryAlarmSeverity);
            } else if (protectionActivePathRole == ProtectionActivePathRole.Tertiary) {
                AlarmSeverity tertiaryAlarmSeverity = getRefLinksSeverity(
                        protectedSiteLinkDto.getTertiaryLinkIds());
                alarmSeverity = StatusUtil.calculateAlarmSeverity(alarmSeverity,
                        tertiaryAlarmSeverity);
            }
        }

        return alarmSeverity;
    }

    protected AlarmSeverity calculateProtectLinkAlarmSeverity(
            ProtectedLinkDto protectedLinkDetailInfo, List<String> refLinkIds,
            AlarmSeverity currentAlarmSeverity, AlarmSeverity alarmSeverity) {
        log.debug(
                "calculate the protection link:{} ref phy linkIds:{} alarmSeverity current:{} update :{}",
                protectedLinkDetailInfo, refLinkIds, currentAlarmSeverity, alarmSeverity);
        List<String> primaryRefLinkIds = protectedLinkDetailInfo.getPrimaryLinkIds();
        List<String> secondaryRefLinkIds = protectedLinkDetailInfo.getSecondaryLinkIds();
        List<String> tertiaryRefLinkIds = protectedLinkDetailInfo.getTertiaryLinkIds();
        Set<ProtectionActivePathRole> protectionActivePathRoles = protectedLinkDetailInfo.getActivePathRoles();
        int activePathRoleSize = protectionActivePathRoles.size();
        AlarmSeverity updateAlarmSeverity = currentAlarmSeverity;
        if (CollectionUtils.isEmpty(protectionActivePathRoles)) {
            updateAlarmSeverity = AlarmSeverity.Unknown;
        } else if (activePathRoleSize > 1) {//mixType
            updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(currentAlarmSeverity,
                    alarmSeverity);
        } else {//onlyOneType
            // 定义枚举到引用链接集合的映射
            Map<ProtectionActivePathRole, Set<String>> roleRefLinkIdsMap = new EnumMap<>(
                    ProtectionActivePathRole.class);
            roleRefLinkIdsMap.put(ProtectionActivePathRole.Primary,
                    new HashSet<>(primaryRefLinkIds));
            roleRefLinkIdsMap.put(ProtectionActivePathRole.Secondary,
                    new HashSet<>(secondaryRefLinkIds));
            roleRefLinkIdsMap.put(ProtectionActivePathRole.Tertiary,
                    new HashSet<>(tertiaryRefLinkIds));
            ProtectionActivePathRole activePathRole = protectionActivePathRoles.iterator()
                    .next();
            Set<String> targetRefLinkIds = roleRefLinkIdsMap.get(activePathRole);
            // when current alarm ref link is protection active path and recalculate the alarm severity
            if (targetRefLinkIds != null && StatusUtil.containsAll(targetRefLinkIds,
                    refLinkIds)) {
                updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(
                        currentAlarmSeverity,
                        alarmSeverity
                );
            }
        }
        return updateAlarmSeverity;
    }

}
