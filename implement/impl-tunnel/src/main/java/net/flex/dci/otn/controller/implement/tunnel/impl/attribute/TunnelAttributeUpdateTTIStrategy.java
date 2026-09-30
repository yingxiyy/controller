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

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.implement.common.impl.TpUpdator;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.tunnel.impl.util.TunnelRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.termination.point.input.Tps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.termination.point.input.TpsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.update.tunnel.input.TTI;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelAttributeUpdateTTIStrategy implements TunnelAttributeUpdateStrategy {


    private final TpUpdator tpUpdator;

    private final TunnelDao tunnelDao;

    @Override
    public boolean supports(UpdateTunnelInput updateTunnelInput) {
        return updateTunnelInput.getTTI() != null;
    }

    @Override
    public void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage) {
        log.info("update tunnel:{} tti", tunnelId);
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
        TTI tti = updateTunnelInput.getTTI();
        AsynchronousExecutor.execute(() -> {
            try {
                updateTTI(tunnel, tti);
                logMessage(BroadCastConstant.MODIFY_TTI, tunnelName, BLANK, taskInfoMessage);
            } catch (CommonException e) {
                logMessage(BroadCastConstant.MODIFY_TTI, tunnelName, e.getMessage(),
                        taskInfoMessage);
            }
        });

    }

    @Override
    public ActionType taskActionType() {
        return ActionType.changeTTI;
    }

//    public void start(String tunnelId, TTI tti) {
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
//                    updateTTI(yangTunnel, tti);
//                    logMessage(BroadCastConstant.MODIFY_TTI, yangTunnel.getFriendlyName(), BLANK);
//                } catch (CommonException e) {
//                    logMessage(BroadCastConstant.MODIFY_TTI, yangTunnel.getFriendlyName(),
//                            e.getMessage());
//                }
//            }
//        }.start();
//    }

    private void updateTTI(Tunnel yangTunnel, TTI tti) throws CommonException {
        if (yangTunnel != null) {
            Boolean autoMode = tti.isAutoMode();
            Boolean isClient = tti.isIsClient();
            Boolean isSourceTp = tti.isIsSourceTp();
            String expected = tti.getExpected();
            String transmit = tti.getTransmit();

            if (isClient) {
                for (SourceTp tp : yangTunnel.getSourceTp()) {
                    if (isSourceTp) {
                        updatePortTTI(tp.getTpRef(), autoMode, expected, transmit);
                    } else {
                        updatePortTTI(tp.getTpRef(), autoMode, transmit, expected);
                    }
                }

                for (DestinationTp tp : yangTunnel.getDestinationTp()) {
                    if (isSourceTp) {
                        updatePortTTI(tp.getTpRef(), autoMode, transmit, expected);
                    } else {
                        updatePortTTI(tp.getTpRef(), autoMode, expected, transmit);
                    }
                }
            } else {
                String ochLinkId = TunnelRoute.getOchLinkId(yangTunnel);

                if (isSourceTp) {
                    updatePortTTI(new TpId(OchLinkIdNamingRule.getTpAId(ochLinkId)), autoMode,
                            expected,
                            transmit);
                    updatePortTTI(new TpId(OchLinkIdNamingRule.getTpZId(ochLinkId)), autoMode,
                            transmit,
                            expected);
                } else {
                    updatePortTTI(new TpId(OchLinkIdNamingRule.getTpAId(ochLinkId)), autoMode,
                            transmit,
                            expected);
                    updatePortTTI(new TpId(OchLinkIdNamingRule.getTpZId(ochLinkId)), autoMode,
                            expected,
                            transmit);
                }
            }
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Tunnel[%s] does not exist.",
                            yangTunnel.getFriendlyName() == null ? yangTunnel.getTunnelId()
                                    .getValue() : yangTunnel.getFriendlyName()));
        }
    }


    private void updatePortTTI(TpId tpId, Boolean autoMode, String expected, String transit) {
        log.info("update tp port tti tpId:{} autoMode={} expected={} transit={}", tpId, autoMode,
                expected, transit);
        tpUpdator.configTerminationPoint(constructTp(tpId, autoMode, expected, transit));
    }

    private Tps constructTp(TpId tpId, Boolean autoMode, String expected, String transit) {
        return new TpsBuilder()
                .setTpId(tpId)
                .setPhysical(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                .setNodeRef(PhysicalTpIdNamingRule.getNodeId(tpId.getValue()))
                                .setProperties(buildProperties(autoMode, expected, transit))
                                .build())
                .build();
    }

    private Properties buildProperties(Boolean autoMode, String expected, String transit) {
        List<Property> pros = new ArrayList<>();
        if (autoMode) {
            Property pro = new PropertyBuilder().setName("tti-msg-auto").setValue("true").build();
            pros.add(pro);
        } else {
            Property proAutoMode = new PropertyBuilder().setName("tti-msg-auto").setValue("false")
                    .build();
            pros.add(proAutoMode);
            Property proExpected = new PropertyBuilder().setName("tti-msg-expected")
                    .setValue(expected).build();
            pros.add(proExpected);
            Property proTransit = new PropertyBuilder().setName("tti-msg-transmit")
                    .setValue(transit).build();
            pros.add(proTransit);
        }
        return new PropertiesBuilder().setProperty(pros).build();
    }


}
