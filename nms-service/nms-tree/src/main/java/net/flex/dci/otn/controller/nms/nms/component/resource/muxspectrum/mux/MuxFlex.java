/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux;

import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.mux.spectrum.OchLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;

public class MuxFlex extends MUX {

    short total = 0;

    public MuxFlex(NeYangModel model, String equipmentId, List<Link> ochList) {
        super(model, equipmentId, ochList, GridType._0);
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

    @Override
    public Spectrum getNextFrequency() {
        Link link = ochList.get(index - 1);
        Link1 och = link.getAugmentation(Link1.class);
        long lowFreq = och.getOch().getLowerFrequency().getValue().longValue();
        long upFreq = och.getOch().getUpperFrequency().getValue().longValue();
        long centFreq = (upFreq - lowFreq) / 2 + lowFreq;

        Spectrum port = port(centFreq, lowFreq, upFreq, index);

        index++;
        return port;
    }

    @Override
    protected Spectrum port(long centFreq, long lowFreq, long upFreq, short index) {
        //the index is ochList's index.

        SpectrumBuilder sb = getSpectrum(centFreq, lowFreq, upFreq, getDot());
        Link link = ochList.get(index - 1);
        boolean foundMatch = false;
        if (link.getSupportingLink() != null) {
            List<String> equipmentRefSupportingLinks = link.getSupportingLink().stream().map(
                            SupportingLink::getLinkRef).map(Uri::getValue)
                    .filter(supportingLinkId -> supportingLinkId.contains(equipmentId))
                    .collect(Collectors.toList());
            for (String supportingLinkId : equipmentRefSupportingLinks) {
//                Pattern pattern = Pattern.compile(formatting.getPortMatchingRegex());
                Matcher matcher = pattern.matcher(supportingLinkId);
                if (matcher.find()) {
                    String name = matcher.group(0);
                    sb.setName(name);
                    String[] ids = name.split(formatting.getKeyBeforChannelNo());
                    sb.setIndex(Integer.parseInt(ids[1]));
                    sb.setKey(new SpectrumKey(sb.getIndex()));
                    sb.setOchLink(new OchLinkBuilder()
                            .setTopologyId(new TopologyId(Constants.OCH_TOPO_KEY))
                            .setLinkId(link.getLinkId())
                            .setImplementState(
                                    link.getAugmentation(Link1.class).getOch().getImplementState())
                            .build());
                    foundMatch = true;
                    break;
                }
            }
        }
        return foundMatch ? sb.build() : null;
    }
}
