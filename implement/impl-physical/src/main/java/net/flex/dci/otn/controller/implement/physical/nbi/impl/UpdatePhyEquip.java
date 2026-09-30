/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.Constant.Reboot;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.physical.util.PhysicalNodeUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.equip.input.Equipments;

/**
 * @version 1.0
 */
@Slf4j
public class UpdatePhyEquip extends BaseImpl {

    private final PhyNodeDao phyNodeDao;
    //    private ZkResourceLock locker;
    private Node ntNode;

    public UpdatePhyEquip() {
        phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
//        locker = new ZkResourceLock();
        ntNode = null;
    }

    @Override
    public UpdateEquipOutput doIt(UpdateEquipInput input) throws CommonException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments dbEq = checkParam(
                input);

        try {
//            lockResource(input);
//            updatePhysical(input, dbEq);
        } catch (Exception e) {
            log.error("failed to update equipment physical properties,the reason is :{}",
                    e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
        }

        UpdateEquipOutputBuilder outBuilder = new UpdateEquipOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }

    private void updatePhysical(UpdateEquipInput input,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments dbEq) {

        String dispString = "";
        try {
            Physical phNodeAttr = ntNode.getAugmentation(Node1.class).getPhysical();

            taskInfoMessage.setResourceId(ntNode.getNodeId().getValue());
            dispString = phNodeAttr.getFriendlyName();
            if (dispString == null || dispString.isEmpty()) {
                dispString = ntNode.getNodeId().getValue();
            }
            updatePhysical(input.getEquipments().get(0));
            logMessage(BroadCastConstant.UPDATE_EQ, dispString, BLANK);
        } catch (Exception e) {
            log.error("error happen", e);
            logMessage(BroadCastConstant.UPDATE_EQ, dispString, e.getMessage());
        }
    }

//    private void lockResource(UpdateEquipInput input) {
//        String nodeId = getNodeId(input);
//        locker.addResource(nodeId);
//
//        try {
//            locker.getLock();
//        } catch (Exception e) {
//            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
//                    "the Node has been used by other, please wait");
//        }
//    }

    private String getNodeId(UpdateEquipInput input) {
        Equipments inputEq = input.getEquipments().get(0);
        String nodeId = PhysicalEqpIdNamingRule.getNodeId(inputEq.getEquipmentId());
        return nodeId;
    }

    /**
     * the command only change one NE
     *
     * @param input
     * @return
     * @throws CommonException
     */
    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments checkParam(
            UpdateEquipInput input) throws CommonException {
        if (input.getEquipments().size() > 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "only suport change one Equipment's attributes");
        }

        String nodeId = getNodeId(input);
        ntNode = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (ntNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required node " + nodeId);
        }

        Equipments inputEq = input.getEquipments().get(0);
        Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> dbEqOp =
                ntNode.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                        .filter(t -> t.getEquipmentId().equals(inputEq.getEquipmentId()))
                        .findFirst();
        if (!dbEqOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required equipment");
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments dbEq = dbEqOp.get();

        if (inputEq.getProperties() != null) {
            for (Property pro : inputEq.getProperties().getProperty()) {
                if (!(pro.getName().equals(Reboot.ColdReboot) ||
                        pro.getName().equals(Reboot.WarmReboot) ||
                        pro.getName().equals(Constant.EquipmentCustomerInfo))) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "not support attribute change " + pro.getName());
                }
            }
        }

        ImplementState state = dbEq.getImplementState();
        if (state.equals(ImplementState.Allocate) || state.equals(ImplementState.Plan)) {
            if (inputEq.getAdminState() != null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the method only support change implemented equipment");
            }
            if (inputEq.getProperties() != null) {
                if (inputEq.getProperties().getProperty().stream().filter(t ->
                        (t.getName().equals(Reboot.ColdReboot) || t.getName()
                                .equals(Reboot.WarmReboot))).findAny().isPresent()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "the method only support change implemented equipment");
                }
            }
        }
        if (EquipType.EMPTY == dbEq.getEquipType()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "required equipment " + inputEq.getEquipmentId() + "is empty");
        } else if (PhysicalNodeUtils.checkEquipmentRebooting(dbEq)) {
            throw new CommonException(CommonExceptionType.NO_PERMISSION,
                    "required equipment " + inputEq.getEquipmentId() + "is rebooting");
        }
        return dbEq;
    }


    /**
     * support change adminStaus reboot(warm/cold) custom-info and adminStatus.
     *
     * @param eqp
     */
    private void updatePhysical(Equipments eqp) throws CommonException {
        Node want2update = null;  //0 is config, 1 is impl

        log.info("Start to config phy equipment {}", eqp.getEquipmentId());
        if (eqp.getProperties() != null) {//update Properties
            want2update = generateProperties(eqp, want2update);
            ntNode = generateProperties(eqp, ntNode);
        } else if (eqp.getAdminState() != null) {
            want2update = generateAdminState(eqp, want2update);
            ntNode = generateAdminState(eqp, ntNode);
        }

        log.info("Finish to config phy equipment {}", eqp.getEquipmentId());
        phyNodeDao.saveConfigPhyNode(ntNode);
        writeNe(want2update);
    }

    private Node generateAdminState(Equipments eqp, Node newNode) {
        newNode = PhysicalNodeUtils
                .setEquipAdminState(newNode, ntNode.getNodeId(), eqp.getEquipmentId(),
                        eqp.getAdminState());

        return newNode;
    }

    /**
     * properties reboot(warm/cold) custom-info
     *
     * @param eq
     * @param newNode
     * @throws CommonException
     */
    private Node generateProperties(Equipments eq, Node newNode) throws CommonException {
        for (Property prop : eq.getProperties().getProperty()) {
            if (prop.getName().equalsIgnoreCase(Constant.EquipmentCustomerInfo)) {
                newNode = PhysicalNodeUtils
                        .equipProperties(newNode, ntNode.getNodeId(), eq.getEquipmentId(),
                                prop.getName(), prop.getValue());
            } else if (prop.getName().equalsIgnoreCase(Reboot.ColdReboot) || prop.getName()
                    .equalsIgnoreCase(Reboot.WarmReboot)) {
                newNode = PhysicalNodeUtils
                        .equipProperties(newNode, ntNode.getNodeId(), eq.getEquipmentId(),
                                prop.getName(), prop.getValue());
                //reboot cannot cooperate with other actions.
                break;
            } else {
                log.error("find INVALID_PARAMETER {}" + prop.getName());
            }
        }
        return newNode;
    }

    private void writeNe(Node implNode) throws CommonException {
        NeManagerRpc neManagerRpc = SpringBeanFinder.getBean(NeManagerRpc.class);
        ConfigNeOutput result = neManagerRpc.configNe(implNode);

        if (result.getFailObj() != null && result.getFailObj().getObject() != null) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    convert(result.getFailObj()));
        }

    }


}
