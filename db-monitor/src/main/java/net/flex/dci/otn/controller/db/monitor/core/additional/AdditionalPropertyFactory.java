package net.flex.dci.otn.controller.db.monitor.core.additional;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @date 2022/11/17 16:26
 */
@Component
@Slf4j
public class AdditionalPropertyFactory {

    private static final ConcurrentHashMap<ObjectType, IAdditionPropertyFactory> additionPropertyMap = new ConcurrentHashMap<>();

    public AdditionalPropertyFactory(
            List<IAdditionPropertyFactory> additionPropertyFactoryFactories) {
        additionPropertyFactoryFactories.forEach(
                additionalFactory -> additionPropertyMap.put(additionalFactory.getObjectType(),
                        additionalFactory));
    }


    /**
     * get additional property for the id like phy node ,site link ,tunnel
     *
     * @param id
     * @return
     */
    public List<Property> getAdditionalProperty(String id) {
        log.debug("get additional property from id:{}", id);
        ObjectType objectType = getObjectTypeById(id);
        return additionPropertyMap.get(objectType).getAdditionalProperty(id);
    }

    public Document enrichAdditionalProperty(String id, Document changeDoc) {
        log.debug("enrich the additional property to the document for the id:{}", id);
        ObjectType objectType = getObjectTypeById(id);
        return additionPropertyMap.get(objectType).addAdditionalProperty(id, changeDoc);
    }

    private ObjectType getObjectTypeById(String id) {
        ObjectType objectType = null;
        if (PhysicalNodeIdNamingRule.isPhyNodeId(id)) {
            objectType = ObjectType.Node;
        } else if (PhysicalLinkIdNamingRule.isPhysicalLinkId(id)) {
            objectType = ObjectType.Link;
        } else if (SiteLinkIdNamingRule.isSiteLink(id)) {
            objectType = ObjectType.Sitelink;
        } else if (TunnelIdNamingRule.isTunnelId(id)) {
            objectType = ObjectType.Tunnel;
        }

        return objectType;
    }
}
