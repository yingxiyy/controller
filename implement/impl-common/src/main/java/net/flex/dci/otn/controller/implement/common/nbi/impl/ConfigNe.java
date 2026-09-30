/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.nbi.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigObjectType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.SuccessObjBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
class ConfigNe implements Callable<StepResult> {

    private NodeBuilder node;
    private boolean intercepted = false;

    public ConfigNe(NodeBuilder node, boolean intercepted) {
        this.node = node;
        this.intercepted = intercepted;
    }

    @Override
    public StepResult call() throws Exception {
        StepResult result = new StepResult();
        result.setNb(node);
        
        PhyNodeDao mongoDaoUtil = SpringBeanFinder.getBean(PhyNodeDao.class);
        Node dbNode = mongoDaoUtil.getConfigPhyNodeById(node.getNodeId().getValue());
        Node1 node1 = dbNode.getAugmentation(Node1.class);
        if (node1 == null) {
        	return null;
        } else {
        	boolean hasIp = false;
        	if (node1.getPhysical() != null && node1.getPhysical().getIp() != null) {
        		hasIp = true;
        	}
        	if (intercepted && !hasIp) {
        		ConfigNeOutput output = fillOutput(node.build());
        		result.setException(null);
	            result.setResult(output);
        	} else {
		        try {
		        	log.trace("configNe input: {}", node.build());
		            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object>
		            opResult = new LinkedList<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object>();

		            NodeBuilder newNode = stripOPEquip(opResult);

		            NeManagerRpc config = SpringBeanFinder.getBean(NeManagerRpc.class);
		            ConfigNeOutput output = config.configNe(newNode.build());

		            ConfigNeOutputBuilder outBuilder = new ConfigNeOutputBuilder(output);

		            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object>
		            succObjs = null;
		            if (outBuilder.getSuccessObj() != null && outBuilder.getSuccessObj().getObject() != null) {
		            	succObjs = outBuilder.getSuccessObj().getObject();
		            } else {
		            	succObjs = new ArrayList<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object>();
		            }
		            succObjs.addAll(opResult);
		            outBuilder.setSuccessObj(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.SuccessObjBuilder()
		            		.setObject(succObjs).build());

		            if (outBuilder.getFailObj().getObject() != null && outBuilder.getFailObj().getObject().size() > 0) {
		            	for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object obj: outBuilder.getFailObj().getObject()) {
		            		if (obj.getMessageInfo()!= null && !obj.getMessageInfo().isEmpty()) {
		            			result.setException(new CommonException(CommonExceptionType.DEVICE_ERROR, obj.getMessageInfo()));
		            		}
		            	}

		            } else {
		            	result.setException(null);
		            }
		            result.setResult(outBuilder.build());
		            log.trace("configNe result: {}", result);
		        } catch (CommonException e) {
		        	log.error("Exception during config ne", e);
		            result.setException(e);
		            result.setResult(null);
		        }
        	}
        }
        return result;
    }
    
    private NodeBuilder stripOPEquip(List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object> opResult) {
    	NodeBuilder newNodeBuilder = new NodeBuilder(node.build());
    	Node1 node1 = newNodeBuilder.getAugmentation(Node1.class);
        if (node1 != null) {
	    	Physical phy = node1.getPhysical();
	    	List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> 
	    	eqList = new LinkedList<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments>();
	    	if (phy != null && phy.getEquipments() != null) {
	    		eqList.addAll(phy.getEquipments());
	    	}
	    	
    		Iterator<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> it = eqList.iterator();
    		while (it.hasNext()) {
    			org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments eq = it.next();
    			EquipType eqType = eq.getEquipType();
    			boolean isOp = false;
    			if (EquipType.TRANSCEIVER.equals(eqType) && eq.getEquipmentId().endsWith("OSC")) {
    				isOp = true;
    			} else if (!EquipType.OT.equals(eqType) && !EquipType.Other.equals(eqType)) {
    				isOp = true;
    			}
    			if (isOp) {
    				opResult.add(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.ObjectBuilder()
    	    			.setObjectType(ConfigObjectType.EQUIP)
    	    			.setObjectId(eq.getEquipmentId())
    	    			.build());
    				it.remove();
    			}
    		}
    		newNodeBuilder.addAugmentation(Node1.class, new Node1Builder(node1)
    				.setPhysical(new PhysicalBuilder(phy)
    						.setEquipments(eqList)
    						.build())
    				.build());
        }
    	return newNodeBuilder;
    }
    
    private ConfigNeOutput fillOutput(Node node) {
    	List<TerminationPoint> tps = node.getTerminationPoint();
    	List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object> objs = new ArrayList<>();
    	for (TerminationPoint tp: tps) {
    		objs.add(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.ObjectBuilder()
    				.setObjectType(ConfigObjectType.PORT)
    				.setObjectId(tp.getTpId().getValue())
    				.build());
    	}
    	Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 != null) {
	    	Physical phy = node1.getPhysical();
	    	if (phy.getCrossConnections() != null) {
	    		for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections xc: phy.getCrossConnections()) {
	    			objs.add(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.ObjectBuilder()
	    	    			.setObjectType(ConfigObjectType.XC)
	    	    			.setObjectId(xc.getCrossConnectionId().getValue())
	    	    			.build());
	    		}
	    	}
	
	    	if (phy.getEquipments() != null) {
	    		for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments eq : phy.getEquipments()) {
	    			objs.add(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.ObjectBuilder()
	    	    			.setObjectType(ConfigObjectType.EQUIP)
	    	    			.setObjectId(eq.getEquipmentId())
	    	    			.build());
	    		}
	    	}
	
	    	if (phy.getInternalLinks() != null) {
	    		for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks il: phy.getInternalLinks()) {
	    			objs.add(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.ObjectBuilder()
	    	    			.setObjectType(ConfigObjectType.InternalLink)
	    	    			.setObjectId(il.getKey().getLinkName())
	    	    			.build());
	    		}
	    	}
	    	
	    	if (phy.getOCMGripGroups() != null ||phy.getSystem() != null) {
	    		objs.add(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.ObjectBuilder()
    	    			.setObjectType(ConfigObjectType.NE)
    	    			.setObjectId(node.getNodeId().getValue())
    	    			.build());
	    	}
        }

    	ConfigNeOutputBuilder builder = new ConfigNeOutputBuilder();
    	builder.setSuccessObj(new SuccessObjBuilder().setObject(objs).build());
    	return builder.build();
    }
}