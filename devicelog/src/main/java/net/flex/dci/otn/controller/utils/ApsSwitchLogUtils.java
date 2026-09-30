package net.flex.dci.otn.controller.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.apsswitchlog.dto.SortCondition;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 *
 * 2025/9/13
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ApsSwitchLogUtils {

    public static Sort getSortElement(List<SortCondition> sortConditionList) {
        List<Order> orders = new ArrayList<>();
        for (SortCondition sortCondition : sortConditionList) {
            Order order = new Order(
                    "asc".equalsIgnoreCase(sortCondition.getDirection()) ? Direction.ASC
                            : Direction.DESC, sortCondition.getOrderElement().getElementName());
            orders.add(order);
        }

        return Sort.by(orders);
    }


    public static NodeType getNodeType(Node ne) {
        Node1 nodeArg = ne.getAugmentation(Node1.class);
        Physical physical = nodeArg == null ? null : nodeArg.getPhysical();
        NodeType nodeType = null;
        if (physical != null) {
            nodeType = physical.getNodeType();
        }

        return nodeType;
    }

}
