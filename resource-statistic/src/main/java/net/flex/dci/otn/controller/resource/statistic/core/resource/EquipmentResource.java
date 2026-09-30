package net.flex.dci.otn.controller.resource.statistic.core.resource;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.springframework.stereotype.Component;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class EquipmentResource extends AbstractResource {


    protected EquipmentResource(SiteLinkDao siteLinkDao, TunnelDao tunnelDao, PhyNodeDao phyNodeDao,
            EquipmentsDao equipmentsDao,
            SubNetTreeNodeDao subNetTreeNodeDao) {
        super(siteLinkDao, tunnelDao, phyNodeDao, equipmentsDao, subNetTreeNodeDao);
    }

    public List<String> extractRelativeCardIdsFromConnect(String siteLinkId, String tunnelId) {
        log.debug("extract relative card ids from connect siteLink");
        List<String> cardIds = new ArrayList<>();
        
        return new ArrayList<>();
    }
}
