/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.base.NetConfAggregationDao;
import net.flex.dci.otc.mongo.base.aggregation.AggregationDao;
import net.flex.dci.otc.mongo.base.aggregation.AggregationPageDao;
import net.flex.dci.otc.mongo.base.aggregation.SimpleAggregationDao;
import net.flex.dci.otc.mongo.base.core.MongoDaoImpl;
import net.flex.dci.otc.mongo.base.core.SimpleMongoDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.OchNodeDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TopologyDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.dao.impl.OchLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.OchNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.PhyLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.PhyNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.SiteLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.SiteNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.SubNetTreeNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TopologyDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TunnelDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ViewLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ViewNodeDaoImpl;
import net.flex.dci.otc.mongo.repository.SubnetTreeNodeRepository;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otc.serialization.autoconfigure.SerializationAutoConfigure;
import net.flex.dci.otc.tools.lock.AbstractResourceLock;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.common.service.WdmUtilService;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.config.NEDefaultSystemConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.designer.site.LinkService;
import net.flex.dci.otn.controller.allocate.designer.site.XCService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OTAllocate;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OlsNodeService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtNodeService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtReusedStrategy;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTransceiverService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTpService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtXcService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.RoadmService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelLinkService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelNewOchAllocate;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelSiteAllocate;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutputBuilder;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

final class MongoDaoTestSupport {

    private static final List<Object> PUBLISHED_MESSAGES = Collections.synchronizedList(new ArrayList<>());

    private static JsonUtil jsonUtil;

    private MongoDaoTestSupport() {
    }

    static TunnelDao buildTunnelDao(MongoTemplate mongoTemplate) throws Exception {
        TunnelDaoImpl tunnelDao = new TunnelDaoImpl();
        wireBasicDao(tunnelDao, mongoTemplate);
        return tunnelDao;
    }

    static OchLinkDao buildOchLinkDao(MongoTemplate mongoTemplate) throws Exception {
        OchLinkDaoImpl ochLinkDao = new OchLinkDaoImpl();
        wireBasicDao(ochLinkDao, mongoTemplate);
        return ochLinkDao;
    }

    static SiteLinkDao buildSiteLinkDao(MongoTemplate mongoTemplate) throws Exception {
        SiteLinkDaoImpl siteLinkDao = new SiteLinkDaoImpl();
        wireBasicDao(siteLinkDao, mongoTemplate);
        return siteLinkDao;
    }

    static SiteLinkDao buildSiteLinkDao(MongoTemplate mongoTemplate, OchLinkDao ochLinkDao,
                                        SubNetTreeNodeDao subNetTreeNodeDao) throws Exception {
        SiteLinkDaoImpl siteLinkDao = new SiteLinkDaoImpl();
        wireBasicDao(siteLinkDao, mongoTemplate);
        setField(siteLinkDao, "ochLinkDao", ochLinkDao);
        setField(siteLinkDao, "subNetTreeNodeDao", subNetTreeNodeDao);
        return siteLinkDao;
    }

    static PhyLinkDao buildPhyLinkDao(MongoTemplate mongoTemplate) throws Exception {
        PhyLinkDaoImpl phyLinkDao = new PhyLinkDaoImpl();
        wireBasicDao(phyLinkDao, mongoTemplate);
        return phyLinkDao;
    }

    static PhyNodeDao buildPhyNodeDao(MongoTemplate mongoTemplate) throws Exception {
        PhyNodeDaoImpl phyNodeDao = new PhyNodeDaoImpl();
        wireBasicDao(phyNodeDao, mongoTemplate);
        return phyNodeDao;
    }

    static SiteNodeDao buildSiteNodeDao(MongoTemplate mongoTemplate) throws Exception {
        SiteNodeDaoImpl siteNodeDao = new SiteNodeDaoImpl();
        wireBasicDao(siteNodeDao, mongoTemplate);
        return siteNodeDao;
    }

