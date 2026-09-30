package net.flex.dci.otc.controller.status.alarm.service.extractor;

import static net.flex.dci.otc.controller.status.util.Constants.EMPTY;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otc.controller.status.alarm.utils.AlarmConstants;
import net.flex.dci.otc.controller.status.alarm.utils.AlarmQueryObjectUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.SortDetailDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmAttributeNameType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetCurrentAlarmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.filter.attributes.ObjectInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.sort.query.params.SortInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/1/10 15:43
 */
@Slf4j
@Component
public class AlarmQueryParamExtractor {


    private Map<AlarmObjectType, IExtractor> detailExtractMap = new HashMap<>();

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final PhyNodeDao phyNodeDao;

    public AlarmQueryParamExtractor(List<IExtractor> extractors,
            SubNetTreeNodeDao subNetTreeNodeDao, PhyNodeDao phyNodeDao) {
        this.detailExtractMap = extractors.stream().collect(HashMap::new,
                (map, extractor) -> map.put(extractor.getAlarmObjectType(), extractor),
                Map::putAll);
        this.subNetTreeNodeDao = subNetTreeNodeDao;
        this.phyNodeDao = phyNodeDao;
    }

    public AlarmConditionDto parseQueryCondition(GetCurrentAlarmsInput input)
            throws Exception {
        log.debug("start to parse the query param,value is {}", input);
        AlarmConditionDto alarmConditionDto = getParams(input);
        List<SortDetailDto> sortBy = getSortList(input.getSortInfos());
        alarmConditionDto.setSortBy(sortBy);
        return alarmConditionDto;
    }


    private List<SortDetailDto> getSortList(
            List<SortInfos> sortList) {
        List<SortDetailDto> list = new ArrayList<>();
        if (sortList != null && sortList.size() > 0) {
            for (SortInfos sort : sortList) {

                String attr = null;
                if (sort.getSortName() == AlarmAttributeNameType.AlarmId) {
                    attr = AlarmConstants.ALARM_ID;
                } else if (sort.getSortName() == AlarmAttributeNameType.Serverity) {
                    attr = AlarmConstants.SEVERITY;
                } else if (sort.getSortName() == AlarmAttributeNameType.ResourceRef) {
                    attr = AlarmConstants.RESOURCE_REF;
                } else if (sort.getSortName() == AlarmAttributeNameType.AlarmText) {
                    attr = AlarmConstants.ALARM_TEXT;
                } else if (sort.getSortName() == AlarmAttributeNameType.AlarmGroup) {
                    attr = AlarmConstants.ALARM_GROUP;
                } else if (sort.getSortName() == AlarmAttributeNameType.AlarmTypeId) {
                    attr = AlarmConstants.ALARM_TYPE_ID;
                } else if (sort.getSortName() == AlarmAttributeNameType.CreationTime) {
                    attr = AlarmConstants.CREATION_TIME;
                } else if (sort.getSortName() == AlarmAttributeNameType.CreationReceivedTime) {
                    attr = AlarmConstants.RECEIVED_TIME;
                } else if (sort.getSortName() == AlarmAttributeNameType.ClearTime) {
                    attr = AlarmConstants.CLEARED_TIME;
                } else if (sort.getSortName() == AlarmAttributeNameType.ClearReceivedTime) {
                    attr = AlarmConstants.RECEIVED_CLEAR_TIME;
                } else if (sort.getSortName() == AlarmAttributeNameType.ArchiveTime) {
                    attr = AlarmConstants.ARCHIVED_TIME;
                } else if (sort.getSortName() == AlarmAttributeNameType.ArchiveType) {
                    attr = AlarmConstants.ACTION_TYPE;
                } else if (sort.getSortName() == AlarmAttributeNameType.NeId) {
                    attr = AlarmConstants.NE_ID;
                } else if (sort.getSortName() == AlarmAttributeNameType.NmlKey) {
                    attr = AlarmConstants.NML_KEY;
                } else if (sort.getSortName() == AlarmAttributeNameType.Sa) {
                    attr = AlarmConstants.SA;
                } else if (sort.getSortName() == AlarmAttributeNameType.EquipmentRef) {
                    attr = AlarmConstants.COMPONENT_REF;
                } else if (sort.getSortName() == AlarmAttributeNameType.NmlKeyName) {
                    attr = AlarmConstants.NML_KEY_NAME;
                }
                SortDetailDto sortDetailDto = SortDetailDto.builder().name(attr)
                        .direction(sort.isAscending() ? Direction.ASC : Direction.DESC).build();
//                map.put("attribute", attr);
//                map.put("ascending", sort.isAscending() ? "asc" : "desc");
                list.add(sortDetailDto);
            }
        }
        return list;
    }


