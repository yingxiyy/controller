package net.flex.dci.otn.controller.subnet.manager.component.notification;

import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;

/**
 * 2026/3/2
 *
 * @author musa
 * @version 1.0
 **/
public interface SubnetChangeNotify {

    void sendSubnetCreateNotify(SubNetTreeNode parentSubnetNode, SubNetTreeNode subNetTreeNode);

    void sendSubnetModifyNotify(SubNetTreeNode subNetTreeNode);

    void sendSubnetDeleteNotify(SubNetTreeNode parentSubnetNode, SubNetTreeNode subNetTreeNode);

}