    static TopologyDao buildTopologyDao(MongoTemplate mongoTemplate) throws Exception {
        TopologyDaoImpl topologyDao = new TopologyDaoImpl();
        wireBasicDao(topologyDao, mongoTemplate);
        return topologyDao;
    }

    static ViewLinkDao buildViewLinkDao(MongoTemplate mongoTemplate) throws Exception {
        ViewLinkDaoImpl viewLinkDao = new ViewLinkDaoImpl();
        wireBasicDao(viewLinkDao, mongoTemplate);
        return viewLinkDao;
    }

    static ViewNodeDao buildViewNodeDao(MongoTemplate mongoTemplate) throws Exception {
        ViewNodeDaoImpl viewNodeDao = new ViewNodeDaoImpl();
        wireBasicDao(viewNodeDao, mongoTemplate);
        return viewNodeDao;
    }

    static OchNodeDao buildOchNodeDao(MongoTemplate mongoTemplate) throws Exception {
        OchNodeDaoImpl ochNodeDao = new OchNodeDaoImpl();
        wireBasicDao(ochNodeDao, mongoTemplate);
        return ochNodeDao;
    }

    static SubNetTreeNodeDao buildSubNetTreeNodeDao(MongoTemplate mongoTemplate) throws Exception {
        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        SubnetTreeNodeRepository repository = repositoryFactory.getRepository(SubnetTreeNodeRepository.class);
        SubNetTreeNodeDaoImpl subNetTreeNodeDao = new SubNetTreeNodeDaoImpl(repository);
        wireBasicDao(subNetTreeNodeDao, mongoTemplate);
        return subNetTreeNodeDao;
    }

    static TunnelBinder buildTunnelBinder(MongoTemplate mongoTemplate) throws Exception {
        TunnelDao tunnelDao = buildTunnelDao(mongoTemplate);
        OchLinkDao ochLinkDao = buildOchLinkDao(mongoTemplate);
        SubNetTreeNodeDao subNetTreeNodeDao = buildSubNetTreeNodeDao(mongoTemplate);
        SiteLinkDao siteLinkDao = buildSiteLinkDao(mongoTemplate, ochLinkDao, subNetTreeNodeDao);
        PhyLinkDao phyLinkDao = buildPhyLinkDao(mongoTemplate);
        PhyNodeDao phyNodeDao = buildPhyNodeDao(mongoTemplate);
        SiteNodeDao siteNodeDao = buildSiteNodeDao(mongoTemplate);
        TopologyDao topologyDao = buildTopologyDao(mongoTemplate);
        ViewLinkDao viewLinkDao = buildViewLinkDao(mongoTemplate);
        ViewNodeDao viewNodeDao = buildViewNodeDao(mongoTemplate);
        OchNodeDao ochNodeDao = buildOchNodeDao(mongoTemplate);
        MultipleTransaction multipleTransaction = new MultipleTransaction(topologyDao, viewLinkDao, viewNodeDao,
                tunnelDao, ochLinkDao, ochNodeDao, siteLinkDao, siteNodeDao, phyLinkDao, phyNodeDao);

        NeDesigner neDesigner = buildNeDesigner();
        initializeSpringBeanFinder(tunnelDao, ochLinkDao, siteLinkDao, phyLinkDao, siteNodeDao, topologyDao,
                subNetTreeNodeDao, neDesigner, phyNodeDao, viewLinkDao, viewNodeDao, ochNodeDao,
                multipleTransaction);

        TunnelUtilService tunnelUtilService = new TunnelUtilService();
        setField(tunnelUtilService, "ochLinkDao", ochLinkDao);
        setField(tunnelUtilService, "tunnelDao", tunnelDao);
        setField(tunnelUtilService, "neDesigner", neDesigner);

        TunnelComputer2 tunnelComputer2 = new TunnelComputer2();
        setField(tunnelComputer2, "COMPUTED_ROUTE_LIMIT", 5);
        setField(tunnelComputer2, "siteLinkDao", siteLinkDao);
        setField(tunnelComputer2, "ochLinkDao", ochLinkDao);
        setField(tunnelComputer2, "phyLinkDao", phyLinkDao);
        setField(tunnelComputer2, "siteNodeDao", siteNodeDao);
        setField(tunnelComputer2, "tunnelUtils", new TunnelUtils());
        setField(tunnelComputer2, "neDesigner", neDesigner);
        setField(tunnelComputer2, "tunnelUtilService", tunnelUtilService);

        TunnelBinder tunnelBinder = new TunnelBinder(null);
        setField(tunnelBinder, "tunnelDao", tunnelDao);
        setField(tunnelBinder, "ochLinkDao", ochLinkDao);
        setField(tunnelBinder, "siteLinkDao", siteLinkDao);
        setField(tunnelBinder, "phyLinkDao", phyLinkDao);
        setField(tunnelBinder, "tunnelComputer2", tunnelComputer2);
        setField(tunnelBinder, "tunnelNewOchAllocate", buildTunnelNewOchAllocate(tunnelDao, ochLinkDao, siteLinkDao,
                phyLinkDao, phyNodeDao, siteNodeDao));
        setField(tunnelBinder, "jsonOutputer", buildJsonOutputer());
        setField(tunnelBinder, "phyLinkFriendlyName", new PhyLinkFriendlyName());
        setField(tunnelBinder, "phyNodeFriendlyName", new PhyNodeFriendlyName());
        setField(tunnelBinder, "resourceLockFactory",
                (java.util.function.Supplier<AbstractResourceLock>) NoopResourceLock::new);
        setFinalField(tunnelBinder, "multipleTransaction", multipleTransaction);
        return tunnelBinder;
    }