    private AlarmConditionDto getParams(GetCurrentAlarmsInput input) throws Exception {
        String nmlKey = input.getNmlKey();
        String nmlKeyName = input.getNmlKeyName();
        String alarmTypeId = input.getAlarmTypeId();
        String alarmText = input.getAlarmText();
        String alarmGroup = input.getAlarmGroup();
        BigInteger startDate = input.getTimeStart();
        BigInteger endDate = input.getTimeEnd();
        BigInteger clearStartDate = input.getClearTimeStart();
        BigInteger clearEndDate = input.getClearTimeEnd();
        AlarmSeverity serverity = input.getServerity();
        String subnetId = input.getSubnetId();
        String siteId = input.getSiteId();

        AlarmConditionDto alarmConditionDto = new AlarmConditionDto();
        if (nmlKey != null) {
//            params.put("nmlKey", nmlKey);
            alarmConditionDto.setNmlKey(nmlKey);
        }
        if (startDate != null) {
//            params.put("startDate", startDate);
            alarmConditionDto.setStartTime(startDate);
        }
        if (clearStartDate != null) {
//            params.put("clearStartDate", clearStartDate);
            alarmConditionDto.setClearStartTime(clearStartDate);
        }
        if (alarmText != null) {
//            params.put("alarmText", alarmText);
            alarmConditionDto.setAlarmText(alarmText);
        }
        if (alarmGroup != null) {
//            params.put("alarmGroup", alarmGroup);
            alarmConditionDto.setAlarmGroup(alarmGroup);
        }
        if (endDate != null) {
//            params.put("endDate", endDate);
            alarmConditionDto.setEndTime(endDate);
        }
        if (clearEndDate != null) {
//            params.put("clearEndDate", clearEndDate);
            alarmConditionDto.setClearEndTime(clearEndDate);
        }
        if (alarmTypeId != null) {
//            params.put("alarmTypeId", alarmTypeId);
            alarmConditionDto.setAlarmTypeId(alarmTypeId);
        }
        if (serverity != null) {
//            params.put("severity", serverity.ordinal());
            alarmConditionDto.setSeverity(serverity.ordinal());
        }
        if (nmlKeyName != null) {
//            params.put("nmlKeyName", nmlKeyName);
            alarmConditionDto.setNmlKeyName(nmlKeyName);
            alarmConditionDto.setNmlKey(nmlKeyName);
        }
        if (siteId != null) {
            alarmConditionDto.setSiteId(siteId);
        }
//        if (input.getObjectInfo() != null) {
//            params.putAll(this.getQueryParam(input.getObjectInfo()));
//        }
        if (subnetId != null) {
            List<String> phyNeIds = phyNodeDao.retrieveAllPhyNodeIdsBySubnetIds(
                    Collections.singletonList(subnetId));
            if (phyNeIds.isEmpty()) {
                alarmConditionDto.setNeIds(Collections.singleton(EMPTY));
            } else {
                alarmConditionDto.setNeIds(new HashSet<>(phyNeIds));
            }


        }
        if (input.getObjectInfo() != null) {
            carefulDistinctionParam(input.getObjectInfo(), alarmConditionDto);
        }
        return alarmConditionDto;
    }

