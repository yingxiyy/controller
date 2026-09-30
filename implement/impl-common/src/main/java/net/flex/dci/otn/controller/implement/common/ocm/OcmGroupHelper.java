package net.flex.dci.otn.controller.implement.common.ocm;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.Constant;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

import java.util.*;

@Slf4j
public class OcmGroupHelper {
    private PhyNodeDao phyNodeDao;
    private Site siteLinkAttr;
    private RouteInfo rInfo;
    private NeYangModel yangModel;
    private WDM_Band band;
    private List<Available> bandScopes;

    public OcmGroupHelper(Link siteLink) {
        siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        init();
    }

    private void init() {
        rInfo = new RouteInfo();
        rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());

        yangModel = CommonUtils.getYangModelInProperties(siteLinkAttr.getProperties());
        band = CommonUtils.getWDMBand(siteLinkAttr);

        bandScopes = FrequencyAvailable.getInitializedAvailableList(yangModel, band, GridType._0);
    }

    public Map<Node, OCMGripGroupsKey> fetchAllOcmGroupKey(long lowerFrequency) {
        Map<Node, OCMGripGroupsKey> ocmGroupKeyMap = new HashMap<>();

        List<String> xcIdList = rInfo.getXcIdList();
        xcIdList.forEach(xcId-> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            Optional<CrossConnections> xcOp = nodeAttr.getCrossConnections().stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(xcId)).findAny();
            CrossConnections xc = xcOp.get();
            String ocmId = CrossConnectionSlotNamingRule.getOcmNameOverAmplifierXc(xc);
            ocmGroupKeyMap.put(node, new OCMGripGroupsKey(fetchIndex(lowerFrequency), ocmId));
        });

        return ocmGroupKeyMap;
    }

    private int fetchIndex(long lowerFrequency) {
        //this is multiple 波段 如 C+L
        if (yangModel.equals(NeYangModel.ByteDance) && band.equals(WDM_Band.C_L)) {
            if (lowerFrequency >= Long.valueOf(Constant.MaxLowerFrequency.MUX64_BD_C) && lowerFrequency <= Long.valueOf(Constant.MaxUpperFrequency.MUX64_BD_C)) {
                return 3;
            } else {
                return 5;
            }
        }
        return 3;
    }

}
