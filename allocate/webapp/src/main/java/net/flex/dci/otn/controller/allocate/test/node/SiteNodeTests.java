/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.test.node;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.serialization.ParserUtils;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCreator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateSitesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateSitesOutput;

@Slf4j
public class SiteNodeTests  {
	/**
	 * create one site nodes.
	 */
	public void case1() {
		String data = "{\n" +
				"    \"input\": {\n" +
				"        \"site\": [\n" +
				"            {\n" +
				"                \"site-topology:site\": {\n" +
				"                    \"friendly-name\": \"cd-" + System.currentTimeMillis() + "\",\n" +
				"                    \"domain-name\": \"beijing\"\n" +
				"                }\n" +
				"            }\n" +
				"        ]\n" +
				"    }\n" +
				"}";

		createSitNode(data);
	}

	private void createSitNode(String json) {
		String namespace = "site-topology";
		String cmd = "create-sites";

		try {
			CreateSitesInput input = (CreateSitesInput) ParserUtils.parseRpcInput(namespace, cmd, json);
			log.debug("createSites input {}", input);
			SiteNodeCreator creator = new SiteNodeCreator();
			CreateSitesOutput output = creator.doIt(input);

			String result = ParserUtils.serializeOutPutDataObject(namespace, cmd, output);

			log.debug("createSites output {}", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