    static TunnelComputer2 buildTunnelComputer2(MongoTemplate mongoTemplate) throws Exception {
        TunnelDao tunnelDao = buildTunnelDao(mongoTemplate);
        OchLinkDao ochLinkDao = buildOchLinkDao(mongoTemplate);
        SubNetTreeNodeDao subNetTreeNodeDao = buildSubNetTreeNodeDao(mongoTemplate);
        SiteLinkDao siteLinkDao = buildSiteLinkDao(mongoTemplate, ochLinkDao, subNetTreeNodeDao);
        PhyLinkDao phyLinkDao = buildPhyLinkDao(mongoTemplate);
        PhyNodeDao phyNodeDao = buildPhyNodeDao(mongoTemplate);
        SiteNodeDao siteNodeDao = buildSiteNodeDao(mongoTemplate);
        TopologyDao topologyDao = buildTopologyDao(mongoTemplate);
        ViewLinkDao viewLinkDao = buildViewLinkDao(mongoTemplate);
        ViewNodeDao viewNodeDao = buildViewNodeDao(mongoTemplate);
        OchNodeDao ochNodeDao = buildOchNodeDao(mongoTemplate);
        MultipleTransaction multipleTransaction = new MultipleTransaction(topologyDao, viewLinkDao, viewNodeDao,
                tunnelDao, ochLinkDao, ochNodeDao, siteLinkDao, siteNodeDao, phyLinkDao, phyNodeDao);

        NeDesigner neDesigner = buildNeDesigner();
        initializeSpringBeanFinder(tunnelDao, ochLinkDao, siteLinkDao, phyLinkDao, siteNodeDao, topologyDao,
                subNetTreeNodeDao, neDesigner, phyNodeDao, viewLinkDao, viewNodeDao, ochNodeDao,
                multipleTransaction);

        TunnelUtilService tunnelUtilService = new TunnelUtilService();
        setField(tunnelUtilService, "ochLinkDao", ochLinkDao);
        setField(tunnelUtilService, "tunnelDao", tunnelDao);
        setField(tunnelUtilService, "neDesigner", neDesigner);

        TunnelComputer2 tunnelComputer2 = new TunnelComputer2();
        setField(tunnelComputer2, "COMPUTED_ROUTE_LIMIT", 5);
        setField(tunnelComputer2, "siteLinkDao", siteLinkDao);
        setField(tunnelComputer2, "ochLinkDao", ochLinkDao);
        setField(tunnelComputer2, "phyLinkDao", phyLinkDao);
        setField(tunnelComputer2, "siteNodeDao", siteNodeDao);
        setField(tunnelComputer2, "tunnelUtils", new TunnelUtils());
        setField(tunnelComputer2, "neDesigner", neDesigner);
        setField(tunnelComputer2, "tunnelUtilService", tunnelUtilService);
        return tunnelComputer2;
    }

