package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;

public class InsertNodeInSiteLinkResult {
    private final String newNodeId;
    private final String siteLinkId;
    private final BomInfo bomInfo;
    private final boolean applyRequired;

    InsertNodeInSiteLinkResult(String newNodeId, String siteLinkId, BomInfo bomInfo,
            boolean applyRequired) {
        this.newNodeId = newNodeId;
        this.siteLinkId = siteLinkId;
        this.bomInfo = bomInfo;
        this.applyRequired = applyRequired;
    }

    public String getNewNodeId() {
        return newNodeId;
    }

    public String getSiteLinkId() {
        return siteLinkId;
    }

    public BomInfo getBomInfo() {
        return bomInfo;
    }

    public boolean isApplyRequired() {
        return applyRequired;
    }
}
