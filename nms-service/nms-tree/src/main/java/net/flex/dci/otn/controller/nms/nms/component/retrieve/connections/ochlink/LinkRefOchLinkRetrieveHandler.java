package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.ochlink;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/12 22:07
 */
@Slf4j
@Component
public class LinkRefOchLinkRetrieveHandler extends AbstractOchLinkRetrieveHandler {

    public LinkRefOchLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }
}