    private static void initializeSpringBeanFinder(TunnelDao tunnelDao, OchLinkDao ochLinkDao, SiteLinkDao siteLinkDao,
                                                   PhyLinkDao phyLinkDao, SiteNodeDao siteNodeDao,
                                                   TopologyDao topologyDao, SubNetTreeNodeDao subNetTreeNodeDao,
                                                   NeDesigner neDesigner, PhyNodeDao phyNodeDao,
                                                   ViewLinkDao viewLinkDao, ViewNodeDao viewNodeDao,
                                                   OchNodeDao ochNodeDao,
                                                   MultipleTransaction multipleTransaction) throws Exception {
        AllocatorConfig allocatorConfig = new AllocatorConfig();
        setField(allocatorConfig, "yangModel", "ByteDance");
        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("tunnelDao", tunnelDao);
        context.getBeanFactory().registerSingleton("ochLinkDao", ochLinkDao);
        context.getBeanFactory().registerSingleton("siteLinkDao", siteLinkDao);
        context.getBeanFactory().registerSingleton("phyLinkDao", phyLinkDao);
        context.getBeanFactory().registerSingleton("phyNodeDao", phyNodeDao);
        context.getBeanFactory().registerSingleton("siteNodeDao", siteNodeDao);
        context.getBeanFactory().registerSingleton("topologyDao", topologyDao);
        context.getBeanFactory().registerSingleton("subNetTreeNodeDao", subNetTreeNodeDao);
        context.getBeanFactory().registerSingleton("viewLinkDao", viewLinkDao);
        context.getBeanFactory().registerSingleton("viewNodeDao", viewNodeDao);
        context.getBeanFactory().registerSingleton("ochNodeDao", ochNodeDao);
        context.getBeanFactory().registerSingleton("multipleTransaction", multipleTransaction);
        context.getBeanFactory().registerSingleton("neDesigner", neDesigner);
        context.getBeanFactory().registerSingleton("allocatorConfig", allocatorConfig);
        context.getBeanFactory().registerSingleton("neManagerRpc", noopNeManagerRpc());
        context.refresh();

        new SpringBeanFinder().setApplicationContext(context);
        stubKafkaPublishers();
    }

    private static NeManagerRpc noopNeManagerRpc() {
        return (NeManagerRpc) Proxy.newProxyInstance(NeManagerRpc.class.getClassLoader(),
                new Class[]{NeManagerRpc.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass().equals(Object.class)) {
                        if ("toString".equals(method.getName())) {
                            return "NoopNeManagerRpc";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                    }
                    if ("configNe".equals(method.getName())) {
                        return new ConfigNeOutputBuilder().build();
                    }
                    if ("removeResource".equals(method.getName())) {
                        return new RemoveResourceOutputBuilder().build();
                    }
                    if ("batchConfigNe".equals(method.getName())) {
                        return new BatchConfigNeOutputBuilder().build();
                    }
                    return null;
                });
    }

