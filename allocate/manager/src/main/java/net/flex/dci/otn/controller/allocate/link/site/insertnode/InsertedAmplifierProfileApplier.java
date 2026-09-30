package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.common.ByteDanceSpecConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.Amplifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
final class InsertedAmplifierProfileApplier {
    private static final BigDecimal INSERTED_NODE_TARGET_GAIN = new BigDecimal("20.0");

    private InsertedAmplifierProfileApplier() {
    }

    static Node apply(Link siteLink, Node insertedNode) {
        Site site = siteLink.getAugmentation(Link1.class).getSite();
        WDM_Band wdmBand = WDM_Band.fromString(site.getLinkGroup());
        ByteDanceSpecConfig config = ByteDanceSpecConfig.load();
        Physical physical = insertedNode.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> updatedXcs = new ArrayList<>();
        int amplifierCount = 0;
        int appliedCount = 0;

        for (CrossConnections xc : physical.getCrossConnections()) {
            if (xc.getAmplifier() == null) {
                updatedXcs.add(xc);
                continue;
            }
            amplifierCount++;

            String profileKey = profileKey(xc.getDescription());
            if (profileKey == null) {
                log.warn("cannot resolve inserted amplifier profile: node={}, xc={}, description={}",
                        insertedNode.getNodeId().getValue(), xcId(xc), xc.getDescription());
                updatedXcs.add(xc);
                continue;
            }

            try {
                ByteDanceSpecConfig.AmplifierProfile profile =
                        config.getAmplifierProfile(profileKey, wdmBand, site.getGrid());
                Amplifier amplifier = applyDefaults(xc.getAmplifier(), profile, profileKey);
                updatedXcs.add(new CrossConnectionsBuilder(xc)
                        .setAmplifier(amplifier)
                        .setProperties(addMissingProperties(xc.getProperties(), profile.properties))
                        .build());
                appliedCount++;
            } catch (CommonException e) {
                log.warn("cannot apply inserted amplifier profile: node={}, xc={}, profile={}, reason={}",
                        insertedNode.getNodeId().getValue(), xcId(xc), profileKey, e.getMessage());
                updatedXcs.add(xc);
            }
        }

        if (amplifierCount == 0) {
            log.warn("inserted node has no amplifier XC: node={}",
                    insertedNode.getNodeId().getValue());
            return insertedNode;
        }
        if (appliedCount == 0) {
            log.warn("no amplifier profile was applied to inserted node: node={}",
                    insertedNode.getNodeId().getValue());
        }

        return new NodeBuilder(insertedNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(physical)
                                .setCrossConnections(updatedXcs)
                                .build())
                        .build())
                .build();
    }

    private static Amplifier applyDefaults(Amplifier amplifier,
            ByteDanceSpecConfig.AmplifierProfile profile, String profileKey) {
        profile.validate(profileKey);
        AmplifierBuilder builder = new AmplifierBuilder(amplifier);

        builder.setTargetGain(INSERTED_NODE_TARGET_GAIN);
        builder.setGainRange(profile.resolveGainRange(
                profileKey, INSERTED_NODE_TARGET_GAIN.floatValue()));
        if (amplifier.getTargetGainTilt() == null && profile.targetGainTilt != null) {
            builder.setTargetGainTilt(BigDecimal.valueOf(profile.targetGainTilt.doubleValue()));
        }
        if (amplifier.getAmpMode() == null) {
            builder.setAmpMode(profile.resolveAmpMode(profileKey));
        }
        if (amplifier.getTargetAttenuation() == null && profile.targetAttenuation != null) {
            builder.setTargetAttenuation(BigDecimal.valueOf(profile.targetAttenuation.doubleValue()));
        }
        if (amplifier.isAutoPowerReduction() == null && profile.autoPowerReduction != null) {
            builder.setAutoPowerReduction(profile.autoPowerReduction);
        }
        if (amplifier.isEnable() == null && profile.enable != null) {
            builder.setEnable(profile.enable);
        }
        return builder.build();
    }

    private static Properties addMissingProperties(Properties properties,
            Map<String, String> defaults) {
        Properties result = properties;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String currentValue = PropertyTool.getValue(result, entry.getKey());
            if (currentValue == null || currentValue.trim().isEmpty()) {
                result = PropertyTool.addProperty(result, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    static String profileKey(String description) {
        if (description == null) {
            return null;
        }
        String upperDescription = description.toUpperCase(Locale.ROOT);
        if (upperDescription.contains("ILAL_EAST") || upperDescription.contains("ILAL_WEST")) {
            return "DGE_EAST_L";
        }
        if (upperDescription.contains("ILAC_EAST") || upperDescription.contains("ILAC_WEST")
                || upperDescription.contains("ILA_EAST") || upperDescription.contains("ILA_WEST")) {
            return "DGE_EAST_C";
        }
        return null;
    }

    private static String xcId(CrossConnections xc) {
        return xc.getCrossConnectionId() == null ? null : xc.getCrossConnectionId().getValue();
    }
}
