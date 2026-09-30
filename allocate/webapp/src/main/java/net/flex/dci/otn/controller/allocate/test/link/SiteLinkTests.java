/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.test.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.serialization.ParserUtils;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkCreator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkOutput;

@Slf4j
public class SiteLinkTests  {

	/**
	 * 	 grid = 0, flex
	 * 	 grid = 1, 50
	 * 	 grid = 2, 100
	 * 	 grid = 3, 75
	 */

	/**
	 * create fix grid(50) siteLink as
	 *   O------O
	 */

	
	public void case1(String nodeA, String nodeZ, int grid) {
		log.debug("create siteLink case1 on {}", grid);
		log.debug("   O------O");
		String data = "{\n" +
				"    \"site-topology:input\": {\n" +
				"        \"frequency-grid\": " + grid + ",\n" +
				"        \"link-model\": \"2\",\n" +
				"        \"order-id\": \"order_" + System.currentTimeMillis() + "\",\n" +
				"        \"singleFrequencyPower\": -3,\n" +
				"        \"risk-group-name\": \"r2\",\n" +
				"        \"plane-name\": \"p1\",\n" +
				"        \"segment\": [\n" +
				"            {\n" +
				"                \"index\": 1,\n" +
				"                \"source\": \"" + nodeA + "\",\n" +
				"                \"destination\": \"" + nodeZ + "\",\n" +
				"                \"source-node-type\": \"SITE\",\n" +
				"                \"destination-node-type\": \"SITE\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }\n" +
				"        ],\n" +
				"        \"vendor-occupation-rate\": [ {\n" +
				"            \"vendor-name\": \"II-VI\",\n" +
				"            \"node-type\": \"OPC4\",\n" +
				"            \"product-type\": \"OPC-4\",\n" +
				"            \"number\": \"1\"\n" +
				"            }\n" +
				"        ]\n" +
				"    }\n" +
				"}";

		createSiteLink(data);
	}

	/**
	 * create flex grid siteLink as
	 *   O---I---O
	 */
	
	public void case2(String nodeA, String nodeI, String nodeZ, int grid) {
		log.debug("create siteLink case2 on {}", grid);
		log.debug("   O---I---O");

		String data = "{\n" +
				"    \"site-topology:input\": {\n" +
				"        \"frequency-grid\": " + grid + ",\n" +
				"        \"link-model\": \"2\",\n" +
				"        \"order-id\": \"order_" + System.currentTimeMillis() + "\",\n" +
				"        \"singleFrequencyPower\": -3,\n" +
				"        \"risk-group-name\": \"r2\",\n" +
				"        \"plane-name\": \"p1\",\n" +
				"        \"segment\": [\n" +
				"            {\n" +
				"                \"index\": 1,\n" +
				"                \"source\": \"" + nodeA + "\",\n" +
				"                \"destination\": \"" + nodeI + "\",\n" +
				"                \"source-node-type\": \"SITE\",\n" +
				"                \"destination-node-type\": \"ILA\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            },\n" +
				"            {\n" +
				"                \"index\": 2,\n" +
				"                \"source\": \"" + nodeI + "\",\n" +
				"                \"destination\": \"" + nodeZ + "\",\n" +
				"                \"source-node-type\": \"ILA\",\n" +
				"                \"destination-node-type\": \"SITE\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }" +
				"        ],\n" +
				"        \"vendor-occupation-rate\": [ {\n" +
				"            \"vendor-name\": \"II-VI\",\n" +
				"            \"node-type\": \"OPC4\",\n" +
				"            \"product-type\": \"OPC-4\",\n" +
				"            \"number\": \"1\"\n" +
				"            }\n" +
				"        ]\n" +
				"    }\n" +
				"}";
		createSiteLink(data);
	}

	/**
	 * create flex grid siteLink as
	 *   O---I---O
	 *   O-------O
	 */
	
	public void case3(String nodeA, String nodeI, String nodeZ, int grid) {
		log.debug("create siteLink case3 on {}", grid);
		log.debug("   O---I---O");
		log.debug("   O-------O");

		String data = "{\n" +
				"    \"site-topology:input\": {\n" +
				"        \"frequency-grid\": " + grid + ",\n" +
				"        \"link-model\": \"4\",\n" +
				"        \"order-id\": \"order_" + System.currentTimeMillis() + "\",\n" +
				"        \"singleFrequencyPower\": -3,\n" +
				"        \"risk-group-name\": \"r2\",\n" +
				"        \"plane-name\": \"p1\",\n" +
				"        \"segment\": [\n" +
				"            {\n" +
				"                \"index\": 1,\n" +
				"                \"source\": \"" + nodeA + "\",\n" +
				"                \"destination\": \"" + nodeI + "\",\n" +
				"                \"source-node-type\": \"SITE\",\n" +
				"                \"destination-node-type\": \"ILA\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            },\n" +
				"            {\n" +
				"                \"index\": 2,\n" +
				"                \"source\": \"" + nodeI + "\",\n" +
				"                \"destination\": \"" + nodeZ + "\",\n" +
				"                \"source-node-type\": \"ILA\",\n" +
				"                \"destination-node-type\": \"SITE\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }," +
				"            {\n" +
				"                \"index\": 3,\n" +
				"                \"source\": \"" + nodeA + "\",\n" +
				"                \"destination\": \"" + nodeZ + "\",\n" +
				"                \"source-node-type\": \"SITE\",\n" +
				"                \"destination-node-type\": \"SITE\",\n" +
				"                \"role\": \"spare\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }" +
				"        ],\n" +
				"        \"vendor-occupation-rate\": [ {\n" +
				"            \"vendor-name\": \"II-VI\",\n" +
				"            \"node-type\": \"OPC4\",\n" +
				"            \"product-type\": \"OPC-4\",\n" +
				"            \"number\": \"1\"\n" +
				"            }\n" +
				"        ]\n" +
				"    }\n" +
				"}";
		createSiteLink(data);
	}

