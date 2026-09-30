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
public class TransceiverResource extends AbstractResource {


    protected TransceiverResource(SiteLinkDao siteLinkDao, TunnelDao tunnelDao,
            PhyNodeDao phyNodeDao,
            EquipmentsDao equipmentsDao,
            SubNetTreeNodeDao subNetTreeNodeDao) {
        super(siteLinkDao, tunnelDao, phyNodeDao, equipmentsDao, subNetTreeNodeDao);
    }

    public List<String> extractConnectionRelativeTransceiverIds(String siteLinkId,
            String tunnelId) {
        return new ArrayList<>();
    }
}
