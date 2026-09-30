package net.flex.dci.otc.controller.status.core.evaluate;

import java.util.List;
import net.flex.dci.otc.controller.status.core.enums.LinkType;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinkAlarmState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @version 1.0
 * @date 8/23/2023 4:14 PM
 */
public interface StateEvaluate {

    void evaluateAlarmState(List<String> linkIds, LinkType linkType);

    List<ViewLinkAlarmState> calculateViewLinksAlarmState(List<Link> viewLinks,
            LinkType linkType);

}