    private static TunnelNewOchAllocate buildTunnelNewOchAllocate(TunnelDao tunnelDao, OchLinkDao ochLinkDao,
                                                                   SiteLinkDao siteLinkDao, PhyLinkDao phyLinkDao,
                                                                   PhyNodeDao phyNodeDao, SiteNodeDao siteNodeDao)
            throws Exception {
        NEInfoConfig neInfoConfig = buildNeInfoConfig();
        JsonYangConverter jsonYangConverter = new JsonYangConverter();
        JsonOutputer jsonOutputer = buildJsonOutputer();
        TunnelUtils tunnelUtils = new TunnelUtils();
        EquipmentRepo equipmentRepo = new EquipmentRepo();
        setField(equipmentRepo, "jsonYangConverter", jsonYangConverter);
        TpRepo tpRepo = new TpRepo();
        setField(tpRepo, "jsonYangConverter", jsonYangConverter);
        LinkRepo linkRepo = new LinkRepo();
        NodeUtils nodeUtils = new NodeUtils();
        setField(nodeUtils, "equipmentRepo", equipmentRepo);
        setField(nodeUtils, "neInfoConfig", neInfoConfig);
        setField(nodeUtils, "tunnelUtils", tunnelUtils);
        OlsNodeService olsNodeService = new OlsNodeService();
        setField(olsNodeService, "phyNodeDao", phyNodeDao);
        setField(olsNodeService, "linkRepo", linkRepo);
        setField(olsNodeService, "tpRepo", tpRepo);
        setField(olsNodeService, "xcRepo", new XCRepo());
        setField(olsNodeService, "jsonOutputer", jsonOutputer);
        TunnelSiteAllocate tunnelSiteAllocate = new TunnelSiteAllocate();
        setField(tunnelSiteAllocate, "wdmUtilService", new WdmUtilService());
        setField(tunnelSiteAllocate, "equipmentRepo", equipmentRepo);
        setField(tunnelSiteAllocate, "neInfoConfig", neInfoConfig);
        setField(tunnelSiteAllocate, "olsNodeService", olsNodeService);
        setField(tunnelSiteAllocate, "phyNodeDao", phyNodeDao);
        setField(tunnelSiteAllocate, "tunnelUtils", tunnelUtils);
        setField(tunnelSiteAllocate, "nodeUtils", nodeUtils);
        setField(tunnelSiteAllocate, "yangModel", "ByteDance");
        TunnelLinkService tunnelLinkService = new TunnelLinkService();
        setField(tunnelLinkService, "linkRepo", linkRepo);
        OtTransceiverService otTransceiverService = new OtTransceiverService();
        setField(otTransceiverService, "equipmentRepo", equipmentRepo);
        setField(otTransceiverService, "tunnelUtils", tunnelUtils);
        OtXcService otXcService = new OtXcService();
        setField(otXcService, "xcRepo", new XCRepo());
        setField(otXcService, "xcService", new XCService());
        setField(otXcService, "tunnelUtils", tunnelUtils);
        setField(otXcService, "neInfoConfig", neInfoConfig);
        setField(otXcService, "nodeUtils", nodeUtils);
        OtNodeService otNodeService = new OtNodeService();
        setField(otNodeService, "otTransceiverService", otTransceiverService);
        setField(otNodeService, "linkRepo", linkRepo);
        OtTpService otTpService = new OtTpService();
        setField(otTpService, "tunnelUtils", tunnelUtils);
        setField(otNodeService, "otTpService", otTpService);
        setField(otNodeService, "nodeUtils", nodeUtils);
        setField(otNodeService, "tunnelUtils", tunnelUtils);
        setField(otNodeService, "neInfoConfig", neInfoConfig);
        setField(otNodeService, "equipmentRepo", equipmentRepo);
        setField(otNodeService, "tpRepo", tpRepo);
        OtReusedStrategy otReusedStrategy = new OtReusedStrategy();
        setField(otReusedStrategy, "phyNodeDao", phyNodeDao);
        setField(otReusedStrategy, "nodeUtils", nodeUtils);
        setField(otReusedStrategy, "neInfoConfig", neInfoConfig);
        setField(otReusedStrategy, "yangModel", "ByteDance");
        RoadmService roadmService = new RoadmService();
        setField(roadmService, "tunnelUtils", tunnelUtils);
        setField(roadmService, "phyNodeDao", phyNodeDao);
        setField(roadmService, "linkRepo", linkRepo);
        setField(roadmService, "phyLinkDao", phyLinkDao);
        setField(roadmService, "tpRepo", tpRepo);
        setField(roadmService, "olsNodeService", olsNodeService);
        setField(roadmService, "nodeUtils", nodeUtils);
        NEDefaultSystemConfig neDefaultSystemConfig = new NEDefaultSystemConfig();
        neDefaultSystemConfig.load();
        NeNodeRepo neNodeRepo = new NeNodeRepo();
        setField(neNodeRepo, "neDefaultSystemConfig", neDefaultSystemConfig);
        setField(neNodeRepo, "equipmentRepo", equipmentRepo);
        setField(neNodeRepo, "tpRepo", tpRepo);
        setField(neNodeRepo, "neInfoConfig", neInfoConfig);
        setField(neNodeRepo, "yangModel", "ByteDance");
        OTAllocate otAllocate = new OTAllocate();
        setField(otAllocate, "neNodeRepo", neNodeRepo);
        setField(otAllocate, "equipmentRepo", equipmentRepo);
        setField(otAllocate, "tpRepo", tpRepo);
        setField(otAllocate, "otXcService", otXcService);
        setField(otAllocate, "otNodeService", otNodeService);
        setField(otAllocate, "olsNodeService", olsNodeService);
        setField(otAllocate, "phyLinkDao", phyLinkDao);
        setField(otAllocate, "phyNodeDao", phyNodeDao);
        setField(otAllocate, "jsonOutputer", jsonOutputer);
        setField(otAllocate, "tunnelLinkService", tunnelLinkService);
        setField(otAllocate, "tunnelUtils", tunnelUtils);
        setField(otAllocate, "tunnelSiteAllocate", tunnelSiteAllocate);
        setField(otAllocate, "otReusedStrategy", otReusedStrategy);
        setField(otAllocate, "nodeUtils", nodeUtils);
        setField(otAllocate, "otTransceiverService", otTransceiverService);
        setField(otAllocate, "linkService", new LinkService());
        setField(otAllocate, "neInfoConfig", neInfoConfig);
        setField(otAllocate, "yangModel", "ByteDance");
        setField(otAllocate, "siteLinkDao", siteLinkDao);
        setField(otAllocate, "siteNodeDao", siteNodeDao);
        setField(otAllocate, "roadmService", roadmService);
        setField(otAllocate, "ochLinkDao", ochLinkDao);
        TunnelNewOchAllocate tunnelNewOchAllocate = new TunnelNewOchAllocate();
        setField(tunnelNewOchAllocate, "ochLinkDao", ochLinkDao);
        setField(tunnelNewOchAllocate, "phyNodeDao", phyNodeDao);
        setField(tunnelNewOchAllocate, "siteLinkDao", siteLinkDao);
        setField(tunnelNewOchAllocate, "nodeUtils", nodeUtils);
        setField(tunnelNewOchAllocate, "otReusedStrategy", otReusedStrategy);
        setField(tunnelNewOchAllocate, "otNodeService", otNodeService);
        setField(tunnelNewOchAllocate, "neInfoConfig", neInfoConfig);
        setField(tunnelNewOchAllocate, "tunnelUtils", tunnelUtils);
        setField(tunnelNewOchAllocate, "tunnelSiteAllocate", tunnelSiteAllocate);
        setField(tunnelNewOchAllocate, "otAllocate", otAllocate);
        setField(tunnelNewOchAllocate, "linkRepo", linkRepo);
        setField(tunnelNewOchAllocate, "phyLinkDao", phyLinkDao);
        setField(tunnelNewOchAllocate, "tunnelDao", tunnelDao);
        setField(tunnelNewOchAllocate, "roadmService", roadmService);
        return tunnelNewOchAllocate;
    }

