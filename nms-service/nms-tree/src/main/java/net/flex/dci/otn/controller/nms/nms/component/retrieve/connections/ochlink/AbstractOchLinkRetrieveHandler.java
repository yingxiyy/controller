package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.ochlink;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.AbstractNMSRetrieveHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;

/**
 * @version 1.0
 * @date 2022/3/12 20:49
 */
@Slf4j
public abstract class AbstractOchLinkRetrieveHandler extends AbstractNMSRetrieveHandler {

    public AbstractOchLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


}
