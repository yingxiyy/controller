package net.flex.dci.otn.controller.nms.operations;

import java.io.UnsupportedEncodingException;

/**
 * @version 1.0
 * @date 10/16/2023 4:30 PM
 */
public interface ITopologyOperations extends IOperations {

    String getNetworkTopology(String identifier) throws UnsupportedEncodingException;
}
