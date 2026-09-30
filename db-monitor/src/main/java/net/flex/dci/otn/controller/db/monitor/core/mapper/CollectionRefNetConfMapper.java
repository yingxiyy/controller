/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core.mapper;

import static net.flex.dci.otc.common.util.YangConstants.OCH_TOPO_KEY;
import static net.flex.dci.otc.common.util.YangConstants.PHY_TOPO_KEY;
import static net.flex.dci.otc.common.util.YangConstants.SITE_TOPO_KEY;
import static net.flex.dci.otc.common.util.YangConstants.SITE_VIEW_TOPO_KEY;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.ADAPTER_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.OCH_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.OCH_NODE_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.PHY_NODE_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SCHEDULES_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SFTP_SERVER_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SITE_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.SITE_NODE_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.TELEMETRY_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.TOPOLOGY_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.TUNNEL_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.VIEW_LINK_COLLECTION;
import static net.flex.dci.otc.mongo.constants.MongoNetConfConstants.VIEW_NODE_COLLECTION;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.data.YangDataUtil;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.db.monitor.core.handler.IChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.AdapterChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.FtpChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.ScheduleChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.TelemetryServerChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.TunnelChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.link.OchLinkChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.link.PhyLinkChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.link.SiteLinkChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.link.ViewLinkChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.node.OchNodeChangeDataBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.node.PhyNodeChangeBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.node.SiteNodeChangeDataBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.handler.impl.node.ViewNodeChangeDataBodyHandler;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.CollectionDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;

/**
 * @version 1.0
 * @date 2021/11/5 14:43
 */
@Slf4j
public final class CollectionRefNetConfMapper {

    public static final Map<String, CollectionDto> collectionKeyMap = new HashMap<>();

    public static final Map<String, IChangeBodyHandler> changeBodyHandlerMap = new HashMap<>();

    static {
        loadCollectionMap();
        loadChangeBodyHandlerMap();
    }

    private static void loadChangeBodyHandlerMap() {
        //node change handler
        changeBodyHandlerMap.put(PHY_NODE_COLLECTION, new PhyNodeChangeBodyHandler());
        changeBodyHandlerMap.put(SITE_NODE_COLLECTION, new SiteNodeChangeDataBodyHandler());
        changeBodyHandlerMap.put(VIEW_NODE_COLLECTION, new ViewNodeChangeDataBodyHandler());
        changeBodyHandlerMap.put(OCH_NODE_COLLECTION, new OchNodeChangeDataBodyHandler());
        //link change handler
        changeBodyHandlerMap.put(PHY_LINK_COLLECTION, new PhyLinkChangeBodyHandler());
        changeBodyHandlerMap.put(SITE_LINK_COLLECTION, new SiteLinkChangeBodyHandler());
        changeBodyHandlerMap.put(VIEW_LINK_COLLECTION, new ViewLinkChangeBodyHandler());
        changeBodyHandlerMap.put(OCH_LINK_COLLECTION, new OchLinkChangeBodyHandler());
        //tunnel change handler
        changeBodyHandlerMap.put(TUNNEL_COLLECTION, new TunnelChangeBodyHandler());
        //ftp
        changeBodyHandlerMap.put(SFTP_SERVER_COLLECTION, new FtpChangeBodyHandler());
        //schedule
        changeBodyHandlerMap.put(SCHEDULES_COLLECTION, new ScheduleChangeBodyHandler());
        changeBodyHandlerMap.put(ADAPTER_COLLECTION, new AdapterChangeBodyHandler());
        changeBodyHandlerMap.put(TELEMETRY_COLLECTION, new TelemetryServerChangeBodyHandler());
    }

