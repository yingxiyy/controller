package net.flex.dci.otn.controller.allocate.link.site;

import net.flex.dci.otc.common.util.PropertyTool;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class SiteLinkNodeOperationState {
    public static final String PROPERTY_NAME = "node-operation-state";
    public static final String NODE_ID_PROPERTY_NAME = "node-operation-node-id";
    public static final String WAITING_INSERT_APPLY = "WAITING_INSERT_APPLY";
    public static final String INSERT_APPLYING = "INSERT_APPLYING";
    public static final String INSERT_APPLYING_DEVICE_STARTED = "INSERT_APPLYING_DEVICE_STARTED";
    public static final String INSERT_APPLY_FAILED_BEFORE_DEVICE = "INSERT_APPLY_FAILED_BEFORE_DEVICE";
    public static final String INSERT_APPLY_FAILED_AFTER_DEVICE_STARTED =
            "INSERT_APPLY_FAILED_AFTER_DEVICE_STARTED";
    /** Legacy state whose device-side progress is unknown. */
    public static final String INSERT_APPLY_FAILED = "INSERT_APPLY_FAILED";

    private SiteLinkNodeOperationState() {
    }

    public static String get(Link siteLink) {
        Site site = getSite(siteLink);
        return site == null ? null : PropertyTool.getValue(site.getProperties(), PROPERTY_NAME);
    }

    public static String getNodeId(Link siteLink) {
        Site site = getSite(siteLink);
        return site == null ? null : PropertyTool.getValue(site.getProperties(), NODE_ID_PROPERTY_NAME);
    }

    public static boolean isActive(Link siteLink) {
        return get(siteLink) != null;
    }

    public static boolean canStartInsertApply(Link siteLink) {
        String state = get(siteLink);
        return WAITING_INSERT_APPLY.equals(state)
                || INSERT_APPLY_FAILED_BEFORE_DEVICE.equals(state);
    }

    public static Link set(Link siteLink, String state, String nodeId) {
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("node operation state is required");
        }
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("node operation node id is required");
        }
        Site site = requireSite(siteLink);
        Properties properties = PropertyTool.addProperty(site.getProperties(), PROPERTY_NAME, state);
        properties = PropertyTool.addProperty(properties, NODE_ID_PROPERTY_NAME, nodeId);
        return rebuild(siteLink, site, properties);
    }

    public static Link remove(Link siteLink) {
        Site site = requireSite(siteLink);
        Properties properties = site.getProperties();
        if (properties == null || properties.getProperty() == null) {
            return siteLink;
        }
        List<Property> retained = properties.getProperty().stream()
                .filter(property -> !PROPERTY_NAME.equals(property.getName())
                        && !NODE_ID_PROPERTY_NAME.equals(property.getName()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (retained.size() == properties.getProperty().size()) {
            return siteLink;
        }
        Properties updatedProperties = retained.isEmpty() ? null
                : new PropertiesBuilder(properties).setProperty(retained).build();
        return rebuild(siteLink, site, updatedProperties);
    }

    private static Site getSite(Link siteLink) {
        if (siteLink == null) {
            return null;
        }
        Link1 augmentation = siteLink.getAugmentation(Link1.class);
        return augmentation == null ? null : augmentation.getSite();
    }

    private static Site requireSite(Link siteLink) {
        Site site = getSite(siteLink);
        if (site == null) {
            throw new IllegalArgumentException("siteLink has no site attributes");
        }
        return site;
    }

    private static Link rebuild(Link siteLink, Site site, Properties properties) {
        Link1 augmentation = siteLink.getAugmentation(Link1.class);
        return new LinkBuilder(siteLink)
                .addAugmentation(Link1.class, new Link1Builder(augmentation)
                        .setSite(new SiteBuilder(site)
                                .setProperties(properties)
                                .build())
                        .build())
                .build();
    }
}
