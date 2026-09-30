package net.flex.dci.otn.controller.allocate.network;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLink2Input;

import java.util.concurrent.Callable;

@Slf4j
public class SiteLinkCreator implements Callable<CreationResult>{
    private net.flex.dci.otn.controller.allocate.link.site.SiteLinkCreator<CreateLink2Input> linkCreator;
    private ChangedObject changedObject;
    private CreateLink2Input creationParam;
    private String linkName;

    public SiteLinkCreator(ChangedObject changedObject, CreateLink2Input creationParam, boolean isRoadm) {
        this.changedObject = changedObject;
        this.creationParam = creationParam;
        linkName = creationParam.getFriendlyName();

        linkCreator = new net.flex.dci.otn.controller.allocate.link.site.SiteLinkCreator(changedObject, isRoadm);
    }

    public CreationResult create() {
        CreationResult result = new CreationResult();
        result.setLinkName(linkName);

        log.debug("start {} creation", linkName);
        try {
            Link siteLink = linkCreator.creationLogic(creationParam, true);
            String siteLinkId = siteLink.getLinkId().getValue();

            result.setCreatedLink(changedObject.getChangedSiteLink(siteLinkId));
            result.setErrorInfo(null);

            log.debug("create network's siteLink success {}", linkName);
        } catch (Exception e) {
            result.setCreatedLink(null);
            result.setErrorInfo(e.getMessage());

            log.error("create network's siteLink faile {}", linkName, e);
        }
        return result;
    }

    @Override
    public CreationResult call() throws Exception {
        log.info("start create network's siteLink {}", linkName);
        return create();
    }
}
