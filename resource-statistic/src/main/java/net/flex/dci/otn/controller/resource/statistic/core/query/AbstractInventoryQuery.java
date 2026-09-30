package net.flex.dci.otn.controller.resource.statistic.core.query;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 2026/7/13
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractInventoryQuery<T> implements InventoryQuery<T> {

    @Autowired
    protected EquipmentsDao equipmentsDao;

    @Autowired
    protected TunnelDao tunnelDao;

    @Autowired
    protected TerminationPointDao terminationPointDao;

    @Autowired
    protected PhyNodeDao phyNodeDao;

    @Autowired
    protected SiteLinkDao siteLinkDao;

    @Autowired
    protected OchLinkDao ochLinkDao;

    @Autowired
    protected DciTopologyCacheManager topologyCacheManager;


    protected abstract Class<T> getClazz();


}
