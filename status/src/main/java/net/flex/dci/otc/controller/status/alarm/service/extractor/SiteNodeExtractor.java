package net.flex.dci.otc.controller.status.alarm.service.extractor;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/11 16:33
 */
@Slf4j
@Component
public class SiteNodeExtractor extends AbstractExtractor {

    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        QueryParamDto queryParamDto = new QueryParamDto();
        log.debug("start to extract site node id :{}", id);
        Node siteNode = siteNodeDao.getSiteNodeById(id);
        queryParamDto = getSiteNodeQueryParam(siteNode);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.SiteNode;
    }

    private QueryParamDto getSiteNodeQueryParam(Node siteNode) {
        QueryParamDto queryParamDto = new QueryParamDto();
        if (siteNode != null && siteNode.getSupportingNode() != null
                && siteNode.getSupportingNode().size() > 0) {
            Set<String> neIdSet = siteNode.getSupportingNode().stream()
                    .map(supportingNode -> supportingNode.getNodeRef().getValue()).collect(
                            Collectors.toSet());
            queryParamDto.setNeIds(neIdSet);
        } else {

            queryParamDto.setNmlKeyLike(siteNode.getNodeId().getValue());
        }
        return queryParamDto;
    }
}
