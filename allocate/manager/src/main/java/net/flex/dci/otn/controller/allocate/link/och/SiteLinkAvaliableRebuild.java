package net.flex.dci.otn.controller.allocate.link.och;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SiteLinkAvaliableRebuild {
    private static final OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
    private static final SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

    private List<Link> allSiteLinks;
    public SiteLinkAvaliableRebuild(List<Link> allSiteLinks) {
        this.allSiteLinks = allSiteLinks;
    }


    public boolean checkOverlapAndRebuild(Link ochCheckingLink) {
        Och ochLinkAttr = ochCheckingLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
        long checkingLower = ochLinkAttr.getLowerFrequency().getValue().longValue();
        long checkingUpper = ochLinkAttr.getUpperFrequency().getValue().longValue();

        boolean existsOverlap = false;
        for (Link siteLink : allSiteLinks) {
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
            List<String> ochLinkIds = siteLinkAttr.getSupportedLink().stream().map(x -> x.getLinkRef().getValue()).collect(Collectors.toList());
            ochLinkIds.remove(ochCheckingLink.getLinkId().getValue());

            List<Link> ochLinks = ochLinkDao.listAllOchLinksByIds(ochLinkIds);
            boolean found = ochLinks.stream().anyMatch(ochLink -> {
                Och ochLinkAtrr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
                long ochLower = ochLinkAtrr.getLowerFrequency().getValue().longValue();
                long ochUpper = ochLinkAtrr.getUpperFrequency().getValue().longValue();

                return checkingLower < ochUpper && checkingUpper > ochLower;
            });
            if (found) {
                existsOverlap = true;
                rebuildAvaliableFrequency(siteLink, ochLinks);
            }
        }
        return existsOverlap;
    }

    public void rebuildAvaliableFrequency(Link siteLink, List<Link> ochLinks) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        WDM_Band band = WDM_Band.fromString(siteLinkAttr.getLinkGroup());
        String yangModel = PropertyTool.getValue(siteLinkAttr.getProperties(), "yang-model");
        NeYangModel neYangModel = NeYangModel.valueOf(yangModel);

        log.debug("rebuild site link available frequency, siteLinkId: {}, band: {}, yangModel: {}, grid: {}",
                siteLink.getLinkId().getValue(), band, yangModel, siteLinkAttr.getGrid());
        List<Available> initAva = FrequencyAvailable.getInitializedAvailableList(neYangModel, band, siteLinkAttr.getGrid());

        Link newSiteLink = setAvailable(siteLink, initAva);
        FrequencyAvailable frequencyAvailable = new FrequencyAvailable(newSiteLink);

        ochLinks.forEach(ochLink -> frequencyAvailable.remove(getOccupation(ochLink)));
        newSiteLink = setAvailable(siteLink, frequencyAvailable.getAvailableList());

        siteLinkDao.saveSiteLink(newSiteLink);
    }

    private Available getOccupation(Link ochLink) {
        Och ochLinkAtrr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

        return new AvailableBuilder()
                .setLowerFrequency(ochLinkAtrr.getLowerFrequency())
                .setUpperFrequency(ochLinkAtrr.getUpperFrequency())
                .build();
    }

    private Link setAvailable(Link siteLink, List<Available> initAva) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        return new LinkBuilder(siteLink)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setSite(new SiteBuilder(siteLinkAttr)
                                .setAvailable(initAva)
                                .build())
                        .build())
                .build();
    }
}
