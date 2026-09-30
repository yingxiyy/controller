package net.flex.dci.otc.controller.ne.manager.utils;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NE_YANG_VERSION;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.controller.ne.manager.dto.NeInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

/**
 * @version 1.0
 * @date 6/11/2025 3:39 PM
 */
@Slf4j
public class NeInfoUtil {

    public static NeInfo getNeInfo(Node ne) {
        String version = null;
        Node1 neArg = ne.getAugmentation(Node1.class);
        Physical physical = neArg == null ? null : neArg.getPhysical();
        assert physical != null;
        Properties properties = physical.getProperties();
        version = PropertyTool.getValue(properties, NE_YANG_VERSION);
        NodeType nodeType = null;
        nodeType = physical.getNodeType();
        String ip = physical.getIp();
        String vendor = physical.getVendorName();
        return NeInfo.builder().yangVersion(version).ip(ip).vendor(vendor).nodeType(nodeType)
                .build();
    }

    public static String getYangVersion(Node ne) {
        String version = null;
        Node1 neArg = ne.getAugmentation(Node1.class);
        Physical physical = neArg == null ? null : neArg.getPhysical();
        assert physical != null;
        Properties properties = physical.getProperties();
        version = PropertyTool.getValue(properties, NE_YANG_VERSION);
        return version;
    }

    public static NodeType getNodeType(Node ne) {
        Node1 nodeArg = ne.getAugmentation(Node1.class);
        Physical physical = nodeArg == null ? null : nodeArg.getPhysical();
        NodeType nodeType = null;
        if (physical != null) {
            nodeType = physical.getNodeType();
        }

        return nodeType;
    }

    public static String getIp(Node ne) {
        String ip = null;
        Node1 nodeArg = ne.getAugmentation(Node1.class);

        if (nodeArg != null && nodeArg.getPhysical() != null) {
            ip = nodeArg.getPhysical().getIp();
        }
        return ip;
    }
}
