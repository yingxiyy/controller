/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryFriendly;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryReg;
import org.springframework.data.mongodb.core.MongoTemplate;

class TunnelComputer2GetShortPathsTest {

    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE = "sotn";
    private static final String COMPUTE_INPUT_JSON = "tunnel/compute-tunnels-2-input.json";

    @Test
    void shouldGetShortPaths() throws Exception {
        String inputJson = readClasspathResource(COMPUTE_INPUT_JSON);
        ComputeTunnels2Input computeInput = (ComputeTunnels2Input) MongoDaoTestSupport.getJsonUtil()
                .fromRpcJsonToDataObject("tunnel:compute-tunnels-2", inputJson, true);

        String mongoUri = System.getProperty("mongoUri", DEFAULT_MONGO_URI);
        String databaseName = System.getProperty("mongoDatabase", DEFAULT_DATABASE);

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
            TunnelComputer2 tunnelComputer2 = MongoDaoTestSupport.buildTunnelComputer2(mongoTemplate);

            List<ShortPath> shortPaths = tunnelComputer2.getShortPaths(computeInput);

            assertNotNull(shortPaths);
            assertFalse(shortPaths.isEmpty(), "getShortPaths should calculate add-leg route candidates");
            assertPrimaryRegMatchesIntermediateSiteLinks(shortPaths);

            System.out.println("getShortPathsSize=" + shortPaths.size());
            printShortPaths(shortPaths);
        }
    }

    private static void assertPrimaryRegMatchesIntermediateSiteLinks(List<ShortPath> shortPaths) {
        for (ShortPath shortPath : shortPaths) {
            assertNotNull(shortPath.getSiteLinkRoute().getPrimaryReg(),
                    "addFriendlyNameAndReg should initialize primaryReg");
            long siteLinkCount = shortPath.getSiteLinkRoute().getPrimary().stream()
                    .filter(SiteLinkIdNamingRule::isSiteLink)
                    .count();
            if (siteLinkCount > 1) {
                assertTrue(shortPath.getSiteLinkRoute().getPrimaryReg().size() == siteLinkCount - 1,
                        "primaryReg should have one intermediate site for each adjacent siteLink pair");
            }
        }
    }

    private static void printShortPaths(List<ShortPath> shortPaths) {
        for (int i = 0; i < shortPaths.size(); i++) {
            SiteLinkRoute route = shortPaths.get(i).getSiteLinkRoute();
            System.out.println("shortPath[" + i + "].primary:");
            if (route.getPrimaryFriendly() == null || route.getPrimaryFriendly().isEmpty()) {
                printRawPrimary(route);
            } else {
                for (PrimaryFriendly primaryFriendly : route.getPrimaryFriendly()) {
                    System.out.println("  pathName=" + primaryFriendly.getName());
                    System.out.println("  linkId=" + primaryFriendly.getLinkId());
                }
            }

            System.out.println("shortPath[" + i + "].primaryReg:");
            if (route.getPrimaryReg() == null || route.getPrimaryReg().isEmpty()) {
                System.out.println("  <empty>");
            } else {
                for (PrimaryReg primaryReg : route.getPrimaryReg()) {
                    System.out.println("  regName=" + primaryReg.getSiteName());
                    System.out.println("  regSiteId=" + primaryReg.getSiteId());
                    System.out.println("  regType=" + primaryReg.getType());
                }
            }
        }
    }

    private static void printRawPrimary(SiteLinkRoute route) {
        if (route.getPrimary() == null || route.getPrimary().isEmpty()) {
            System.out.println("  <empty>");
            return;
        }
        for (String linkId : route.getPrimary()) {
            System.out.println("  linkId=" + linkId);
        }
    }

    private static String readClasspathResource(String resourceName) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Missing classpath resource: " + resourceName);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
