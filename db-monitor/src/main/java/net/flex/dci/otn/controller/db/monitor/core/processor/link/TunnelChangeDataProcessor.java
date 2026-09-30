package net.flex.dci.otn.controller.db.monitor.core.processor.link;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.TUNNEL_ID;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.TunnelDao;
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
public class TunnelChangeDataProcessor extends AbstractChangeDataProcessor {

    private final TunnelDao tunnelDao;

    public TunnelChangeDataProcessor(
            DciTopologyCacheManager dciTopologyCacheManager,
            AdditionalPropertyFactory additionalPropertyFactory, TunnelDao tunnelDao) {
        super(dciTopologyCacheManager, additionalPropertyFactory);
        this.tunnelDao = tunnelDao;
    }

    @Override
    public ChangeObject enrichChangeObject(ChangeObject changeObject) {
        log.debug("enrich the change object for the tunnel  ");
        Document document = changeObject.getChangeBody();
        String tunnelId = document.getString(TUNNEL_ID);
        try {
            log.debug("enrich the change object for the tunnel id is:{}", tunnelId);
            dciTopologyCacheManager.removeAsync(tunnelId);
            Document richDoc = additionalPropertyFactory.enrichAdditionalProperty(tunnelId,
                    document);
            changeObject.setChangeBody(richDoc);
            return changeObject;
        } finally {
            dciTopologyCacheManager.removeAsync(tunnelId);

        }

    }

}
