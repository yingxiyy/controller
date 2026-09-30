/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.List;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.data.mongodb.core.MongoTemplate;

class GetTunnelMongoTest {

    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE = "sotn";

    @Test
    void shouldGetTunnelByTunnelId() throws Exception {
        String mongoUri = System.getProperty("mongoUri", DEFAULT_MONGO_URI);
        String databaseName = System.getProperty("mongoDatabase", DEFAULT_DATABASE);

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
            TunnelDao tunnelDao = MongoDaoTestSupport.buildTunnelDao(mongoTemplate);

            String tunnelId = System.getProperty("tunnelId");
            if (tunnelId == null || tunnelId.trim().isEmpty()) {
                List<String> tunnelIds = tunnelDao.getAllTunnelIds();
                assertFalse(tunnelIds.isEmpty(), "no tunnel data found in " + databaseName + ".config-tunnel");
                tunnelId = tunnelIds.get(0);
            }

            Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);

            assertNotNull(tunnel, "tunnel not found: " + tunnelId);
            assertEquals(tunnelId, tunnel.getTunnelId().getValue());

            System.out.println("tunnelId=" + tunnel.getTunnelId().getValue());
            System.out.println("friendlyName=" + tunnel.getFriendlyName());
            System.out.println("implementState=" + tunnel.getImplementState());
            System.out.println("supportingLink=" + tunnel.getSupportingLink());
        }
    }
}
