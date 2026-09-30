package net.flex.dci.otn.controller.implement.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps.ApsMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.springframework.util.CollectionUtils;

/**
 *
 * 2025/9/6
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApsSwitchConfig implements Serializable {

    private ApsConfig aps;

    public static ApsSwitchConfig parseApsSwitch(ApsSwitch apsSwitch) {
        Aps aps = apsSwitch.getAps();
        PropertyList properties = convertProperty(aps.getProperties());
        ApsConfig apsConfig = ApsConfig.builder()
                .apsMode(aps.getApsMode())
                .holdOffTime(aps.getHoldOffTime())
                .waitToRestoreTime(aps.getWaitToRestoreTime())
                .revertive(aps.isRevertive())
                .properties(properties)
                .build();
        return ApsSwitchConfig.builder().aps(apsConfig).build();
    }

    private static PropertyList convertProperty(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties properties) {
        if (properties == null) {
            return null;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property> propertyList = properties.getProperty();
        if (CollectionUtils.isEmpty(propertyList)) {
            return null;
        }
        List<Property> property = propertyList.stream()
                .map(pro -> Property.builder().name(pro.getName()).value(pro.getValue()).build())
                .collect(Collectors.toList());
        return PropertyList.builder().property(property).build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApsConfig implements Serializable {

        private ApsPath activePath;
        private ApsMode apsMode;
        private ApsPath forceToPort;
        private Long holdOffTime;
        private String name;
        private PropertyList properties;
        private Long waitToRestoreTime;
        private Boolean revertive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PropertyList implements Serializable {

        private List<Property> property;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Property implements Serializable {

        private String name;
        private Object value;
    }
}


