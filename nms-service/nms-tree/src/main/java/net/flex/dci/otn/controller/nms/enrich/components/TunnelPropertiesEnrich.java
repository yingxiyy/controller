package net.flex.dci.otn.controller.nms.enrich.components;

import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEST_TP_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.DOMAIN_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_NODE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_SITE_NAME;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SOURCE_TP_NAME;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.LinkNodeInfo;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/1/17 13:29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelPropertiesEnrich implements DataObjectPropertiesEnrich {

    private final DciTopologyCacheManager cacheManager;

    @Override
    public DataObject enrichProperties(DataObject dataObject) {
        log.debug("start to enrich tunnel data object");
        DataObject richedDataObject = enrichTunnel((Tunnel) dataObject);
        return richedDataObject;
    }


    /**
     * enrich tunnel data
     *
     * @param tunnel
     * @return
     */
    private DataObject enrichTunnel(Tunnel tunnel) {
        Properties richProperties = enrichTunnelProperties(tunnel);
        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.fieldsFrom(tunnel);
        tunnelBuilder.setSupportingLink(null);
        tunnelBuilder.setExplictRoute(null);
        tunnelBuilder.setKey(new TunnelKey(tunnel.getTunnelId()));
        tunnelBuilder.setProperties(richProperties);
        return tunnelBuilder.build();
    }


    private Properties enrichTunnelProperties(Tunnel tunnel) {
        log.debug("enrich Phy Link Properties for tunnel id:{}", tunnel.getTunnelId().getValue());

        try {
            TunnelCache tunnelCache = cacheManager.getValue(
                    tunnel.getTunnelId().getValue(),
                    TunnelCache.class);
            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            List<Property> pList = tunnel.getProperties().getProperty();
            if (pList == null) {
                pList = new ArrayList<>();
            }
            LinkNodeInfo destination = tunnelCache.getDestination();
            LinkNodeInfo source = tunnelCache.getSource();

            PropertyTool.putKeyValue(pList, SOURCE_NODE_NAME, source.getNodeName());
            PropertyTool.putKeyValue(pList, SOURCE_NE_ID, source.getNodeId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_ID, source.getPortId());
            PropertyTool.putKeyValue(pList, SOURCE_TP_NAME, source.getPortName());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_ID, source.getSiteId());
            PropertyTool.putKeyValue(pList, SOURCE_SITE_NAME, source.getSiteName());

            PropertyTool.putKeyValue(pList, DEST_NODE_NAME, destination.getNodeName());
            PropertyTool.putKeyValue(pList, DEST_NE_ID, destination.getNodeId());
            PropertyTool.putKeyValue(pList, DEST_TP_ID, destination.getPortId());
            PropertyTool.putKeyValue(pList, DEST_TP_NAME, destination.getPortName());
            PropertyTool.putKeyValue(pList, DEST_SITE_NAME, destination.getSiteName());
            PropertyTool.putKeyValue(pList, DEST_SITE_ID, destination.getSiteId());

            PropertyTool.putKeyValue(pList, DOMAIN_NAME, tunnelCache.getDomainName());

            return propertiesBuilder.setProperty(pList).build();
        } catch (Exception ex) {
            log.error("failed to load the phy node cache exception ,message :{}", ex.getMessage(),
                    ex);
            return null;
        }
    }
}
