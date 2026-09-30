package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.mux.spectrum.OchLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

/**
 * @version 1.0
 * @date 2022/12/29 15:00
 */
public class Wss extends MUX {

    private final Map<Long, CrossConnections> crossConnectionFreqMap;
    short total = 0;

    public Wss(NeYangModel model, String equipmentId, List<Link> ochList,
            List<CrossConnections> crossConnections) {
        super(model, equipmentId, ochList, GridType._0);
        crossConnectionFreqMap = crossConnections.stream()
                .map(crossConnection -> {
                    Available availableFrequency = CrossConnectionSlotNamingRule.getFrequencyScope(
                            crossConnection);
                    return new AbstractMap.SimpleEntry<>(
                            availableFrequency.getLowerFrequency().getValue().longValue(),
                            crossConnection
                    );
                })
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing, // 如果有重复的键，保留第一个出现的
                        HashMap::new
                ));

    }

    @Override
    public Spectrum getNextFrequency() {
        Link ochLink = ochList.get(index - 1);
        Och ochLinkPhysical = ochLink.getAugmentation(Link1.class).getOch();
        long lowFreq = ochLinkPhysical.getLowerFrequency().getValue().longValue();
        long upFreq = ochLinkPhysical.getUpperFrequency().getValue().longValue();
        long centFreq = (upFreq - lowFreq) / 2 + lowFreq;

        Spectrum port = port(centFreq, lowFreq, upFreq, index);

        index++;
        return port;
    }

    @Override
    protected Spectrum port(long centFreq, long lowFreq, long upFreq, short index) {
        //the index is ochList's index.
        SpectrumBuilder spectrumBuilder = getSpectrum(centFreq, lowFreq, upFreq, getDot());
        spectrumBuilder.setIndex((int) index);
        spectrumBuilder.setKey(new SpectrumKey((int) index));
        Link link = ochList.get(index - 1);
        if (crossConnectionFreqMap.containsKey(lowFreq)) {
            CrossConnections refCrossConnection = crossConnectionFreqMap.get(lowFreq);
            String wssSpectrumName = CrossConnectionUtils.generateWssCrossName(refCrossConnection,
                    centFreq);
            spectrumBuilder.setName(wssSpectrumName);
            spectrumBuilder.setOchLink(new OchLinkBuilder()
                    .setTopologyId(new TopologyId(Constants.OCH_TOPO_KEY))
                    .setLinkId(link.getLinkId())
                    .setImplementState(
                            link.getAugmentation(Link1.class).getOch().getImplementState())
                    .build());
        }
        return spectrumBuilder.build();
    }

    @Override
    short totalOchNumbers() {

        if (ochList != null) {
            total = (short) ochList.size();
        }

        return total;
    }

    @Override
    short getDot() {
        return 5;
    }
}
