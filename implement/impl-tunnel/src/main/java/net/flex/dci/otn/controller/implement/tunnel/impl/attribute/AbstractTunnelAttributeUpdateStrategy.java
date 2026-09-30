package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 2026/2/26
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractTunnelAttributeUpdateStrategy implements
        TunnelAttributeUpdateStrategy {

    @Autowired
    protected TunnelDao tunnelDao;


}
