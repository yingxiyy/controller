package net.flex.dci.otc.controller.otdr.components;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/30 15:38
 */
@Component
@Slf4j
public abstract class BaseComponent {

    @Autowired
    protected AdapterDao adapterDao;

    @Autowired
    protected OTDRRpc otdrRpc;

    @Autowired
    protected PhyNodeDao phyNodeDao;

    @Autowired
    protected PhyLinkDao phyLinkDao;
}
