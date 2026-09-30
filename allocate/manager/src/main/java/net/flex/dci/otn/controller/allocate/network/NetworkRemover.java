package net.flex.dci.otn.controller.allocate.network;

import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.link.site.WssLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public class NetworkRemover {
    private final ChangedObject changedObject;

    public NetworkRemover(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    /**
     * SiteLink删除时统一由WssLink清理SiteNode关系和对应物理资源，
     * 避免这里只删internalLink却遗漏EXP端口状态。
     */
    public void remove(Link removedSiteLink) {
        new WssLink(changedObject).remove(removedSiteLink);
    }
}
