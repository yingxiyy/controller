/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate;

import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.TopologyDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.risk.group.RiskGroupBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Creates the initial config-topo document through a supported child data path.
 */
@Slf4j
@Repository
public class SiteTopologyInitializerDao {

    private static final String DEFAULT_RISK_GROUP_NAME = "DefaultRiskPlane";

    @Autowired
    private TopologyDao topologyDao;

    public void initialize() {
        // MongoDaoImpl rejects direct Topology writes. Saving the default risk group uses the same
        // child-path behavior that created config-topo before risk-group saves became conditional.
        topologyDao.mergeRiskGroup(new RiskGroupBuilder()
                .setRiskGroupName(DEFAULT_RISK_GROUP_NAME)
                .setPlane(new LinkedList<>())
                .build());
        log.info("initialized config-topo with risk group {}", DEFAULT_RISK_GROUP_NAME);
    }
}
