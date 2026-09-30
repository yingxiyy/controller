package net.flex.dci.otc.controller.status.alarm.service.extractor;

import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.getNmlKeysForTp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/11 16:33
 */
@Slf4j
@Component
public class TunnelExtractor extends AbstractExtractor {


    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        log.debug("start to get detail tunnel for id {}", id);
        QueryParamDto queryParamDto = new QueryParamDto();
        Tunnel tunnel = tunnelDao.getTunnelById(id);
        Set<String> nmlKeySet = new HashSet<>();
        nmlKeySet.add(id);
        extractTp(tunnel, nmlKeySet);
        extractLink(tunnel, nmlKeySet);
        queryParamDto.setNmlKeySet(nmlKeySet);
//        queryParamDto.setSa(true);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.Tunnel;
    }

    private void extractLink(Tunnel tunnel, Set<String> nmlKeySet) {
        List<SupportingLink> supportingLinkList = tunnel
                .getSupportingLink();
        if (supportingLinkList != null) {
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink supportingLink : supportingLinkList) {
                String ochLinkId = supportingLink.getLinkRef().getValue();
                nmlKeySet.addAll(getOchNmlKeys(ochLinkId));
            }
        }

    }

    private void extractTp(Tunnel tunnel, Set<String> nmlKeySet) {
        String srcTpId = tunnel.getSourceTp().get(0).getTpRef().getValue();
        String destTpId = tunnel.getDestinationTp().get(0).getTpRef().getValue();
        nmlKeySet.addAll(getNmlKeysForTp(srcTpId));
        nmlKeySet.addAll(getNmlKeysForTp(destTpId));
    }


}
