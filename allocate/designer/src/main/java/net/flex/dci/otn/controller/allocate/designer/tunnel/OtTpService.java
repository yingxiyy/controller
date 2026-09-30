/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import static net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil.FEC_DISABLE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.FecMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.ClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.line.attributes.ModelSpec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.line.attributes.ModelSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OtTpService {

    @Autowired
    private TunnelUtils tunnelUtils;


    public static final String TX_LASER = "tx_laser";
    public static final String TX_LASER_0 = "tx_laser_0";
    public static final String TX_LASER_1 = "tx_laser_1";
    public static final String TX_LASER_2 = "tx_laser_2";
    public static final String TX_LASER_3 = "tx_laser_3";

    public TerminationPoint createBusyLPortTp(TerminationPoint tp,
            Class<? extends SignalProtocolType> lineSignalRate, BigInteger centFreq,
            BigDecimal outputPower, SERVICETYPE serviceType,
            String opMode) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical oldPhysical = tp
                .getAugmentation(TerminationPoint1.class).getPhysical();

        List<Property> properties = new ArrayList<>();
        List<Property> oldProperties = oldPhysical.getProperties() == null ? null
                : oldPhysical.getProperties().getProperty();
        if (oldProperties != null && !oldProperties.isEmpty()) {
            properties.addAll(oldProperties);
        }

//        properties.add(new PropertyBuilder().setName("operationMode").setValue(tunnelUtils.getLPortOperationMode(lineSignalRate)).build());
        properties.add(new PropertyBuilder().setName("tti-msg-auto").setValue("true").build());
        properties.add(getLaserProperty(TX_LASER));

        if (tunnelUtils.isReg(serviceType)) {
//            properties.add(new PropertyBuilder().setName("loopback-mode").setValue("FACLITY").build());
            properties.add(
                    new PropertyBuilder().setName("regen-failure-propagated").setValue("GEN_AIS")
                            .build());
            properties.add(new PropertyBuilder().setName("ber-failure-threshold").setValue("0.021")
                    .build());
        }

        ModelSpec modelSpec = new ModelSpecBuilder()
                .setOpMode(opMode)
                .setServiceType(serviceType)
                .build();

        return new TerminationPointBuilder().setTpId(tp.getTpId())
                .setKey(new TerminationPointKey(new TpId(tp.getTpId())))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder()
                                .setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                oldPhysical)
                                                .setConnectionStatus(ConnectionStatus.Busy)
                                                .setOtuLine(new OtuLineBuilder()
                                                        .setCentralFrequency(
                                                                new FrequencyType(centFreq))
                                                        .setSignalRate(lineSignalRate)
                                                        .setTargetOutputPower(outputPower)
                                                        .setModelSpec(modelSpec)
                                                        .build())
                                                .setProperties(new PropertiesBuilder()
                                                        .setProperty(properties).build())
                                                .setAdminState(AdminStatus.Up)
                                                .build())
                                .build()).build();
    }


    public TerminationPoint createBusyCPortTp(TerminationPoint tp,
            Class<? extends SignalProtocolType> signalRate,
            Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical oldPhysical = tp.getAugmentation(
                TerminationPoint1.class).getPhysical();

        List<Property> properties = new ArrayList<>();
        List<Property> oldProperties = oldPhysical.getProperties() == null ? null
                : oldPhysical.getProperties().getProperty();
        if (oldProperties != null && !oldProperties.isEmpty()) {
            properties.addAll(oldProperties);
        }

        properties.add(new PropertyBuilder().setName("test-signal").setValue("false").build());
        properties.add(new PropertyBuilder().setName("loopback-mode").setValue("NONE").build());
        properties.add(new PropertyBuilder().setName("tti-msg-auto").setValue("true").build());
        properties.add(getLaserProperty(TX_LASER_0));
        properties.add(getLaserProperty(TX_LASER_1));
        properties.add(getLaserProperty(TX_LASER_2));
        properties.add(getLaserProperty(TX_LASER_3));

        return new TerminationPointBuilder().setTpId(tp.getTpId())
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setPhysical(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                        oldPhysical)
                                        .setConnectionStatus(ConnectionStatus.Busy)
                                        .setOtuClient(
                                                new OtuClientBuilder()
                                                        .setSignalRate(signalRate)
                                                        .setClient(new ClientBuilder()
                                                                .setEthComplianceCode(clientMedium)
                                                                .setFecMode(
                                                                        getFecMode(clientMedium))
                                                                .build())
                                                        .build())
                                        .setProperties(
                                                new PropertiesBuilder().setProperty(properties)
                                                        .build())
                                        .setAdminState(AdminStatus.Up)
                                        .build()).build())
                .build();
    }

    private FecMode getFecMode(Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) {
        if (FEC_DISABLE.contains(clientMedium)) {
            return FecMode.Disable;
        }
        return FecMode.Enable;

    }

    /**
     * 激光器开关
     *
     * @param name
     * @return
     */
    private Property getLaserProperty(String name) {
        return new PropertyBuilder().setName(name).setValue("true").build();
    }
}