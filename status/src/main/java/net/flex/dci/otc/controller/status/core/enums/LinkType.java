package net.flex.dci.otc.controller.status.core.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;

/**
 * @version 1.0
 * @date 8/23/2023 4:32 PM
 */
@Getter
public enum LinkType {
    PHYSICAL_LINK(Arrays.asList(ViewLinkType.OsLink, ViewLinkType.OtsLink)),
    SITE_LINK(Collections.singletonList(ViewLinkType.SiteLink)),
    OCH_LINK(Collections.singletonList(ViewLinkType.OchLink));

    private final List<ViewLinkType> relatedViewLinkTypes;

    LinkType(List<ViewLinkType> relatedViewLinkTypes) {
        this.relatedViewLinkTypes = relatedViewLinkTypes;
    }

    public static LinkType fromViewLinkType(ViewLinkType viewLinkType) {
        if (viewLinkType == null) {
            throw new IllegalArgumentException("ViewLinkType cannot be null");
        }
        for (LinkType linkType : values()) {
            if (linkType.relatedViewLinkTypes.contains(viewLinkType)) {
                return linkType;
            }
        }
        throw new IllegalArgumentException("Unknown ViewLinkType: " + viewLinkType);
    }
}
