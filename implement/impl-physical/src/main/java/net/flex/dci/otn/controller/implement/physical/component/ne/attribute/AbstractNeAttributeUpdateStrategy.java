package net.flex.dci.otn.controller.implement.physical.component.ne.attribute;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 2026/4/14
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractNeAttributeUpdateStrategy implements NeAttributeUpdateStrategy {

    @Autowired
    protected PhyNodeDao phyNodeDao;

    public void validateChangeNodeCommon(Nodes node) {
        if (node.getNodeId() == null || node.getNodeId().getValue() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to Config phy node because node id is null");
        }

        Physical phy = node.getPhysical();
        if (phy == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to Config phy node without detail info");
        }

        boolean existedNode = phyNodeDao.existsCfgNode(node.getNodeId().getValue());
        if (!existedNode) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find node " + node.getNodeId().getValue());
        }
    }

}
