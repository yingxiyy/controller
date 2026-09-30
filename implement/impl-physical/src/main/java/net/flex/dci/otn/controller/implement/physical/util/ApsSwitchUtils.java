package net.flex.dci.otn.controller.implement.physical.util;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.APS_GROUP_MEMBER_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

/**
 *
 * 2025/9/7
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ApsSwitchUtils {

    public static ApsSwitch compareAndGetApsSwitchConfig(ApsSwitch apsSwitch, Aps currentAps) {
        log.trace("aps configuration set:{} compare and current aps attribute:{}",
                apsSwitch.getAps(), currentAps);
        Aps targetAps = apsSwitch.getAps();
        String apsName = currentAps.getName();
        String apsGroupNum = getApsGroupNum(apsName);
        ApsSwitchBuilder apsSwitchBuilder = new ApsSwitchBuilder();
        ApsBuilder apsBuilder = new ApsBuilder();
        boolean hasChanges = false;
        //compare revertive
        if (targetAps.isRevertive() != null && !Objects.equals(targetAps.isRevertive(),
                currentAps.isRevertive())) {
            apsBuilder.setRevertive(targetAps.isRevertive());
            hasChanges = true;
            log.debug("Revertive changed: {} -> {}", currentAps.isRevertive(),
                    targetAps.isRevertive());
        }

        // 比较 wait-to-restore-time 属性
        if (targetAps.getWaitToRestoreTime() != null && !Objects.equals(
                targetAps.getWaitToRestoreTime(), currentAps.getWaitToRestoreTime())) {
            apsBuilder.setWaitToRestoreTime(targetAps.getWaitToRestoreTime());
            hasChanges = true;
            log.debug("WaitToRestoreTime changed: {} -> {}",
                    currentAps.getWaitToRestoreTime(), targetAps.getWaitToRestoreTime());
        }

        if (targetAps.getApsMode() != null && !Objects.equals(targetAps.getApsMode(),
                currentAps.getApsMode())) {
            apsBuilder.setApsMode(targetAps.getApsMode());
            hasChanges = true;
            log.debug("ApsMode changed: {} -> {}",
                    currentAps.getApsMode(), targetAps.getApsMode());
        }

        if (targetAps.getHoldOffTime() != null && !Objects.equals(targetAps.getHoldOffTime(),
                currentAps.getHoldOffTime())) {
            apsBuilder.setHoldOffTime(targetAps.getHoldOffTime());
            hasChanges = true;
            log.debug("HoldOffTime changed: {} -> {}",
                    currentAps.getHoldOffTime(), targetAps.getHoldOffTime());
        }

        List<Property> changedProperties = getChangedProperties(targetAps.getProperties(),
                apsGroupNum,
                currentAps.getProperties());
        // 比较 properties 属性
        if (!changedProperties.isEmpty()) {
            PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
            apsBuilder.setProperties(propertiesBuilder.setProperty(changedProperties).build());
            hasChanges = true;
            log.debug("Properties changed");
        }

        if (hasChanges) {
            ApsSwitch result = apsSwitchBuilder.setAps(apsBuilder.build()).build();
            log.debug("APS configuration changes detected: {}", result);
            return result;
        } else {
            log.debug("No APS configuration changes detected");
            return null;
        }
    }

    /**
     * get aps GroupNum
     *
     * @param apsName
     * @return
     */
    public static String getApsGroupNum(String apsName) {
        int lastDash = apsName.lastIndexOf('-');
        return (lastDash == -1) ? "" : apsName.substring(lastDash + 1);
    }

    /**
     * detected the properties change
     *
     * @param targetProps
     * @param apsGroupNum
     * @param currentProps
     * @return
     */

    private static List<Property> getChangedProperties(Properties targetProps,
            String apsGroupNum, Properties currentProps) {
        List<Property> changed = new ArrayList<>();
        if (targetProps == null && currentProps == null) {
            return changed;
        }

        if (targetProps == null || currentProps == null) {
            return changed;
        }

        List<Property> targetProperties = targetProps.getProperty();
        List<Property> currentProperties = currentProps.getProperty();

        if (targetProperties == null && currentProperties == null) {
            return changed;
        }

        if (targetProperties == null) {
            return changed;
        }
        if (currentProperties == null) {
            changed.addAll(targetProperties);
            return changed;
        }

        Map<String, String> currentPropsMap = new HashMap<>();
        for (Property prop : currentProperties) {
            currentPropsMap.put(prop.getName(), prop.getValue());
        }

        for (Property targetProp : targetProperties) {
            String propertyName = targetProp.getName();
            String propertyValue = targetProp.getValue();
            if (isAspGroupPortProperty(propertyName)) {
                propertyName = apsGroupNum + propertyName;
                targetProp = new PropertyBuilder().setName(propertyName).setValue(propertyValue)
                        .build();
            }

            String currentValue = currentPropsMap.get(targetProp.getName());
            if (!Objects.equals(targetProp.getValue(), currentValue)) {
                changed.add(targetProp);
            }
        }

        return changed;
    }

    private static boolean isAspGroupPortProperty(String propertyName) {
        boolean isMember = APS_GROUP_MEMBER_PREFIX.stream()
                .anyMatch(propertyName::startsWith);
        return isMember;
    }

    public static String getApsProperty(CrossConnections apsCrossConnection, String key) {
        String value = PropertyTool.getValue(apsCrossConnection.getAps().getProperties(),
                key);
        return value;
    }

}
