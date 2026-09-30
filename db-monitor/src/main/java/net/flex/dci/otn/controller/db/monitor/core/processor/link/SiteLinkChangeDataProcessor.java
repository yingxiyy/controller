package net.flex.dci.otn.controller.db.monitor.core.processor.link;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.LINK_ID;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.additional.AdditionalPropertyFactory;
import net.flex.dci.otn.controller.db.monitor.core.processor.AbstractChangeDataProcessor;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.ChangeObject;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/14 15:32
 */
@Component
@Slf4j
public class SiteLinkChangeDataProcessor extends AbstractChangeDataProcessor {

    public SiteLinkChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
    }


    @Override
    public ChangeObject enrichChangeObject(ChangeObject changeObject) {
        log.debug("enrich the change object for the site link ");
        Document document = changeObject.getChangeBody();
        String linkId = document.getString(LINK_ID);
        try {
            dciTopologyCacheManager.removeAsync(linkId);
            log.debug("enrich the change object for the site link id is:{}", linkId);
            Document richDoc = additionalPropertyFactory.enrichAdditionalProperty(linkId, document);
            changeObject.setChangeBody(richDoc);
            dciTopologyCacheManager.removeAsync(linkId);
            return changeObject;
        } finally {
            dciTopologyCacheManager.removeAsync(linkId);
        }

    }
}
