package net.flex.dci.otn.controller.nms.properties.scan;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 * @version 1.0
 * @date 11/27/2023 4:38 PM
 */
@Data
public class TelecomScanPortConfiguration implements Serializable {

    @JSONField(name = "support-business-card-type")
    private EquipType[] supportBusinessCardType;

    @JSONField(name = "support-scan-port-type")
    private PortType[] supportScanPortType;

    @JSONField(name = "scan-port-type")
    private PortType[] scanPortType;

    @JSONField(name = "scan-card-type")
    private EquipType[] supportScanCardType;


    @JSONField(name = "ocm")
    private Map<String, Map<String, String>> ocmConfig;

    @JSONField(name = "otdr")
    private Map<EquipType, OTDRScanConfiguration> otdrScanConfiguration;
}
