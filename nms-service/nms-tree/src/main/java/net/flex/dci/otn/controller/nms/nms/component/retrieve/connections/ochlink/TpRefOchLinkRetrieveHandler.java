package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.ochlink;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/12 22:03
 */
@Slf4j
@Component
public class TpRefOchLinkRetrieveHandler extends AbstractOchLinkRetrieveHandler {

    public TpRefOchLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }
}
