/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.base.core.MongoDaoImpl;
import net.flex.dci.otc.mongo.base.core.SimpleMongoDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.OchNodeDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
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
import net.flex.dci.otc.mongo.dao.impl.TopologyDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TunnelDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ViewLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ViewNodeDaoImpl;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otc.serialization.autoconfigure.SerializationAutoConfigure;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput.TargetApsMember;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

final class TunnelImplementorMongoTestSupport {

    private static JsonUtil jsonUtil;

    private TunnelImplementorMongoTestSupport() {
    }

    static MongoFixture open(String mongoUri, String databaseName) throws Exception {
        MongoClient mongoClient = MongoClients.create(mongoUri);
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);

        TunnelDao tunnelDao = buildTunnelDao(mongoTemplate);
        OchLinkDao ochLinkDao = buildOchLinkDao(mongoTemplate);
        SiteLinkDao siteLinkDao = buildSiteLinkDao(mongoTemplate);
        PhyLinkDao phyLinkDao = buildPhyLinkDao(mongoTemplate);
        PhyNodeDao phyNodeDao = buildPhyNodeDao(mongoTemplate);
        SiteNodeDao siteNodeDao = buildSiteNodeDao(mongoTemplate);
        TopologyDao topologyDao = buildTopologyDao(mongoTemplate);
        ViewLinkDao viewLinkDao = buildViewLinkDao(mongoTemplate);
        ViewNodeDao viewNodeDao = buildViewNodeDao(mongoTemplate);
        OchNodeDao ochNodeDao = buildOchNodeDao(mongoTemplate);

        MultipleTransaction multipleTransaction = new MultipleTransaction(topologyDao, viewLinkDao, viewNodeDao,
                tunnelDao, ochLinkDao, ochNodeDao, siteLinkDao, siteNodeDao, phyLinkDao, phyNodeDao);
        initializeSpringBeanFinder(tunnelDao, ochLinkDao, siteLinkDao, phyLinkDao, phyNodeDao, siteNodeDao,
                topologyDao, viewLinkDao, viewNodeDao, ochNodeDao, multipleTransaction);
        stubKafkaPublishers();

