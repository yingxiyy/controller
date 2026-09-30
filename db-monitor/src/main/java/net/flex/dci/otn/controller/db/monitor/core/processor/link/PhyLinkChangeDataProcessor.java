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
 * @date 2022/11/14 15:30
 */

@Slf4j
@Component
public class PhyLinkChangeDataProcessor extends AbstractChangeDataProcessor {

    public PhyLinkChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
    }

    @Override
    public ChangeObject enrichChangeObject(ChangeObject changeObject) {
        log.debug("enrich the change object for the site link ");
        Document document = changeObject.getChangeBody();
        String linkId = document.getString(LINK_ID);
        log.debug("enrich the change object for the  link id is:{}", linkId);
        Document richDoc = additionalPropertyFactory.enrichAdditionalProperty(linkId, document);
        changeObject.setChangeBody(richDoc);
        return changeObject;

    }
}
