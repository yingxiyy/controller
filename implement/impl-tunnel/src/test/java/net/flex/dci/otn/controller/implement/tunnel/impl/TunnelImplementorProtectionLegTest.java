/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.tools.lock.AbstractResourceLock;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.data.mongodb.core.MongoTemplate;

class TunnelImplementorProtectionLegTest {

    private static final String COLLECTION_OCH_LINK = "config-och-link";
    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_MONGO_DATABASE = "sotn";
    private static final String ACTION_ADD_LEG = "add-leg";
    private static final String ACTION_REMOVE_LEG = "remove-leg";
    private static final String BINDING_3_RD_LEG = "binding3rdLeg";
    private static final String BINDING_3_RD_LEG_RESTORE_TUNNEL = "binding3rdLegRestoreTunnel";
    private static final String PROPERTY_START_SYNC_ADD_TUNNEL_ID = "startSyncAddTunnelId";
    private static final String PROPERTY_START_SYNC_REMOVE_TUNNEL_ID = "startSyncRemoveTunnelId";
    private static final String PROPERTY_ADD_TUNNEL_ID = "addTunnelId";
    private static final String PROPERTY_REMOVE_TUNNEL_ID = "removeTunnelId";
    private static final String DEFAULT_ADD_LEG_TUNNEL_ID =
            "Tunnel-Site-2071434922207350784#Ne-2073251017209810944#LINECARD-1-3#PORT-1-3-C5-Site-2071434923100737536#Ne-2073251018631680000#LINECARD-1-3#PORT-1-3-C5";

    private static TunnelImplementorMongoTestSupport.MongoFixture mongoFixture;
    private static MongoTemplate mongoTemplate;
    private static TunnelDao tunnelDao;
    private static OchLinkDao ochLinkDao;

    @BeforeAll
    static void initializeRealMongoDaosBeforeTunnelImplementorClassLoads() throws Exception {
        String mongoUri = System.getProperty("mongoUri", DEFAULT_MONGO_URI);
        String mongoDatabase = System.getProperty("mongoDatabase", DEFAULT_MONGO_DATABASE);
        mongoFixture = TunnelImplementorMongoTestSupport.open(mongoUri, mongoDatabase);
        mongoTemplate = mongoFixture.mongoTemplate();
        tunnelDao = mongoFixture.tunnelDao();
        ochLinkDao = mongoFixture.ochLinkDao();
    }

    @AfterAll
    static void closeMongoClient() {
        if (mongoFixture != null) {
            mongoFixture.close();
        }
    }

    @Test
    void runAddLegImplementorDoIt() throws Exception {
        ProtectionFlowSample sample = getSpecifiedProtectionFlowSample(ACTION_ADD_LEG,
                PROPERTY_ADD_TUNNEL_ID, PROPERTY_START_SYNC_ADD_TUNNEL_ID);
        TunnelImplementor implementor = newImplementor(sample.tunnel, ImplementState.Implement);

        invokeDoIt(implementor);

        Tunnel updatedTunnel = tunnelDao.getTunnelById(sample.tunnel.getTunnelId().getValue());
        assertNotNull(updatedTunnel);
        assertEquals(ImplementState.Implement, updatedTunnel.getImplementState());

        Link updatedOchLink = ochLinkDao.getOchLinkByLinkId(sample.ochLink.getLinkId().getValue());
        assertNotNull(updatedOchLink);
        Och updatedOch = updatedOchLink.getAugmentation(Link1.class).getOch();
        assertEquals(ImplementState.Implement, updatedOch.getImplementState());
        assertFalse(PropertyTool.existProperty(updatedOch.getProperties(), BINDING_3_RD_LEG),
                "successful add-leg implement should clear the OCH binding marker");
        assertFalse(PropertyTool.existProperty(updatedTunnel.getProperties(), BINDING_3_RD_LEG),
                "successful add-leg implement should clear the tunnel binding marker");
        assertFalse(PropertyTool.existProperty(updatedTunnel.getProperties(), BINDING_3_RD_LEG_RESTORE_TUNNEL),
                "successful add-leg implement should clear the tunnel restore marker");
    }

