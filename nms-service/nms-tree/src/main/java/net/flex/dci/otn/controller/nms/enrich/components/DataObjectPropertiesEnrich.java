package net.flex.dci.otn.controller.nms.enrich.components;

import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @version 1.0
 * @date 2023/1/17 13:30
 */
public interface DataObjectPropertiesEnrich {

    DataObject enrichProperties(DataObject dataObject);
}
