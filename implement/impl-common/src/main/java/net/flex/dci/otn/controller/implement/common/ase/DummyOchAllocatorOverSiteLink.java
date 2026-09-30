package net.flex.dci.otn.controller.implement.common.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * siteLink 上添加了一个DummyOchLink 的属性，保存所有的假波信息
 */
@Slf4j
public class DummyOchAllocatorOverSiteLink {
    private Link siteLink;
    Site linkAttr;

    private ChangedObject changedObject;

    public DummyOchAllocatorOverSiteLink(Link siteLink, ChangedObject changedObject) {
        this.siteLink = siteLink;
        linkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();

        this.changedObject = changedObject;
    }

    private List<String> getSiteLinkId(Link ochLink) {
        List<String> siteLinkIdList = new ArrayList<>();
        ochLink.getSupportingLink().forEach(sl -> {
            String linkId = sl.getLinkRef().getValue();
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                siteLinkIdList.add(linkId);
            }
        });
        return siteLinkIdList;
    }

    //找出需要提前释放/创建的假波，
    public List<Link> allocateAllDummyOchs() {
        if (!isFlexGridSiteLink()) {
            return new ArrayList<>();
        }
        DummyListManagerWrapOnOch dummyListManagerWrapOnOch = new DummyListManagerWrapOnOch(changedObject, siteLink);

        dummyListManagerWrapOnOch.addBusinessOch(0, 0);  //important logic here

        return dummyListManagerWrapOnOch.fetchCreatedDummyOch();
    }

    private boolean isFlexGridSiteLink() {
        if (linkAttr.getGrid().equals(GridType._0)) {
            return true;
        }
        return false;
    }


    
    public List<Link> getNeedRemovedOch() {
        return linkAttr.getDummyLink().stream()
                .map(ochLinkId -> changedObject.getChangedOchLink(ochLinkId))
                .collect(Collectors.toList());
    }
//
//    public void removeAllDummyOchs() {
//        linkAttr.getDummyLink().forEach(ochLinkId->changedObject.addRemovedOchLink(ochLinkId));
//        Link newSiteLink = new LinkBuilder(siteLink).addAugmentation(Link1.class, new Link1Builder()
//                        .setSite(new SiteBuilder(linkAttr)
//                                .setDummyLink(new ArrayList<>())
//                                .build())
//                        .build())
//                .build();
//        changedObject.addChangedSiteLink(newSiteLink);
//    }
}