    @Test
    void runDoItForRemoveLegMarker() throws Exception {
        ProtectionFlowSample sample = getSpecifiedProtectionFlowSample(ACTION_REMOVE_LEG,
                PROPERTY_REMOVE_TUNNEL_ID, PROPERTY_START_SYNC_REMOVE_TUNNEL_ID);
        TunnelImplementor implementor = newImplementor(sample.tunnel, ImplementState.Allocate);

        Throwable thrown = invokeDoItExpectingDeviceFlowFailure(implementor);

        assertStackContains(thrown, "TunnelImplementor.toAllocate");
        assertStackContains(thrown, "TunnelImplementor.doIt");
    }

    @Test
    void startSyncActionForAddLegMarker() throws Exception {
        ProtectionFlowSample sample = getSpecifiedProtectionFlowSample(ACTION_ADD_LEG,
                PROPERTY_ADD_TUNNEL_ID, PROPERTY_START_SYNC_ADD_TUNNEL_ID);
        LifeCycleSevice lifeCycleSevice = buildLifeCycle(sample.tunnel.getTunnelId().getValue(),
                ImplementState.Implement);
        TunnelImplementor implementor = new TunnelImplementor(sample.tunnel.getTunnelId().getValue(),
                ImplementState.Implement, lifeCycleSevice);
        useNoopResourceLock(implementor);

        implementor.startSyncAction();

        TaskInfoMessage taskInfoMessage = lifeCycleSevice.getTaskInfoMessage();
        assertNotNull(taskInfoMessage);
        assertEquals(sample.tunnel.getTunnelId().getValue(), taskInfoMessage.getResourceId());
        assertEquals(sample.tunnel.getFriendlyName(), taskInfoMessage.getResourceName());
        assertNotNull(taskInfoMessage.getEndTime(), "startSyncAction should always finish the lifecycle record");
    }

    @Test
    void startSyncActionForRemoveLegMarker() throws Exception {
        ProtectionFlowSample sample = getSpecifiedProtectionFlowSample(ACTION_REMOVE_LEG,
                PROPERTY_REMOVE_TUNNEL_ID, PROPERTY_START_SYNC_REMOVE_TUNNEL_ID);
        LifeCycleSevice lifeCycleSevice = buildLifeCycle(sample.tunnel.getTunnelId().getValue(),
                ImplementState.Allocate);
        TunnelImplementor implementor = new TunnelImplementor(sample.tunnel.getTunnelId().getValue(),
                ImplementState.Allocate, lifeCycleSevice);
        useNoopResourceLock(implementor);

        implementor.startSyncAction();

        TaskInfoMessage taskInfoMessage = lifeCycleSevice.getTaskInfoMessage();
        assertNotNull(taskInfoMessage);
        assertEquals(sample.tunnel.getTunnelId().getValue(), taskInfoMessage.getResourceId());
        assertEquals(sample.tunnel.getFriendlyName(), taskInfoMessage.getResourceName());
        assertNotNull(taskInfoMessage.getEndTime(), "startSyncAction should always finish the lifecycle record");
        System.out.println("removeStartSyncSuccessfully=" + taskInfoMessage.getSuccessfully());
        System.out.println("removeStartSyncErrorReason=" + taskInfoMessage.getErrorReason());
        assertFalse(taskInfoMessage.getErrorReason() != null
                        && taskInfoMessage.getErrorReason().contains("NullPointerException"),
                "startSyncAction remove-leg should not fail with NullPointerException");
    }

//    @Test
    void findProtectionFlowSamplesFromMongo() {
        Optional<ProtectionFlowSample> addLegSample = findFirstProtectionFlowSample(ACTION_ADD_LEG);
        addLegSample.ifPresent(sample -> System.out.println("addLegTunnelId="
                + sample.tunnel.getTunnelId().getValue()
                + ", ochLinkId=" + sample.ochLink.getLinkId().getValue()
                + ", friendlyName=" + sample.tunnel.getFriendlyName()));

        Optional<ProtectionFlowSample> removeLegSample = findFirstProtectionFlowSample(ACTION_REMOVE_LEG);
        removeLegSample.ifPresent(sample -> System.out.println("removeLegTunnelId="
                + sample.tunnel.getTunnelId().getValue()
                + ", ochLinkId=" + sample.ochLink.getLinkId().getValue()
                + ", friendlyName=" + sample.tunnel.getFriendlyName()));

        assertTrue(addLegSample.isPresent() || removeLegSample.isPresent(),
                "local Mongo has no protection add/remove marker sample");
    }

