package net.flex.dci.otn.controller.nms.enrich;

import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @version 1.0
 * @date 2022/12/20 10:29
 */
public interface TopologyOperationEnrichHandler {

    DataObject enrichProperties(DataObject dataObject);

}
