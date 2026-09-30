package net.flex.dci.otn.controller.nms.nms.enums;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;

/**
 * @version 1.0
 * @date 1/21/2024 5:23 PM
 */
public enum NMSViewLinkType {
    OS_LINK(ViewLinkType.OsLink.name(), ViewLinkType.OsLink),
    OTS_LINK(ViewLinkType.OtsLink.name(), ViewLinkType.OtsLink),
    ALL("all", null),
    OCH_LINK(ViewLinkType.OchLink.name(), ViewLinkType.OchLink),
    SITE_LINK(ViewLinkType.SiteLink.name(), ViewLinkType.SiteLink);
    private String viewLinkTypeName;

    private ViewLinkType linkType;

    NMSViewLinkType(String name, ViewLinkType linkType) {
        this.viewLinkTypeName = name;
        this.linkType = linkType;
    }

    public static NMSViewLinkType getNMSViewLinkType(ViewLinkType viewLinkType) {
        NMSViewLinkType result = ALL;
        if (viewLinkType == null) {
            return result;
        }

        for (NMSViewLinkType nmsViewLinkType : NMSViewLinkType.values()) {
            if (nmsViewLinkType.getLinkType() != null && nmsViewLinkType.getLinkType()
                    .equals(viewLinkType)) {
                result = nmsViewLinkType;
                break;
            }
        }
        return result;
    }

    public String getViewLinkTypeName() {
        return viewLinkTypeName;
    }

    public ViewLinkType getLinkType() {
        return linkType;
    }
}
