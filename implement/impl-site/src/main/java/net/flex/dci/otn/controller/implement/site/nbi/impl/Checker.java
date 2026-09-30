package net.flex.dci.otn.controller.implement.site.nbi.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.util.AseSiteLinkSupport;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Checker {

    private final SiteLinkDao siteLinkDao;

    public List<String> filterOutCBand(List<String> siteLinkIdList) {
        return filterAseSupportedSiteLinks(siteLinkIdList);
    }

    public List<String> filterAseSupportedSiteLinks(List<String> siteLinkIdList) {
        List<String> result = new ArrayList<>();
        siteLinkIdList.forEach(siteLinkId->{
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);

            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
            // Keep legacy ByteDance C+L and Bone2.0 Flex policy in one shared predicate.
            if (AseSiteLinkSupport.supportsAse(siteLinkAttr)) {
                result.add(siteLinkId);
            }
        });
        return result;
    }

    public void errorBroadcastMessage(String title, String msg) {
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(title)
                        .message(msg)
                        .error(true)
                        .build());
    }
}
