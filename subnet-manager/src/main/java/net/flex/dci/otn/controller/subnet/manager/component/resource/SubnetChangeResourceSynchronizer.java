package net.flex.dci.otn.controller.subnet.manager.component.resource;

/**
 * 2026/2/14
 *
 * @author musa
 * @version 1.0
 **/
public interface SubnetChangeResourceSynchronizer {

    void synchronizeUpdateSubnetName(String subnetId, String oldSubnetName, String newSubnetName,
            String operator);
}