        return new MongoFixture(mongoClient, mongoTemplate, tunnelDao, ochLinkDao);
    }

    static TunnelDao buildTunnelDao(MongoTemplate mongoTemplate) throws Exception {
        TunnelDaoImpl tunnelDao = new TunnelDaoImpl();
        wireBasicDao(tunnelDao, mongoTemplate);
        return tunnelDao;
    }

    static OchLinkDao buildOchLinkDao(MongoTemplate mongoTemplate) throws Exception {
        OchLinkDaoImpl dao = new OchLinkDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static SiteLinkDao buildSiteLinkDao(MongoTemplate mongoTemplate) throws Exception {
        SiteLinkDaoImpl dao = new SiteLinkDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static PhyLinkDao buildPhyLinkDao(MongoTemplate mongoTemplate) throws Exception {
        PhyLinkDaoImpl dao = new PhyLinkDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static PhyNodeDao buildPhyNodeDao(MongoTemplate mongoTemplate) throws Exception {
        PhyNodeDaoImpl dao = new PhyNodeDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static SiteNodeDao buildSiteNodeDao(MongoTemplate mongoTemplate) throws Exception {
        SiteNodeDaoImpl dao = new SiteNodeDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static TopologyDao buildTopologyDao(MongoTemplate mongoTemplate) throws Exception {
        TopologyDaoImpl dao = new TopologyDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static ViewLinkDao buildViewLinkDao(MongoTemplate mongoTemplate) throws Exception {
        ViewLinkDaoImpl dao = new ViewLinkDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static ViewNodeDao buildViewNodeDao(MongoTemplate mongoTemplate) throws Exception {
        ViewNodeDaoImpl dao = new ViewNodeDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static OchNodeDao buildOchNodeDao(MongoTemplate mongoTemplate) throws Exception {
        OchNodeDaoImpl dao = new OchNodeDaoImpl();
        wireBasicDao(dao, mongoTemplate);
        return dao;
    }

    static synchronized JsonUtil getJsonUtil() throws Exception {
        if (jsonUtil == null) {
            jsonUtil = new SerializationAutoConfigure().initJsonUtil();
        }
        return jsonUtil;
    }

    private static void wireBasicDao(Object dao, MongoTemplate mongoTemplate) throws Exception {
        SimpleMongoDao simpleMongoDao = new SimpleMongoDao(mongoTemplate);

        MongoDaoImpl mongoDao = new MongoDaoImpl();
        setField(mongoDao, "mongoManager", simpleMongoDao);
        setField(mongoDao, "jsonUtil", getJsonUtil());

        setField(dao, "mongoDao", mongoDao);
        setField(dao, "mongoTemplate", mongoTemplate);
    }

    private static void initializeSpringBeanFinder(TunnelDao tunnelDao, OchLinkDao ochLinkDao,
            SiteLinkDao siteLinkDao, PhyLinkDao phyLinkDao, PhyNodeDao phyNodeDao, SiteNodeDao siteNodeDao,
            TopologyDao topologyDao, ViewLinkDao viewLinkDao, ViewNodeDao viewNodeDao, OchNodeDao ochNodeDao,
            MultipleTransaction multipleTransaction) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("phyNodeDao", phyNodeDao);
        context.getBeanFactory().registerSingleton("tunnelDao", tunnelDao);
        context.getBeanFactory().registerSingleton("ochLinkDao", ochLinkDao);
        context.getBeanFactory().registerSingleton("siteLinkDao", siteLinkDao);
        context.getBeanFactory().registerSingleton("phyLinkDao", phyLinkDao);
        context.getBeanFactory().registerSingleton("siteNodeDao", siteNodeDao);
        context.getBeanFactory().registerSingleton("topologyDao", topologyDao);
        context.getBeanFactory().registerSingleton("viewLinkDao", viewLinkDao);
        context.getBeanFactory().registerSingleton("viewNodeDao", viewNodeDao);
        context.getBeanFactory().registerSingleton("ochNodeDao", ochNodeDao);
        context.getBeanFactory().registerSingleton("multipleTransaction", multipleTransaction);
        context.getBeanFactory().registerSingleton("implConfig", new ImplConfig());
        context.getBeanFactory().registerSingleton("neManagerRpc", noopNeManagerRpc());
//        context.getBeanFactory().registerSingleton("apsSwitchManager", new NoopApsSwitchManager());
        context.refresh();

        new SpringBeanFinder().setApplicationContext(context);
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

    private static void stubKafkaPublishers() throws Exception {
        PublishService publishService = new NoopPublishService();
        setStaticField(TaskInfoMessager.class, "publishService", publishService);
        setStaticField(BroadcastMessager.class, "publishService", publishService);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
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

    static final class MongoFixture implements AutoCloseable {
        private final MongoClient mongoClient;
        private final MongoTemplate mongoTemplate;
        private final TunnelDao tunnelDao;
        private final OchLinkDao ochLinkDao;

        private MongoFixture(MongoClient mongoClient, MongoTemplate mongoTemplate, TunnelDao tunnelDao,
                OchLinkDao ochLinkDao) {
            this.mongoClient = mongoClient;
            this.mongoTemplate = mongoTemplate;
            this.tunnelDao = tunnelDao;
            this.ochLinkDao = ochLinkDao;
        }

        MongoTemplate mongoTemplate() {
            return mongoTemplate;
        }

        TunnelDao tunnelDao() {
            return tunnelDao;
        }

        OchLinkDao ochLinkDao() {
            return ochLinkDao;
        }

        @Override
        public void close() {
            mongoClient.close();
        }
    }

    private static final class NoopPublishService extends PublishService {
        private NoopPublishService() {
            super(null);
        }

        @Override
        public void send(String topic, Object message) {
        }

        @Override
        public void send(String topic, String key, Object message) {
        }

        @Override
        public void newTopic(String topic) {
        }
    }

//    private static final class NoopApsSwitchManager implements ApsSwitchManager {
//
//        @Override
//        public void batchTunnelApsSwitch(List<String> tunnelIds, ApsSwitch apsSwitch, ApsPath targetApsPath,
//                TaskInfoMessage taskInfoMessage) {
//        }
//
//        @Override
//        public SwitchResult executeApsSwitch(String neId, String apsName, String apsCrossConnectionId,
//                ApsPath targetPath, TaskInfoMessage taskInfoMessage) {
//            return SwitchResult.builder().code(SetResultCode.SUCCESS).message("noop").build();
//        }
//
//        @Override
//        public RestoreResult executeRestore(String neId, String apsName, String apsCrossConnectionId,
//                TargetApsMember restoreMember, TaskInfoMessage restoreTaskInfo) {
//            return null;
//        }
//    }
}
