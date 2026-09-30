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
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.topology.type.ViewTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.topology.type.ViewTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.OtnPhyTopology1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.OtnPhyTopology1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.topology.type.OchTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.topology.type.OchTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Topology1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Topology1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopologyBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author YYX
 * @version 1.0
 */

/**
 * 检查topo数据结构是否准备好，没有准备就写一个到数据库
 */

@Slf4j
@Service
public class TopologyInitialor {

    /**
     * 检查site topo, 没有就添加所有topo
     */
    @Autowired
    TopologyDao topologyDao;
    @Autowired
    SiteTopologyInitializerDao siteTopologyInitializerDao;

    public void initTopo() {
        Site siteTopology = null;
        // getTopology() returns a synthesized object even when config-topo is absent. The site
        // attributes are read from config-topo itself, so a null value is the real initialization
        // condition.
        try {
            siteTopology = topologyDao.getSiteTopologyAttribute();
        } catch (Exception e) {
            // Preserve the original fallback: a failed read is followed by an initialization
            // attempt, whose write error is propagated to the caller.
            log.warn("failed to read site topology attributes, try to initialize config-topo", e);
        }

        if (siteTopology == null) {
            log.info("site topology attributes are missing, initialize config-topo");
            siteTopologyInitializerDao.initialize();
        }
    }

    private void createViewTopo() {
        TopologyId topoId = new TopologyId("site-" + ViewTopology.QNAME.getLocalName());
        TopologyTypes type = new TopologyTypesBuilder().addAugmentation(
                        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.TopologyTypes1.class,
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.TopologyTypes1Builder()
                                .setViewTopology(new ViewTopologyBuilder().build()).build())
                .build();

        Topology topo = new TopologyBuilder().setKey(new TopologyKey(topoId)).setTopologyId(topoId)
                .setTopologyTypes(type)
                .setLink(new LinkedList<>()).setNode(new LinkedList<>()).build();

        InstanceIdentifier<Topology> path = InstanceIdentifier.builder(NetworkTopology.class)
                .build().child(Topology.class,
                        new TopologyKey(topoId));

//    topologyDao.updateTopology(path, topo);
    }

    private void createSiteTopo() {
        TopologyId topoId = new TopologyId(SiteTopology.QNAME.getLocalName());
        TopologyTypes type = new TopologyTypesBuilder().addAugmentation(TopologyTypes1.class,
                        new TopologyTypes1Builder()
                                .setOtnPhyTopology(new OtnPhyTopologyBuilder().addAugmentation(
                                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.OtnPhyTopology1.class,
                                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.OtnPhyTopology1Builder()
                                                        .setSiteTopology(new SiteTopologyBuilder().build())
                                                        .build())
                                        .build()).build())
                .build();

        Topology1 siteTopo = new Topology1Builder()
                .setSite(new SiteBuilder().setRiskGroup(new LinkedList<>()).build())
                .build();
        Topology topo = new TopologyBuilder().setKey(new TopologyKey(topoId)).setTopologyId(topoId)
                .setTopologyTypes(type)
                .addAugmentation(Topology1.class, siteTopo)
                .setLink(new LinkedList<>()).setNode(new LinkedList<>()).build();

        InstanceIdentifier<Topology> path = InstanceIdentifier.builder(NetworkTopology.class)
                .build().child(Topology.class,
                        new TopologyKey(topoId));

//    topologyDao.updateTopology(path, topo);
    }

    private void createOchTopo() {
        TopologyId topoId = new TopologyId(OchTopology.QNAME.getLocalName());
        TopologyTypes type = new TopologyTypesBuilder()
                .addAugmentation(TopologyTypes1.class,
                        new TopologyTypes1Builder()
                                .setOtnPhyTopology(new OtnPhyTopologyBuilder()
                                        .addAugmentation(OtnPhyTopology1.class,
                                                new OtnPhyTopology1Builder()
                                                        .setOchTopology(new OchTopologyBuilder()
                                                                .build()).build())
                                        .build())
                                .build())
                .build();

        Topology topo = new TopologyBuilder().setKey(new TopologyKey(topoId)).setTopologyId(topoId)
                .setTopologyTypes(type)
                .setLink(new LinkedList<>()).setNode(new LinkedList<>()).build();

        InstanceIdentifier<Topology> path = InstanceIdentifier.builder(NetworkTopology.class)
                .build().child(Topology.class, new TopologyKey(topoId));

//    topologyDao.updateTopology(path, topo);
    }

    private void createPhyTopo() {
        TopologyId topoId = new TopologyId(OtnPhyTopology.QNAME.getLocalName());
        TopologyTypes type = new TopologyTypesBuilder()
                .addAugmentation(TopologyTypes1.class,
                        new TopologyTypes1Builder()
                                .setOtnPhyTopology(new OtnPhyTopologyBuilder().build()).build())
                .build();

        Topology topo = new TopologyBuilder().setKey(new TopologyKey(topoId)).setTopologyId(topoId)
                .setTopologyTypes(type)
                .setLink(new LinkedList<>()).setNode(new LinkedList<>()).build();

        InstanceIdentifier<Topology> path = InstanceIdentifier.builder(NetworkTopology.class)
                .build().child(Topology.class, new TopologyKey(topoId));
//    topologyDao.updateTopology(path, topo);
    }
}
