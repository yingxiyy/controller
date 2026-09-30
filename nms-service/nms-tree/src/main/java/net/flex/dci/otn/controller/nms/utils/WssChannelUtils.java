package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.ASE_CONTROL_MODE;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.ASE_INJECTION_HYSTERESIS;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.ASE_INJECTION_THRESHOLD;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.AUTO_CONTROL_ACTIVE_THRESHOLD_DEST;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.AUTO_CONTROL_ACTIVE_THRESHOLD_SOURCE;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.CHANNEL_INDEX;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.DEST_CALIBRATION_OPTICAL_POWER;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.DEST_TO_SOURCE_POWER_CONTROL_MODE;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.SOURCE_CALIBRATION_OPTICAL_POWER;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.SOURCE_TO_DEST_POWER_CONTROL_MODE;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.TARGET_DEST_PORT_OUTPUT_OPTICAL_POWER;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_PROPERTIES.TARGET_SOURCE_PORT_OUTPUT_OPTICAL_POWER;

import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssASEControl;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssChannelControl;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssPowerControl;
import net.flex.dci.otn.controller.nms.nms.enums.CustomAseControlMode;
import net.flex.dci.otn.controller.nms.nms.enums.CustomPowerControlMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 7/7/2025 3:08 PM
 */
@Slf4j
public class WssChannelUtils {

    public static WssChannelControl parseWssChannelControl(List<Property> properties) {
        log.debug("parse wss channel control config from properties:{}", properties);
        if (CollectionUtils.isEmpty(properties)) {
            return null;
        }
        WssChannelControl wssChannelControl = WssChannelControl.builder().build();
        WssPowerControl azPowerControl = WssPowerControl.builder().build();
        WssPowerControl zaPowerControl = WssPowerControl.builder().build();
        WssASEControl aseControl = WssASEControl.builder().build();

        for (Property property : properties) {
            String key = property.getKey().getName();
            String value = property.getValue();
            switch (key) {
                case CHANNEL_INDEX:
                    wssChannelControl.setChannelIndex(property.getValue());
                    break;
                case ASE_CONTROL_MODE:
                    if (StringUtils.hasText(value)) {
                        CustomAseControlMode customAseControlMode = CustomAseControlMode.fromName(
                                value);
                        aseControl.setAseControlMode(customAseControlMode);
                    }
                    break;
                case ASE_INJECTION_THRESHOLD:
                    BigDecimal threshold = BigDecimal.valueOf(Double.parseDouble(value));
                    aseControl.setInjectionThreshold(threshold);
                    break;
                case ASE_INJECTION_HYSTERESIS:
                    BigDecimal hysteresis = BigDecimal.valueOf(Double.parseDouble(value));
                    aseControl.setInjectionHysteresis(hysteresis);
                    break;
                case SOURCE_TO_DEST_POWER_CONTROL_MODE:
                    CustomPowerControlMode azControlMode = CustomPowerControlMode.fromName(value);
                    azPowerControl.setMode(azControlMode);
                    break;
                case DEST_TO_SOURCE_POWER_CONTROL_MODE:
                    CustomPowerControlMode zaControlMode = CustomPowerControlMode.fromName(value);
                    zaPowerControl.setMode(zaControlMode);
                    break;
                case TARGET_DEST_PORT_OUTPUT_OPTICAL_POWER:
                    BigDecimal targetDestOpticalPower = BigDecimal.valueOf(
                            Double.parseDouble(value));
                    zaPowerControl.setTargetPower(targetDestOpticalPower);
                    break;
                case TARGET_SOURCE_PORT_OUTPUT_OPTICAL_POWER:
                    BigDecimal targetSourceOpticalPower = BigDecimal.valueOf(
                            Double.parseDouble(value));
                    azPowerControl.setTargetPower(targetSourceOpticalPower);
                    break;
                case AUTO_CONTROL_ACTIVE_THRESHOLD_DEST:
                    BigDecimal activeThresholdDest = BigDecimal.valueOf(Double.parseDouble(value));
                    zaPowerControl.setActivationThreshold(activeThresholdDest);
                    break;
                case AUTO_CONTROL_ACTIVE_THRESHOLD_SOURCE:
                    BigDecimal activeThresholdSource = BigDecimal.valueOf(
                            Double.parseDouble(value));
                    azPowerControl.setActivationThreshold(activeThresholdSource);
                    break;
                case SOURCE_CALIBRATION_OPTICAL_POWER:
                    BigDecimal sourceCalibrationPower = BigDecimal.valueOf(
                            Double.parseDouble(value));
                    azPowerControl.setCalibrationPower(sourceCalibrationPower);
                    break;
                case DEST_CALIBRATION_OPTICAL_POWER:
                    BigDecimal destCalibrationPower = BigDecimal.valueOf(
                            Double.parseDouble(value));
                    zaPowerControl.setCalibrationPower(destCalibrationPower);
                    break;
            }
        }
        wssChannelControl.setASEControl(aseControl);
        wssChannelControl.setAzPowerControl(azPowerControl);
        wssChannelControl.setZaPowerControl(zaPowerControl);
        return wssChannelControl;
    }
}
