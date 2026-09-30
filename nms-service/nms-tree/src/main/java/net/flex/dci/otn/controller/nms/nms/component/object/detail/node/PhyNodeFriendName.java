package net.flex.dci.otn.controller.nms.nms.component.object.detail.node;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 5:01 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyNodeFriendName implements NeFriendName {

    private final DciTopologyCacheManager dciTopologyCacheManager;

    @Override
    public String getNodeName(String neId) {
        log.debug("start to get the phy node friend name the ne id is:{}", neId);
        PhyNodeCache phyNodeCache = dciTopologyCacheManager.getValue(neId, PhyNodeCache.class);
//        if (null == phyNodeCache) {
//            log.error("failed to find the PHY node,the PHY node id is:{}", neId);
//        }
//        Physical nePhysical = phyNode.getAugmentation(Node1.class).getPhysical();
        String friendName = phyNodeCache.getFriendlyName();
        return friendName;
    }

    @Override
    public String supportTopologyId() {
        return TopoNameConstants.Phy_Topo_Key;
    }
}
