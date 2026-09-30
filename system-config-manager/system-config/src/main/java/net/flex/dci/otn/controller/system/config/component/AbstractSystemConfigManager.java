package net.flex.dci.otn.controller.system.config.component;

import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
public abstract class AbstractSystemConfigManager {

    protected final NeManagerRpc neManagerRpc;

    public AbstractSystemConfigManager(NeManagerRpc neManagerRpc) {

        this.neManagerRpc = neManagerRpc;
    }

}
