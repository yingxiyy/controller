package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import static net.flex.dci.otn.controller.nms.utils.Constants.RESOURCE_REMOVE;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResourceEntity.ResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MatchingResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MatchingResourcesBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MissingResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MissingResourcesBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.ParentResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.missing.resources.ResourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 8/5/2025 10:39 AM
 */
@Slf4j
public abstract class AbstractAlarmResourceLocator {

    @Autowired
    protected NetconfTopology netconfTopology;

    public abstract LocateResourcesByAlarmOutput getLocateByAlarmResourceId(String alarmResourceId);

    public abstract NMSResourceType locateResourceType();


    /**
     * resource remove
     *
     * @param alarmResourceId
     * @return
     */
    protected LocateResourcesByAlarmOutput resourceRemove(String alarmResourceId) {
        log.debug("alarm relative resource removed :{}", alarmResourceId);
        LocateResourcesByAlarmOutputBuilder locateResourcesByAlarmOutputBuilder = new LocateResourcesByAlarmOutputBuilder();
        MissingResourcesBuilder missingResourcesBuilder = new MissingResourcesBuilder();
        ResourceBuilder resourceBuilder = new ResourceBuilder();
        resourceBuilder.setResourceId(alarmResourceId);
        resourceBuilder.setReason(RESOURCE_REMOVE);
        missingResourcesBuilder.setResource(Collections.singletonList(resourceBuilder.build()));
        locateResourcesByAlarmOutputBuilder.setMissingResources(missingResourcesBuilder.build());
        return locateResourcesByAlarmOutputBuilder.build();
    }

    /**
     * build matching resources
     *
     * @param alarmId
     * @param friendlyName
     * @param resourceType
     * @return
     */
    protected MatchingResources buildMatchingResources(String alarmId, String friendlyName,
            ResourceType resourceType) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.matching.resources.ResourceBuilder resourceBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.matching.resources.ResourceBuilder();
        resourceBuilder.setResourceType(resourceType);
        resourceBuilder.setResourceName(friendlyName);
        resourceBuilder.setResourceId(alarmId);
        MatchingResourcesBuilder matchingResourcesBuilder = new MatchingResourcesBuilder();
        matchingResourcesBuilder.setResource(Collections.singletonList(resourceBuilder.build()));
        return matchingResourcesBuilder.build();
    }

    protected LocateResourcesByAlarmOutput buildLocateResourcesOutput(
            MatchingResources matchingResources, ParentResources parentResources,
            MissingResources missingResources) {
        LocateResourcesByAlarmOutputBuilder locateResourcesByAlarmOutputBuilder = new LocateResourcesByAlarmOutputBuilder();
        locateResourcesByAlarmOutputBuilder.setMatchingResources(matchingResources);
        locateResourcesByAlarmOutputBuilder.setParentResources(parentResources);
        locateResourcesByAlarmOutputBuilder.setMissingResources(missingResources);
        return locateResourcesByAlarmOutputBuilder.build();
    }
}