    @Test
    void shouldRecognizeAddLegMarkerFromLocalMongo() {
        ProtectionFlowSample sample = getSpecifiedProtectionFlowSample(ACTION_ADD_LEG,
                PROPERTY_ADD_TUNNEL_ID, PROPERTY_START_SYNC_ADD_TUNNEL_ID);
        Och och = sample.och;

        assertEquals(ImplementState.PartialImplement, och.getImplementState());
        assertTrue(PropertyTool.existProperty(och.getProperties(), BINDING_3_RD_LEG));
        assertTrue(PropertyTool.existProperty(sample.tunnel.getProperties(), BINDING_3_RD_LEG));
        assertTrue(PropertyTool.existProperty(sample.tunnel.getProperties(), BINDING_3_RD_LEG_RESTORE_TUNNEL));
        assertNotNull(och.getExplictRoute());
        assertFalse(och.getExplictRoute().getRoute().isEmpty());
        assertNotNull(och.getExplictRoute().getRoute().get(0).getThird());
        assertFalse(och.getExplictRoute().getRoute().get(0).getThird().isEmpty());
    }

    @Test
    void shouldRecognizeRemoveLegMarkerFromLocalMongo() throws Exception {
        TunnelImplementor implementor = newImplementor(ImplementState.Allocate);
        ProtectionFlowSample sample = getSpecifiedProtectionFlowSample(ACTION_REMOVE_LEG,
                PROPERTY_REMOVE_TUNNEL_ID, PROPERTY_START_SYNC_REMOVE_TUNNEL_ID);
        Och och = sample.och;

        Object context = invoke(implementor, "getProtectionChangeContext", new Class[]{Och.class}, och);
        int legCount = (Integer) invoke(implementor, "getProtectionLegCount", new Class[]{Och.class}, och);
        int toLegCount = (Integer) invoke(context, "getToLegCount");

        assertNotNull(context);
        assertEquals(ACTION_REMOVE_LEG, invoke(context, "getAction"));
        assertFalse(((List<?>) invoke(context, "getSiteLinkIds")).isEmpty());
        assertTrue((Boolean) invoke(implementor, "isProtectionLegChange",
                new Class[]{Och.class, context.getClass()}, och, context));

        if (toLegCount == 1 && legCount > 1) {
            assertTrue((Boolean) invoke(implementor, "isProtectionRemoveToSingleLegBeforeDbTrim",
                    new Class[]{Och.class, context.getClass()}, och, context));
            assertFalse((Boolean) invoke(implementor, "isProtectionRemoveToSingleLeg",
                    new Class[]{Och.class, context.getClass()}, och, context));
        } else if (toLegCount == 1 && legCount == 1) {
            assertTrue((Boolean) invoke(implementor, "isProtectionRemoveToSingleLeg",
                    new Class[]{Och.class, context.getClass()}, och, context));
            assertFalse((Boolean) invoke(implementor, "isProtectionRemoveToSingleLegBeforeDbTrim",
                    new Class[]{Och.class, context.getClass()}, och, context));
        } else {
            assertTrue((Integer) invoke(context, "getFromLegCount") > toLegCount);
        }
    }

    @Test
    void shouldNotInferProtectionLegChangeWithoutMarkerFromLocalMongo() throws Exception {
        TunnelImplementor implementor = newImplementor(ImplementState.Implement);
        Optional<Och> ochFromMongo = findFirstOchWithoutMarker();
        assumeTrue(ochFromMongo.isPresent(), "local Mongo has no OCH without protection marker sample");
        Och och = ochFromMongo.get();

        Object context = invoke(implementor, "getProtectionChangeContext", new Class[]{Och.class}, och);

        assertNull(context);
        assertFalse((Boolean) invoke(implementor, "isProtectionLegChange",
                new Class[]{Och.class, protectionChangeContextClass()}, och, null));
    }

