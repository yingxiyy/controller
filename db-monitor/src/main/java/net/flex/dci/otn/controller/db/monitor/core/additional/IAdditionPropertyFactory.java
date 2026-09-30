package net.flex.dci.otn.controller.db.monitor.core.additional;

import java.util.List;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;

/**
 * @version 1.0
 * @date 2022/11/17 16:26
 */
public interface IAdditionPropertyFactory {

    Document addAdditionalProperty(String id, Document document);

    List<Property> getAdditionalProperty(String id);

    ObjectType getObjectType();
    

}
