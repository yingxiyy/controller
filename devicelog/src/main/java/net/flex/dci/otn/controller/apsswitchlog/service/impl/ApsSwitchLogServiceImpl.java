package net.flex.dci.otn.controller.apsswitchlog.service.impl;

import static net.flex.dci.otn.controller.utils.Constants.DOT_SPLIT_REGEX;
import static net.flex.dci.otn.controller.utils.Constants.EMPTY_TAG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otn.controller.apsswitchlog.component.ApsSwitchLogConvertor;
import net.flex.dci.otn.controller.apsswitchlog.component.ResourceLookupResolver;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsRelatedTunnelOrSiteLink;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogDto;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogQueryParamDto;
import net.flex.dci.otn.controller.apsswitchlog.service.ApsSwitchLogService;
import net.flex.dci.otn.controller.utils.ApsSwitchLogUtils;
import net.flex.dci.otn.db.jpa.entity.ApsSwitchLog;
import net.flex.dci.otn.db.jpa.service.dao.ApsSwitchLogDaoService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApsSwitchLogServiceImpl implements ApsSwitchLogService {

    private final ApsSwitchLogDaoService apsSwitchLogDaoService;

    private final ApsSwitchLogConvertor apsSwitchLogConvertor;

    private final ResourceLookupResolver resourceLookupResolver;

    private final PhyNodeDao phyNodeDao;

    private final SubNetTreeNodeDao subNetTreeNodeDao;


    @Override
    public PageApsSwitchLogDto listAllApsSwitchLogByCondition(
            PageApsSwitchLogQueryParamDto pageQueryParamDto) {

        int offset = pageQueryParamDto.getPage();
        int limit = pageQueryParamDto.getLimit();
        log.debug("retrieve all aps switch log by condition offset is {},limit is {}", offset,
                limit);
        String activePath = pageQueryParamDto.getActivePath();
        Long startTime = pageQueryParamDto.getStartTime();
        Long endTime = pageQueryParamDto.getEndTime();
        String subnetId = pageQueryParamDto.getSubnetId();
        String realActivePath = null;
        Integer activeIndex = null;
        if (null != activePath) {
            String[] activePathElement = activePath.split(DOT_SPLIT_REGEX);
            realActivePath = activePathElement[0];
            activeIndex = Integer.valueOf(activePathElement[1].trim());
        }
        if (null != startTime) {
            startTime = TimeUnit.MILLISECONDS.toNanos(startTime);
        }
        if (null != endTime) {
            endTime = TimeUnit.MILLISECONDS.toNanos(endTime);
        }
        List<String> neIds = new ArrayList<>();
        boolean hasNeFilter = false;
        if (StringUtils.hasText(pageQueryParamDto.getNeName())) {
            neIds = getNeIdsByNeNameRegex(pageQueryParamDto.getNeName());
            hasNeFilter = true;
        }
        if (StringUtils.hasText(subnetId)) {
            hasNeFilter = true;
            List<String> phyNodeIds = phyNodeDao.retrieveAllPhyNodeIdsBySubnetIds(
                    Collections.singletonList(subnetId));
            if (CollectionUtils.isEmpty(neIds)) {
                neIds = phyNodeIds;
            } else {
                Set<String> phyNodeIdSet = new HashSet<>(phyNodeIds);
                neIds = neIds.stream()
                        .filter(phyNodeIdSet::contains)
                        .collect(Collectors.toList());
            }
        }
//        if (StringUtils.hasText(pageQueryParamDto.getNeName())) {
//            neIds = getNeIdsByNeNameRegex(pageQueryParamDto.getNeName());
//        }
//        if (StringUtils.hasText(subnetId)) {
//            List<String> phyNodeIds = phyNodeDao.retrieveAllPhyNodeIdsBySubnetIds(
//                    Collections.singletonList(subnetId));
//            if (CollectionUtils.isEmpty(neIds)) {
//                neIds = phyNodeIds;
//            } else {
//                Set<String> phyNodeIdSet = new HashSet<>(phyNodeIds);
//                neIds = neIds.stream()
//                        .filter(phyNodeIdSet::contains)
//                        .collect(Collectors.toList());
//            }
//        }

        Page<ApsSwitchLog> apsSwitchLogPage = new PageImpl<>(new ArrayList<>(),
                PageRequest.of(offset, limit), 0);
        if (hasNeFilter && CollectionUtils.isEmpty(neIds)) {
            apsSwitchLogPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(offset, limit), 0);
        } else if (CollectionUtils.isEmpty(neIds)) {
            apsSwitchLogPage = apsSwitchLogDaoService.listAllApsSwitchLogByCondition(
                    offset,
                    limit,
                    pageQueryParamDto.getSort(),
                    pageQueryParamDto.getNeId(),
                    realActivePath,
                    activeIndex,
                    pageQueryParamDto.getApsModuleName(),
                    pageQueryParamDto.getTriggerType(),
                    pageQueryParamDto.getApsMode(),
                    startTime,
                    endTime);
        } else {
            apsSwitchLogPage = apsSwitchLogDaoService.listAllApsSwitchLogByNeIdsAndCondition(
                    offset,
                    limit,
                    pageQueryParamDto.getSort(),
                    neIds,
                    realActivePath,
                    activeIndex,
                    pageQueryParamDto.getApsModuleName(),
                    pageQueryParamDto.getTriggerType(),
                    pageQueryParamDto.getApsMode(),
                    startTime,
                    endTime);
        }
        PageApsSwitchLogDto pageIdcData = apsSwitchLogConvertor.convertApsSwitchLogPaged(
                apsSwitchLogPage);
        return pageIdcData;
    }

    private List<String> getNeIdsByNeNameRegex(String neName) {
        log.debug("get neIds by neName regex:{}", neName);
        List<Node> nodes = phyNodeDao.listAllNodeByNameRegex(neName);
        return nodes.stream().map(NodeAttributes::getNodeId).map(Uri::getValue).collect(
                Collectors.toList());
    }

    @Override
    public void deleteApsSwitchLog(Long id) {
        log.debug("delete aps switch log,the switch log id is:{}", id);
        apsSwitchLogDaoService.deleteApsSwitchLogByLogID(id);
    }

    @Override
    public List<ApsRelatedTunnelOrSiteLink> findApsRelativeSiteLinkOrTunnel(String neId,
            String apsName) {
        log.debug("find relative SiteLink and tunnel by neId:{} and aps name:{}", neId, apsName);
        Node node = phyNodeDao.getPhyNodeById(neId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the node " + neId + " is not existed");
        }
        NodeType nodeType = ApsSwitchLogUtils.getNodeType(node);

        return Collections.emptyList();
    }

    @Override
    public PageApsSwitchLogDto retrieveResourceAllApsSwitchLogByCondition(
            PageApsSwitchLogQueryParamDto pageQueryParamDto) {
        int offset = pageQueryParamDto.getPage();
        int limit = pageQueryParamDto.getLimit();
        log.debug("list all task Info data offset is {},limit is {} by ", offset, limit);
        String activePath = pageQueryParamDto.getActivePath();
        Long startTime = pageQueryParamDto.getStartTime();
        Long endTime = pageQueryParamDto.getEndTime();
        String realActivePath = null;
        Integer activeIndex = null;
        if (null != activePath) {
            String[] activePathElement = activePath.split(DOT_SPLIT_REGEX);
            realActivePath = activePathElement[0];
            activeIndex = Integer.valueOf(activePathElement[1].trim());
        }
        if (null != startTime) {
            startTime = TimeUnit.MILLISECONDS.toNanos(startTime);
        }
        if (null != endTime) {
            endTime = TimeUnit.MILLISECONDS.toNanos(endTime);
        }
        List<String> relativeResourceId = resourceLookupResolver.resolveResource(
                pageQueryParamDto.getResourceType(), pageQueryParamDto.getResourceId());
        if (relativeResourceId.contains(EMPTY_TAG)) {
            return PageApsSwitchLogDto.builder().build();
        }
        if (StringUtils.hasText(pageQueryParamDto.getNeName())) {
            relativeResourceId = getRelativeResourceIdByNeNameRegex(pageQueryParamDto.getNeName(),
                    relativeResourceId);
        }
        Page<ApsSwitchLog> apsSwitchLogPage = apsSwitchLogDaoService.listAllApsSwitchLogByNeIdsAndCondition(
                offset,
                limit,
                pageQueryParamDto.getSort(),
                relativeResourceId,
                realActivePath,
                activeIndex,
                pageQueryParamDto.getApsModuleName(),
                pageQueryParamDto.getTriggerType(),
                pageQueryParamDto.getApsMode(),
                startTime,
                endTime);

        PageApsSwitchLogDto pageIdcData = apsSwitchLogConvertor.convertApsSwitchLogPaged(
                apsSwitchLogPage);
        return pageIdcData;
    }

    /**
     * filter the relative resource Id by neNameRegex
     *
     * @param neName
     * @return
     */
    private List<String> getRelativeResourceIdByNeNameRegex(String neName,
            List<String> relativeResourceId) {
        List<String> relativeResourceIds = relativeResourceId.stream()
                .filter(neId -> phyNodeDao.getFriendlyName(neId).contains(neName))
                .collect(Collectors.toList());
        return relativeResourceIds;
    }

}