    private static NeDesigner buildNeDesigner() throws Exception {
        NeDesigner neDesigner = new NeDesigner();
        setField(neDesigner, "neInfoConfig", buildNeInfoConfig());
        return neDesigner;
    }

    private static NEInfoConfig buildNeInfoConfig() throws Exception {
        NEInfoConfig neInfoConfig = new NEInfoConfig();
        setField(neInfoConfig, "yangModel", "ByteDance");
        setField(neInfoConfig, "resourceLoader", new DefaultResourceLoader());
        neInfoConfig.load();
        return neInfoConfig;
    }

    private static JsonOutputer buildJsonOutputer() throws Exception {
        JsonOutputer jsonOutputer = new JsonOutputer();
        setField(jsonOutputer, "jsonUtil", getJsonUtil());
        Method init = JsonOutputer.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(jsonOutputer);
        return jsonOutputer;
    }

    private static void wireBasicDao(Object dao, MongoTemplate mongoTemplate) throws Exception {
        SimpleMongoDao simpleMongoDao = new SimpleMongoDao(mongoTemplate);

        MongoDaoImpl mongoDao = new MongoDaoImpl();
        setField(mongoDao, "mongoManager", simpleMongoDao);
        setField(mongoDao, "jsonUtil", getJsonUtil());

        setField(dao, "mongoDao", mongoDao);
        setField(dao, "aggregationDao", buildNetConfAggregationDao(mongoTemplate));
        setField(dao, "mongoTemplate", mongoTemplate);
    }

