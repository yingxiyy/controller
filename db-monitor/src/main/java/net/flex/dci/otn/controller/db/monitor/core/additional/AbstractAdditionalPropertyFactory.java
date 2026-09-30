package net.flex.dci.otn.controller.db.monitor.core.additional;

import com.alibaba.fastjson.JSON;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.dto.Properties;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;
import net.flex.dci.otn.controller.db.monitor.utils.Constants;
import net.flex.dci.otn.controller.db.monitor.utils.PhysicalPropertyTool;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.ApsXCCache;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/17 16:57
 */
@Component
@Slf4j
public abstract class AbstractAdditionalPropertyFactory implements IAdditionPropertyFactory {

    protected static final String PROPERTIES = "properties";
    @Autowired
    protected DciTopologyCacheManager dciTopologyCacheManager;

    protected abstract String getPhysicalKey();


    protected Document addAdditionalProperty(Document document,
            List<Property> additionalProperty) {
        Document properties = (Document) ((Document) document.get(
                getPhysicalKey())).get(PROPERTIES);
        if (properties == null) {
            Properties nProperties = Properties.builder().property(additionalProperty).build();
            Document nPropertyDocument = Document.parse(
                    JSON.toJSONString(nProperties));
            ((Document) document.get(getPhysicalKey())).put(PROPERTIES, nPropertyDocument);
        } else {
            String propertiesJson = properties.toJson(JsonWriterSettings.builder().build());
            Properties propertyList = JSON.parseObject(propertiesJson, Properties.class);
            List<Property> mergeProperty = PhysicalPropertyTool.mergeProperty(
                    propertyList.getProperty(), additionalProperty);
            Properties nProperties = Properties.builder().property(mergeProperty).build();
            Document nPropertyDocument = Document.parse(
                    JSON.toJSONString(nProperties));
            ((Document) document.get(getPhysicalKey())).put(PROPERTIES, nPropertyDocument);
        }
        return document;
    }

    protected String getApsActivePath(String apsXCId) {
        if (apsXCId == null) {
            return Constants.NONE;
        }
        log.debug("get active path");
        ApsXCCache apsXCCache = dciTopologyCacheManager.getValue(apsXCId);
        if (null == apsXCCache) {
            return Constants.NONE;
        }
        return apsXCCache.getActivePath() == null ? ApsPath.PRIMARY.name()
                : apsXCCache.getActivePath();
    }

}
