package net.flex.dci.otn.controller.implement.common.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.implement.common.impl.OcmDataBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

import java.util.*;

@Slf4j
public class AseOcmGenerator {

    /**
     * 基于dummyLink 生成OCM 信息
     * @param siteLink
     * @param dummyOchLinks
     * @return <nodeId, ocmGroup>
     */
    public Map<String, List<OCMGripGroups>> generate(Link siteLink, List<Link> dummyOchLinks) {
        WDM_Band band = WDM_Band.C_L;
        List<Available> bandScopes = FrequencyAvailable.getInitializedAvailableList(NeYangModel.ByteDance, band, GridType._0);

        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        List<CrossConnectionAttributes> amplifierXcList = fetchSiteLinkXcIdList(siteLinkAttr.getExplictRoute().getRoute());
        OcmDataBuilder ocmDataBuilder = new OcmDataBuilder();
        ocmDataBuilder.buildOcmGroup(bandScopes, amplifierXcList, dummyOchLinks);

        return ocmDataBuilder.getUpdatedOcmGroupMap();
    }


    private List<CrossConnectionAttributes> fetchSiteLinkXcIdList(List<Route> route) {
        List<CrossConnectionAttributes> amplifierXcList = new ArrayList<>();

        route.stream().forEach(x-> {
            // Bone2.0 routes also contain FMUX/APS XCs; OCM groups use amplifier slots only.
            amplifierXcList.addAll(x.getPrimary().getCrossConnections().stream()
                    .filter(xc -> xc.getAmplifier() != null)
                    .collect(java.util.stream.Collectors.toList()));
            if (x.getSecondary() != null)
                amplifierXcList.addAll(x.getSecondary().getCrossConnections().stream()
                        .filter(xc -> xc.getAmplifier() != null)
                        .collect(java.util.stream.Collectors.toList()));
            if (x.getThird() != null)
                x.getThird().forEach(third -> amplifierXcList.addAll(third.getCrossConnections()
                        .stream().filter(xc -> xc.getAmplifier() != null)
                        .collect(java.util.stream.Collectors.toList())));
        });

        return amplifierXcList;
    }

}