    private static NetConfAggregationDao buildNetConfAggregationDao(MongoTemplate mongoTemplate) throws Exception {
        SimpleAggregationDao simpleAggregationDao = new SimpleAggregationDao(mongoTemplate);
        AggregationDao aggregationDao = new AggregationDao(simpleAggregationDao);
        AggregationPageDao aggregationPageDao = new AggregationPageDao(simpleAggregationDao, mongoTemplate);

        NetConfAggregationDao netConfAggregationDao = new NetConfAggregationDao();
        setField(netConfAggregationDao, "aggregationDao", aggregationDao);
        setField(netConfAggregationDao, "aggregationPageDao", aggregationPageDao);
        setField(netConfAggregationDao, "jsonUtil", getJsonUtil());
        return netConfAggregationDao;
    }

    static synchronized JsonUtil getJsonUtil() throws Exception {
        if (jsonUtil == null) {
            jsonUtil = new SerializationAutoConfigure().initJsonUtil();
        }
        return jsonUtil;
    }

    static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setFinalField(Object target, String fieldName, Object value) throws Exception {
        setField(target, fieldName, value);
    }

    private static void stubKafkaPublishers() throws Exception {
        PUBLISHED_MESSAGES.clear();
        PublishService publishService = new NoopPublishService();
        setStaticField(TaskInfoMessager.class, "publishService", publishService);
        setStaticField(BroadcastMessager.class, "publishService", publishService);
    }

    static List<Object> getPublishedMessages() {
        synchronized (PUBLISHED_MESSAGES) {
            return new ArrayList<>(PUBLISHED_MESSAGES);
        }
    }

    private static void setStaticField(Class<?> type, String fieldName, Object value) throws Exception {
        Field field = findField(type, fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static final class NoopResourceLock extends AbstractResourceLock {

        @Override
        public void addResource(String resource) {
            // Unit tests run without ZooKeeper; resource collection is intentionally skipped.
        }

        @Override
        protected boolean tryLock() {
            return true;
        }

        @Override
        protected void waitLock(long timeout, TimeUnit unit) {
            // Unit tests run without ZooKeeper; lock acquisition is intentionally a no-op.
        }

        @Override
        protected void releaseLock() {
            // Unit tests run without ZooKeeper; lock release is intentionally a no-op.
        }
    }

    private static final class NoopPublishService extends PublishService {
        private NoopPublishService() {
            super(null);
        }

        @Override
        public void send(String topic, Object message) {
            PUBLISHED_MESSAGES.add(message);
        }

        @Override
        public void send(String topic, String key, Object message) {
            PUBLISHED_MESSAGES.add(message);
        }

        @Override
        public void send(String topic, byte[] message) {
        }

        @Override
        public void newTopic(String topic) {
        }
    }

}