    private static ProtectionFlowSample getSpecifiedProtectionFlowSample(String action,
                                                                         String primaryProperty,
                                                                         String compatibleProperty) {
        String configuredTunnelId = getConfiguredTunnelId(primaryProperty, compatibleProperty);
        if (isBlank(configuredTunnelId) && ACTION_ADD_LEG.equals(action)) {
            configuredTunnelId = DEFAULT_ADD_LEG_TUNNEL_ID;
        }
        assumeTrue(!isBlank(configuredTunnelId), "set -D" + primaryProperty + " or -D" + compatibleProperty
                + " to run " + action + " manual test");

        Optional<ProtectionFlowSample> sample = findProtectionFlowSampleByTunnelId(action, configuredTunnelId);
        assumeTrue(sample.isPresent(), primaryProperty + "/" + compatibleProperty
                + " points to a tunnel without " + action + " marker data: " + configuredTunnelId);
        return sample.get();
    }

    private static Optional<ProtectionFlowSample> findProtectionFlowSampleByTunnelId(String action, String tunnelId) {
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (tunnel == null) {
            return Optional.empty();
        }

        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
        if (ochLink == null || ochLink.getAugmentation(Link1.class) == null
                || ochLink.getAugmentation(Link1.class).getOch() == null) {
            return Optional.empty();
        }

        Och och = ochLink.getAugmentation(Link1.class).getOch();
        if (ACTION_ADD_LEG.equals(action)) {
            if (!ImplementState.PartialImplement.equals(och.getImplementState())
                    || !PropertyTool.existProperty(och.getProperties(), BINDING_3_RD_LEG)
                    || !PropertyTool.existProperty(tunnel.getProperties(), BINDING_3_RD_LEG)
                    || !PropertyTool.existProperty(tunnel.getProperties(), BINDING_3_RD_LEG_RESTORE_TUNNEL)) {
                return Optional.empty();
            }
            return Optional.of(new ProtectionFlowSample(ochLink, och, tunnel));
        }
        Object context = getQuietly(newImplementor(targetStateForAction(action)), "getProtectionChangeContext",
                new Class[]{Och.class}, och);
        if (context == null || !action.equals(getQuietly(context, "getAction", new Class[]{}))) {
            return Optional.empty();
        }
        return Optional.of(new ProtectionFlowSample(ochLink, och, tunnel));
    }

    private static Optional<ProtectionFlowSample> findFirstProtectionFlowSample(String action) {
        List<Document> documents = mongoTemplate.getCollection(COLLECTION_OCH_LINK)
                .find(new Document("data.link.0.och-topology:och.properties.property",
                        new Document("$elemMatch", new Document("name", "protection-change-action")
                                .append("value", action))))
                .limit(100)
                .into(new java.util.ArrayList<>());
        for (Document document : documents) {
            Optional<Link> link = getOchLinkFromDocument(document);
            if (link.isPresent()) {
                Optional<Tunnel> tunnel = getFirstSupportedTunnel(link.get());
                if (tunnel.isPresent()) {
                    Och och = link.get().getAugmentation(Link1.class).getOch();
                    return Optional.of(new ProtectionFlowSample(link.get(), och, tunnel.get()));
                }
            }
        }
        return Optional.empty();
    }

    private static ImplementState targetStateForAction(String action) {
        return ACTION_REMOVE_LEG.equals(action) ? ImplementState.Allocate : ImplementState.Implement;
    }

    private static String getConfiguredTunnelId(String primaryProperty, String compatibleProperty) {
        String value = System.getProperty(primaryProperty);
        if (!isBlank(value)) {
            return value.trim();
        }
        value = System.getProperty(compatibleProperty);
        if (!isBlank(value)) {
            return value.trim();
        }
        return null;
    }

    private static Optional<Och> findFirstOchWithoutMarker() {
        List<Document> documents = mongoTemplate.getCollection(COLLECTION_OCH_LINK)
                .find()
                .limit(3000)
                .into(new java.util.ArrayList<>());
        for (Document document : documents) {
            Optional<Och> och = getOchFromDocument(document);
            if (och.isPresent()
                    && getQuietly(newImplementor(ImplementState.Implement), "getProtectionChangeContext",
                    new Class[]{Och.class}, och.get()) == null) {
                return och;
            }
        }
        return Optional.empty();
    }