    /**
     * aim to distinctionParam like tunnel site link  site phy link
     *
     * @param objectInfo
     * @param alarmConditionDto
     */
    private void carefulDistinctionParam(ObjectInfo objectInfo,
            AlarmConditionDto alarmConditionDto) {
        if (objectInfo.getObjectType() == null || objectInfo.getObjectId() == null) {
            return;
        }
        AlarmObjectType type = objectInfo.getObjectType();
        String id = objectInfo.getObjectId();

        if (type == null) {
            type = AlarmQueryObjectUtils.getAlarmObjectTypeByObjectId(id);
        }
        QueryParamDto queryParamDto = detailExtractMap.get(type)
                .extractDetailInfo(id, alarmConditionDto);
        if (queryParamDto != null) {
            alarmConditionDto.setNmlKeyLikeSet(queryParamDto.getNmlKeyLikeSet());
            alarmConditionDto.setNmlKeySet(queryParamDto.getNmlKeySet());
            Set<String> neIds =
                    CollectionUtils.isEmpty(alarmConditionDto.getNeIds()) ? new HashSet<>()
                            : alarmConditionDto.getNeIds();
            if (!CollectionUtils.isEmpty(queryParamDto.getNeIds())) {
                neIds.addAll(queryParamDto.getNeIds());
            }
            alarmConditionDto.setNeIds(neIds);
            alarmConditionDto.setNeId(queryParamDto.getNeId());
            alarmConditionDto.setNmlKey(queryParamDto.getNmlKeyLike());
            alarmConditionDto.setSa(queryParamDto.getSa());
        }
    }

//    private Map<String, Object> getQueryParam(ObjectInfo objectInfo) {
//        Map<String, Object> params = new HashMap<>();
//        if (objectInfo.getObjectType() != null && objectInfo.getObjectId() != null) {
//            AlarmObjectType type = objectInfo.getObjectType();
//            String id = objectInfo.getObjectId();
//            if (type == AlarmObjectType.SiteNode) {
//                Node siteNode = siteNodeDao.getSiteNodeById(id);
//                if (siteNode != null && siteNode.getSupportingNode() != null
//                        && siteNode.getSupportingNode().size() > 0) {
//                    List<String> neIdList = new ArrayList<>();
//                    for (SupportingNode sn : siteNode.getSupportingNode()) {
//                        neIdList.add(sn.getNodeRef().getValue());
//                    }
//                    params.put("neIdList", neIdList);
//                } else {
//                    params.put("nmlKeyLike1",
//                            new StringBuilder(siteNode.getNodeId().getValue()).append("%")
//                                    .toString());
//                }
//            } else if (type == AlarmObjectType.PhyNode) {
//                String neId = id;
//                params.put("nmlKeyLike1", new StringBuilder(neId).append("%").toString());
//            } else if (type == AlarmObjectType.Rack) {
//                String rackId = id;
//                String siteNodeId = id.split("#")[0];
//
//                SupportingRack rack = rackDao.getRackBySiteIdRackRef(siteNodeId, rackId);
//
//                if (rack != null && rack.getSupportingNe() != null
//                        && rack.getSupportingNe().size() > 0) {
//                    List<String> neIdList = new ArrayList<String>();
//                    for (SupportingNe sn : rack.getSupportingNe()) {
//                        neIdList.add(sn.getNodeRef().getValue());
//                    }
//                    params.put("neIdList", neIdList);
//                }
//            } else if (type == AlarmObjectType.PhyLink) {
//                params.put("nmlKeySet", this.getPhyLinkNmlKeys(id));
//                params.put("sa", true);
//            } else if (type == AlarmObjectType.SiteLink) {
//                params.put("nmlKeySet", this.getSiteLinkNmlKeys(id));
//                params.put("sa", true);
//            } else if (type == AlarmObjectType.Tunnel) {
//                params.put("nmlKeySet", this.getTunnelNmlKeys(id));
//                params.put("sa", true);
//            } else if (type == AlarmObjectType.Tp) {
////				params.put("nmlKey", id);
//                String strArr[] = id.split("#");
//                String neId = strArr[0] + "#" + strArr[1];
//                String equipSegment = strArr[2];
//                String tpSegment = strArr[3];
//                String tranceiverSegment =
//                        "TRANSCEIVER" + tpSegment.substring(tpSegment.indexOf("-"));
//                params.put("nmlKeyLike1",
//                        new StringBuilder(neId).append("#").append(tranceiverSegment).append("%")
//                                .toString());
//                params.put("nmlKeyLike2", id);
//            } else if (type == AlarmObjectType.Equip) {
//                if (id.contains("#TRANSCEIVER-")) {
//                    params.put("nmlKey", id);
//                } else {
//                    if (id.contains("#CHASSIS-")) {
//                        String neId = id.split("#")[0] + "#" + id.split("#")[1];
//                        List<String> neIdList = new ArrayList<>();
//                        neIdList.add(neId);
//                        params.put("neIdList", neIdList);
//                    } else {
////						String strArr[] = id.split("-");
//                        String neId = id.split("#")[0] + "#" + id.split("#")[1];
////						int size = strArr.length;
////						String keyStr = "-"+strArr[size-2]+"-"+strArr[size-1];
//                        params.put("nmlKeyLike1", id + "%");
//                        if (id.contains("LINECARD")) {
//                            params.put("nmlKeyLike2",
//                                    new StringBuilder(id.replace("LINECARD", "TRANSCEIVER"))
//                                            .append("%").toString());
//                        }
//                        params.put("neId", neId);
//                    }
//
//                }
//
//            }
//
//        }
//        return params;
//    }
//
//    private Set<String> getTunnelNmlKeys(String tunnelId) {
//        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
//        String srcTpId = tunnel.getSourceTp().get(0).getTpRef().getValue();
//        String destTpId = tunnel.getDestinationTp().get(0).getTpRef().getValue();
//        Set<String> tunnelSet = new HashSet<>();
//        tunnelSet.addAll(this.getNmlKeysForTp(srcTpId));
//        tunnelSet.addAll(this.getNmlKeysForTp(destTpId));
//        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink> supportingLinkList = tunnel
//                .getSupportingLink();
//        if (supportingLinkList != null) {
//            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink supportingLink : supportingLinkList) {
//                String ochLinkId = supportingLink.getLinkRef().getValue();
//                tunnelSet.addAll(this.getOchNmlKeys(ochLinkId));
//            }
//        }
//        tunnelSet.add(tunnelId);
//        return tunnelSet;
//    }
//
//    private Set<String> getOchNmlKeys(String ochLinkId) {
//        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
//        List<SupportingLink> supportingLinkList = ochLink.getSupportingLink();
//        Set<String> ochSet = new HashSet<>();
//        if (supportingLinkList != null) {
//            for (SupportingLink supportingLink : supportingLinkList) {
//                String linkId = supportingLink.getLinkRef().getValue();
//                Set<String> set = this.getPhyLinkNmlKeys(linkId);
//                if (set == null) {
//                    set = this.getSiteLinkNmlKeys(linkId);
//                }
//                ochSet.addAll(set);
//            }
//        }
//        return ochSet;
//    }
//
//    private Set<String> getSiteLinkNmlKeys(String siteLinkId) {
//        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
//        Set<String> set = new HashSet<>();
//        List<SupportingLink> supportingLinkList = siteLink.getSupportingLink();
//        if (supportingLinkList != null) {
//            for (SupportingLink supportingLink : supportingLinkList) {
//                String phyLinkId = supportingLink.getLinkRef().getValue();
//                set.addAll(this.getPhyLinkNmlKeys(phyLinkId));
//            }
//        }
//        set.add(siteLinkId);
//        return set;
//    }
//
//    private Set<String> getPhyLinkNmlKeys(String phyLinkId) {
//        Link phyLink = phyLinkDao.getPhyLinkById(phyLinkId);
//        if (phyLink == null) {
//            return null;
//        }
//        String srcTpId = phyLink.getSource().getSourceTp().getValue();
//        String dstTpId = phyLink.getDestination().getDestTp().getValue();
//        Set<String> set = new HashSet<>();
//        set.addAll(getNmlKeysForTp(srcTpId));
//        set.addAll(getNmlKeysForTp(dstTpId));
//        set.add(phyLinkId);
//        return set;
//    }
//
//    private Set<String> getNmlKeysForTp(String tpId) {
//        String arr[] = tpId.split("#");
//        String neId = arr[0] + "#" + arr[1];
//        String equipId = neId + "#" + arr[2];
//        Set<String> set = new HashSet<String>();
////		set.add(neId);
//        set.add(equipId);
//        set.add(tpId);
//        return set;
//    }

}
