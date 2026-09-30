/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Builder
@Data
@Slf4j
public class TunnelInput {

    private static final BigDecimal DEFAULT_OUTPUT_POWER = new BigDecimal(-1);

    @NonNull
    private List<Link> siteLinks;
    @NonNull
    private String vendorName;
    @NonNull
    private String vendorType;
    @NonNull
    private String cardType;//e.g. 200G-C4L2
    private SERVICETYPE servicetype;

    @NonNull
    private Class<? extends SignalProtocolType> tunnelSignalRate;
    @NonNull
    private Class<? extends SignalProtocolType> lineSignalRate;
    @NonNull
    private Class<? extends ETHERNETCOMPLIANCECODE> clientMedium;

    @Builder.Default
    private BigDecimal outputPower = DEFAULT_OUTPUT_POWER;

    /* 之所以没有把reusedNodesInDb和reusedNodesInMemory合成一个list，主要是担心万一重用策略，有类似先用inDB中的node这种策略**/
    @NonNull
    private List<Node> reusedNodesInDbSrc;//根据利旧策略，提供可利旧的node池
    @NonNull
    private List<Node> reusedNodesInDbDst;//根据利旧策略，提供可利旧的node池

    private List<Node> reusedNodesInMemorySrc;//当前批次创建业务产生的node，这些node还未入库
    private List<Node> reusedNodesInMemoryDst;//当前批次创建业务产生的node，这些node还未入库

    @NonNull
    private FrequencyAvailable frequencyAvailable;
    @NonNull
    private Node siteLinkSrcNode;
    @NonNull
    private Node siteLinkDestNode;
    @NonNull
    private String plane;
    @NonNull
    private String riskGroupName;
}