    private static Optional<Och> getOchFromDocument(Document document) {
        Optional<Link> link = getOchLinkFromDocument(document);
        if (!link.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(link.get().getAugmentation(Link1.class).getOch());
    }

    private static Optional<Link> getOchLinkFromDocument(Document document) {
        String ochLinkId = document.getString("ochLinkId");
        if (ochLinkId == null || ochLinkId.trim().isEmpty()) {
            return Optional.empty();
        }

        Link link = ochLinkDao.getOchLinkByLinkId(ochLinkId);
        if (link == null || link.getAugmentation(Link1.class) == null
                || link.getAugmentation(Link1.class).getOch() == null) {
            return Optional.empty();
        }
        return Optional.of(link);
    }

    private static Optional<Tunnel> getFirstSupportedTunnel(Link ochLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 tunnelLink =
                ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
        if (tunnelLink == null || tunnelLink.getSupportedTunnel() == null
                || tunnelLink.getSupportedTunnel().isEmpty()) {
            return Optional.empty();
        }
        for (SupportedTunnel supportedTunnel : tunnelLink.getSupportedTunnel()) {
            Tunnel tunnel = tunnelDao.getTunnelById(supportedTunnel.getTunnelRef().getValue());
            if (tunnel != null) {
                return Optional.of(tunnel);
            }
        }
        return Optional.empty();
    }

    private static Object getQuietly(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return invoke(target, methodName, parameterTypes, args);
        } catch (Exception e) {
            return new Object();
        }
    }

    private static TunnelImplementor newImplementor(ImplementState targetState) {
        TunnelImplementor implementor = new TunnelImplementor("Tunnel-Test", targetState,
                buildLifeCycle("Tunnel-Test", targetState));
        useNoopResourceLock(implementor);
        return implementor;
    }

    private static TunnelImplementor newImplementor(Tunnel tunnel, ImplementState targetState) throws Exception {
        TunnelImplementor implementor = new TunnelImplementor(tunnel.getTunnelId().getValue(), targetState,
                buildLifeCycle(tunnel.getTunnelId().getValue(), targetState));
        setField(implementor, "tunnel", tunnel);
        useNoopResourceLock(implementor);
        return implementor;
    }

    private static void useNoopResourceLock(TunnelImplementor implementor) {
        try {
            setField(implementor, "resourceLockFactory",
                    (java.util.function.Supplier<AbstractResourceLock>) NoopResourceLock::new);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static LifeCycleSevice buildLifeCycle(String tunnelId, ImplementState targetState) {
        LifeCycleSevice lifeCycleSevice = new LifeCycleSevice();
        lifeCycleSevice.buildLifeService(tunnelId, TaskInfoMessage.ResourceType.tunnel, tunnelId,
                ImplementState.Allocate.equals(targetState) ? TaskInfoMessage.ActionType.deimplement
                        : TaskInfoMessage.ActionType.implement,
                "junit");
        return lifeCycleSevice;
    }

    private static void invokeDoIt(TunnelImplementor implementor) throws Exception {
        invoke(implementor, "doIt", new Class[]{net.flex.dci.otc.mongo.mdoel.ChangedObject.class},
                new net.flex.dci.otc.mongo.mdoel.ChangedObject());
    }

    private static Throwable invokeDoItExpectingDeviceFlowFailure(TunnelImplementor implementor) throws Exception {
        try {
            invokeDoIt(implementor);
        } catch (InvocationTargetException e) {
            return e.getTargetException();
        }
        throw new AssertionError("doIt completed successfully; this test expects local device/lock flow to fail before finishing");
    }

    private static void assertStackContains(Throwable throwable, String expectedFrameText) {
        boolean found = Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .anyMatch(frame -> frame.contains(expectedFrameText));
        assertTrue(found, "expected stack trace to contain " + expectedFrameText + ", actual: " + throwable);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = findMethod(target.getClass(), methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = findMethod(target.getClass(), methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Class<?> protectionChangeContextClass() throws ClassNotFoundException {
        return Class.forName(TunnelImplementor.class.getName() + "$ProtectionChangeContext");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class ProtectionFlowSample {
        private final Link ochLink;
        private final Och och;
        private final Tunnel tunnel;

        private ProtectionFlowSample(Link ochLink, Och och, Tunnel tunnel) {
            this.ochLink = ochLink;
            this.och = och;
            this.tunnel = tunnel;
        }
    }

    private static final class NoopResourceLock extends AbstractResourceLock {

        @Override
        public void addResource(String resource) {
        }

        @Override
        protected boolean tryLock() {
            return true;
        }

        @Override
        protected void waitLock(long timeout, TimeUnit unit) {
        }

        @Override
        protected void releaseLock() {
        }
    }
}
