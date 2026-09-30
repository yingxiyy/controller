/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.logMessage;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.TimeUtil;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.impl.TpUpdator;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.tunnel.impl.util.TunnelRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.update.tunnel.input.TestSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TunnelAttributeUpdateTestSignalStrategy extends AbstractTunnelAttributeUpdateStrategy {

    private static final String TEST_SIGNAL = "test-signal";
    private static final String LOOPBACK_MODE = "loopback-mode";

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private TpUpdator tpUpdator;


    @Override
    public boolean supports(UpdateTunnelInput updateTunnelInput) {
        return updateTunnelInput.getTestSignal() != null;
    }

    @Override
    public void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage) {
        log.info("start to execute update tunnel test signal,tunnel:{}", tunnelId);
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("cannot find required tunnel %s.", tunnelId));
        }
        if (ImplementState.Implement != tunnel.getImplementState()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Tunnel[%s] did not be implemented.", tunnelId));
        }
        String tunnelName = tunnel.getFriendlyName();
        TestSignal testSignal = updateTunnelInput.getTestSignal();
        AsynchronousExecutor.execute(() -> {
            try {
                updateTestSignal(tunnel, testSignal);
                logMessage(BroadCastConstant.MODIFY_TEST_SIGNAL, tunnelName, BLANK,
                        taskInfoMessage);
            } catch (CommonException e) {
                logMessage(BroadCastConstant.MODIFY_TEST_SIGNAL, tunnelName, e.getMessage(),
                        taskInfoMessage);
            }
        });
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.changeTestSignal;
    }

