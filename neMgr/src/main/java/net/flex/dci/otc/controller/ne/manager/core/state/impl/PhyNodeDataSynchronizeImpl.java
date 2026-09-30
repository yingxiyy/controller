package net.flex.dci.otc.controller.ne.manager.core.state.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.controller.ne.manager.core.state.AbstractPhyNodeDataSynchronize;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/17/2023 4:28 PM
 */
@Component
@Slf4j
public class PhyNodeDataSynchronizeImpl {

    private final Map<NeYangModel, AbstractPhyNodeDataSynchronize> synchronizeMap;


    public PhyNodeDataSynchronizeImpl(
            List<AbstractPhyNodeDataSynchronize> phyNodeDataSynchronizes) {
        synchronizeMap = phyNodeDataSynchronizes.stream().collect(HashMap::new,
                (map, synchronizing) -> map.put(synchronizing.supportNeYangModel(), synchronizing),
                HashMap::putAll);
    }

  
    public void synchronizingData(Node phyNe) {
        String phyNodeId = phyNe.getNodeId().getValue();
        log.debug("synchronizing the phy node data,the phy node id is:{}", phyNodeId);
        Physical physical = phyNe.getAugmentation(Node1.class).getPhysical();
        NeYangModel neYangModel = NeYangModel.getModel(physical);
        synchronizeMap.get(neYangModel).synchronizingData(phyNe);
    }
}
