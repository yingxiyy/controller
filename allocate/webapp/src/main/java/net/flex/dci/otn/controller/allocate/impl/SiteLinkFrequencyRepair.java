package net.flex.dci.otn.controller.allocate.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SiteLinkFrequencyRepair {
    private final OchLinkDao ochLinkDao;
    private final SiteLinkDao siteLinkDao;
    private final AllocatorConfig configuration;

    public SiteLinkFrequencyRepair(OchLinkDao ochLinkDao, SiteLinkDao siteLinkDao, AllocatorConfig configuration) {
        this.ochLinkDao = ochLinkDao;
        this.siteLinkDao = siteLinkDao;
        this.configuration = configuration;
    }

    public String autoHeal(String siteLinkId) {
        log.debug("start repair.... {}", siteLinkId);
        try {
            ChangedObject changedObject = new ChangedObject();
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            if (siteLink == null) {
                return "cannot find the siteLink\n";
            }
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

            String originalBandwidth = siteLinkAttr.getBandwidth();
            List<Available> originalAvaList = siteLinkAttr.getAvailable();
            List<SupportedLink> originalSupportedLinkList = siteLinkAttr.getSupportedLink();
            List<Link> ochLinks = ochLinkDao.getAllBusinessOchLinksUnderSiteLinkIds(Collections.singletonList(siteLinkId));
            List<SupportedLink> newSupportedLinkList = ochLinks.stream()
                    .map(this::buildSupportedLink)
                    .collect(Collectors.toList());

            // Use model/band derived from this siteLink itself instead of global allocator config.
            FrequencyAvailable siteContext = new FrequencyAvailable(siteLink);
            List<Available> initAvaList = FrequencyAvailable.getInitializedAvailableList(
                    siteContext.getYangModel(),
                    siteContext.getBandGroup(),
                    siteLinkAttr.getGrid()
            );

            siteLink = new LinkBuilder(siteLink)
                    .addAugmentation(Link1.class, new Link1Builder()
                            .setSite(new SiteBuilder(siteLinkAttr)
                                    .setAvailable(initAvaList)
                                    .build())
                            .build())
                    .build();

            AtomicInteger ochNumber = new AtomicInteger();
            FrequencyAvailable freqAvaObj = new FrequencyAvailable(siteLink);

            // Rebuild from business OCH supporting-link. Dummy OCHs are tracked by siteLink
            // dummy-link and must not be mixed into supported-link or bandwidth accounting.
            ochLinks.forEach(ochLink -> {
                Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

                Available occupied = new AvailableBuilder()
                        .setLowerFrequency(ochLinkAttr.getLowerFrequency())
                        .setUpperFrequency(ochLinkAttr.getUpperFrequency())
                        .build();
                freqAvaObj.remove(occupied);

                ochNumber.getAndIncrement();
            });

            String newBandwidth = String.valueOf(64 - ochNumber.get());
            List<Available> newAvaList = freqAvaObj.getAvailableList();

            boolean sameBandwidth = Objects.equals(originalBandwidth, newBandwidth);
            boolean sameAvailable = normalizeAvailable(originalAvaList).equals(normalizeAvailable(newAvaList));
            boolean sameSupportedLink = normalizeSupportedLink(originalSupportedLinkList)
                    .equals(normalizeSupportedLink(newSupportedLinkList));
            if (sameBandwidth && sameAvailable && sameSupportedLink) {
                return "";
            }

            siteLink = new LinkBuilder(siteLink)
                    .addAugmentation(Link1.class, new Link1Builder()
                            .setSite(new SiteBuilder(siteLinkAttr)
                                    .setAvailable(newAvaList)
                                    .setBandwidth(newBandwidth)
                                    .setSupportedLink(newSupportedLinkList)
                                    .build())
                            .build())
                    .build();

            siteLinkDao.rewriteSiteLink(siteLink);

            return String.format("friendlyName %s----------\norginal bandwidth: %s, supportedLink: %s, avaList: %s\n    new bandwidth: %s, supportedLink: %s, avaList: %s\n\n",
                    siteLinkAttr.getFriendlyName(),
                    originalBandwidth, normalizeSupportedLink(originalSupportedLinkList), originalAvaList,
                    newBandwidth, normalizeSupportedLink(newSupportedLinkList), newAvaList);
        } catch (Exception e) {
            log.error("error", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
        }
    }

    public String autoHealAll() {
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIds();
        log.debug("we have these sitelinks {}", siteLinkIds);

        List<String> repaired = siteLinkIds.stream()
                .map(this::autoHeal)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toList());

        repaired.add(String.format("\n\nall done. repaired %d / %d\n\n", repaired.size(), siteLinkIds.size()));
        return repaired.toString();
    }

    private String normalizeAvailable(List<Available> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(a -> a.getLowerFrequency().getValue().toString() + "-" + a.getUpperFrequency().getValue().toString())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private SupportedLink buildSupportedLink(Link ochLink) {
        return new SupportedLinkBuilder()
                .setTopologyRef(new TopologyId(TopoNameConstants.Och_Topo_Key))
                .setLinkRef(ochLink.getLinkId())
                .build();
    }

    private String normalizeSupportedLink(List<SupportedLink> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .filter(sl -> sl.getLinkRef() != null)
                .map(sl -> sl.getLinkRef().getValue())
                .sorted()
                .collect(Collectors.joining(","));
    }
}
