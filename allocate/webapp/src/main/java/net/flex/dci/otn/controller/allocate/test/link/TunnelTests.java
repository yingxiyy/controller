/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.test.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.serialization.ParserUtils;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelComputer;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelCreator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.*;

@Slf4j
public class TunnelTests {

	/**
	 *
	 * @param nodeA
	 * @param nodeZ
	 * @param cardType
	 * @param LPortRate  look like common-otn-types:prot-OTUc2
	 * @param number
	 */
	
	public void case1(String nodeA, String nodeZ, String cardType, String LPortRate, int number) {
		log.debug("create tunnel case1: {}, {}, {}", cardType, LPortRate, number);

		String data = "{\n" +
				"    \"input\": {\n" +
				"        \"src-site\": \"" + nodeA + "\",\n" +
				"        \"dst-site\": \"" + nodeZ + "\",\n" +
				"        \"card-type\": \"" + cardType + "\",\n" +
				"        \"signal-rate\": \"common-otn-types:prot-100GE\",\n" +
				"        \"client-physical-medium\": \"common-otn-types:ETH_100GBASE_LR4\",\n" +
				"        \"line-signal-rate\": \"" + LPortRate + "\",\n" +
				"        \"routing-policy-id\": 1,\n" +
				"        \"customer\": \"customer\",\n" +
				"        \"order-id\": \"tunnel1\",\n" +
				"        \"bundle-number\": " + number + ",\n" +
				"        \"risk-group-info\": [\n" +
				"            {\n" +
				"                \"risk-group-name\": \"r2\",\n" +
				"                \"plane-name\": \"p1\"\n" +
				"            }\n" +
				"        ],\n" +
				"        \"vendor-occupation-rate\": [\n" +
				"            {\n" +
				"                \"vendor-name\": \"II-VI\",\n" +
				"                \"node-type\": \"TPC4\",\n" +
				"                \"product-type\": \"OPC-4\",\n" +
				"                \"number\": \"" + number + "\"\n" +
				"            }\n" +
				"        ],\n" +
				"        \"mandatory-node\": [],\n" +
				"        \"mandatory-site-link\": []\n" +
				"    }\n" +
				"}";

		ComputeTunnelsOutput output = computeTunnel(data);
		createTunnel(data, output);
	}

	private void createTunnel(String json, ComputeTunnelsOutput computeOutput) {
		String namespace = "tunnel";
		String create = "create-tunnel";
		String compute = "compute-tunnels";
		if (computeOutput == null)
			return;

		try {
			ComputeTunnelsInput computeInput = (ComputeTunnelsInput) ParserUtils.parseRpcInput(namespace, compute, json);
			CreateTunnelInput createInput = new CreateTunnelInputBuilder(computeInput)
					.setRouteBundleInfo(computeOutput.getResult().get(0).getRouteBundleInfo())
					.build();

			TunnelCreator creator = new TunnelCreator();
			CreateTunnelOutput output = creator.doIt(createInput);

			String result = ParserUtils.serializeOutPutDataObject(namespace, create, output);
			log.debug("createTunnel output {}", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ComputeTunnelsOutput computeTunnel(String json) {
		String namespace = "tunnel";
		String cmd = "compute-tunnels";

		try {
			ComputeTunnelsInput input = (ComputeTunnelsInput) ParserUtils.parseRpcInput(namespace, cmd, json);
			log.debug("computeTunnel input {}", input);
			TunnelComputer creator = new TunnelComputer();
			ComputeTunnelsOutput output = creator.doIt(input);

			String result = ParserUtils.serializeOutPutDataObject(namespace, cmd, output);
			log.debug("computeTunnel output {}", result);

			return output;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void deleteTunnel() {
		TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
		tunnelDao.listTunnels();
	}
}