//    public void start(String tunnelId, TestSignal testSignal) {
//        ChangedObject changedObject = new ChangedObject();
//
//        Tunnel yangTunnel = changedObject.getChangedTunnel(tunnelId);
//        if (yangTunnel == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    String.format("cannot find required tunnel.", tunnelId));
//        }
//
//        if (ImplementState.Implement != yangTunnel.getImplementState()) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    String.format("Tunnel[%s] did not impl.", tunnelId));
//        }
//
//        new Thread() {
//            public void run() {
//                try {
//                    updateTestSignal(yangTunnel, testSignal);
//                    logMessage(BroadCastConstant.MODIFY_TEST_SIGNAL, yangTunnel.getFriendlyName(),
//                            BLANK);
//                } catch (CommonException e) {
//                    logMessage(BroadCastConstant.MODIFY_TEST_SIGNAL, yangTunnel.getFriendlyName(),
//                            e.getMessage());
//                }
//            }
//        }.start();
//    }

    private void updateTestSignal(Tunnel yangTunnel, TestSignal testSignal) throws CommonException {
        log.debug("start setting test signal");
        Boolean isClient = testSignal.isIsClient();
        Boolean isSourceTp = testSignal.isIsSourceTp();
        Boolean testSignalEnabled = testSignal.isTestSignal();
        /*
        description "Controller will automatically set two Client ports of tunnel.
      1. source-tp=true and test-signal=true.
        controller will automatically set another C port working as terminal loopback.
      2. source-tp=true and test-signal=false.
        controller will automaticall set another C port working as NONE loopback.";
         */
        String loopbackMode = "";
        if (isClient) {
            if (testSignalEnabled) {
                loopbackMode = "TERMINAL";
            } else {
                loopbackMode = "NONE";
            }

            String srcTp = TunnelIdNamingRule.getATp(yangTunnel.getTunnelId().getValue());
            String dstTp = TunnelIdNamingRule.getZtp(yangTunnel.getTunnelId().getValue());

            if (isSourceTp) {
                updateLinkTestSignal(srcTp, testSignalEnabled, dstTp, loopbackMode);
            } else {
                updateLinkTestSignal(dstTp, testSignalEnabled, srcTp, loopbackMode);
            }
        } else {
            if (testSignalEnabled) {
                loopbackMode = "FACILITY";
            } else {
                loopbackMode = "NONE";
            }

            //OCH 的A/Z 可能和UI（Tunnel）的不一样， UI提供的isSourceTp 是业务的
            String ochLinkId = TunnelRoute.getOchLinkId(yangTunnel);

            String srcNode = PhysicalTpIdNamingRule.getNodeId(
                    TunnelIdNamingRule.getATp(yangTunnel.getTunnelId().getValue()));
            String srcTp, dstTp;
            if (OchLinkIdNamingRule.getTpAId(ochLinkId).contains(srcNode)) {
                srcTp = OchLinkIdNamingRule.getTpAId(ochLinkId);
                dstTp = OchLinkIdNamingRule.getTpZId(ochLinkId);
            } else {
                srcTp = OchLinkIdNamingRule.getTpZId(ochLinkId);
                dstTp = OchLinkIdNamingRule.getTpAId(ochLinkId);
            }

            if (isSourceTp) {
                updateLinkTestSignal(srcTp, testSignalEnabled, dstTp, loopbackMode);
            } else {
                updateLinkTestSignal(dstTp, testSignalEnabled, srcTp, loopbackMode);
            }
        }
    }

    private void updateLinkTestSignal(String testSignalTp, Boolean testSignalEnabled,
            String loopbackTp, String loopbackMode) {
        //step1 make loopback
        setTpLoopback(loopbackTp, loopbackMode);

        try {
            //wait for loopback to be effective
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for loopback to take effect", e);
            Thread.currentThread().interrupt();
        }
        
        //step2 make signaltest
        setTpTestSignal(testSignalTp, testSignalEnabled);
    }

    private void setTpLoopback(String loopbackTp, String loopbackMode) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(loopbackTp);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (loopbackMode.equals("NONE")) {
            setTpLoopbackState(loopbackTp, loopbackMode);
            setTpAdminState(loopbackTp, AdminStatus.Up);
        } else {
            setTpAdminState(loopbackTp, AdminStatus.Maintenance);
            setTpLoopbackState(loopbackTp, loopbackMode);
        }
        saveLoopBackConfig(node, loopbackTp, loopbackMode);
    }

    private void saveLoopBackConfig(Node node, String loopbackTp, String loopbackMode) {
        AdminStatus adminStatus;
        if (loopbackMode.equals("NONE")) {
            adminStatus = AdminStatus.Up;
        } else {
            adminStatus = AdminStatus.Maintenance;
        }

        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(x -> {
            if (x.getTpId().getValue().equals(loopbackTp)) {
                Physical tpAttr = x.getAugmentation(TerminationPoint1.class).getPhysical();
                return new TerminationPointBuilder(x)
                        .addAugmentation(TerminationPoint1.class,
                                new TerminationPoint1Builder().setPhysical(
                                        new PhysicalBuilder(tpAttr)
                                                .setAdminState(adminStatus)
                                                .setProperties(PropertyTool.addProperty(
                                                        tpAttr.getProperties(), LOOPBACK_MODE,
                                                        loopbackMode))
                                                .build()
                                ).build()
                        ).build();
            } else {
                return x;
            }
        }).collect(Collectors.toList());
        Node newNode = new NodeBuilder(node).setTerminationPoint(newTpList).build();
        phyNodeDao.saveConfigPhyNode(newNode);
    }

    private void setTpAdminState(String tpId, AdminStatus adminStatus) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        TerminationPoint tp = new TerminationPointBuilder()
                .setTpId(new TpId(tpId))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setPhysical(new PhysicalBuilder()
                                .setAdminState(adminStatus)
                                .build()
                        ).build()
                ).build();

        tpUpdator.write2Ne(tp, nodeId);
    }

    private void setTpLoopbackState(String tpId, String loopbackMode) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        TerminationPoint tp = new TerminationPointBuilder()
                .setTpId(new TpId(tpId))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setPhysical(new PhysicalBuilder()
                                .setProperties(
                                        PropertyTool.addProperty(null, LOOPBACK_MODE, loopbackMode))
                                .build()
                        ).build()
                ).build();

        tpUpdator.write2Ne(tp, nodeId);
    }


    private void setTpTestSignal(String testSignalTp, boolean testSignalEnabled) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(testSignalTp);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (!testSignalEnabled) {
            setTpTestSignalState(testSignalTp, testSignalEnabled);
            setTpAdminState(testSignalTp, AdminStatus.Up);
        } else {
            setTpAdminState(testSignalTp, AdminStatus.Maintenance);
            setTpTestSignalState(testSignalTp, testSignalEnabled);
        }
        saveTestSignalConfig(node, testSignalTp, testSignalEnabled);
    }

    private void saveTestSignalConfig(Node node, String testSignalTp, Boolean testSignalEnabled) {
        AdminStatus adminStatus;
        if (!testSignalEnabled) {
            adminStatus = AdminStatus.Up;
        } else {
            adminStatus = AdminStatus.Maintenance;
        }

        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(x -> {
            if (x.getTpId().getValue().equals(testSignalTp)) {
                Physical tpAttr = x.getAugmentation(TerminationPoint1.class).getPhysical();
                return new TerminationPointBuilder(x)
                        .addAugmentation(TerminationPoint1.class,
                                new TerminationPoint1Builder().setPhysical(
                                        new PhysicalBuilder(tpAttr)
                                                .setAdminState(adminStatus)
                                                .setProperties(PropertyTool.addProperty(
                                                        tpAttr.getProperties(), TEST_SIGNAL,
                                                        testSignalEnabled.toString()))
                                                .build()
                                ).build()
                        ).build();
            } else {
                return x;
            }
        }).collect(Collectors.toList());
        Node newNode = new NodeBuilder(node).setTerminationPoint(newTpList).build();
        phyNodeDao.saveConfigPhyNode(newNode);
    }

    private void setTpTestSignalState(String tpId, Boolean testSignalEnabled) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        TerminationPoint tp = new TerminationPointBuilder()
                .setTpId(new TpId(tpId))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setPhysical(new PhysicalBuilder()
                                .setProperties(PropertyTool.addProperty(null, TEST_SIGNAL,
                                        testSignalEnabled.toString()))
                                .build()
                        ).build()
                ).build();

        tpUpdator.write2Ne(tp, nodeId);
    }


}