	/**
	 * create flex grid siteLink as
	 *   O---I---I---O
	 *   O---I-------O
	 */
	
	public void case4(String nodeA, String nodeIM1, String nodeIM2, String nodeIS1, String nodeZ, int grid) {
		log.debug("create siteLink case4 on {}", grid);
		log.debug("   O---I---I---O");
		log.debug("   O---I-------O");

		String data = "{\n" +
				"    \"site-topology:input\": {\n" +
				"        \"frequency-grid\": " + grid + ",\n" +
				"        \"link-model\": \"4\",\n" +
				"        \"order-id\": \"order_" + System.currentTimeMillis() + "\",\n" +
				"        \"singleFrequencyPower\": -3,\n" +
				"        \"risk-group-name\": \"r2\",\n" +
				"        \"plane-name\": \"p1\",\n" +
				"        \"segment\": [\n" +
				"            {\n" +
				"                \"index\": 1,\n" +
				"                \"source\": \"" + nodeA + "\",\n" +
				"                \"destination\": \"" + nodeIM1 + "\",\n" +
				"                \"source-node-type\": \"SITE\",\n" +
				"                \"destination-node-type\": \"ILA\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            },\n" +
				"            {\n" +
				"                \"index\": 2,\n" +
				"                \"source\": \"" + nodeIM1 + "\",\n" +
				"                \"destination\": \"" + nodeIM2 + "\",\n" +
				"                \"source-node-type\": \"ILA\",\n" +
				"                \"destination-node-type\": \"ILA\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }," +
				"            {\n" +
				"                \"index\": 3,\n" +
				"                \"source\": \"" + nodeIM2 + "\",\n" +
				"                \"destination\": \"" + nodeZ + "\",\n" +
				"                \"source-node-type\": \"ILA\",\n" +
				"                \"destination-node-type\": \"SITE\",\n" +
				"                \"role\": \"main\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }," +
				"            {\n" +
				"                \"index\": 4,\n" +
				"                \"source\": \"" + nodeA + "\",\n" +
				"                \"destination\": \"" + nodeIS1 + "\",\n" +
				"                \"source-node-type\": \"SITE\",\n" +
				"                \"destination-node-type\": \"ILA\",\n" +
				"                \"role\": \"spare\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }," +
				"            {\n" +
				"                \"index\": 5,\n" +
				"                \"source\": \"" + nodeIS1 + "\",\n" +
				"                \"destination\": \"" + nodeZ + "\",\n" +
				"                \"source-node-type\": \"ILA\",\n" +
				"                \"destination-node-type\": \"SITE\",\n" +
				"                \"role\": \"spare\",\n" +
				"                \"provider\": {\n" +
				"                    \"attenuation\": 22,\n" +
				"                    \"distance\": 100,\n" +
				"                    \"fiber-type\": \"G-652\"\n" +
				"                }\n" +
				"            }" +
				"        ],\n" +
				"        \"vendor-occupation-rate\": [ {\n" +
				"            \"vendor-name\": \"II-VI\",\n" +
				"            \"node-type\": \"OPC4\",\n" +
				"            \"product-type\": \"OPC-4\",\n" +
				"            \"number\": \"1\"\n" +
				"            }\n" +
				"        ]\n" +
				"    }\n" +
				"}";
		createSiteLink(data);
	}

	private void createSiteLink(String json) {
		String namespace = "site-topology";
		String cmd = "create-link";

		try {
			CreateLinkInput input = (CreateLinkInput) ParserUtils.parseRpcInput(namespace, cmd, json);
			log.debug("createLink input {}", input);
			SiteLinkCreator<CreateLinkInput> siteLinkCreator = new SiteLinkCreator();
			CreateLinkOutput output = siteLinkCreator.doIt(input);

			String result = ParserUtils.serializeOutPutDataObject(namespace, cmd, output);
			log.debug("createLink output {}", result);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


}