    private static void loadCollectionMap() {
        try {
            collectionKeyMap.put(PHY_NODE_COLLECTION, CollectionDto.builder().keyName("neId")
                    .method(YangDataUtil.class.getMethod("getPhyNodeIID", String.class))
                    .objectType(ObjectType.Node.name())
                    .objectKeyName("node-id")
                    .topologyType(PHY_TOPO_KEY)
                    .topologyRef(PHY_TOPO_KEY)
                    .dataKey("node")
                    .build());
            collectionKeyMap.put(SITE_NODE_COLLECTION, CollectionDto.builder().keyName("siteId")
                    .method(YangDataUtil.class.getMethod("getSiteIID", String.class))
                    .objectType(ObjectType.Node.name())
                    .objectKeyName("node-id")
                    .topologyType(SITE_TOPO_KEY)
                    .dataKey("node")
                    .topologyRef(SITE_TOPO_KEY).build());
            collectionKeyMap.put(VIEW_NODE_COLLECTION, CollectionDto.builder().keyName("viewNodeId")
                    .method(YangDataUtil.class.getMethod("getViewNodeIID", String.class))
                    .objectType(ObjectType.Node.name())
                    .objectKeyName("node-id")
                    .topologyRef(SITE_VIEW_TOPO_KEY)
                    .topologyType(SITE_VIEW_TOPO_KEY)
                    .dataKey("node")
                    .build());
            collectionKeyMap.put(OCH_NODE_COLLECTION, CollectionDto.builder().keyName("ochNodeId")
                    .method(YangDataUtil.class.getMethod("getOchNodeIID", String.class))
                    .topologyRef(OCH_TOPO_KEY)
                    .topologyType(OCH_TOPO_KEY)
                    .objectType(ObjectType.Node.name())
                    .objectKeyName("node-id")
                    .dataKey("node")
                    .build());
            collectionKeyMap.put(PHY_LINK_COLLECTION, CollectionDto.builder().keyName("linkId")
                    .method(YangDataUtil.class.getMethod("getPhyLinkIID", String.class))
                    .objectType(ObjectType.Link.name())
                    .objectKeyName("link-id")
                    .topologyRef(PHY_TOPO_KEY)
                    .topologyType(PHY_TOPO_KEY)
                    .dataKey("link")
                    .build());
            collectionKeyMap.put(SITE_LINK_COLLECTION, CollectionDto.builder().keyName("siteLinkId")
                    .method(YangDataUtil.class.getMethod("getSiteLinkIID", String.class))
                    .topologyRef(SITE_TOPO_KEY)
                    .topologyType(SITE_TOPO_KEY)
                    .objectType(ObjectType.Link.name())
                    .objectKeyName("link-id")
                    .dataKey("link")
                    .build());
            collectionKeyMap.put(VIEW_LINK_COLLECTION, CollectionDto.builder().keyName("viewLinkId")
                    .method(YangDataUtil.class.getMethod("getViewLinkIID", String.class))
                    .objectKeyName("link-id")
                    .objectType(ObjectType.Link.name())
                    .topologyRef(SITE_VIEW_TOPO_KEY)
                    .topologyType(SITE_VIEW_TOPO_KEY)
                    .dataKey("link")
                    .build());
            collectionKeyMap.put(OCH_LINK_COLLECTION, CollectionDto.builder().keyName("ochLinkId")
                    .method(YangDataUtil.class.getMethod("getOchLinkIID", String.class))
                    .objectType(ObjectType.Link.name())
                    .objectKeyName("link-id")
                    .topologyType(OCH_TOPO_KEY)
                    .topologyRef(OCH_TOPO_KEY)
                    .dataKey("link")
                    .build());
            collectionKeyMap.put(TUNNEL_COLLECTION, CollectionDto.builder().keyName("tunnelId")
                    .method(YangDataUtil.class.getMethod("getTunnelIID", String.class))
                    .objectType(ObjectType.Tunnel.name())
                    .objectKeyName("tunnel-id")
                    .topologyRef(SITE_TOPO_KEY)
                    .topologyType(SITE_TOPO_KEY)
                    .dataKey("tunnel:tunnel")
                    .build());
            collectionKeyMap.put(TOPOLOGY_COLLECTION,
                    CollectionDto.builder().keyName("topology")
                            .dataKey("network-topology")
                            .build());
            collectionKeyMap.put(SCHEDULES_COLLECTION, CollectionDto.builder().keyName("scheduleId")
                    .method(YangDataUtil.class.getMethod("getScheduleIID", String.class))
                    .objectType(ObjectType.Schedule.name())
                    .objectKeyName("id")
                    .dataKey("schedule")
                    .build());
            collectionKeyMap.put(ADAPTER_COLLECTION, CollectionDto.builder().keyName("adapterId")
                    .method(YangDataUtil.class.getMethod("getAdapterIID", String.class))
                    .objectType("Adapter")
                    .topologyRef(PHY_TOPO_KEY)
                    .topologyType(PHY_TOPO_KEY)
                    .objectKeyName("name")
                    .dataKey("adapter")
                    .build());
            collectionKeyMap.put(TELEMETRY_COLLECTION,
                    CollectionDto.builder().keyName("telemetryId")
                            .method(YangDataUtil.class.getMethod("getTelemetryIID", String.class))
                            .topologyRef(PHY_TOPO_KEY)
                            .topologyType(PHY_TOPO_KEY)
                            .objectType("Telemetry")
                            .objectKeyName("name")
                            .dataKey("telemetry-server")
                            .build());
            collectionKeyMap.put(SFTP_SERVER_COLLECTION,
                    CollectionDto.builder().keyName("sftpServerName")
                            .method(YangDataUtil.class.getMethod("getFtpServerIID", String.class))
                            .topologyType("Sftp")
                            .objectKeyName("name")
                            .dataKey("ftp-server")
                            .build());
        } catch (NoSuchMethodException e) {
            log.error("failed to init handler map");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to init handler map");
        }
    }


    public static CollectionDto extractColKeyName(String collectionName) {
        CollectionDto collectionDto = null;
        for (Entry<String, CollectionDto> entry : collectionKeyMap.entrySet()) {
            if (collectionName.contains(entry.getKey())) {
                collectionDto = entry.getValue();
            }
        }
        return collectionDto;
    }


}
