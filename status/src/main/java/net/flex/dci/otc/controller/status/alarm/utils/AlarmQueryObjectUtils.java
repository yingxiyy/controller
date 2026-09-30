package net.flex.dci.otc.controller.status.alarm.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.*;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;

/**
 * @version 1.0
 * @date 9/6/2023 1:28 PM
 */
@Slf4j
public class AlarmQueryObjectUtils {


    /**
     * get objectId by id
     *
     * @param objectId
     * @return
     */
    public static AlarmObjectType getAlarmObjectTypeByObjectId(String objectId) {
        log.debug("get alarm object type by the object id :{}", objectId);
        AlarmObjectType alarmObjectType = null;
        if (SiteNodeIdNamingRule.isSiteNodeId(objectId)) {
            alarmObjectType = AlarmObjectType.SiteNode;
        } else if (PhysicalNodeIdNamingRule.isPhyNodeId(objectId)) {
            alarmObjectType = AlarmObjectType.PhyNode;
        } else if (PhysicalEqpIdNamingRule.isEquipId(objectId)) {
            alarmObjectType = AlarmObjectType.Equip;
        } else if (PhysicalTpIdNamingRule.isTpId(objectId)) {
            alarmObjectType = AlarmObjectType.Tp;
        } else if (PhysicalLinkIdNamingRule.isPhysicalLinkId(objectId)) {
            alarmObjectType = AlarmObjectType.PhyLink;
        } else if (SiteLinkIdNamingRule.isSiteLink(objectId)) {
            alarmObjectType = AlarmObjectType.SiteLink;
        } else if (TunnelIdNamingRule.isTunnelId(objectId)) {
            alarmObjectType = AlarmObjectType.Tunnel;
        }
        return alarmObjectType;
    }
}
