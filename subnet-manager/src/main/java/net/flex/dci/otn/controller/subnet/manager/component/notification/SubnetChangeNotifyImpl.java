package net.flex.dci.otn.controller.subnet.manager.component.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ChangeType;
import net.flex.dci.otc.common.enums.ElementType;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.component.OutputConvertor;
import net.flex.dci.otn.controller.subnet.manager.dto.output.SubNetNode;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import org.springframework.stereotype.Component;

/**
 * 2026/3/2
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubnetChangeNotifyImpl implements SubnetChangeNotify {

    private final OutputConvertor outputConvertor;

    @Override
    public void sendSubnetCreateNotify(SubNetTreeNode parentSubnetNode,
            SubNetTreeNode subNetTreeNode) {
        log.info("send subnet create notification parent:{} create:{}", parentSubnetNode,
                subNetTreeNode);
        if (parentSubnetNode != null) {
            SubNetNode subNetNode = outputConvertor.convertSubNetTreeNode2SubNetNode(
                    parentSubnetNode);
            sendSubnetNotification(subNetNode, ChangeType.UPDATE);
        }
        SubNetNode createNode = outputConvertor.convertSubNetTreeNode2SubNetNode(subNetTreeNode);
        sendSubnetNotification(createNode, ChangeType.CREATE);

    }

    @Override
    public void sendSubnetModifyNotify(SubNetTreeNode subNetTreeNode) {
        log.info("send subnet modify notification subnetTreeNode:{}", subNetTreeNode);
        SubNetNode subNetNode = outputConvertor.convertSubNetTreeNode2SubNetNode(subNetTreeNode);
        sendSubnetNotification(subNetNode, ChangeType.UPDATE);
    }

    @Override
    public void sendSubnetDeleteNotify(SubNetTreeNode parentSubnetNode,
            SubNetTreeNode subNetTreeNode) {
        log.info("send subnet delete notify,parent:{} remove subnetNode:{}", parentSubnetNode,
                subNetTreeNode);
        if (parentSubnetNode != null) {
            SubNetNode subNetNode = outputConvertor.convertSubNetTreeNode2SubNetNode(
                    parentSubnetNode);
            sendSubnetNotification(subNetNode, ChangeType.UPDATE);
        }
        SubNetNode deleteNode = outputConvertor.convertSubNetTreeNode2SubNetNode(subNetTreeNode);
        sendSubnetNotification(deleteNode, ChangeType.DELETE);

    }

    private void sendSubnetNotification(SubNetNode subNetNode, ChangeType changeType) {
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(changeType)
                .elementType(ElementType.SUBNET)
                .content(subNetNode)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }
}
