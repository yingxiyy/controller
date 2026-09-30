package net.flex.dci.otn.controller.resource.statistic.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otn.controller.resource.statistic.core.resource.InventoryResourceExtractor;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractInventoryService {

    protected final PhyNodeDao phyNodeDao;

    protected final SubNetTreeNodeDao subNetTreeNodeDao;

    protected final EquipmentsDao equipmentsDao;

    protected final InventoryResourceExtractor inventoryResourceExtractor;


    protected AbstractInventoryService(PhyNodeDao phyNodeDao, SubNetTreeNodeDao subNetTreeNodeDao,
            EquipmentsDao equipmentsDao, InventoryResourceExtractor inventoryResourceExtractor) {
        this.phyNodeDao = phyNodeDao;
        this.subNetTreeNodeDao = subNetTreeNodeDao;
        this.equipmentsDao = equipmentsDao;
        this.inventoryResourceExtractor = inventoryResourceExtractor;

    }


}
