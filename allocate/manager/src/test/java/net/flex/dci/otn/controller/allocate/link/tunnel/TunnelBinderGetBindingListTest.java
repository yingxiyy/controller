/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.get.binding.list.output.BindingRoutes;
import org.springframework.data.mongodb.core.MongoTemplate;

class TunnelBinderGetBindingListTest {

    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE = "sotn";

    @Test
    void shouldGetBindingList() throws Exception {
        System.setProperty("tunnelId",
            "Tunnel-Site-2075861344313282560#Ne-2076909116688830464#LINECARD-1-3#PORT-1-3-C1-Site-2075861343390535680#Ne-2076909118735650816#LINECARD-1-3#PORT-1-3-C1");

        String mongoUri = System.getProperty("mongoUri", DEFAULT_MONGO_URI);
        String databaseName = System.getProperty("mongoDatabase", DEFAULT_DATABASE);

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
            TunnelBinder tunnelBinder = MongoDaoTestSupport.buildTunnelBinder(mongoTemplate);

            String tunnelId = System.getProperty("tunnelId");
            GetBindingListOutput output;
            if (tunnelId == null || tunnelId.trim().isEmpty()) {
                return;
            } else {
                output = tunnelBinder.getBindingList(buildInput(tunnelId));
            }

            assertNotNull(output, "getBindingList output is null for tunnel " + tunnelId);
            assertNotNull(output.getBindingRoutes(),
                    "bindingRoutes is null for tunnel " + tunnelId);

            System.out.println("tunnelId=" + tunnelId);
            System.out.println("candidateRouteCount=" + output.getBindingRoutes().size());
            output.getBindingRoutes()
                    .forEach(candidate -> System.out.println("candidate=" + candidate));
            for (int i = 0; i < output.getBindingRoutes().size(); i++) {
                String inputJson = buildBindInputJson(tunnelId,
                        output.getBindingRoutes().get(i));
                System.out.println("candidateInputJson[" + i + "]=" + inputJson);
            }
        }
    }

    private static String buildBindInputJson(String tunnelId, BindingRoutes candidate)
            throws Exception {
        BindTunnelInput input = new BindTunnelInputBuilder()
                .setTunnelId(tunnelId)
                .setSiteLinkRoute(candidate.getSiteLinkRoute())
                .build();
        return MongoDaoTestSupport.getJsonUtil().fromRpcDataObjectToJson("tunnel:bind-tunnel", input, true);
    }

    private static String escapeJava(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static GetBindingListInput buildInput(String tunnelId) {
        return new GetBindingListInputBuilder().setTunnelId(tunnelId).build();
    }
}
