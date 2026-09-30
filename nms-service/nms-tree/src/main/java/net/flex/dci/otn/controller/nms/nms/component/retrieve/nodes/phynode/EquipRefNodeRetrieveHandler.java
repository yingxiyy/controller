package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.AbstractNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 10:04
 */
@Slf4j
@Component
public class EquipRefNodeRetrieveHandler extends AbstractNodeRetrieveHandler {


    public EquipRefNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        return null;
    }
}
