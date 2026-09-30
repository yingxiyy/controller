/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import static net.flex.dci.otn.controller.allocate.ne.Amplifier.GainRange.LOW_GAIN_RANGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import net.flex.dci.otn.controller.allocate.ne.Amplifier;
import net.flex.dci.otn.controller.allocate.ne.Amplifier.GainRange;
import net.flex.dci.otn.controller.allocate.ne.Param;
import net.flex.dci.otn.controller.allocate.ne.SupportedSignal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.AmpMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JsonYangConverter {

    private static final String FMUX_32_CARD_TYPE = "FMUX_32";
    private static final String TILA_CARD_TYPE = "TILA";

    /**
     * Input example: "OA-Sig", "MUX-Channel"
     *
     * Output example: OASig, MUXChannel
     *
     * @param jsonPortType
     * @return
     * @throws NeDesignerException
     */
    public PortType getPortType(String jsonPortType) throws NeDesignerException {

        String portTypeName = jsonPortType.replace("-", "");

        PortType result = Arrays.stream(PortType.values()).filter(e -> e.name().equalsIgnoreCase(portTypeName)).findAny().orElse(null);

        if (result == null) {
            throw new NeDesignerException("Unsupported portType: " + jsonPortType);
        }

        return result;
    }

    /**
     * Input example: "MUX_4x100G"
     *
     * Output example: "MUX4x100G"
     *
     * @param serviceType
     * @return
     * @throws NeDesignerException
     */
    public SERVICETYPE getServiceType(String serviceType) throws NeDesignerException {

        String serviceTypeName = serviceType.replace("_", "");

        SERVICETYPE result = Arrays.stream(SERVICETYPE.values()).filter(e -> e.name().equalsIgnoreCase(serviceTypeName)).findAny().orElse(null);

        if (result == null) {
            throw new NeDesignerException("Unsupported serviceType: " + serviceType);
        }

        return result;
    }


    public EquipType getEquipTypeByCardClass(String cardClass) throws NeDesignerException {
        switch (cardClass){
            case "CMUX":
                return EquipType.CMUX64;
            case "PANEL":
                return EquipType.Other;
            case "MUX64":
                return EquipType.MUX;
            default:
                try {
                    return EquipType.valueOf(cardClass);
                } catch (IllegalArgumentException e) {
                    String errorMessage = "Failed to convert cardClass to EquipType, becaus of unsupported cardClass: " + cardClass;
                    log.error(errorMessage, e);
                    throw new NeDesignerException(errorMessage);
                }
        }
    }

    public EquipType getEquipTypeByCardClassAndType(String vendorName, String productType, String cardClass,
            String cardType)
            throws NeDesignerException {
        if (ProductTypeResolver.isBone20ProductType(vendorName, productType)
                && (FMUX_32_CARD_TYPE.equals(cardType) || TILA_CARD_TYPE.equals(cardType))) {
            return parseEnum(cardType, EquipType.class);
        }
        return getEquipTypeByCardClass(cardClass);
    }

    public SupportedSignal getSupportedSignal(Class<? extends SignalProtocolType> signalRate) throws NeDesignerException {
        if (signalRate.equals(Prot100GE.class)) {
            return SupportedSignal.OTU_4;
        } else if (signalRate.equals(ProtOTU4.class)) {
            return SupportedSignal.OTU_4;
        } else if (signalRate.equals(ProtOTUc2.class)) {
            return SupportedSignal.OTU_4_X_2;
        } else if (signalRate.equals(ProtOTUc3.class)) {
            return SupportedSignal.OTU_4_X_3;
        } else if (signalRate.equals(ProtOTUc4.class)) {
            return SupportedSignal.OTU_4_X_4;
//        } else if (signalRate.equals(ProtOTUc6.class)) {
//            return SupportedSignal.OTU_4_X_6;
        } else {
            throw new NeDesignerException("Failed to get ODU type, unsupported signalRate " + signalRate.getSimpleName());
        }
    }

    public static <T extends Enum<T>> T parseEnum(String input, Class<T> enumClass) throws NeDesignerException {
        if (input == null || enumClass == null) {
            throw new IllegalArgumentException("Input string or enum class cannot be null");
        }

        String normalized = input.replace("_", "").toUpperCase();

        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalized)) {
                return constant;
            }
        }

        throw new NeDesignerException("Unknown value: " + input + " for enum type: " + enumClass.getSimpleName());
    }


    public Class<? extends GAINRANGE> getGainRange(Amplifier.GainRange gainRange) throws NeDesignerException {
        switch (gainRange) {
            case LOW_GAIN_RANGE:
                return LOWGAINRANGE.class;
            case MID_GAIN_RANGE:
                return MIDGAINRANGE.class;
            case HIGH_GAIN_RANGE:
                return HIGHGAINRANGE.class;
            case FIXED_GAIN_RANGE:
                return FIXEDGAINRANGE.class;
            default:
                throw new NeDesignerException("Unsupported gainRange:" + gainRange);
        }
    }

    public AmpMode getAmpMode(Amplifier.AmpMode ampMode) throws NeDesignerException {
        return parseEnum(ampMode.value(), AmpMode.class);
    }

    public Properties getProperties(List<Param> params) {
        List<Property> propertyList = new ArrayList<>();
        for (Param param : params) {
            propertyList.add(new PropertyBuilder().setName(param.getName()).setValue(param.getValue()).build());
        }
        return new PropertiesBuilder().setProperty(propertyList).build();
    }

   /* public static EquipmentClass getEquipmentType(EquipType equipType) {
        switch (equipType) {
            case Other:
                return EquipmentClass.CHASSIS;
            case CMUX64:
                return EquipmentClass.CMUX;
            default:
                return EquipmentClass.valueOf(equipType.name());
        }
    }

    public static EquipType getEquipType(EquipmentClass equipmentType) {
        switch (equipmentType) {
            case PANEL:
            case CHASSIS:
                return EquipType.Other;
            case CMUX:
                return EquipType.CMUX64;
            default:
                return EquipType.valueOf(equipmentType.name());
        }
    }*/
}
